package com.workshop.manualorganiser.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.workshop.manualorganiser.R
import com.workshop.manualorganiser.data.ManualPage
import com.workshop.manualorganiser.util.MediaImporter
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManualDetailScreen(
    viewModel: WorkshopViewModel,
    manualId: String,
    onBack: () -> Unit,
    onEditStructure: () -> Unit,
    onScanManual: () -> Unit,
    onUpload: () -> Unit,
    onOpenDestination: (Destination) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manuals by viewModel.manuals.collectAsStateWithLifecycle()
    val manual = manuals.firstOrNull { it.id == manualId }

    var viewing by remember { mutableStateOf<ManualPage?>(null) }
    var captureFile by remember { mutableStateOf<File?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val createExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null && manual != null) viewModel.exportManual(manual, uri)
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = captureFile
        captureFile = null
        if (success && file != null) {
            scope.launch {
                viewModel.importCapture(file)?.let { viewModel.appendPages(manualId, listOf(it)) }
            }
        }
    }

    val pickVisual = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.importImage(uri)?.let { viewModel.appendPages(manualId, listOf(it)) }
            }
        }
    }

    val pickDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
                val pages = if (mime.contains("pdf")) viewModel.importPdf(uri) else listOfNotNull(viewModel.importImage(uri))
                viewModel.appendPages(manualId, pages)
            }
        }
    }

    if (manual == null) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                title = stringResource(R.string.app_name),
                onBack = onBack,
                onScanManual = onScanManual,
                onUpload = onUpload,
                onOpenDestination = onOpenDestination,
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.home_empty))
            }
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = manual.title,
            subtitle = "${manual.category} • ${stringResource(R.string.detail_pages, manual.pageCount)}",
            onBack = onBack,
            onScanManual = onScanManual,
            onUpload = onUpload,
            onOpenDestination = onOpenDestination,
            trailing = {
                IconButton(onClick = onEditStructure) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.ctx_edit_structure))
                }
                IconButton(onClick = {
                    createExport.launch("${safeExportName(manual.title)}.zip")
                }) {
                    Icon(Icons.Default.IosShare, contentDescription = stringResource(R.string.ctx_export))
                }
                IconButton(onClick = {
                    viewModel.shareArchive(manual) { file ->
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(share, null))
                    }
                }) {
                    Icon(Icons.Default.IosShare, contentDescription = stringResource(R.string.action_share))
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            },
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(manual.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (manual.vin.isNotBlank()) {
                            Text("${stringResource(R.string.field_vin)}: ${manual.vin}")
                        }
                        if (manual.notes.isNotBlank()) Text(manual.notes)
                        Text(
                            stringResource(R.string.detail_created, dateFormat.format(Date(manual.createdAt))),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.detail_updated, dateFormat.format(Date(manual.updatedAt))),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                                        ),
                                    )
                                },
                                label = { Text(stringResource(R.string.action_gallery)) },
                                leadingIcon = { Icon(Icons.Default.PhotoLibrary, null) },
                            )
                        }
                        AssistChip(
                            onClick = { pickDocument.launch(arrayOf("application/pdf", "image/*")) },
                            label = { Text(stringResource(R.string.action_import_pdf)) },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                        )
                    }
                }
            }

            if (manual.pages.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.detail_no_pages),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            items(manual.pages, key = { it.id }) { page ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(14.dp)),
                    ) {
                        AsyncImage(
                            model = File(page.uri),
                            contentDescription = page.label.ifBlank { stringResource(R.string.detail_open_page) },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().background(Color(0xFF111827)),
                        )
                    }
                    Text(
                        page.label.ifBlank { File(page.uri).name },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    TextButton(onClick = { viewing = page }) { Text(stringResource(R.string.ctx_open)) }
                }
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                TextButton(onClick = onEditStructure) {
                    Text(stringResource(R.string.ctx_edit_structure))
                }
            }
        }
    }

    viewing?.let { page ->
        AlertDialog(
            onDismissRequest = { viewing = null },
            title = { Text(page.label.ifBlank { File(page.uri).name }) },
            text = { ZoomableImage(File(page.uri)) },
            confirmButton = {
                TextButton(onClick = { viewing = null }) { Text(stringResource(R.string.close)) }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_body, manual.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteManual(manual.id)
                    confirmDelete = false
                    onBack()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Pinch-to-zoom / double-tap-to-reset image viewer. */
@Composable
fun ZoomableImage(file: File, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 8f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    } else {
                        scale = 2.5f
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                ),
        )
    }
}
