package com.manoli.moodmate.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manoli.moodmate.model.MedType
import com.manoli.moodmate.model.MedicationEntry
import com.manoli.moodmate.service.StorageService
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MedicationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val storage = StorageService(context)
    var entries by remember { mutableStateOf(storage.loadMedicationEntries()) }

    // ── Diálogo para añadir ──
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(MedType.MEDICATION) }
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // ── Edición ──
    var showEditDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<MedicationEntry?>(null) }
    var editName by remember { mutableStateOf("") }
    var editDose by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf(MedType.MEDICATION) }

    // ── Eliminación ──
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<MedicationEntry?>(null) }

    // ── Diálogo de añadir ──
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nuevo registro") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        FilterChip(selected = selectedType == MedType.MEDICATION, onClick = { selectedType = MedType.MEDICATION }, label = { Text("💊 Medicación") })
                        FilterChip(selected = selectedType == MedType.THERAPY, onClick = { selectedType = MedType.THERAPY }, label = { Text("🧠 Terapia") })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                    if (selectedType == MedType.MEDICATION) {
                        OutlinedTextField(value = dose, onValueChange = { dose = it }, label = { Text("Dosis (opcional)") }, modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Nota (opcional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    val entry = MedicationEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = Date(),
                        type = selectedType,
                        name = name.trim(),
                        dose = dose.ifBlank { null },
                        note = note.ifBlank { null }
                    )
                    storage.addMedicationEntry(entry)
                    entries = storage.loadMedicationEntries()
                    showAddDialog = false
                    name = ""; dose = ""; note = ""
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } }
        )
    }

    // ── Diálogo de edición ──
    if (showEditDialog && editingEntry != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar registro") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        FilterChip(selected = editType == MedType.MEDICATION, onClick = { editType = MedType.MEDICATION }, label = { Text("💊") })
                        FilterChip(selected = editType == MedType.THERAPY, onClick = { editType = MedType.THERAPY }, label = { Text("🧠") })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                    if (editType == MedType.MEDICATION) {
                        OutlinedTextField(value = editDose, onValueChange = { editDose = it }, label = { Text("Dosis (opcional)") }, modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(value = editNote, onValueChange = { editNote = it }, label = { Text("Nota (opcional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editName.isBlank()) {
                        Toast.makeText(context, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    editingEntry?.let {
                        val updated = it.copy(
                            type = editType,
                            name = editName.trim(),
                            dose = editDose.ifBlank { null },
                            note = editNote.ifBlank { null }
                        )
                        storage.updateMedicationEntry(updated)
                        entries = storage.loadMedicationEntries()
                        showEditDialog = false
                        editingEntry = null
                        Toast.makeText(context, "Registro actualizado", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancelar") } }
        )
    }

    // ── Diálogo de confirmación para eliminar ──
    if (showDeleteDialog && entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar registro") },
            text = { Text("¿Seguro que quieres eliminar este registro de medicación/terapia?") },
            confirmButton = {
                TextButton(onClick = {
                    entryToDelete?.let { storage.deleteMedicationEntry(it.id) }
                    entries = storage.loadMedicationEntries()
                    showDeleteDialog = false
                    entryToDelete = null
                    Toast.makeText(context, "Registro eliminado", Toast.LENGTH_SHORT).show()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; entryToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguimiento", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir")
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay registros. Pulsa + para añadir uno.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries.sortedByDescending { it.timestamp }) { entry ->
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { /* clic normal no hace nada */ },
                                    onLongClick = { showMenu = true }
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (entry.type == MedType.MEDICATION) "💊" else "🧠", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(entry.name, fontWeight = FontWeight.SemiBold)
                                }
                                if (entry.dose != null) Text("Dosis: ${entry.dose}")
                                if (entry.note != null) Text(entry.note)
                                Text(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(entry.timestamp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                onClick = {
                                    showMenu = false
                                    editingEntry = entry
                                    editName = entry.name
                                    editDose = entry.dose ?: ""
                                    editNote = entry.note ?: ""
                                    editType = entry.type
                                    showEditDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar") },
                                onClick = {
                                    showMenu = false
                                    entryToDelete = entry
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}