package com.workshop.manualorganiser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.workshop.manualorganiser.R
import com.workshop.manualorganiser.util.AiClient
import com.workshop.manualorganiser.util.KnowledgeBase
import kotlinx.coroutines.launch

@Composable
fun AiHelpScreen(
    onBack: () -> Unit,
    onOpenDestination: (Destination) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<KnowledgeBase.Article>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    var remoteAnswer by remember { mutableStateOf<String?>(null) }
    var remoteError by remember { mutableStateOf<String?>(null) }
    var serverStatus by remember { mutableStateOf<String?>(null) }
    var checkingServer by remember { mutableStateOf(false) }

    val examples = listOf("P0301", "P0171", "rough idle", "overheating", "ABS light", "no crank")

    fun ask(text: String) {
        query = text
        searched = true
        results = KnowledgeBase.search(text)
        remoteAnswer = null
        remoteError = null

        if (AiClient.isConfigured) {
            val context = results.take(2).joinToString("\n") {
                "${it.code} ${it.title}: ${it.checks.joinToString(" ")}"
            }
            scope.launch {
                AiClient.ask(text, context).fold(
                    onSuccess = { remoteAnswer = it },
                    onFailure = { remoteError = it.message ?: "unknown error" },
                )
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        AppTopBar(
            title = stringResource(R.string.aihelp_title),
            onBack = onBack,
            onOpenDestination = onOpenDestination,
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge {
                    Text(stringResource(if (AiClient.isConfigured) R.string.aihelp_remote_badge else R.string.aihelp_offline_badge))
                }
                if (AiClient.isConfigured) {
                    Button(
                        enabled = !checkingServer,
                        onClick = {
                            checkingServer = true
                            serverStatus = null
                            scope.launch {
                                AiClient.checkServer().fold(
                                    onSuccess = { serverStatus = it },
                                    onFailure = { serverStatus = "ERROR:" + (it.message ?: "unknown error") },
                                )
                                checkingServer = false
                            }
                        },
                    ) { Text(stringResource(R.string.aihelp_check_server)) }
                }
            }

            Text(stringResource(R.string.aihelp_model, AiClient.model), style = MaterialTheme.typography.bodySmall)

            serverStatus?.let { status ->
                if (status.startsWith("ERROR:")) {
                    Text(
                        stringResource(R.string.aihelp_server_error, status.removePrefix("ERROR:")),
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(stringResource(R.string.aihelp_server_online, status))
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.aihelp_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = { ask(query) }, enabled = query.isNotBlank()) {
                Text(stringResource(R.string.aihelp_ask))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                examples.forEach { example ->
                    AssistChip(onClick = { ask(example) }, label = { Text(example) })
                }
            }

            remoteError?.let { error ->
                Text(
                    stringResource(R.string.aihelp_remote_error, error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            remoteAnswer?.takeIf { it.isNotBlank() }?.let { answer ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.aihelp_remote_badge),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(answer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (searched && results.isEmpty()) {
                Text(
                    stringResource(R.string.aihelp_no_match),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            results.forEach { article ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Badge { Text(article.code) }
                            Badge { Text(article.system) }
                            Badge { Text(article.severity) }
                        }
                        Text(
                            article.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        Section(stringResource(R.string.field_notes), article.symptoms.joinToString(" • "))
                        Section("Likely causes", article.causes.joinToString(" • "))
                        Section("Test order", article.checks.joinToString("\n"))
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Text(body, style = MaterialTheme.typography.bodySmall)
    }
}
