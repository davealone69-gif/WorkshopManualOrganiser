package com.workshop.manualorganiser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.workshop.manualorganiser.ui.WorkshopApp
import com.workshop.manualorganiser.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    private val pendingImport = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingImport.value = archiveUri(intent)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WorkshopApp(
                        initialImportUri = pendingImport.value,
                        onImportConsumed = { pendingImport.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingImport.value = archiveUri(intent)
    }

    private fun archiveUri(intent: Intent?): Uri? =
        intent?.data?.takeIf { intent.action == Intent.ACTION_VIEW }
}
