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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                MainScreen()
            }
        }
    }

    // ---------- OPTIONS MENU ----------
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_scan -> {
                Toast.makeText(this, "Scan Manual", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_upload -> {
                Toast.makeText(this, "Upload", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_vin_decoder -> {
                Toast.makeText(this, "VIN Decoder", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_wiring -> {
                Toast.makeText(this, "Wiring Diagrams", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_ai_sort -> {
                Toast.makeText(this, "AI Sort & Structure", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ---------- CONTEXT MENU ----------
    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ctx_open -> { Toast.makeText(this, "Open", Toast.LENGTH_SHORT).show(); true }
            R.id.ctx_edit -> { Toast.makeText(this, "Edit Structure", Toast.LENGTH_SHORT).show(); true }
            R.id.ctx_export -> { Toast.makeText(this, "Export", Toast.LENGTH_SHORT).show(); true }
            R.id.ctx_delete -> { Toast.makeText(this, "Delete", Toast.LENGTH_SHORT).show(); true }
            else -> super.onContextItemSelected(item)
        }
    }

    // ---------- POPUP MENU ----------
    private fun showPopupMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.popup_camera -> { Toast.makeText(this, "Take Photo", Toast.LENGTH_SHORT).show(); true }
                R.id.popup_gallery -> { Toast.makeText(this, "From Gallery", Toast.LENGTH_SHORT).show(); true }
                R.id.popup_pdf -> { Toast.makeText(this, "Import PDF", Toast.LENGTH_SHORT).show(); true }
                R.id.popup_ai_help -> { Toast.makeText(this, "AI Technical Help", Toast.LENGTH_SHORT).show(); true }
                else -> false
            }
        }
        popup.show()
    }

    // ---------- ABOUT / CREDITS ----------
    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Workshop Manual Organiser")
            .setMessage(
                "Version 1.0.0\n\n" +
                "AI-powered workshop manual manager\n" +
                "Scan • Upload • VIN Decoder • Wiring Diagrams • AI Sorting\n\n" +
                "Built by REDRUM Studios"
            )
            .setPositiveButton("OK", null)
            .show()
    }
}

@Composable
fun MainScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Workshop Manual Organiser", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Built by REDRUM Studios", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { }) {
            Text("More Actions (Popup Menu)")
        }
    }
}
