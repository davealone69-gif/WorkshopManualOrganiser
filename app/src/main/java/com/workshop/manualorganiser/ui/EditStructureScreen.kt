package com.workshop.manualorganiser.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workshop.manualorganiser.R
import com.workshop.manualorganiser.util.Taxonomy
import java.io.File

@Composable
fun EditStructureScreen(
    viewModel: WorkshopViewModel,
    manualId: String,
    onBack: () -> Unit,
    onOpenDestination: (Destination) -> Unit,
) {
    val manuals by viewModel.manuals.collectAsStateWithLifecycle()
    val manual = manuals.firstOrNull { it.id == manualId }

    var title by remember(manual?.id) { mutableStateOf(manual?.title.orEmpty()) }
    var category by remember(manual?.id) { mutableStateOf(manual?.category ?: CategoryDefaults.DEFAULT) }
    var notes by remember(manual?.id) { mutableStateOf(manual?.notes.orEmpty()) }
    var vin by remember(manual?.id) { mutableStateOf(manual?.vin.orEmpty()) }
    var renamingPage by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (manual == null) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                title = stringResource(R.string.structure_title),
                onBack = onBack,
                onOpenDestination = onOpenDestination,
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.home_empty))
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = stringResource(R.string.structure_title),
            subtitle = manual.title,
            onBack = onBack,
            onOpenDestination = onOpenDestination,
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(stringResource(R.string.field_title)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
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
                        Text(
                            stringResource(R.string.field_category),
                            style = MaterialTheme.typography.labelLarge,
                        )
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
                        Button(
                            onClick = {
                                viewModel.updateManual(
                                    manual.copy(
                                        title = title.trim().ifBlank { manual.title },
                                        category = category,
                                        notes = notes,
                                        vin = vin,
                                    ),
                                )
                            },
                        ) { Text(stringResource(R.string.save)) }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.detail_pages, manual.pageCount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (manual.pages.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.detail_no_pages),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            itemsIndexed(manual.pages, key = { _, page -> page.id }) { index, page ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                page.label.ifBlank { stringResource(R.string.detail_page_label, index + 1) },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                File(page.uri).name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            enabled = index > 0,
                            onClick = { viewModel.movePage(manualId, index, index - 1) },
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.structure_move_up))
                        }
                        IconButton(
                            enabled = index < manual.pages.lastIndex,
                            onClick = { viewModel.movePage(manualId, index, index + 1) },
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.structure_move_down))
                        }
                        IconButton(onClick = { renamingPage = page.id to page.label }) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = stringResource(R.string.structure_page_label_hint))
                        }
                        IconButton(onClick = { viewModel.removePage(manualId, page.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.structure_remove_page))
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.applySuggestion(manualId, Taxonomy.suggest(manual))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.structure_autosort)) }
            }
        }
    }

    renamingPage?.let { (pageId, currentLabel) ->
        var draft by remember(pageId) { mutableStateOf(currentLabel) }
        AlertDialog(
            onDismissRequest = { renamingPage = null },
            title = { Text(stringResource(R.string.structure_page_label_hint)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updatePageLabel(manualId, pageId, draft)
                    renamingPage = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { renamingPage = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
