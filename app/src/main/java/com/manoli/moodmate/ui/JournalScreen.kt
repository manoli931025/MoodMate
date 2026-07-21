package com.manoli.moodmate.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.manoli.moodmate.model.JournalEntry
import com.manoli.moodmate.service.StorageService
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val storage = StorageService(context)
    var entries by remember { mutableStateOf(storage.loadJournalEntries()) }

    // Estados para la edición a pantalla completa
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var editingEntryId by remember { mutableStateOf<String?>(null) }

    // Diálogo de confirmación para eliminar
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<JournalEntry?>(null) }

    // Si estamos en modo edición (nueva o existente), mostramos la pantalla completa
    if (isEditing) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (editingEntryId == null) "Nueva entrada" else "Editar entrada") },
                    navigationIcon = {
                        IconButton(onClick = {
                            isEditing = false
                            editingEntryId = null
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (editContent.isBlank()) {
                                Toast.makeText(context, "Escribe algo antes de guardar", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            if (editingEntryId != null) {
                                val oldEntry = entries.find { it.id == editingEntryId }
                                if (oldEntry != null) {
                                    val updated = oldEntry.copy(
                                        title = editTitle.ifBlank { null },
                                        content = editContent.trim()
                                    )
                                    storage.updateJournalEntry(updated)
                                }
                            } else {
                                val entry = JournalEntry(
                                    id = UUID.randomUUID().toString(),
                                    timestamp = Date(),
                                    title = editTitle.ifBlank { null },
                                    content = editContent.trim()
                                )
                                storage.addJournalEntry(entry)
                            }
                            entries = storage.loadJournalEntries()
                            isEditing = false
                            editingEntryId = null
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Título (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        placeholder = { Text("Escribe tus pensamientos...") },
                        modifier = Modifier.fillMaxSize(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            }
        }
        return
    }

    // ── Pantalla de lista de entradas ──
    if (showDeleteDialog && entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar entrada") },
            text = { Text("¿Seguro que quieres eliminar esta entrada del diario?") },
            confirmButton = {
                TextButton(onClick = {
                    entryToDelete?.let { storage.deleteJournalEntry(it.id) }
                    entries = storage.loadJournalEntries()
                    showDeleteDialog = false
                    entryToDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diario personal", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = {
                    IconButton(onClick = {
                        editTitle = ""
                        editContent = ""
                        editingEntryId = null
                        isEditing = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir")
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),  // ← Margen añadido
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tu diario está vacío.\nToca + para escribir tu primera entrada.",
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries.sortedByDescending { it.timestamp }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (entry.title != null) Text(entry.title, fontWeight = FontWeight.SemiBold)
                            Text(entry.content, maxLines = 5)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(entry.timestamp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Row {
                                    TextButton(onClick = {
                                        editTitle = entry.title ?: ""
                                        editContent = entry.content
                                        editingEntryId = entry.id
                                        isEditing = true
                                    }) { Text("Editar") }
                                    TextButton(onClick = {
                                        entryToDelete = entry
                                        showDeleteDialog = true
                                    }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}