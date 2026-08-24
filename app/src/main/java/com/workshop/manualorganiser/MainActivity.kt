package com.workshop.manualorganiser

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.workshop.manualorganiser.ui.theme.AppTheme

data class Manual(val id: String, val title: String, val category: String, val notes: String = "")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(Modifier = Modifier.fillMaxSize()) {
                    WorkshopApp(onAbout = { showAbout() })
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_scan -> { Toast.makeText(this, "Scan Manual", Toast.LENGTH_SHORT).show(); true }
        R.id.action_upload -> { Toast.makeText(this, "Upload", Toast.LENGTH_SHORT).show(); true }
        R.id.action_vin_decoder -> { Toast.makeText(this, "VIN Decoder", Toast.LENGTH_SHORT).show(); true }
        R.id.action_wiring -> { Toast.makeText(this, "Wiring Diagrams", Toast.LENGTH_SHORT).show(); true }
        R.id.action_ai_sort -> { Toast.makeText(this, "AI Sort & Structure", Toast.LENGTH_SHORT).show(); true }
        R.id.action_about -> { showAbout(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showAbout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Workshop Manual Organiser")
            .setMessage("Version 1.0.0\n\nAI-powered workshop manual manager\nScan • Upload • VIN Decoder • Wiring • AI Sorting\n\nBuilt by REDRUM Studios")
            .setPositiveButton("OK", null).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkshopApp(onAbout: () -> Unit) {
    var manuals by remember {
        mutableStateOf(
            listOf(
                Manual("1", "Toyota 2JZ-GTE Workshop", "Mechanical", "Engine rebuild"),
                Manual("2", "BMW E46 Electrical", "Electrical", "Wiring diagrams"),
                Manual("3", "Safety Procedures 2024", "Safety", "Shop rules")
            )
        )
    }
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Mechanical") }

    val filtered = remember(manuals, query) {
        if (query.isBlank()) manuals else manuals.filter {
            it.title.contains(query, true) || it.category.contains(query, true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Workshop Manual Organiser", fontWeight = FontWeight.Bold)
                        Text("${manuals.size} manuals • Built by REDRUM Studios", style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Manual") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search title or category…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Build, null, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(if (manuals.isEmpty()) "No manuals yet" else "No matches")
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { manual ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(manual.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(manual.category, style = MaterialTheme.typography.bodySmall)
                                    if (manual.notes.isNotBlank()) Text(manual.notes, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { manuals = manuals.filter { it.id != manual.id } }) {
                                    Icon(Icons.Default.Delete, "Delete")
                                }
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
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = newCategory, onValueChange = { newCategory = it }, label = { Text("Category") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank()) {
                        manuals = manuals + Manual(System.currentTimeMillis().toString(), newTitle, newCategory)
                        newTitle = ""
                        showAdd = false
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
