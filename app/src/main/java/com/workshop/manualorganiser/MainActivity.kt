package com.workshop.manualorganiser

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.workshop.manualorganiser.ui.theme.AppTheme
import java.util.Locale

private data class Manual(
    val id: String,
    val title: String,
    val category: String,
    val notes: String = "",
    val sourceUri: String? = null
)

class MainActivity : androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize()) { WorkshopApp() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkshopApp() {
    var manuals by remember {
        mutableStateOf(
            listOf(
                Manual("1", "Toyota 2JZ-GTE Workshop", "Mechanical", "Engine rebuild"),
                Manual("2", "BMW E46 Electrical", "Electrical", "Wiring diagrams"),
                Manual("3", "Safety Procedures 2024", "Safety", "Shop rules")
            )
        )
    }
    var query by rememberSaveable { mutableStateOf("") }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showVin by rememberSaveable { mutableStateOf(false) }
    var showWiring by rememberSaveable { mutableStateOf(false) }
    var showAiResult by rememberSaveable { mutableStateOf(false) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    var vin by rememberSaveable { mutableStateOf("") }
    var aiSummary by rememberSaveable { mutableStateOf("") }
    var newTitle by rememberSaveable { mutableStateOf("") }
    var newCategory by rememberSaveable { mutableStateOf("Mechanical") }

    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            manuals = manuals + Manual(
                id = "upload-${System.currentTimeMillis()}",
                title = uri.lastPathSegment?.substringAfterLast('/') ?: "Uploaded Manual",
                category = "Imported",
                notes = "Document imported from device",
                sourceUri = uri.toString()
            )
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            manuals = manuals + Manual(
                id = "scan-${System.currentTimeMillis()}",
                title = "Scanned Manual Page",
                category = "Scanned",
                notes = "Captured from camera. Add details from the manual card."
            )
        }
    }

    val wiringLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            manuals = manuals + Manual(
                id = "wiring-${System.currentTimeMillis()}",
                title = uri.lastPathSegment?.substringAfterLast('/') ?: "Wiring Diagram",
                category = "Wiring Diagram",
                notes = "Imported wiring diagram",
                sourceUri = uri.toString()
            )
        }
    }

    val filtered = remember(manuals, query) {
        if (query.isBlank()) manuals else manuals.filter {
            it.title.contains(query, true) || it.category.contains(query, true) || it.notes.contains(query, true)
        }
    }

    fun runAiSort() {
        manuals = manuals.map { manual ->
            val text = "${manual.title} ${manual.notes}".lowercase(Locale.ROOT)
            val category = when {
                "wiring" in text || "wire" in text || "electrical" in text || "ecu" in text || "sensor" in text -> "Electrical"
                "brake" in text || "suspension" in text || "engine" in text || "transmission" in text -> "Mechanical"
                "safety" in text || "hazard" in text -> "Safety"
                else -> manual.category
            }
            manual.copy(category = category)
        }
        aiSummary = "AI Sort & Structure completed. ${manuals.size} manual records were classified using local rules. No cloud upload required."
        showAiResult = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Workshop Manual Organiser", fontWeight = FontWeight.Bold)
                        Text("${manuals.size} manuals • v2.0.0", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMore = true }) { Icon(Icons.Default.MoreVert, "More tools") }
                        DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                            DropdownMenuItem(text = { Text("Scan Manual") }, leadingIcon = { Icon(Icons.Default.CameraAlt, null) }, onClick = { showMore = false; scanLauncher.launch(null) })
                            DropdownMenuItem(text = { Text("Upload") }, leadingIcon = { Icon(Icons.Default.UploadFile, null) }, onClick = { showMore = false; uploadLauncher.launch(arrayOf("application/pdf", "image/*", "text/plain")) })
                            DropdownMenuItem(text = { Text("VIN Decoder") }, leadingIcon = { Icon(Icons.Default.DirectionsCar, null) }, onClick = { showMore = false; showVin = true })
                            DropdownMenuItem(text = { Text("Wiring Diagrams") }, leadingIcon = { Icon(Icons.Default.ElectricalServices, null) }, onClick = { showMore = false; showWiring = true })
                            DropdownMenuItem(text = { Text("AI Sort & Structure") }, leadingIcon = { Icon(Icons.Default.Build, null) }, onClick = { showMore = false; runAiSort() })
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add Manual") })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search title, category or notes…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                ToolButton("Scan", Icons.Default.CameraAlt) { scanLauncher.launch(null) }
                ToolButton("Upload", Icons.Default.UploadFile) { uploadLauncher.launch(arrayOf("application/pdf", "image/*", "text/plain")) }
                ToolButton("VIN", Icons.Default.DirectionsCar) { showVin = true }
                ToolButton("Wiring", Icons.Default.ElectricalServices) { showWiring = true }
            }
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Description, null, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(if (manuals.isEmpty()) "No manuals yet" else "No matches")
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.id }) { manual ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(manual.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(manual.category, style = MaterialTheme.typography.labelMedium)
                                    if (manual.notes.isNotBlank()) Text(manual.notes, style = MaterialTheme.typography.bodySmall)
                                    if (manual.sourceUri != null) Text("Stored source attached", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { manuals = manuals.filterNot { it.id == manual.id } }) { Icon(Icons.Default.Delete, "Delete") }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add Manual") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(newTitle, { newTitle = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(newCategory, { newCategory = it }, label = { Text("Category") }, singleLine = true)
            } },
            confirmButton = { TextButton(onClick = {
                if (newTitle.isNotBlank()) {
                    manuals = manuals + Manual(System.currentTimeMillis().toString(), newTitle, newCategory)
                    newTitle = ""
                    showAdd = false
                }
            }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }

    if (showVin) {
        val cleanVin = vin.trim().uppercase(Locale.ROOT)
        val validFormat = cleanVin.length == 17 && cleanVin.all { it in 'A'..'Z' || it in '0'..'9' } && cleanVin.none { it in "IOQ" }
        AlertDialog(
            onDismissRequest = { showVin = false },
            title = { Text("VIN Decoder") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(vin, { vin = it.take(17) }, label = { Text("17-character VIN") }, singleLine = true)
                Text(when {
                    vin.isBlank() -> "Enter a VIN to validate its structure."
                    validFormat -> "VIN format looks valid. Position 10: ${cleanVin[9]}. Position 11: ${cleanVin[10]}."
                    else -> "VIN format is invalid. Use 17 characters and exclude I, O and Q."
                })
                Text("Offline decoder validates the VIN structure without sending it to a service.", style = MaterialTheme.typography.bodySmall)
            } },
            confirmButton = { TextButton(onClick = { showVin = false }) { Text("Done") } }
        )
    }

    if (showWiring) {
        AlertDialog(
            onDismissRequest = { showWiring = false },
            title = { Text("Wiring Diagrams") },
            text = { Text("Import a PDF or image wiring diagram. It is added to the manual library as a Wiring Diagram record.") },
            confirmButton = { TextButton(onClick = { showWiring = false; wiringLauncher.launch(arrayOf("application/pdf", "image/*")) }) { Text("Import Diagram") } },
            dismissButton = { TextButton(onClick = { showWiring = false }) { Text("Cancel") } }
        )
    }

    if (showAiResult) {
        AlertDialog(onDismissRequest = { showAiResult = false }, title = { Text("AI Sort & Structure") }, text = { Text(aiSummary) }, confirmButton = { TextButton(onClick = { showAiResult = false }) { Text("Done") } })
    }
}

@Composable
private fun ToolButton(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = 2.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(22.dp))
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}
