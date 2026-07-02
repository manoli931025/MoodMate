package com.manoli.moodmate.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
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
import com.manoli.moodmate.model.Entry
import com.manoli.moodmate.model.Mood
import com.manoli.moodmate.service.StorageService
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val storage = StorageService(context)

    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }

    var showFilters by remember { mutableStateOf(false) }
    var filterMood by remember { mutableStateOf<Mood?>(null) }
    var filterStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var filterEndDate by remember { mutableStateOf<LocalDate?>(null) }

    // Estado para editar entrada
    var editingEntry by remember { mutableStateOf<Entry?>(null) }
    var editMood by remember { mutableStateOf(Mood.NEUTRAL) }
    var editEnergy by remember { mutableFloatStateOf(5f) }
    var editStress by remember { mutableFloatStateOf(5f) }
    var editNote by remember { mutableStateOf("") }

    val allEntriesForMonth = remember(currentYear, currentMonth) {
        storage.getEntriesForMonth(currentYear, currentMonth)
    }

    // Filtrado usando LocalDate (sin problemas de zona horaria)
    val filteredEntries = remember(allEntriesForMonth, filterMood, filterStartDate, filterEndDate) {
        val start = filterStartDate
        val end = filterEndDate
        allEntriesForMonth.filter { entry ->
            val matchesMood = filterMood == null || entry.mood == filterMood
            val entryDate = Instant.ofEpochMilli(entry.timestamp.time)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val startOk = start == null || !entryDate.isBefore(start)
            val endOk = end == null || !entryDate.isAfter(end)
            matchesMood && startOk && endOk
        }
    }

    val dayMoodMap = remember(filteredEntries) {
        val cal = Calendar.getInstance()
        val grouped = filteredEntries.groupBy { entry ->
            cal.time = entry.timestamp
            cal.get(Calendar.DAY_OF_MONTH)
        }
        grouped.mapValues { (_, entries) ->
            val moodCount = entries.groupingBy { it.mood }.eachCount()
            val predominantMood = moodCount.maxByOrNull { it.value }?.key ?: Mood.NEUTRAL
            moodColor(predominantMood)
        }
    }

    // Diálogo para eliminar
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<Entry?>(null) }

    // Diálogo de confirmación de eliminación
    if (showDeleteDialog && entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar entrada") },
            text = { Text("¿Seguro que quieres eliminar este check-in? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    entryToDelete?.let { storage.deleteEntry(it.id) }
                    showDeleteDialog = false
                    entryToDelete = null
                    selectedDate = selectedDate // refresca la lista
                    Toast.makeText(context, "Entrada eliminada", Toast.LENGTH_SHORT).show()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    entryToDelete = null
                }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo de edición
    if (editingEntry != null) {
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text("Editar check-in") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Mood.entries.forEach { mood ->
                            val emoji = when (mood) {
                                Mood.GREAT -> "😄"
                                Mood.GOOD -> "🙂"
                                Mood.NEUTRAL -> "😐"
                                Mood.LOW -> "😔"
                                Mood.AWFUL -> "😢"
                            }
                            FilterChip(
                                selected = editMood == mood,
                                onClick = { editMood = mood },
                                label = { Text(emoji) },
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Energía: ${editEnergy.toInt()}")
                    Slider(value = editEnergy, onValueChange = { editEnergy = it }, valueRange = 1f..10f, steps = 8)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Estrés: ${editStress.toInt()}")
                    Slider(value = editStress, onValueChange = { editStress = it }, valueRange = 1f..10f, steps = 8)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text("Nota") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingEntry?.let { entry ->
                        val updated = entry.copy(
                            mood = editMood,
                            energy = editEnergy.toInt(),
                            stress = editStress.toInt(),
                            noteText = editNote.ifBlank { null }
                        )
                        storage.updateEntry(updated)
                        editingEntry = null
                        selectedDate = selectedDate // refresca
                        Toast.makeText(context, "Check-in actualizado", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { showFilters = !showFilters }) {
                        if (showFilters) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cerrar")
                        } else {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filtrar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (showFilters) {
                FilterPanel(
                    filterMood = filterMood,
                    onMoodSelected = { filterMood = it },
                    filterStartDate = filterStartDate,
                    onStartDateSelected = { filterStartDate = it },
                    filterEndDate = filterEndDate,
                    onEndDateSelected = { filterEndDate = it },
                    onClearFilters = {
                        filterMood = null
                        filterStartDate = null
                        filterEndDate = null
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Cabecera del mes con tarjeta
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (currentMonth == 0) { currentMonth = 11; currentYear-- }
                            else { currentMonth-- }
                        }) { Text("◀") }
                        Text(
                            text = "${getMonthName(currentMonth)} $currentYear",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = {
                            if (currentMonth == 11) { currentMonth = 0; currentYear++ }
                            else { currentMonth++ }
                        }) { Text("▶") }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Días de la semana
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cuadrícula del calendario
                    val calendar = Calendar.getInstance()
                    calendar.set(currentYear, currentMonth, 1)
                    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

                    var dayCounter = 1
                    for (week in 0 until 6) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (dayOfWeek in 0 until 7) {
                                val cellDay = if (week == 0 && dayOfWeek + 1 < firstDayOfWeek) null
                                else if (dayCounter > daysInMonth) null
                                else dayCounter++

                                val dotColor = cellDay?.let { dayMoodMap[it] }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(
                                            if (dotColor != null) dotColor.copy(alpha = 0.15f)
                                            else Color.Transparent
                                        )
                                        .clickable(enabled = cellDay != null) {
                                            cellDay?.let { day ->
                                                calendar.set(currentYear, currentMonth, day)
                                                selectedDate = calendar.time
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (cellDay != null) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = cellDay.toString(),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (dotColor != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (dayCounter > daysInMonth) break
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Entradas del día seleccionado
            if (selectedDate != null) {
                val entries = storage.getEntriesByDate(selectedDate!!)

                // Filtrado también para las entradas del día
                val filteredDayEntries = remember(entries, filterMood, filterStartDate, filterEndDate) {
                    val start = filterStartDate
                    val end = filterEndDate
                    entries.filter { entry ->
                        val matchesMood = filterMood == null || entry.mood == filterMood
                        val entryDate = Instant.ofEpochMilli(entry.timestamp.time)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        val startOk = start == null || !entryDate.isBefore(start)
                        val endOk = end == null || !entryDate.isAfter(end)
                        matchesMood && startOk && endOk
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Entradas del ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (filteredDayEntries.isEmpty()) {
                            Text("Sin entradas para este día (con los filtros actuales).")
                        } else {
                            LazyColumn {
                                items(filteredDayEntries) { entry ->
                                    var showMenu by remember { mutableStateOf(false) }
                                    Box {
                                        EntryCard(
                                            entry = entry,
                                            onLongClick = { showMenu = true }
                                        )
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Editar") },
                                                onClick = {
                                                    showMenu = false
                                                    editingEntry = entry
                                                    editMood = entry.mood
                                                    editEnergy = entry.energy.toFloat()
                                                    editStress = entry.stress.toFloat()
                                                    editNote = entry.noteText ?: ""
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
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Panel de filtros (CONVERSIÓN CORRECTA USANDO CALENDAR) ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterPanel(
    filterMood: Mood?,
    onMoodSelected: (Mood?) -> Unit,
    filterStartDate: LocalDate?,
    onStartDateSelected: (LocalDate?) -> Unit,
    filterEndDate: LocalDate?,
    onEndDateSelected: (LocalDate?) -> Unit,
    onClearFilters: () -> Unit
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Valor inicial para el DatePicker: hoy en la zona local
    val todayMillis = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = filterStartDate?.let { date ->
                // Convertir LocalDate a milisegundos UTC para el DatePicker
                date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } ?: todayMillis
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Usar Calendar para extraer la fecha local exacta
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = millis
                        val localDate = LocalDate.of(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                        onStartDateSelected(localDate)
                    }
                    showStartDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = filterEndDate?.let { date ->
                date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } ?: todayMillis
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = millis
                        val localDate = LocalDate.of(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                        onEndDateSelected(localDate)
                    }
                    showEndDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Filtros", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Emoción:")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Mood.entries.forEach { mood ->
                    FilterChip(
                        selected = filterMood == mood,
                        onClick = { onMoodSelected(if (filterMood == mood) null else mood) },
                        label = { Text(emojiForMood(mood)) },
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = filterStartDate?.format(DateTimeFormatter.ofPattern("dd/MM/yy")) ?: "Desde"
                    )
                }
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = filterEndDate?.format(DateTimeFormatter.ofPattern("dd/MM/yy")) ?: "Hasta"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onClearFilters) { Text("Limpiar filtros") }
        }
    }
}

// ── Tarjeta de entrada ──
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryCard(entry: Entry, onLongClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* clic normal */ },
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Ánimo: ${emojiForMood(entry.mood)}", fontWeight = FontWeight.Medium)
            Text("Energía: ${entry.energy}/10")
            Text("Estrés: ${entry.stress}/10")
            if (!entry.noteText.isNullOrBlank()) Text("Nota: ${entry.noteText}")
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(entry.timestamp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ── Funciones auxiliares ──
fun moodColor(mood: Mood): Color = when (mood) {
    Mood.GREAT -> Color(0xFF4CAF50)
    Mood.GOOD -> Color(0xFF8BC34A)
    Mood.NEUTRAL -> Color(0xFFFFC107)
    Mood.LOW -> Color(0xFFFF9800)
    Mood.AWFUL -> Color(0xFFF44336)
}

fun getMonthName(month: Int): String {
    val cal = Calendar.getInstance()
    cal.set(Calendar.MONTH, month)
    return SimpleDateFormat("MMMM", Locale("es")).format(cal.time)
}

fun emojiForMood(mood: Mood): String = when (mood) {
    Mood.GREAT -> "😄"
    Mood.GOOD -> "🙂"
    Mood.NEUTRAL -> "😐"
    Mood.LOW -> "😔"
    Mood.AWFUL -> "😢"
}