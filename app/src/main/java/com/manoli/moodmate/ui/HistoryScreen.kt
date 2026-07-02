package com.manoli.moodmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val storage = StorageService(context)

    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }

    var showFilters by remember { mutableStateOf(false) }
    var filterMood by remember { mutableStateOf<Mood?>(null) }
    var filterStartDate by remember { mutableStateOf<Date?>(null) }
    var filterEndDate by remember { mutableStateOf<Date?>(null) }

    val allEntriesForMonth = remember(currentYear, currentMonth) {
        storage.getEntriesForMonth(currentYear, currentMonth)
    }

    val filteredEntries = remember(allEntriesForMonth, filterMood, filterStartDate, filterEndDate) {
        allEntriesForMonth.filter { entry ->
            val matchesMood = filterMood == null || entry.mood == filterMood
            val afterStart = filterStartDate == null || !entry.timestamp.before(filterStartDate)
            val beforeEnd = filterEndDate == null || !entry.timestamp.after(filterEndDate)
            matchesMood && afterStart && beforeEnd
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
                    TextButton(
                        onClick = { showFilters = !showFilters }
                    ) {
                        if (showFilters) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cerrar")
                        } else {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
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
                val filteredDayEntries = entries.filter { entry ->
                    val matchesMood = filterMood == null || entry.mood == filterMood
                    val afterStart = filterStartDate == null || !entry.timestamp.before(filterStartDate)
                    val beforeEnd = filterEndDate == null || !entry.timestamp.after(filterEndDate)
                    matchesMood && afterStart && beforeEnd
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
                                    EntryCard(entry)
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

// ── Panel de filtros ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterPanel(
    filterMood: Mood?,
    onMoodSelected: (Mood?) -> Unit,
    filterStartDate: Date?,
    onStartDateSelected: (Date?) -> Unit,
    filterEndDate: Date?,
    onEndDateSelected: (Date?) -> Unit,
    onClearFilters: () -> Unit
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = filterStartDate?.time ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onStartDateSelected(Date(millis))
                    }
                    showStartDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = filterEndDate?.time ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onEndDateSelected(Date(millis))
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
                        text = filterStartDate?.let {
                            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(it)
                        } ?: "Desde"
                    )
                }
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = filterEndDate?.let {
                            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(it)
                        } ?: "Hasta"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onClearFilters) { Text("Limpiar filtros") }
        }
    }
}

// ── Componente de tarjeta de entrada ─────────────────────
@Composable
fun EntryCard(entry: Entry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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

// ── Funciones auxiliares ─────────────────────────────────
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