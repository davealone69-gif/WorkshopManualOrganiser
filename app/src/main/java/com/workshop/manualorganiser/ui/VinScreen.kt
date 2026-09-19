package com.workshop.manualorganiser.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.workshop.manualorganiser.R
import com.workshop.manualorganiser.util.VinDecoder

@Composable
fun VinScreen(
    viewModel: WorkshopViewModel,
    onBack: () -> Unit,
    onOpenDestination: (Destination) -> Unit,
) {
    val manuals by viewModel.manuals.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<VinDecoder.Result?>(null) }
    var attachTarget by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxWidth()) {
        AppTopBar(
            title = stringResource(R.string.vin_title),
            onBack = onBack,
            onOpenDestination = onOpenDestination,
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(17)
                    result = null
                },
                label = { Text(stringResource(R.string.vin_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { result = viewModel.decodeVin(input) }) {
                    Text(stringResource(R.string.vin_decode))
                }
                TextButton(onClick = {
                    input = ""
                    result = null
                }) { Text(stringResource(R.string.vin_clear)) }
            }

            Text(
                stringResource(R.string.vin_examples),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VinDecoder.EXAMPLES.forEach { example ->
                    AssistChip(
                        onClick = {
                            input = example
                            result = viewModel.decodeVin(example)
                        },
                        label = { Text(example) },
                    )
                }
            }

            result?.let { decoded ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            decoded.vin.ifBlank { "—" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        when {
                            decoded.errors.contains(VinDecoder.VinError.WRONG_LENGTH) ->
                                Text(
                                    stringResource(R.string.vin_error_length, decoded.vin.length),
                                    color = MaterialTheme.colorScheme.error,
                                )

                            decoded.errors.contains(VinDecoder.VinError.ILLEGAL_CHARACTER) ->
                                Text(
                                    stringResource(R.string.vin_error_chars),
                                    color = MaterialTheme.colorScheme.error,
                                )
                        }

                        if (decoded.vin.length == 17) {
                            DetailRow(stringResource(R.string.vin_region), decoded.region)
                            DetailRow(stringResource(R.string.vin_country), decoded.country)
                            DetailRow(stringResource(R.string.vin_manufacturer), decoded.manufacturer)
                            DetailRow(
                                stringResource(R.string.vin_year),
                                decoded.modelYear?.toString() ?: stringResource(R.string.vin_unknown),
                            )
                            DetailRow(stringResource(R.string.vin_plant), decoded.plantCode)
                            DetailRow(stringResource(R.string.vin_serial), decoded.serial)
                            DetailRow(
                                stringResource(R.string.vin_checksum),
                                when (decoded.checksumStatus) {
                                    VinDecoder.Checksum.VALID -> stringResource(R.string.vin_checksum_ok)
                                    VinDecoder.Checksum.INVALID -> stringResource(R.string.vin_checksum_bad)
                                    VinDecoder.Checksum.NOT_APPLICABLE -> stringResource(R.string.vin_checksum_na)
                                },
                            )
                        }

                        decoded.notes.forEach { note ->
                            Text(
                                "• $note",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (manuals.isNotEmpty() && decoded.vin.length == 17) {
                    Text(
                        stringResource(R.string.vin_attach),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        manuals.forEach { manual ->
                            FilterChip(
                                selected = attachTarget == manual.id,
                                onClick = { attachTarget = manual.id },
                                label = { Text(manual.title.take(24)) },
                            )
                        }
                    }
                    Button(
                        enabled = attachTarget != null,
                        onClick = {
                            attachTarget?.let { viewModel.attachVin(it, decoded.vin) }
                        },
                    ) { Text(stringResource(R.string.vin_attach)) }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
