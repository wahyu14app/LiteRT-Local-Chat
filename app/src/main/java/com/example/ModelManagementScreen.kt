package com.example

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagementScreen(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var cpuModels by remember { mutableStateOf(emptyList<File>()) }
    var gpuModels by remember { mutableStateOf(emptyList<File>()) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf(0f) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImportType by remember { mutableStateOf("GPU") }

    fun refreshModels() {
        cpuModels = AppConfig.MODELS_CPU_DIR.listFiles()?.toList() ?: emptyList()
        gpuModels = AppConfig.MODELS_GPU_DIR.listFiles()?.toList() ?: emptyList()
    }

    LaunchedEffect(Unit) {
        refreshModels()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            showImportDialog = true
        }
    }

    fun importModel(uri: Uri, isGpu: Boolean) {
        coroutineScope.launch {
            isImporting = true
            importProgress = 0f
            withContext(Dispatchers.IO) {
                try {
                    val contentResolver = context.contentResolver
                    val fileName = "imported_model_${System.currentTimeMillis()}.litertlm" // Or try to get real name
                    val targetDir = if (isGpu) AppConfig.MODELS_GPU_DIR else AppConfig.MODELS_CPU_DIR
                    val targetFile = File(targetDir, fileName)
                    
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            val buffer = ByteArray(8192)
                            var length: Int
                            val totalSize = input.available().toFloat() // Note: available() might not give total size accurately for streams
                            var copied = 0f
                            while (input.read(buffer).also { length = it } > 0) {
                                output.write(buffer, 0, length)
                                copied += length
                                if (totalSize > 0) {
                                   importProgress = copied / totalSize
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            isImporting = false
            refreshModels()
        }
    }

    if (showImportDialog && selectedUri != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Model") },
            text = {
                Column {
                    Text("Select where to import this model:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedImportType == "GPU",
                            onClick = { selectedImportType = "GPU" }
                        )
                        Text("GPU (Recommended)", modifier = Modifier.clickable { selectedImportType = "GPU" })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedImportType == "CPU",
                            onClick = { selectedImportType = "CPU" }
                        )
                        Text("CPU", modifier = Modifier.clickable { selectedImportType = "CPU" })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    importModel(selectedUri!!, selectedImportType == "GPU")
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Model Management") },
            actions = {
                IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Model")
                }
            }
        )

        if (isImporting) {
            LinearProgressIndicator(
                progress = { importProgress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Importing model... This may take a while.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("GPU Models", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (gpuModels.isEmpty()) {
                item { Text("No GPU models found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(gpuModels) { file ->
                ModelItem(
                    file = file,
                    onDelete = {
                        file.delete()
                        refreshModels()
                    },
                    onLoad = {
                        viewModel.loadModel(context, file.absolutePath, useGpu = true)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("CPU Models", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (cpuModels.isEmpty()) {
                item { Text("No CPU models found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(cpuModels) { file ->
                ModelItem(
                    file = file,
                    onDelete = {
                        file.delete()
                        refreshModels()
                    },
                    onLoad = {
                        viewModel.loadModel(context, file.absolutePath, useGpu = false)
                    }
                )
            }
        }
    }
}

@Composable
fun ModelItem(file: File, onDelete: () -> Unit, onLoad: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onLoad() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                val sizeMb = file.length() / (1024 * 1024)
                Text("$sizeMb MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
