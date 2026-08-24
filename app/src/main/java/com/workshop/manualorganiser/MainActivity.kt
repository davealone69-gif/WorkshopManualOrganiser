package com.workshop.manualorganiser

import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.workshop.manualorganiser.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(onPopup = { showPopupMenu(it) })
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_scan -> { toast("Scan Manual"); true }
            R.id.action_upload -> { toast("Upload"); true }
            R.id.action_vin_decoder -> { toast("VIN Decoder"); true }
            R.id.action_wiring -> { toast("Wiring Diagrams"); true }
            R.id.action_ai_sort -> { toast("AI Sort & Structure"); true }
            R.id.action_about -> { showAboutDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ctx_open -> { toast("Open"); true }
            R.id.ctx_edit -> { toast("Edit Structure"); true }
            R.id.ctx_export -> { toast("Export"); true }
            R.id.ctx_delete -> { toast("Delete"); true }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun showPopupMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.popup_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.popup_camera -> { toast("Take Photo"); true }
                    R.id.popup_gallery -> { toast("From Gallery"); true }
                    R.id.popup_pdf -> { toast("Import PDF"); true }
                    R.id.popup_ai_help -> { toast("AI Technical Help"); true }
                    else -> false
                }
            }
            show()
        }
    }

    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Workshop Manual Organiser")
            .setMessage("Version 1.0.0\n\nAI-powered workshop manual manager\nScan • Upload • VIN Decoder • Wiring • AI Sorting\n\nBuilt by REDRUM Studios")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

@Composable
fun MainScreen(onPopup: (View) -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Workshop Manual Organiser", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier = Modifier.height(8.dp))
        Text("Built by REDRUM Studios", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { }) { Text("More Actions") }
    }
}
