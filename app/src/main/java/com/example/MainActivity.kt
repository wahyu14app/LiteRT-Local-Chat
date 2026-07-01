package com.example

import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.settings.SettingsRepository
import com.example.settings.dataStore
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val viewModel: ChatViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // enableEdgeToEdge()
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(intent)
            }
        }
    }
    
    val intent = android.content.Intent(this, EngineService::class.java)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
    setContent {
      MyApplicationTheme {
        MainScreen(viewModel = viewModel)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ChatViewModel) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  
  var currentScreen by remember { mutableStateOf("HOME") }
  
  LaunchedEffect(Unit) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          if (!Environment.isExternalStorageManager()) {
              try {
                  val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                  intent.addCategory("android.intent.category.DEFAULT")
                  intent.data = Uri.parse(String.format("package:%s", context.packageName))
                  context.startActivity(intent)
              } catch (e: Exception) {
                  val intent = Intent()
                  intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                  context.startActivity(intent)
              }
          } else {
              AppConfig.ensureDirectoriesExist()
          }
      } else {
          AppConfig.ensureDirectoriesExist()
      }
  }
  
  LaunchedEffect(uiState.errorEvent) {
      uiState.errorEvent?.let { errorMsg ->
          snackbarHostState.showSnackbar(errorMsg)
          viewModel.clearErrorEvent()
      }
  }
  
  Scaffold(
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.background)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
               imageVector = Icons.Default.SmartToy,
               contentDescription = "Bot",
               tint = MaterialTheme.colorScheme.primary
            )
          }
          Column {
             Text(
               text = uiState.activeProject?.let { java.io.File(it, "name.txt").takeIf { it.exists() }?.readText() ?: it.name } ?: "LiteRT Local",
               style = MaterialTheme.typography.titleMedium,
               fontWeight = FontWeight.Medium
             )
             Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
               val statusColor = when {
                 uiState.isLoadingModel -> androidx.compose.ui.graphics.Color.Yellow
                 uiState.modelLoadError != null -> MaterialTheme.colorScheme.error
                 uiState.isModelLoaded -> com.example.ui.theme.ActiveGreen
                 else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
               }
               val statusText = when {
                 uiState.isLoadingModel -> "Loading..."
                 uiState.modelLoadError != null -> "Error"
                 uiState.isModelLoaded -> if (uiState.activeProject != null) "PROJECT: ${uiState.currentModelName ?: "Ready"}" else (uiState.currentModelName ?: "Ready")
                 else -> "No Model Loaded"
               }
               Box(modifier = Modifier.size(8.dp).background(statusColor, RoundedCornerShape(50)))
               Text(
                 text = statusText,
                 style = MaterialTheme.typography.labelSmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant
               )
             }
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          if (uiState.activeProject != null) {
              IconButton(onClick = { viewModel.setActiveProject(null) }) {
                  Icon(imageVector = Icons.Default.Close, contentDescription = "Exit Project Mode", tint = MaterialTheme.colorScheme.error)
              }
          }
          IconButton(onClick = {}) {
            Icon(imageVector = Icons.Default.Analytics, contentDescription = "Analytics", tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          IconButton(onClick = {}) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    },
    bottomBar = {
       Column(modifier = Modifier.fillMaxWidth()) {
         Divider(color = MaterialTheme.colorScheme.outline)
         Row(
           modifier = Modifier
             .fillMaxWidth()
             .background(MaterialTheme.colorScheme.background)
             .padding(horizontal = 8.dp, vertical = 8.dp),
           horizontalArrangement = Arrangement.SpaceAround
         ) {
           Column(
             modifier = Modifier.clickable { currentScreen = "HOME" },
             horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
           ) {
              Box(
                modifier = Modifier
                  .background(if(currentScreen == "HOME") MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(50))
                  .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
              ) {
                 Icon(imageVector = Icons.Default.Analytics, contentDescription = "Home", tint = if(currentScreen == "HOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Text("Home", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = if(currentScreen == "HOME") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
           }
           Column(
             modifier = Modifier.clickable { currentScreen = "CHAT_LIST" },
             horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
           ) {
              Box(
                modifier = Modifier
                  .background(if(currentScreen in listOf("CHAT", "CHAT_LIST", "SESSION_LIST")) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(50))
                  .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
              ) {
                 Icon(imageVector = Icons.Default.Chat, contentDescription = "Chat", tint = if(currentScreen in listOf("CHAT", "CHAT_LIST", "SESSION_LIST")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Text("Chat", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = if(currentScreen in listOf("CHAT", "CHAT_LIST", "SESSION_LIST")) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
           }
           Column(
             modifier = Modifier.clickable { currentScreen = "MODELS" },
             horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
           ) {
              Box(
                modifier = Modifier
                  .background(if(currentScreen == "MODELS") MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(50))
                  .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
              ) {
                 Icon(imageVector = Icons.Default.Extension, contentDescription = "Models", tint = if(currentScreen == "MODELS") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Text("Models", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = if(currentScreen == "MODELS") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
           }
         }
       }
    }
  ) { innerPadding ->
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      color = MaterialTheme.colorScheme.background
    ) {
      when (currentScreen) {
          "HOME" -> {
              HomeScreen(
                  onNavigateToChat = { currentScreen = "CHAT_LIST" },
                  onNavigateToModels = { currentScreen = "MODELS" },
                  onNavigateToSettings = { currentScreen = "SETTINGS" },
                  onNavigateToDocs = { currentScreen = "DOCS" }
              )
          }
          "CHAT" -> {
              if (!uiState.isModelLoaded) {
                ModelSetupScreen(
                  isLoading = uiState.isLoadingModel,
                  error = uiState.modelLoadError,
                  onLoadModel = { path, useGpu, ctx -> viewModel.loadModel(context = ctx, rawModelPath = path, useGpu = useGpu) }
                )
              } else {
                ChatInterface(
                  messages = uiState.messages,
                  isGenerating = uiState.isGenerating,
                  onSendMessage = viewModel::sendMessage,
                  onClearChat = viewModel::clearConversation
                )
              }
          }
          "MODELS" -> {
              ModelManagementScreen(
                  viewModel = viewModel,
                  onNavigateToChat = { currentScreen = "CHAT_LIST" }
              )
          }
          "CHAT_LIST" -> {
              ProjectsScreen(
                  viewModel = viewModel,
                  onProjectSelected = { currentScreen = "SESSION_LIST" }
              )
          }
          "SESSION_LIST" -> {
              SessionsScreen(
                  viewModel = viewModel,
                  onSessionSelected = { currentScreen = "CHAT" },
                  onBack = { currentScreen = "CHAT_LIST" }
              )
          }
          "SETTINGS" -> {
              SettingsScreen()
          }
          "DOCS" -> {
              DocsScreen()
          }
      }
    }
  }
}

@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDocs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Local AI Engine",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "Manage your local AI models and integrations",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MenuCard(
                modifier = Modifier.weight(1f),
                title = "Chat",
                description = "Talk to AI",
                icon = Icons.Default.Chat,
                onClick = onNavigateToChat
            )
            MenuCard(
                modifier = Modifier.weight(1f),
                title = "Models",
                description = "Import & Manage",
                icon = Icons.Default.Extension,
                onClick = onNavigateToModels
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MenuCard(
                modifier = Modifier.weight(1f),
                title = "Settings",
                description = "AI Parameters",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings
            )
            MenuCard(
                modifier = Modifier.weight(1f),
                title = "Docs",
                description = "Developer IPC",
                icon = Icons.Default.AttachFile,
                onClick = onNavigateToDocs
            )
        }
    }
}

@Composable
fun MenuCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.size(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context.dataStore) }
    val coroutineScope = rememberCoroutineScope()
    
    val temperature by settingsRepo.temperature.collectAsState(initial = 0.8f)
    val topK by settingsRepo.topK.collectAsState(initial = 40)
    val maxTokens by settingsRepo.maxTokens.collectAsState(initial = 1024)
    val topP by settingsRepo.topP.collectAsState(initial = 0.9f)
    val thinkingEnabled by settingsRepo.thinkingEnabled.collectAsState(initial = false)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("AI Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Temperature: ${String.format("%.2f", temperature)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Slider(
                    value = temperature,
                    onValueChange = { newValue -> 
                        coroutineScope.launch { settingsRepo.setTemperature(newValue) }
                    },
                    valueRange = 0f..2f
                )
                Text("Control the randomness of the model's output.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Max Tokens: ${maxTokens}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Slider(
                    value = maxTokens.toFloat(),
                    onValueChange = { newValue -> 
                        coroutineScope.launch { settingsRepo.setMaxTokens(newValue.toInt()) }
                    },
                    valueRange = 10f..4096f
                )
                Text("Limit the maximum length of generated responses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DocsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Developer Documentation", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(8.dp))
        Text("How to interact with this AI engine from other apps using Android Intents.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.size(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sending a Prompt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.size(8.dp))
                val code = """
val intent = Intent("com.example.litert.ACTION_PROMPT")
intent.putExtra("prompt", "Hello AI!")
intent.putExtra("session_id", "my_app_session")
intent.putExtra("reply_action", "com.myapp.ACTION_REPLY")
sendBroadcast(intent)
                """.trimIndent()
                Box(modifier = Modifier
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .fillMaxWidth()) {
                    Text(text = code, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
        }
        
        Spacer(modifier = Modifier.size(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Receiving a Response", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Register a BroadcastReceiver in your app for the 'reply_action' you provided.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.size(8.dp))
                val code = """
val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val response = intent.getStringExtra("response")
        val sessionId = intent.getStringExtra("session_id")
        // Handle AI response
    }
}
registerReceiver(receiver, IntentFilter("com.myapp.ACTION_REPLY"))
                """.trimIndent()
                Box(modifier = Modifier
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .fillMaxWidth()) {
                    Text(text = code, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun ModelSetupScreen(
  isLoading: Boolean,
  error: String?,
  onLoadModel: (String, Boolean, android.content.Context) -> Unit
) {
  val context = LocalContext.current
  var cpuModels by remember { mutableStateOf(emptyList<java.io.File>()) }
  var gpuModels by remember { mutableStateOf(emptyList<java.io.File>()) }
  
  LaunchedEffect(Unit) {
      AppConfig.ensureDirectoriesExist()
      cpuModels = AppConfig.MODELS_CPU_DIR.listFiles()?.toList() ?: emptyList()
      gpuModels = AppConfig.MODELS_GPU_DIR.listFiles()?.toList() ?: emptyList()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.size(16.dp))
    Text(
      text = if (isLoading) "Loading Model..." else "No Model Loaded",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold
    )
    if (isLoading) {
      Spacer(modifier = Modifier.size(16.dp))
      androidx.compose.material3.CircularProgressIndicator()
    } else {
        Spacer(modifier = Modifier.size(8.dp))
        Text(
          text = "Select a model to load",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.size(24.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (gpuModels.isNotEmpty()) {
                item { Text("GPU Models", style = MaterialTheme.typography.labelMedium) }
                items(gpuModels) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onLoadModel(file.absolutePath, true, context) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(file.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (cpuModels.isNotEmpty()) {
                item { Spacer(modifier = Modifier.size(8.dp)) }
                item { Text("CPU Models", style = MaterialTheme.typography.labelMedium) }
                items(cpuModels) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onLoadModel(file.absolutePath, false, context) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(file.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (gpuModels.isEmpty() && cpuModels.isEmpty()) {
                item {
                    Text(
                      text = "No models found. Please go to the 'Models' tab to import an AI model.",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                      modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    if (error != null) {
      Spacer(modifier = Modifier.size(16.dp))
      Text(
        text = error,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall
      )
    }
  }
}

@Composable
fun ChatInterface(
  messages: List<ChatMessage>,
  isGenerating: Boolean,
  onSendMessage: (String) -> Unit,
  onClearChat: () -> Unit
) {
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item { Spacer(modifier = Modifier.size(8.dp)) }
      item { 
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
          ) {
              Text("Mulai mengobrol dengan AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              TextButton(onClick = onClearChat) {
                  Icon(Icons.Default.Delete, contentDescription = "Bersihkan", modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Bersihkan Obrolan")
              }
          }
      }
      items(messages, key = { it.id }) { message ->
        MessageBubble(message = message)
      }
      item { Spacer(modifier = Modifier.size(8.dp)) }
    }
    
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
          .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = {}) {
          Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          modifier = Modifier.weight(1f),
          placeholder = { Text("Tanya sesuatu...", color = com.example.ui.theme.TextMutedColor) },
          enabled = !isGenerating,
          colors = TextFieldDefaults.colors(
             focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
             unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
             disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
             focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
             unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
             disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
          ),
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground)
        )
        IconButton(
          onClick = {
            if (inputText.isNotBlank()) {
              onSendMessage(inputText)
              inputText = ""
            }
          },
          enabled = !isGenerating && inputText.isNotBlank(),
          modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = MaterialTheme.colorScheme.onPrimary
          )
        }
      }
    }
  }
}

@Composable
fun ModelStatsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CURRENT MODEL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .background(com.example.ui.theme.OutlineColor.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Gemma 2B",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox(modifier = Modifier.weight(1f), label = "RAM", value = "1.2 GB")
                StatBox(modifier = Modifier.weight(1f), label = "LATENCY", value = "42ms")
                StatBox(modifier = Modifier.weight(1f), label = "TEMP", value = "0.7")
            }
        }
    }
}

@Composable
fun StatBox(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = com.example.ui.theme.TextMutedColor)
        Spacer(modifier = Modifier.size(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
  val isUser = message.isUser
  
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
  ) {
    if (isUser) {
       Text("User", style = MaterialTheme.typography.labelSmall, color = com.example.ui.theme.TextMutedColor, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
    } else {
       Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {
          Text("LiteRT-LM", style = MaterialTheme.typography.labelSmall, color = com.example.ui.theme.TextMutedColor)
          Spacer(modifier = Modifier.size(4.dp))
          Box(modifier = Modifier.background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
             Text("AI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
       }
    }
    
    val bubbleShape = RoundedCornerShape(
      topStart = 16.dp,
      topEnd = 16.dp,
      bottomStart = if (isUser) 16.dp else 4.dp,
      bottomEnd = if (isUser) 4.dp else 16.dp
    )
    
    Box(
      modifier = Modifier
        .fillMaxWidth(0.9f)
        .let {
            if (!isUser) {
                it.border(1.dp, MaterialTheme.colorScheme.outline, bubbleShape)
            } else {
                it
            }
        }
        .background(
            if (isUser) MaterialTheme.colorScheme.primaryContainer else com.example.ui.theme.White,
            bubbleShape
        )
        .padding(12.dp)
    ) {
      Text(
        text = parseMarkdownToAnnotatedString(message.text),
        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp)
      )
    }
  }
}

@Composable
fun parseMarkdownToAnnotatedString(text: String): androidx.compose.ui.text.AnnotatedString {
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val codeColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    return androidx.compose.ui.text.buildAnnotatedString {
        var currentIndex = 0
        // Simple regex to match **bold**, *italic*, `code`, and ```code block```
        // Note: this is a basic implementation that captures main formatting patterns
        val pattern = Regex("```(.*?)```|\\*\\*(.*?)\\*\\*|\\*(.*?)\\*|`(.*?)`", RegexOption.DOT_MATCHES_ALL)
        
        val matches = pattern.findAll(text)
        
        for (match in matches) {
            // Append the text before the match
            if (match.range.first > currentIndex) {
                append(text.substring(currentIndex, match.range.first))
            }
            
            // Apply styles based on the captured group
            when {
                match.groups[1] != null -> {
                    // Code block
                    withStyle(androidx.compose.ui.text.SpanStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        background = codeBg,
                        color = codeColor
                    )) {
                        append(match.groups[1]?.value ?: "")
                    }
                }
                match.groups[2] != null -> {
                    // Bold
                    withStyle(androidx.compose.ui.text.SpanStyle(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )) {
                        append(match.groups[2]?.value ?: "")
                    }
                }
                match.groups[3] != null -> {
                    // Italic
                    withStyle(androidx.compose.ui.text.SpanStyle(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )) {
                        append(match.groups[3]?.value ?: "")
                    }
                }
                match.groups[4] != null -> {
                    // Inline code
                    withStyle(androidx.compose.ui.text.SpanStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        background = codeBg,
                        color = codeColor
                    )) {
                        append(match.groups[4]?.value ?: "")
                    }
                }
            }
            currentIndex = match.range.last + 1
        }
        
        // Append any remaining text
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}
