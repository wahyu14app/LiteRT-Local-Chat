package com.example

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(viewModel: ChatViewModel, onSessionSelected: () -> Unit, onBack: () -> Unit) {
    val activeProject = viewModel.uiState.collectAsState().value.activeProject
    var sessions by remember { mutableStateOf(emptyList<String>()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newSessionName by remember { mutableStateOf("") }
    
    val baseDir = activeProject ?: AppConfig.CONVERSATIONS_DIR

    LaunchedEffect(baseDir) {
        val sessionsDir = File(baseDir, "sessions")
        if (!sessionsDir.exists()) sessionsDir.mkdirs()
        sessions = sessionsDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
        if (sessions.isEmpty()) {
            val defaultSession = File(sessionsDir, "default")
            defaultSession.mkdirs()
            sessions = listOf("default")
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Buat Sesi Baru") },
            text = {
                OutlinedTextField(
                    value = newSessionName,
                    onValueChange = { newSessionName = it },
                    label = { Text("Nama Sesi") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newSessionName.isNotBlank()) {
                        val id = UUID.randomUUID().toString()
                        val sessionsDir = File(baseDir, "sessions")
                        val newSessionDir = File(sessionsDir, id)
                        newSessionDir.mkdirs()
                        File(newSessionDir, "name.txt").writeText(newSessionName)
                        sessions = sessionsDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
                        showCreateDialog = false
                        newSessionName = ""
                    }
                }) {
                    Text("Buat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Pilih Sesi") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                }
            },
            actions = {
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Buat Sesi Baru")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sessions) { sessionId ->
                val sessionDir = File(File(baseDir, "sessions"), sessionId)
                val nameFile = File(sessionDir, "name.txt")
                val sessionName = if (nameFile.exists()) nameFile.readText() else if(sessionId == "default") "Sesi Default" else sessionId

                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.setSession(sessionId)
                        onSessionSelected()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ChatBubble, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(sessionName, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
