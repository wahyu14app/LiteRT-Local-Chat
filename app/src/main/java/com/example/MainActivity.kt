package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val viewModel: ChatViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        ChatScreen(viewModel = viewModel)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  
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
               text = "LiteRT Local",
               style = MaterialTheme.typography.titleMedium,
               fontWeight = FontWeight.Medium
             )
             Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
               Box(modifier = Modifier.size(8.dp).background(com.example.ui.theme.ActiveGreen, RoundedCornerShape(50)))
               Text(
                 text = "MODEL LOADED",
                 style = MaterialTheme.typography.labelSmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant
               )
             }
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
           Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Box(
                modifier = Modifier
                  .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                  .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
              ) {
                 Icon(imageVector = Icons.Default.Chat, contentDescription = "Chat", tint = MaterialTheme.colorScheme.primary)
              }
              Text("Chat", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
           }
           Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Box(
                modifier = Modifier
                  .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
              ) {
                 Icon(imageVector = Icons.Default.Extension, contentDescription = "Models", tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Text("Models", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
           }
           Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Box(
                modifier = Modifier
                  .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
              ) {
                 Icon(imageVector = Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Text("History", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
      if (!uiState.isModelLoaded) {
        ModelSetupScreen(
          isLoading = uiState.isLoadingModel,
          error = uiState.modelLoadError,
          onLoadModel = { path, useGpu, ctx -> viewModel.loadModel(context = ctx, modelPath = path, useGpu = useGpu) }
        )
      } else {
        ChatInterface(
          messages = uiState.messages,
          isGenerating = uiState.isGenerating,
          onSendMessage = viewModel::sendMessage
        )
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
  var modelPath by remember { mutableStateOf("/data/local/tmp/model.litertlm") }
  
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = "Load Local Model",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold
    )
    
    Spacer(modifier = Modifier.size(16.dp))
    
    Text(
      text = "Enter the absolute path to your .litertlm file on the device.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    Spacer(modifier = Modifier.size(24.dp))
    
    OutlinedTextField(
      value = modelPath,
      onValueChange = { modelPath = it },
      label = { Text("Model Path") },
      modifier = Modifier.fillMaxWidth(),
      enabled = !isLoading
    )
    
    if (error != null) {
      Spacer(modifier = Modifier.size(8.dp))
      Text(
        text = error,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall
      )
    }
    
    Spacer(modifier = Modifier.size(24.dp))
    
    Button(
      onClick = { onLoadModel(modelPath, true, context) },
      enabled = !isLoading,
      modifier = Modifier.fillMaxWidth()
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(24.dp),
          color = MaterialTheme.colorScheme.onPrimary,
          strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Loading...")
      } else {
        Text("Load Model (GPU)")
      }
    }
    
    Spacer(modifier = Modifier.size(8.dp))
    
    Button(
      onClick = { onLoadModel(modelPath, false, context) },
      enabled = !isLoading,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Load Model (CPU)")
    }
  }
}

@Composable
fun ChatInterface(
  messages: List<ChatMessage>,
  isGenerating: Boolean,
  onSendMessage: (String) -> Unit
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
      item { ModelStatsCard() }
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
        text = message.text,
        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp)
      )
    }
  }
}
