package com.workshop.manualorganiser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workshop.manualorganiser.BuildConfig
import com.workshop.manualorganiser.R

@Composable
fun AboutScreen(
    viewModel: WorkshopViewModel,
    onBack: () -> Unit,
    onOpenDestination: (Destination) -> Unit,
) {
    val manuals by viewModel.manuals.collectAsStateWithLifecycle()
    var showPrivacy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        AppTopBar(
            title = stringResource(R.string.about_title),
            onBack = onBack,
            onOpenDestination = onOpenDestination,
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(
                            R.string.version_line,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    StatRow(stringResource(R.string.about_manuals), manuals.size.toString())
                    StatRow(stringResource(R.string.about_pages), manuals.sumOf { it.pageCount }.toString())
                    StatRow(stringResource(R.string.about_storage), viewModel.storageSummary())
                }
            }

            TextButton(onClick = { showPrivacy = true }) {
                Text(stringResource(R.string.about_privacy))
            }
        }
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text(stringResource(R.string.privacy_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
