package com.workshop.manualorganiser.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.workshop.manualorganiser.util.Taxonomy

@Composable
fun AiSortScreen(
    viewModel: WorkshopViewModel,
    onBack: () -> Unit,
    onOpenDestination: (Destination) -> Unit,
) {
    val manuals by viewModel.manuals.collectAsStateWithLifecycle()
    var selectedId by remember { mutableStateOf<String?>(manuals.firstOrNull()?.id) }
    var suggestion by remember { mutableStateOf<Taxonomy.Suggestion?>(null) }

    val selected = manuals.firstOrNull { it.id == selectedId } ?: manuals.firstOrNull()

    Column(Modifier.fillMaxWidth()) {
        AppTopBar(
            title = stringResource(R.string.aisort_title),
            onBack = onBack,
            onOpenDestination = onOpenDestination,
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.aisort_intro), style = MaterialTheme.typography.bodyMedium)

            if (manuals.isEmpty()) {
                Text(
                    stringResource(R.string.aisort_no_manuals),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(stringResource(R.string.aisort_pick), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    manuals.forEach { manual ->
                        FilterChip(
                            selected = manual.id == selected?.id,
                            onClick = {
                                selectedId = manual.id
                                suggestion = null
                            },
                            label = { Text(manual.title.take(28)) },
                        )
                    }
                }

                Button(
                    enabled = selected != null,
                    onClick = { selected?.let { suggestion = Taxonomy.suggest(it) } },
                ) { Text(stringResource(R.string.aisort_run)) }
            }

            suggestion?.let { result ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.aisort_suggested),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            result.category,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${stringResource(R.string.aisort_confidence)}: ${(result.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        LinearProgressIndicator(
                            progress = { result.confidence },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            stringResource(R.string.aisort_reasons),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        if (result.matched.isEmpty()) {
                            Text(
                                "No distinctive keywords found — the manual stays in General.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(result.matched.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                        }

                        Text(
                            stringResource(R.string.aisort_sections),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        result.sections.forEachIndexed { index, section ->
                            Text("${index + 1}. $section", style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            enabled = selected != null,
                            onClick = { selected?.let { viewModel.applySuggestion(it.id, result) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.aisort_apply)) }
                    }
                }
            }
        }
    }
}
