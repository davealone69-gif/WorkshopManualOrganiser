package com.workshop.manualorganiser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.workshop.manualorganiser.R
import com.workshop.manualorganiser.util.WiringData

@Composable
fun WiringScreen(
    onBack: () -> Unit,
    onOpenDestination: (Destination) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }

    val (systems, pinouts) = remember(query) { WiringData.search(query) }

    Column(Modifier.fillMaxWidth()) {
        AppTopBar(
            title = stringResource(R.string.wiring_title),
            subtitle = stringResource(R.string.wiring_tap_to_zoom),
            onBack = onBack,
            onOpenDestination = onOpenDestination,
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.wiring_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.wiring_systems, systems.size)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.wiring_pinouts, pinouts.size)) })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Reference") })
        }

        when (tab) {
            0 -> LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(systems, key = { it.id }) { system ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(system.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(system.summary, style = MaterialTheme.typography.bodyMedium)
                            system.checks.forEach { check ->
                                Text(
                                    "• $check",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            1 -> LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(pinouts, key = { it.id }) { pinout ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(pinout.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(pinout.description, style = MaterialTheme.typography.bodySmall)
                            pinout.pins.forEach { (pin, function) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        pin,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                    Text(function, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            else -> LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Wire colour codes (DIN)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            WiringData.colours.forEach { colour ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(colour.code, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("${colour.colour} (${colour.german})", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("DIN 72552 terminals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            WiringData.terminals.forEach { (terminal, meaning) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(terminal, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(meaning, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
