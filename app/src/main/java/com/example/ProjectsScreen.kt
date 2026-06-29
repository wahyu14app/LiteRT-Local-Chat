package com.example

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(viewModel: ChatViewModel, onProjectSelected: (File) -> Unit) {
    var projects by remember { mutableStateOf(AppConfig.PROJECTS_DIR.listFiles()?.filter { it.isDirectory } ?: emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Project") },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Project Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newProjectName.isNotBlank()) {
                        val id = UUID.randomUUID().toString()
                        val projectDir = File(AppConfig.PROJECTS_DIR, id)
                        projectDir.mkdirs()
                        File(projectDir, "name.txt").writeText(newProjectName)
                        File(projectDir, "data").mkdirs()
                        File(projectDir, "instructions.txt").writeText("""
You are an AI assistant in Project Mode. You have access to tools to read/write files and search the web.
All files are saved in the project data directory.

To use a tool, you can output an XML block:
<tool action="write_file" path="config.json">
{
  "key": "value"
}
</tool>

<tool action="read_file" path="config.json" />

<tool action="web_search" query="android development" />

Alternatively, you can use JSON format for tools:
{"action": "write_file", "path": "test.txt", "content": "hello"}
{"action": "read_file", "path": "test.txt"}
{"action": "web_search", "query": "android development"}

Whenever you find new information, write it to a file in a structured format like JSON.
                        """.trimIndent())
                        
                        projects = AppConfig.PROJECTS_DIR.listFiles()?.filter { it.isDirectory } ?: emptyList()
                        showCreateDialog = false
                        newProjectName = ""
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Projects Mode") },
            actions = {
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New Project")
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(projects) { projectDir ->
                val nameFile = File(projectDir, "name.txt")
                val projectName = if (nameFile.exists()) nameFile.readText() else projectDir.name
                
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.setActiveProject(projectDir)
                        onProjectSelected(projectDir)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(projectName, style = MaterialTheme.typography.titleMedium)
                            Text(projectDir.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
