package com.workshop.manualorganiser.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.workshop.manualorganiser.R
import com.workshop.manualorganiser.data.Manual
import com.workshop.manualorganiser.data.ManualPage
import com.workshop.manualorganiser.util.MediaImporter
import kotlinx.coroutines.launch
import java.io.File

/**
 * Creates a manual, allowing pages to be captured or imported before it is saved.
 * This is what makes "Scan Manual" and "Upload" real actions rather than toasts.
 */
@Composable
fun AddManualDialog(
    viewModel: WorkshopViewModel,
    onDismiss: () -> Unit,
    onCreated: (Manual) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CategoryDefaults.DEFAULT) }
    var notes by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var pendingPages by remember { mutableStateOf<List<ManualPage>>(emptyList()) }
    var captureFile by remember { mutableStateOf<File?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = captureFile
        captureFile = null
        if (success && file != null) {
            scope.launch {
                viewModel.importCapture(file)?.let { page -> pendingPages = pendingPages + page }
            }
        }
    }

    val pickVisual = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.importImage(uri)?.let { page -> pendingPages = pendingPages + page }
            }
        }
    }

    val pickDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
                val added = if (mime.contains("pdf")) viewModel.importPdf(uri) else listOfNotNull(viewModel.importImage(uri))
                pendingPages = pendingPages + added
            }
        }
    }

    val canSave = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_add)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(stringResource(R.string.field_category), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategoryDefaults.ALL.forEach { option ->
                        FilterChip(
                            selected = option == category,
                            onClick = { category = option },
                            label = { Text(option) },
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.field_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = vin,
                    onValueChange = { vin = it.uppercase().take(17) },
                    label = { Text(stringResource(R.string.field_vin)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            val file = runCatching { MediaImporter.newCaptureFile(context) }.getOrNull()
                            if (file != null) {
                                captureFile = file
                                runCatching { takePicture.launch(uriFor(context, file)) }
                            }
                        },
                        label = { Text(stringResource(R.string.action_add_photo)) },
                        leadingIcon = { Icon(Icons.Default.PhotoCamera, null) },
                    )
                    AssistChip(
                        onClick = {
                            pickVisual.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        label = { Text(stringResource(R.string.action_gallery)) },
                        leadingIcon = { Icon(Icons.Default.PhotoLibrary, null) },
                    )
                }

                AssistChip(
                    onClick = { pickDocument.launch(arrayOf("application/pdf")) },
                    label = { Text(stringResource(R.string.action_import_pdf)) },
                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                )

                Text(
                    stringResource(R.string.detail_pages, pendingPages.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                pendingPages.takeLast(4).forEach { page ->
                    Text(
                        "• ${File(page.uri).name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    viewModel.addManual(
                        title = title,
                        category = category,
                        notes = notes,
                        vin = vin,
                        pages = pendingPages,
                        onCreated = onCreated,
                    )
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
