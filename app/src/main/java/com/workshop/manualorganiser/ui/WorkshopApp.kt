package com.workshop.manualorganiser.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.FileProvider
import com.workshop.manualorganiser.R
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Every place the overflow menu can navigate to. */
enum class Destination { VIN, WIRING, AI_SORT, AI_HELP, ABOUT }

/** The app's navigation stack (a single, explicit list of screens). */
sealed interface Screen {
    data object Home : Screen
    data class Detail(val manualId: String) : Screen
    data class Structure(val manualId: String) : Screen
    data object Vin : Screen
    data object Wiring : Screen
    data object AiSort : Screen
    data object AiHelp : Screen
    data object About : Screen
}

@Composable
fun WorkshopApp(
    viewModel: WorkshopViewModel = viewModel(),
    initialImportUri: Uri? = null,
    onImportConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var captureFile by remember { mutableStateOf<File?>(null) }

    val message by viewModel.message.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    LaunchedEffect(initialImportUri) {
        initialImportUri?.let {
            viewModel.importArchive(it)
            onImportConsumed()
        }
    }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    fun timestamp(): String =
        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

    // ---- Scan Manual: capture a page and start a new manual from it ----------
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = captureFile
        captureFile = null
        if (success && file != null) {
            scope.launch {
                val page = viewModel.importCapture(file)
                if (page == null) {
                    snackbarHostState.showSnackbar(context.getString(R.string.import_nothing))
                } else {
                    viewModel.addManual(
                        title = context.getString(R.string.menu_scan) + " • " + timestamp(),
                        category = CategoryDefaults.DEFAULT,
                        notes = "",
                        vin = "",
                        pages = listOf(page),
                    ) { created -> screen = Screen.Detail(created.id) }
                }
            }
        }
    }

    // ---- Upload: import a PDF or image and start a new manual from it --------
    val pickDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
                val pages = if (mime.contains("pdf")) {
                    viewModel.importPdf(uri)
                } else {
                    listOfNotNull(viewModel.importImage(uri))
                }
                if (pages.isEmpty()) {
                    snackbarHostState.showSnackbar(context.getString(R.string.import_nothing))
                } else {
                    viewModel.addManual(
                        title = context.getString(R.string.menu_upload) + " • " + timestamp(),
                        category = CategoryDefaults.DEFAULT,
                        notes = "",
                        vin = "",
                        pages = pages,
                    ) { created -> screen = Screen.Detail(created.id) }
                }
            }
        }
    }

    val startScan: () -> Unit = {
        val file = runCatching {
            com.workshop.manualorganiser.util.MediaImporter.newCaptureFile(context)
        }.getOrNull()
        if (file == null) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.err_no_camera)) }
        } else {
            captureFile = file
            runCatching { takePicture.launch(uriFor(context, file)) }.onFailure {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.err_no_camera)) }
            }
        }
    }

    val startUpload: () -> Unit = { pickDocument.launch(arrayOf("application/pdf", "image/*")) }

    val goTo: (Destination) -> Unit = { destination ->
        screen = when (destination) {
            Destination.VIN -> Screen.Vin
            Destination.WIRING -> Screen.Wiring
            Destination.AI_SORT -> Screen.AiSort
            Destination.AI_HELP -> Screen.AiHelp
            Destination.ABOUT -> Screen.About
        }
    }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = screen) {
                Screen.Home -> HomeScreen(
                    viewModel = viewModel,
                    onOpen = { screen = Screen.Detail(it) },
                    onEditStructure = { screen = Screen.Structure(it) },
                    onScanManual = startScan,
                    onUpload = startUpload,
                    onOpenDestination = goTo,
                )

                is Screen.Detail -> ManualDetailScreen(
                    viewModel = viewModel,
                    manualId = current.manualId,
                    onBack = { screen = Screen.Home },
                    onEditStructure = { screen = Screen.Structure(current.manualId) },
                    onScanManual = startScan,
                    onUpload = startUpload,
                    onOpenDestination = goTo,
                )

                is Screen.Structure -> EditStructureScreen(
                    viewModel = viewModel,
                    manualId = current.manualId,
                    onBack = { screen = Screen.Detail(current.manualId) },
                    onOpenDestination = goTo,
                )

                Screen.Vin -> VinScreen(
                    viewModel = viewModel,
                    onBack = { screen = Screen.Home },
                    onOpenDestination = goTo,
                )

                Screen.Wiring -> WiringScreen(
                    onBack = { screen = Screen.Home },
                    onOpenDestination = goTo,
                )

                Screen.AiSort -> AiSortScreen(
                    viewModel = viewModel,
                    onBack = { screen = Screen.Home },
                    onOpenDestination = goTo,
                )

                Screen.AiHelp -> AiHelpScreen(
                    onBack = { screen = Screen.Home },
                    onOpenDestination = goTo,
                )

                Screen.About -> AboutScreen(
                    viewModel = viewModel,
                    onBack = { screen = Screen.Home },
                    onOpenDestination = goTo,
                )
            }

            if (busy) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}

/** Categories offered when a manual is created without classification. */
object CategoryDefaults {
    const val DEFAULT = "General"
    val ALL = listOf(
        "General", "Engine", "Electrical", "Transmission", "Brakes",
        "Suspension", "Body & Interior", "Safety", "Diagnostics",
    )
}

fun uriFor(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

/**
 * Shared top bar. The overflow menu is a real Compose menu, so every advertised
 * action is reachable from every screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onScanManual: (() -> Unit)? = null,
    onUpload: (() -> Unit)? = null,
    onOpenDestination: (Destination) -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                }
            }
        },
        actions = {
            trailing()
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_more))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (onScanManual != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_scan)) },
                        leadingIcon = { Icon(Icons.Default.PhotoCamera, null) },
                        onClick = { menuOpen = false; onScanManual() },
                    )
                }
                if (onUpload != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_upload)) },
                        leadingIcon = { Icon(Icons.Default.Upload, null) },
                        onClick = { menuOpen = false; onUpload() },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_vin)) },
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, null) },
                    onClick = { menuOpen = false; onOpenDestination(Destination.VIN) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_wiring)) },
                    leadingIcon = { Icon(Icons.Default.Build, null) },
                    onClick = { menuOpen = false; onOpenDestination(Destination.WIRING) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_ai_sort)) },
                    leadingIcon = { Icon(Icons.Default.Construction, null) },
                    onClick = { menuOpen = false; onOpenDestination(Destination.AI_SORT) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_ai_help)) },
                    leadingIcon = { Icon(Icons.Default.Psychology, null) },
                    onClick = { menuOpen = false; onOpenDestination(Destination.AI_HELP) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_about)) },
                    leadingIcon = { Icon(Icons.Default.Info, null) },
                    onClick = { menuOpen = false; onOpenDestination(Destination.ABOUT) },
                )
            }
        },
    )
}
