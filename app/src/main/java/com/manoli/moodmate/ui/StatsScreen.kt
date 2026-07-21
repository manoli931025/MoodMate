package com.manoli.moodmate.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.manoli.moodmate.model.Entry
import com.manoli.moodmate.model.Mood
import com.manoli.moodmate.service.StorageService
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val storage = StorageService(context)

    var selectedPeriod by remember { mutableStateOf(7) }
    val entries = remember(selectedPeriod) { storage.getEntriesLastDays(selectedPeriod) }

    // ── Datos básicos ──
    val moodValues = entries.map { moodToValue(it.mood).toFloat() }
    val energyValues = entries.map { it.energy.toFloat() }
    val stressValues = entries.map { it.stress.toFloat() }

    val averageMood = if (moodValues.isNotEmpty()) moodValues.average() else 0.0
    val averageEnergy = if (energyValues.isNotEmpty()) energyValues.average() else 0.0
    val averageStress = if (stressValues.isNotEmpty()) stressValues.average() else 0.0

    // ── Termómetro emocional (últimos 3 días vs histórico) ──
    val last3Entries = remember { storage.getEntriesLastDays(3) }
    val allEntries = remember { storage.loadEntries() }
    val emotionalStatus = remember(last3Entries, allEntries) {
        getEmotionalStatus(last3Entries, allEntries)
    }

    // ── Patrones semanales ──
    val weeklyPattern = remember(allEntries) { getWeeklyPattern(allEntries) }

    // ── Sueño (regularidad y correlación) ──
    val sleepAnalysis = remember(entries) { getSleepAnalysis(entries) }

    // ── Medicación/terapia ──
    val medEntries = remember { storage.loadMedicationEntries() }
    val medAnalysis = remember(entries, medEntries) { getMedicationAnalysis(entries, medEntries) }

    // ── Resumen inteligente ──
    val previousPeriodEntries = remember(selectedPeriod) {
        if (selectedPeriod == 7) {
            val last14 = storage.getEntriesLastDays(14)
            val last7Set = entries.map { it.timestamp.time }.toSet()
            last14.filter { it.timestamp.time !in last7Set }
        } else emptyList()
    }
    val weeklySummary = remember(entries, previousPeriodEntries) {
        generateWeeklySummary(entries, previousPeriodEntries)
    }

    // ── Insights ──
    val insights = remember(entries) { generateInsights(entries) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { exportAndShare(context, storage) }) {
                        Icon(Icons.Default.Send, contentDescription = "Exportar datos")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Selector de período
                Row {
                    listOf(7 to "1 sem", 14 to "2 sem", 30 to "1 mes").forEach { (days, label) ->
                        FilterChip(
                            selected = selectedPeriod == days,
                            onClick = { selectedPeriod = days },
                            label = { Text(label) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── 1. Termómetro emocional ──
                if (emotionalStatus.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🌡️ Termómetro emocional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(emotionalStatus)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── 2. Patrones semanales ──
                if (weeklyPattern.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📅 Patrón semanal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(weeklyPattern)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── 3. Gráficos de evolución (ánimo, energía, estrés) ──
                ChartCard("Evolución del ánimo", moodValues, averageMood, Color(0xFF4A90D9), Color(0xFF4A90D9))
                Spacer(modifier = Modifier.height(12.dp))
                ChartCard("Evolución de la energía", energyValues, averageEnergy, Color(0xFF4CAF50), Color(0xFF4CAF50))
                Spacer(modifier = Modifier.height(12.dp))
                ChartCard("Evolución del estrés", stressValues, averageStress, Color(0xFFF44336), Color(0xFFF44336))
                Spacer(modifier = Modifier.height(16.dp))

                // ── 4. Análisis de sueño ──
                if (sleepAnalysis.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("💤 Sueño y ánimo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(sleepAnalysis)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── 5. Medicación/terapia ──
                if (medAnalysis.first.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("💊 Medicación y ánimo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(medAnalysis.first)
                            // Pequeño gráfico de barras comparativo
                            if (medAnalysis.second != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                                    val (withMed, withoutMed) = medAnalysis.second!!
                                    val maxVal = maxOf(withMed, withoutMed, 5f)
                                    val barWidth = size.width / 4
                                    // Barra "Con"
                                    drawRect(
                                        Color(0xFF4CAF50),
                                        Offset(barWidth * 0.5f, size.height - (withMed / maxVal * size.height)),
                                        androidx.compose.ui.geometry.Size(barWidth, withMed / maxVal * size.height)
                                    )
                                    // Barra "Sin"
                                    drawRect(
                                        Color(0xFF9E9E9E),
                                        Offset(barWidth * 2.5f, size.height - (withoutMed / maxVal * size.height)),
                                        androidx.compose.ui.geometry.Size(barWidth, withoutMed / maxVal * size.height)
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Text("Con: ${"%.1f".format(medAnalysis.second!!.first)}", style = MaterialTheme.typography.labelSmall)
                                    Text("Sin: ${"%.1f".format(medAnalysis.second!!.second)}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── 6. Resumen inteligente ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📋 Resumen inteligente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(weeklySummary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // ── 7. Promedios y tendencia ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Resumen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Promedio ánimo:"); Text("${"%.1f".format(averageMood)} / 5.0", fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tendencia:")
                            Text(
                                when {
                                    entries.size < 2 -> "Sin datos"
                                    moodValues.last() > moodValues.first() -> "Mejorando 😊"
                                    moodValues.last() < moodValues.first() -> "Empeorando 😟"
                                    else -> "Estable 😐"
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // ── 8. Insights personalizados ──
                if (insights.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("💡 Para ti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(12.dp))
                            insights.forEach { insight ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("•", modifier = Modifier.padding(end = 8.dp))
                                    Text(insight)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Indicador de scroll
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Desliza hacia abajo para ver más",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Pastilla flotante
            if (scrollState.canScrollForward) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Desliza para ver más", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ── Componentes de gráficos ──
@Composable
fun ChartCard(title: String, values: List<Float>, average: Double, lineColor: Color, gradientColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            if (values.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    val maxY = 10f; val minY = 1f
                    val points = values.mapIndexed { index, value ->
                        Offset(
                            x = index.toFloat() / (values.size - 1).coerceAtLeast(1) * size.width,
                            y = size.height - ((value - minY) / (maxY - minY)) * size.height
                        )
                    }
                    if (points.size > 1) {
                        val fillPath = Path().apply {
                            moveTo(points.first().x, size.height)
                            for (i in points.indices) {
                                if (i == 0) lineTo(points[i].x, points[i].y)
                                else {
                                    val p0 = points[i - 1]; val p1 = points[i]
                                    cubicTo((p0.x + p1.x) / 2, p0.y, (p0.x + p1.x) / 2, p1.y, p1.x, p1.y)
                                }
                            }
                            lineTo(points.last().x, size.height); close()
                        }
                        drawPath(fillPath, Brush.verticalGradient(listOf(gradientColor.copy(alpha = 0.25f), gradientColor.copy(alpha = 0.05f))))
                    }
                    if (points.size > 1) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val p0 = points[i - 1]; val p1 = points[i]
                                cubicTo((p0.x + p1.x) / 2, p0.y, (p0.x + p1.x) / 2, p1.y, p1.x, p1.y)
                            }
                        }
                        drawPath(linePath, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    points.forEach { drawCircle(Color.White, 5f, it); drawCircle(lineColor, 3.5f, it) }
                    val avgY = size.height - ((average.toFloat() - minY) / (maxY - minY)) * size.height
                    drawLine(Color(0xFFFF9800).copy(alpha = 0.7f), Offset(0f, avgY), Offset(size.width, avgY), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("Promedio: ${"%.1f".format(average)} / 10", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text("Sin datos") }
            }
        }
    }
}

// ── Funciones analíticas ──
fun getEmotionalStatus(last3: List<Entry>, all: List<Entry>): String {
    if (last3.isEmpty() || all.size < 5) return ""
    val recentAvg = last3.map { moodToValue(it.mood) }.average()
    val overallAvg = all.map { moodToValue(it.mood) }.average()
    val diff = ((recentAvg - overallAvg) / overallAvg * 100).toInt()
    return when {
        diff > 10 -> "Tu ánimo reciente está un $diff% por encima de tu promedio. ¡Buen momento!"
        diff < -10 -> "Tu ánimo reciente está un ${-diff}% por debajo de tu promedio. Cuídate."
        else -> "Tu ánimo se mantiene estable, cerca de tu promedio."
    }
}

fun getWeeklyPattern(entries: List<Entry>): String {
    if (entries.size < 14) return ""
    val dayNames = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
    val byDay = entries.groupBy { val cal = Calendar.getInstance(); cal.time = it.timestamp; cal.get(Calendar.DAY_OF_WEEK) - 1 }
    val averages = dayNames.indices.map { idx -> byDay[idx]?.map { moodToValue(it.mood) }?.average() ?: 0.0 }
    val bestDay = averages.indexOfFirst { it == averages.maxOrNull() }
    val worstDay = averages.indexOfFirst { it == averages.minOrNull() }
    if (bestDay == worstDay) return ""
    return "Tu mejor día suele ser el ${dayNames[bestDay]} (promedio ${"%.1f".format(averages[bestDay])}) y el peor el ${dayNames[worstDay]} (${"%.1f".format(averages[worstDay])})."
}

fun getSleepAnalysis(entries: List<Entry>): String {
    val withSleep = entries.filter { it.sleepHours != null }
    if (withSleep.size < 5) return ""
    val low = withSleep.filter { it.sleepHours!! < 6.0 }
    val high = withSleep.filter { it.sleepHours!! >= 6.0 }
    val sb = StringBuilder()
    if (low.isNotEmpty() && high.isNotEmpty()) {
        val avgLow = low.map { moodToValue(it.mood) }.average()
        val avgHigh = high.map { moodToValue(it.mood) }.average()
        val diff = ((avgHigh - avgLow) / avgLow * 100).toInt()
        sb.append("Con <6 h: ánimo ${"%.1f".format(avgLow)}. Con ≥6 h: ${"%.1f".format(avgHigh)} (${if (diff > 0) "+$diff%" else "$diff%"} de cambio). ")
    }
    // Regularidad
    val hours = withSleep.map { it.sleepHours!! }
    val avg = hours.average()
    val variance = hours.map { (it - avg) * (it - avg) }.average()
    val stdDev = sqrt(variance)
    if (stdDev < 1.0) sb.append("Tienes un horario de sueño regular (desviación ${"%.1f".format(stdDev)} h), lo cual favorece la estabilidad emocional.")
    else sb.append("Tu sueño es irregular (desviación ${"%.1f".format(stdDev)} h). Intenta acostarte a la misma hora cada día.")
    return sb.toString()
}

fun getMedicationAnalysis(entries: List<Entry>, medEntries: List<com.manoli.moodmate.model.MedicationEntry>): Pair<String, Pair<Float, Float>?> {
    if (medEntries.isEmpty() || entries.size < 7) return Pair("", null)
    val daysWithMed = medEntries.map { val cal = Calendar.getInstance(); cal.time = it.timestamp; cal.get(Calendar.DAY_OF_YEAR) }.toSet()
    val withMed = entries.filter { val cal = Calendar.getInstance(); cal.time = it.timestamp; cal.get(Calendar.DAY_OF_YEAR) in daysWithMed }.map { moodToValue(it.mood) }
    val withoutMed = entries.filter { val cal = Calendar.getInstance(); cal.time = it.timestamp; cal.get(Calendar.DAY_OF_YEAR) !in daysWithMed }.map { moodToValue(it.mood) }
    if (withMed.isEmpty() || withoutMed.isEmpty()) return Pair("Sigue registrando para ver el efecto de la medicación.", null)
    val avgWith = withMed.average().toFloat()
    val avgWithout = withoutMed.average().toFloat()
    val diff = ((avgWith - avgWithout) / avgWithout * 100).toInt()
    val text = "Ánimo promedio con medicación/terapia: ${"%.1f".format(avgWith)}. Sin ella: ${"%.1f".format(avgWithout)} (${if (diff > 0) "+$diff%" else "$diff%"} de diferencia)."
    return Pair(text, Pair(avgWith, avgWithout))
}

fun generateWeeklySummary(currentEntries: List<Entry>, previousEntries: List<Entry>): String {
    if (currentEntries.isEmpty()) return "Registra al menos 7 días para ver un resumen semanal."
    val avgMood = currentEntries.map { moodToValue(it.mood) }.average()
    val avgEnergy = currentEntries.map { it.energy }.average()
    val avgStress = currentEntries.map { it.stress }.average()
    val sleepData = currentEntries.filter { it.sleepHours != null }
    val avgSleep = if (sleepData.isNotEmpty()) sleepData.map { it.sleepHours!! }.average() else Double.NaN
    val sb = StringBuilder()
    sb.append("Esta semana: ánimo ${"%.1f".format(avgMood)}/5, energía ${"%.1f".format(avgEnergy)}/10, estrés ${"%.1f".format(avgStress)}/10.")
    if (!avgSleep.isNaN()) sb.append(" Sueño promedio: ${"%.1f".format(avgSleep)}h.")
    if (previousEntries.isNotEmpty()) {
        val prevMood = previousEntries.map { moodToValue(it.mood) }.average()
        val prevEnergy = previousEntries.map { it.energy }.average()
        val prevStress = previousEntries.map { it.stress }.average()
        sb.append("\nvs. semana anterior: ")
        sb.append(if (avgMood > prevMood) "ánimo ↑" else if (avgMood < prevMood) "ánimo ↓" else "ánimo =")
        sb.append(", ")
        sb.append(if (avgEnergy > prevEnergy) "energía ↑" else if (avgEnergy < prevEnergy) "energía ↓" else "energía =")
        sb.append(", ")
        sb.append(if (avgStress > prevStress) "estrés ↑" else if (avgStress < prevStress) "estrés ↓" else "estrés =")
        sb.append(".")
    }
    return sb.toString()
}

fun generateInsights(entries: List<Entry>): List<String> {
    if (entries.size < 5) return listOf("Registra al menos 5 días para recibir consejos personalizados.")
    val insights = mutableListOf<String>()
    val avgStress = entries.map { it.stress }.average()
    val avgMood = entries.map { moodToValue(it.mood) }.average()
    val last3 = entries.takeLast(3)
    val last3Stress = last3.map { it.stress }.average()

    if (avgStress > 7.0) insights.add("Estrés alto (${"%.1f".format(avgStress)}/10). Prueba el ejercicio de respiración 4‑7‑8.")
    if (last3Stress > avgStress + 1.5) insights.add("Tu estrés ha aumentado en los últimos días. ¿Necesitas un descanso?")
    if (avgMood < 2.5) insights.add("Ánimo bajo general. Escribir un diario de gratitud podría ayudarte.")
    val sleepDays = entries.filter { it.sleepHours != null && it.sleepHours!! < 6.0 }
    if (sleepDays.size >= 3) insights.add("Dormiste poco varios días. Intenta acostarte 30 min antes.")
    if (entries.none { it.noteText?.contains("ejercicio", true) == true }) insights.add("¿Hace cuánto no te estiras? Completa un ejercicio en la sección Ejercicios.")
    if (insights.isEmpty()) insights.add("Sigue registrando. Cada día nos acercamos a descubrir tus patrones.")
    return insights
}

// ── Utilidades ──
fun moodToValue(mood: Mood): Int = when (mood) {
    Mood.GREAT -> 5; Mood.GOOD -> 4; Mood.NEUTRAL -> 3; Mood.LOW -> 2; Mood.AWFUL -> 1
}

fun exportAndShare(context: android.content.Context, storage: StorageService) {
    val csv = storage.exportToCsv()
    try {
        val cacheDir = File(context.cacheDir, "exports")
        cacheDir.mkdirs()
        val file = File(cacheDir, "moodmate_export_${System.currentTimeMillis()}.csv")
        file.writeText(csv)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Exportar historial"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error al exportar", Toast.LENGTH_SHORT).show()
    }
}