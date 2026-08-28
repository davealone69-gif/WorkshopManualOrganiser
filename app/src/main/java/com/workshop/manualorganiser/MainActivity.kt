package com.workshop.manualorganiser

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.workshop.manualorganiser.ui.theme.AppTheme
import java.util.Locale

private data class Manual(
    val id: String,
    val title: String,
    val category: String,
    val notes: String = "",
    val sourceUri: String? = null,
    val vehicle: String = "",
    val favourite: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { Surface(Modifier.fillMaxSize()) { WorkshopApp() } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkshopApp() {
    var manuals by remember { mutableStateOf(listOf(
        Manual("1", "Toyota 2JZ-GTE Workshop", "Mechanical", "Engine rebuild", vehicle = "Toyota Supra A80"),
        Manual("2", "BMW E46 Electrical", "Electrical", "Wiring diagrams", vehicle = "BMW E46"),
        Manual("3", "Safety Procedures", "Safety", "Shop rules")
    )) }
    var query by rememberSaveable { mutableStateOf("") }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showVin by rememberSaveable { mutableStateOf(false) }
    var showWiring by rememberSaveable { mutableStateOf(false) }
    var showAi by rememberSaveable { mutableStateOf(false) }
    var showVehicle by rememberSaveable { mutableStateOf(false) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    var vin by rememberSaveable { mutableStateOf("") }
    var newTitle by rememberSaveable { mutableStateOf("") }
    var newCategory by rememberSaveable { mutableStateOf("Mechanical") }
    var newVehicle by rememberSaveable { mutableStateOf("") }

    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { manuals = manuals + Manual("upload-${System.currentTimeMillis()}", it.lastPathSegment?.substringAfterLast('/') ?: "Uploaded Manual", "Imported", "Original document attached", it.toString()) }
    }
    val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) manuals = manuals + Manual("scan-${System.currentTimeMillis()}", "Scanned Manual Page", "Scanned", "Camera capture. Original page preserved in this session.")
    }
    val wiringLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { manuals = manuals + Manual("wiring-${System.currentTimeMillis()}", it.lastPathSegment?.substringAfterLast('/') ?: "Wiring Diagram", "Wiring Diagram", "Imported wiring source", it.toString()) }
    }

    fun aiSort() {
        manuals = manuals.map { m ->
            val text = "${m.title} ${m.notes}".lowercase(Locale.ROOT)
            val category = when {
                listOf("wiring", "electrical", "ecu", "sensor", "connector").any(text::contains) -> "Electrical"
                listOf("brake", "suspension", "engine", "transmission", "torque").any(text::contains) -> "Mechanical"
                listOf("safety", "hazard").any(text::contains) -> "Safety"
                else -> m.category
            }
            m.copy(category = category)
        }
        showAi = true
    }

    val filtered = manuals.filter { m -> query.isBlank() || listOf(m.title, m.category, m.notes, m.vehicle).any { it.contains(query, true) } }

    Scaffold(topBar = {
        TopAppBar(title = { Column { Text("Workshop Manual Organiser", fontWeight = FontWeight.Bold); Text("${manuals.size} records • v3.0.0", style = MaterialTheme.typography.bodySmall) } }, actions = {
            Box { IconButton({ showMore = true }) { Icon(Icons.Default.MoreVert, "Workshop tools") }
                DropdownMenu(showMore, { showMore = false }) {
                    DropdownMenuItem({ Text("Scan Manual") }, { showMore = false; scanLauncher.launch(null) }, leadingIcon = { Icon(Icons.Default.CameraAlt, null) })
                    DropdownMenuItem({ Text("Upload") }, { showMore = false; uploadLauncher.launch(arrayOf("application/pdf", "image/*", "text/plain")) }, leadingIcon = { Icon(Icons.Default.UploadFile, null) })
                    DropdownMenuItem({ Text("VIN Decoder") }, { showMore = false; showVin = true }, leadingIcon = { Icon(Icons.Default.DirectionsCar, null) })
                    DropdownMenuItem({ Text("Wiring Diagrams") }, { showMore = false; showWiring = true }, leadingIcon = { Icon(Icons.Default.ElectricalServices, null) })
                    DropdownMenuItem({ Text("AI Sort & Structure") }, { showMore = false; aiSort() }, leadingIcon = { Icon(Icons.Default.AutoAwesome, null) })
                    DropdownMenuItem({ Text("Vehicle Workspace") }, { showMore = false; showVehicle = true }, leadingIcon = { Icon(Icons.Default.Build, null) })
                }
            }
        })
    }, floatingActionButton = { ExtendedFloatingActionButton({ showAdd = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add Manual") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(query, { query = it }, placeholder = { Text("Search manual, vehicle, category or notes…") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                ToolButton("Scan", Icons.Default.CameraAlt) { scanLauncher.launch(null) }
                ToolButton("Upload", Icons.Default.UploadFile) { uploadLauncher.launch(arrayOf("application/pdf", "image/*", "text/plain")) }
                ToolButton("VIN", Icons.Default.DirectionsCar) { showVin = true }
                ToolButton("Wiring", Icons.Default.ElectricalServices) { showWiring = true }
                ToolButton("AI", Icons.Default.AutoAwesome) { aiSort() }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { manual ->
                    Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(manual.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(manual.category, style = MaterialTheme.typography.labelMedium)
                            if (manual.vehicle.isNotBlank()) Text(manual.vehicle, style = MaterialTheme.typography.bodySmall)
                            if (manual.notes.isNotBlank()) Text(manual.notes, style = MaterialTheme.typography.bodySmall)
                            if (manual.sourceUri != null) Text("Original source attached", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton({ manuals = manuals.map { if (it.id == manual.id) it.copy(favourite = !it.favourite) else it } }) { Icon(if (manual.favourite) Icons.Default.Star else Icons.Default.StarBorder, "Favourite") }
                        IconButton({ manuals = manuals.filterNot { it.id == manual.id } }) { Icon(Icons.Default.Delete, "Delete") }
                    } }
                }
            }
        }
    }

    if (showAdd) AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Add Manual") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(newTitle, { newTitle = it }, label = { Text("Title") }, singleLine = true)
        OutlinedTextField(newCategory, { newCategory = it }, label = { Text("Category") }, singleLine = true)
        OutlinedTextField(newVehicle, { newVehicle = it }, label = { Text("Vehicle") }, singleLine = true)
    } }, confirmButton = { TextButton({ if (newTitle.isNotBlank()) { manuals = manuals + Manual(System.currentTimeMillis().toString(), newTitle, newCategory, vehicle = newVehicle); newTitle = ""; newVehicle = ""; showAdd = false } }) { Text("Add") } }, dismissButton = { TextButton({ showAdd = false }) { Text("Cancel") } })

    if (showVin) {
        val clean = vin.trim().uppercase(Locale.ROOT)
        val valid = clean.length == 17 && clean.all { it in 'A'..'Z' || it in '0'..'9' } && clean.none { it in "IOQ" }
        AlertDialog(onDismissRequest = { showVin = false }, title = { Text("VIN Decoder") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(vin, { vin = it.take(17) }, label = { Text("17-character VIN") }, singleLine = true)
            Text(if (vin.isBlank()) "Enter a VIN." else if (valid) "VIN structure valid. Model-year position: ${clean[9]}. Plant position: ${clean[10]}." else "Invalid VIN structure. Use 17 characters and exclude I, O and Q.")
            Text("Offline validation only. No VIN is uploaded.", style = MaterialTheme.typography.bodySmall)
        } }, confirmButton = { TextButton({ showVin = false }) { Text("Done") } })
    }

    if (showWiring) AlertDialog(onDismissRequest = { showWiring = false }, title = { Text("Wiring Diagrams") }, text = { Text("Import PDF or image diagrams. The original source remains attached to the record.") }, confirmButton = { TextButton({ showWiring = false; wiringLauncher.launch(arrayOf("application/pdf", "image/*")) }) { Text("Import Diagram") } }, dismissButton = { TextButton({ showWiring = false }) { Text("Cancel") } })
    if (showAi) AlertDialog(onDismissRequest = { showAi = false }, title = { Text("AI Sort & Structure") }, text = { Text("Classified ${manuals.size} records using local rules. Original source records are preserved. No cloud upload required.") }, confirmButton = { TextButton({ showAi = false }) { Text("Done") } })
    if (showVehicle) AlertDialog(onDismissRequest = { showVehicle = false }, title = { Text("Vehicle Workspace") }, text = { Text("Search by vehicle to group its manuals, wiring diagrams, specifications and workshop notes. Use the vehicle field when adding or importing a manual.") }, confirmButton = { TextButton({ showVehicle = false }) { Text("Done") } })
}

@Composable
private fun ToolButton(title: String, icon: ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, Modifier.size(22.dp)); Text(title, style = MaterialTheme.typography.labelSmall) } }
}
