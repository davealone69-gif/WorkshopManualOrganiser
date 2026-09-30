package com.workshop.manualorganiser

import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalAppSettings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.draw.scale
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap

object GlobalState {
    val savedManuals = androidx.compose.runtime.mutableStateListOf(
        SavedManual("Prius High Voltage System", listOf("Wiring", "Engines"), true),
        SavedManual("Adrulee Module Guide", listOf("Adrulee", "Accessories"), false)
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                WorkshopApp()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Home)
    object Library : Screen("library", "Library", Icons.AutoMirrored.Filled.MenuBook)
    object ManualViewer : Screen("viewer", "Viewer", Icons.Filled.Visibility)
    object Collaboration : Screen("collab", "Teams", Icons.Filled.Group)
    object AI : Screen("ai", "AI Assist", Icons.Filled.SmartToy)
    object VINDecoder : Screen("vin", "VIN Decoder", Icons.Filled.DirectionsCar)
    object Scanner : Screen("scanner", "Scan Manual", Icons.Filled.DocumentScanner)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

@Composable
fun WorkshopApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = listOf(
        Screen.Dashboard,
        Screen.Library,
        Screen.VINDecoder,
        Screen.Collaboration,
        Screen.Settings,
        Screen.AI
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints {
            val isExpanded = maxWidth > 600.dp
            
            if (isExpanded) {
                PermanentNavigationDrawer(
                    drawerContent = {
                        PermanentDrawerSheet(
                            Modifier.width(240.dp),
                            drawerContainerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Workshop Pro",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            screens.forEach { screen ->
                                NavigationDrawerItem(
                                    icon = { Icon(screen.icon, contentDescription = null) },
                                    label = { Text(screen.title) },
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Dashboard.route)
                                            launchSingleTop = true
                                        }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                ) {
                    AppNavHost(navController)
                }
            } else {
                Scaffold(
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            screens.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = null) },
                                    label = { Text(screen.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Dashboard.route)
                                            launchSingleTop = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    AppNavHost(navController, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToViewer = { navController.navigate(Screen.ManualViewer.route) },
                onNavigateToVIN = { navController.navigate(Screen.VINDecoder.route) }
            )
        }
        composable(Screen.Library.route) { LibraryScreen(navController) }
        composable(Screen.ManualViewer.route) { ManualViewerScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Collaboration.route) { CollaborationScreen() }
        composable(Screen.AI.route) { AIScreen() }
        composable(Screen.VINDecoder.route) { VINDecoderScreen() }
        composable(Screen.Scanner.route) { ScannerScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onNavigateToViewer: () -> Unit, onNavigateToVIN: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    val recentManuals = listOf(
        "Honda Civic 2018 Engine Manual" to "PDF • 12 MB • V2.1",
        "Yamaha R1 2020 Service Guide" to "Offline • Tagged: Engine"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workshop Dashboard", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = {
                        android.widget.Toast.makeText(context, "Printing to connected printer...", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Print, contentDescription = "Print")
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Alerts")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToVIN,
                icon = { Icon(Icons.Filled.CenterFocusWeak, contentDescription = "Scan VIN") },
                text = { Text("Scan VIN") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search titles, tags, or concepts (AI)...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
            
            item {
                Text(
                    text = "Recent Manuals",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            items(recentManuals) { (title, desc) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToViewer() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook, 
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VINDecoderScreen() {
    var isDecoding by remember { mutableStateOf(false) }
    var decodedInfo by remember { mutableStateOf<String?>(null) }
    var showUpgrades by remember { mutableStateOf(false) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedImage = bitmap
            isDecoding = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AI VIN Plate Decoder",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { 
                    try {
                        cameraLauncher.launch()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "No camera app available", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (capturedImage != null) {
                androidx.compose.foundation.Image(
                    bitmap = capturedImage!!.asImageBitmap(),
                    contentDescription = "Captured Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else if (isDecoding) {
                CircularProgressIndicator()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "Upload Photo",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Tap to capture VIN Plate Photo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = {
                try {
                    cameraLauncher.launch()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "No camera app available", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Capture Image with AI", style = MaterialTheme.typography.titleMedium)
        }
        
        LaunchedEffect(isDecoding) {
            if (isDecoding) {
                delay(2000)
                decodedInfo = "Vehicle: 1999 Nissan Skyline GT-R (R34)\nEngine: RB26DETT\nChassis: BNR34-123456\nColor: Midnight Purple II"
                showUpgrades = true
                isDecoding = false
            }
        }
        
        if (decodedInfo != null) {
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Vehicle Specifications",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(decodedInfo!!, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)
                }
            }
        }
        
        if (showUpgrades) {
            Spacer(Modifier.height(32.dp))
            Text(
                "Recommended Major Upgrades",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            
            val upgrades = listOf(
                "N1 Twin Turbos" to "Increases boost pressure for higher top-end power.",
                "HKS Intercooler Piping" to "Improves airflow and cooling efficiency.",
                "Ohlins Road & Track Suspension" to "Superior handling and track performance.",
                "Brembo 6-Piston Big Brake Kit" to "Essential stopping power for high-hp builds."
            )
            
            upgrades.forEach { (upgrade, desc) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(upgrade, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualViewerScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAIChat by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }

    DisposableEffect(context) {
        val textToSpeech = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.US
                val voices = tts?.voices
                val femaleVoice = voices?.firstOrNull { 
                    it.name.contains("female", ignoreCase = true) || 
                    it.name.contains("en-us-x-sfg", ignoreCase = true)
                }
                if (femaleVoice != null) {
                    tts?.voice = femaleVoice
                }
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Honda Civic 2018 Engine", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Page 42 - Timing Belt Diagram", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        android.widget.Toast.makeText(context, "Printing to connected printer...", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Print, contentDescription = "Print")
                    }
                    IconButton(onClick = {
                        if (isSpeaking) {
                            tts?.stop()
                            isSpeaking = false
                        } else {
                            tts?.speak(
                                "Page 42, Timing Belt Diagram. To calibrate the pump, set the torque to 25 Newton meters and verify the alignment mark.",
                                android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                                null,
                                "TTS_ID"
                            )
                            isSpeaking = true
                        }
                    }) {
                        Icon(
                            if (isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop Reading" else "Read Aloud",
                            tint = if (isSpeaking) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { /* Annotate */ }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Annotate")
                    }
                    IconButton(onClick = { showAIChat = !showAIChat }) {
                        Icon(Icons.Filled.SmartToy, contentDescription = "AI Assistant", tint = if (showAIChat) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(2f).fillMaxHeight().background(Color.LightGray), contentAlignment = Alignment.Center) {
                        DiagramContent()
                    }
                    if (showAIChat) {
                        Card(modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp), shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)) {
                            AIChatContent()
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(if (showAIChat) 1f else 2f).fillMaxWidth().background(Color.LightGray), contentAlignment = Alignment.Center) {
                        DiagramContent()
                    }
                    if (showAIChat) {
                        Card(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
                            AIChatContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagramContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Filled.Image, 
            contentDescription = "Diagram", 
            modifier = Modifier.size(120.dp),
            tint = Color.Gray
        )
        Spacer(Modifier.height(16.dp))
        Text("Interactive Engine Diagram")
        Text("(Clickable hotspots for part numbers & torque specs)")
    }
    
    // Mock hotspot
    Box(
        modifier = Modifier
            .offset(x = (-40).dp, y = (-20).dp)
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            .clickable { /* Show Part Info */ }
    )
}

@Composable
fun AIChatContent(viewModel: AIChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isSpeaking by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    var useThinking by remember { mutableStateOf(false) }
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    DisposableEffect(context) {
        val textToSpeech = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.US
                val voices = tts?.voices
                val femaleVoice = voices?.firstOrNull { 
                    it.name.contains("female", ignoreCase = true) || 
                    it.name.contains("en-us-x-sfg", ignoreCase = true)
                }
                if (femaleVoice != null) {
                    tts?.voice = femaleVoice
                }
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gemini Assistant", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Thinking", style = MaterialTheme.typography.bodySmall, color = if (useThinking) MaterialTheme.colorScheme.primary else Color.Gray)
                Switch(checked = useThinking, onCheckedChange = { useThinking = it }, modifier = Modifier.scale(0.8f))
            }
        }
        Spacer(Modifier.height(8.dp))
        
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!msg.isUser) {
                        Icon(
                            Icons.Filled.SmartToy, 
                            contentDescription = "AI",
                            modifier = Modifier.size(24.dp).padding(top = 4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (msg.isUser) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(12.dp)
                            .weight(1f, fill = false)
                    ) {
                        if (msg.isThinking) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Thinking...", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Text(
                                msg.text,
                                color = if (msg.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    if (!msg.isUser && !msg.isThinking && msg.text.isNotBlank()) {
                        IconButton(
                            onClick = {
                                if (isSpeaking) {
                                    tts?.stop()
                                    isSpeaking = false
                                } else {
                                    tts?.speak(msg.text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
                                    isSpeaking = true
                                }
                            },
                            modifier = Modifier.size(32.dp).padding(start = 4.dp, top = 4.dp)
                        ) {
                            Icon(
                                if (isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                                contentDescription = "Read Aloud",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask something...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewModel.sendMessage(inputText, useThinking)
                    inputText = ""
                },
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}


data class SavedManual(val title: String, val tags: List<String>, val isEnhanced: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(navController: NavHostController? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val savedManuals = GlobalState.savedManuals
    
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            isScanning = true
            scanResult = null
            scope.launch {
                delay(2500)
                isScanning = false
                scanResult = "AI Scan Complete: Cleaned up degraded text, colorized wiring diagrams, and indexed 12 pages."
            }
        }
    }
    
    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Manual Export")
        putExtra(android.content.Intent.EXTRA_TEXT, "Exported manual content here. (Integration point for files)")
    }
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Library") },
                actions = {
                    IconButton(onClick = { fileLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Filled.FileUpload, contentDescription = "Import Manual")
                    }
                    IconButton(onClick = { 
                        shareLauncher.launch(android.content.Intent.createChooser(shareIntent, "Export via...")) 
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Export via Bluetooth/WiFi/USB")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController?.navigate(Screen.Scanner.route) },
                icon = { Icon(Icons.Filled.DocumentScanner, "Scan") },
                text = { Text("Digitize Manual") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Library & Tags", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                FilterChip(selected = true, onClick = {}, label = { Text("All") })
                FilterChip(selected = false, onClick = {}, label = { Text("Engines") })
                FilterChip(selected = false, onClick = {}, label = { Text("Wiring") })
                FilterChip(selected = false, onClick = {}, label = { Text("Car Stereo") })
                FilterChip(selected = false, onClick = {}, label = { Text("Aftermarket Fridges") })
                FilterChip(selected = false, onClick = {}, label = { Text("Accessories") })
                FilterChip(selected = false, onClick = {}, label = { Text("Adrulee") })
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { fileLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = "Import Manual")
                Spacer(Modifier.width(8.dp))
                Text("Import Manual (PDF, Docs, Images)", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { shareLauncher.launch(android.content.Intent.createChooser(shareIntent, "Export via...")) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Filled.Share, contentDescription = "Export Manuals")
                Spacer(Modifier.width(8.dp))
                Text("Export Manuals (Bluetooth, WiFi, USB)", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(Modifier.height(24.dp))
            if (isScanning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(16.dp))
                        Text("AI is scanning, cleaning text, and colorizing wiring diagrams...", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            } else if (scanResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Result", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(scanResult!!, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Indexed Contents & Enhancements:", style = MaterialTheme.typography.titleSmall)
                        Text("1. Wiring Diagram (B&W -> Color Enhanced)\n2. Engine Specs (Illegible text restored)\n3. Troubleshooting (3 Interactive Photos extracted)", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray).clickable {
                                android.widget.Toast.makeText(context, "Opening interactive photo view...", android.widget.Toast.LENGTH_SHORT).show()
                            }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Image, contentDescription = "Extracted Photo", tint = Color.White)
                            }
                            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray).clickable {
                                android.widget.Toast.makeText(context, "Opening interactive photo view...", android.widget.Toast.LENGTH_SHORT).show()
                            }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Image, contentDescription = "Extracted Photo", tint = Color.White)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { 
                            scanResult = null
                            savedManuals.add(0, SavedManual("Newly Imported Manual", listOf("Adrulee", "AI Enhanced"), true))
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Save & View Library")
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            Text("Saved Manuals", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            savedManuals.forEach { manual ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        android.widget.Toast.makeText(context, "Opening ${manual.title}...", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Book, contentDescription = "Manual", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(manual.title, style = MaterialTheme.typography.titleMedium)
                            if (manual.isEnhanced) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Filled.AutoAwesome, contentDescription = "Enhanced", tint = Color.Magenta, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            manual.tags.forEach { tag ->
                                AssistChip(onClick = {}, label = { Text(tag, style = MaterialTheme.typography.bodySmall) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollaborationScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Team Workspaces", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = "Safety", tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Safety Alert Mode Active", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("Technician Mike flagged a critical high-voltage warning on the Prius manual.")
            }
        }
    }
}

@Composable
fun AIScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("AI Auto-Tagging & OCR", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
    }
}


@Composable
fun ScannerScreen(onBack: () -> Unit) {
    var isScanning by remember { mutableStateOf(false) }
    var scanComplete by remember { mutableStateOf(false) }
    var scannedPages by remember { mutableStateOf(0) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedImage = bitmap
            isScanning = true
            scanComplete = false
            scope.launch {
                delay(1500)
                scannedPages++
                isScanning = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Digitize Manual") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (capturedImage != null && !isScanning && !scanComplete) {
                     androidx.compose.foundation.Image(
                        bitmap = capturedImage!!.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else if (isScanning) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Scanning page...", color = Color.White, modifier = Modifier.padding(top = 64.dp))
                } else if (scanComplete) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CheckCircle, "Done", tint = Color.Green, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("$scannedPages pages digitized", color = Color.White)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.DocumentScanner, "Scanner", tint = Color.White, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Align manual page in frame", color = Color.White)
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        try {
                            cameraLauncher.launch()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "No camera app available", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Filled.CameraAlt, "Capture")
                    Spacer(Modifier.width(8.dp))
                    Text("Capture Page")
                }
                
                if (scannedPages > 0) {
                    Button(
                        onClick = {
                            scanComplete = true
                            GlobalState.savedManuals.add(0, SavedManual("Newly Digitized Manual", listOf("Scans", "AI Processed"), true))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Filled.Save, "Save")
                        Spacer(Modifier.width(8.dp))
                        Text("Save as PDF")
                    }
                }
            }
        }
    }
}


@Composable
fun SettingsScreen() {
    val appSettings = LocalAppSettings.current
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = appSettings.themeMode.value == mode,
                    onClick = { appSettings.themeMode.value = mode },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text("Font", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFontFamily.entries.forEach { font ->
                FilterChip(
                    selected = appSettings.fontFamily.value == font,
                    onClick = { appSettings.fontFamily.value = font },
                    label = { Text(font.displayName) }
                )
            }
        }
    }
}

