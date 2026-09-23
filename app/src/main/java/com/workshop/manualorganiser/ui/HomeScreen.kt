package com.workshop.manualorganiser.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.workshop.manualorganiser.R
import com.workshop.manualorganiser.data.Manual
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: WorkshopViewModel,
    onOpen: (String) -> Unit,
    onEditStructure: (String) -> Unit,
    onScanManual: () -> Unit,
    onUpload: () -> Unit,
    onOpenDestination: (Destination) -> Unit,
) {
    val manuals by viewModel.manuals.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<Manual?>(null) }
    var confirmDelete by remember { mutableStateOf<Manual?>(null) }

    val createExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        val manual = pendingExport
        pendingExport = null
        if (uri != null && manual != null) viewModel.exportManual(manual, uri)
    }

    val filtered = remember(manuals, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            manuals
        } else {
            manuals.filter { manual ->
                manual.title.lowercase().contains(q) ||
                    manual.category.lowercase().contains(q) ||
                    manual.notes.lowercase().contains(q) ||
                    manual.vin.lowercase().contains(q)
            }
        }
    }

    val totalPages = manuals.sumOf { it.pageCount }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.home_count, manuals.size, totalPages),
                onScanManual = onScanManual,
                onUpload = onUpload,
                onOpenDestination = onOpenDestination,
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.home_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (!loaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(
                                if (manuals.isEmpty()) R.string.home_empty else R.string.home_no_matches,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.id }) { manual ->
                        ManualCard(
                            manual = manual,
                            menuOpen = menuFor == manual.id,
                            onOpen = { onOpen(manual.id) },
                            onRequestMenu = { menuFor = manual.id },
                            onDismissMenu = { menuFor = null },
                            onEditStructure = { menuFor = null; onEditStructure(manual.id) },
                            onExport = {
                                menuFor = null
                                pendingExport = manual
                                createExport.launch("${safeExportName(manual.title)}.zip")
                            },
                            onDelete = { menuFor = null; confirmDelete = manual },
                        )
                    }
                    item { Spacer(Modifier.height(84.dp)) }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text(stringResource(R.string.home_add)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }

    if (showAdd) {
        AddManualDialog(
            viewModel = viewModel,
            onDismiss = { showAdd = false },
            onCreated = { created ->
                showAdd = false
                onOpen(created.id)
            },
        )
    }

    confirmDelete?.let { manual ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_body, manual.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteManual(manual.id)
                    confirmDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ManualCard(
    manual: Manual,
    menuOpen: Boolean,
    onOpen: () -> Unit,
    onRequestMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onEditStructure: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onOpen, onLongClick = onRequestMenu),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val thumbnail = manual.pages.firstOrNull()?.uri
                if (thumbnail != null) {
                    AsyncImage(
                        model = File(thumbnail),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(manual.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(manual.category, style = MaterialTheme.typography.bodySmall)
                    Text(
                        buildString {
                            append("${manual.pageCount} page(s)")
                            if (manual.vin.isNotBlank()) append(" • VIN ${manual.vin}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (manual.notes.isNotBlank()) {
                        Text(
                            manual.notes,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(onClick = onRequestMenu) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.ctx_edit_structure))
                }
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ctx_open)) },
                leadingIcon = { Icon(Icons.Default.FileOpen, null) },
                onClick = { onDismissMenu(); onOpen() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ctx_edit_structure)) },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = onEditStructure,
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ctx_export)) },
                leadingIcon = { Icon(Icons.Default.IosShare, null) },
                onClick = onExport,
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ctx_delete)) },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                onClick = onDelete,
            )
        }
    }
}

fun safeExportName(title: String): String =
    title.trim().replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').ifBlank { "manual" }
