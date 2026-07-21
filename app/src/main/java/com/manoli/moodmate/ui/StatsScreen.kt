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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val storage = StorageService(context)

    var selectedPeriod by remember { mutableStateOf(7) }
    val entries = remember(selectedPeriod) { storage.getEntriesLastDays(selectedPeriod) }

    val moodValues = entries.map { moodToValue(it.mood).toFloat() }
    val averageMood = if (moodValues.isNotEmpty()) moodValues.average() else 0.0

    val energyValues = entries.map { it.energy.toFloat() }
    val stressValues = entries.map { it.stress.toFloat() }
    val averageEnergy = if (energyValues.isNotEmpty()) energyValues.average() else 0.0
    val averageStress = if (stressValues.isNotEmpty()) stressValues.average() else 0.0

    val moodTrend = when {
        entries.size < 2 -> "Sin datos suficientes"
        moodValues.last() > moodValues.first() -> "Mejorando 😊"
        moodValues.last() < moodValues.first() -> "Empeorando 😟"
        else -> "Estable 😐"
    }

    val sleepInsight = remember(entries) {
        val withSleep = entries.filter { it.sleepHours != null }
        if (withSleep.size < 2) {
            "Registra tus horas de sueño para ver cómo afectan a tu ánimo."
        } else {
            val lowSleep = withSleep.filter { it.sleepHours!! < 6.0 }
            val highSleep = withSleep.filter { it.sleepHours!! >= 6.0 }
            if (lowSleep.isEmpty() || highSleep.isEmpty()) {
                "Necesitas datos con y sin sueño suficiente (< 6 h y ≥ 6 h) para ver una correlación."
            } else {
                val avgLow = lowSleep.map { moodToValue(it.mood) }.average()
                val avgHigh = highSleep.map { moodToValue(it.mood) }.average()
                val diffPercent = ((avgHigh - avgLow) / avgLow * 100).let { "%.0f".format(it) }
                "Cuando duermes menos de 6 h, tu ánimo promedio es ${"%.1f".format(avgLow)}. " +
                "Con 6 h o más, sube a ${"%.1f".format(avgHigh)} (un $diffPercent % mejor)."
            }
        }
    }

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
        // Envolvemos todo en un Box para poder posicionar la pastilla flotante con align
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                // Gráfico de ánimo
                ChartCard(
                    title = "Evolución del ánimo",
                    values = moodValues,
                    average = averageMood,
                    lineColor = Color(0xFF4A90D9),
                    gradientColor = Color(0xFF4A90D9)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Gráfico de energía
                ChartCard(
                    title = "Evolución de la energía",
                    values = energyValues,
                    average = averageEnergy,
                    lineColor = Color(0xFF4CAF50),
                    gradientColor = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Gráfico de estrés
                ChartCard(
                    title = "Evolución del estrés",
                    values = stressValues,
                    average = averageStress,
                    lineColor = Color(0xFFF44336),
                    gradientColor = Color(0xFFF44336)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Resumen semanal inteligente
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "📋 Resumen inteligente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(weeklySummary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Resumen de promedios
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Resumen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Promedio ánimo:")
                            Text("${"%.1f".format(averageMood)} / 5.0", fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tendencia:")
                            Text(moodTrend, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "💤 Sueño y ánimo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(sleepInsight)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
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

                if (insights.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "💡 Para ti",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            insights.forEach { insight ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text("•", modifier = Modifier.padding(end = 8.dp))
                                    Text(insight)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Pastilla flotante (si hay contenido por deslizar)
            if (scrollState.canScrollForward) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)  // ahora sí está dentro de BoxScope
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Desliza para ver más",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChartCard(
    title: String,
    values: List<Float>,
    average: Double,
    lineColor: Color,
    gradientColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (values.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val maxY = 10f
                    val minY = 1f
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
                                    val cx = (p0.x + p1.x) / 2
                                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                }
                            }
                            lineTo(points.last().x, size.height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    gradientColor.copy(alpha = 0.25f),
                                    gradientColor.copy(alpha = 0.05f)
                                )
                            )
                        )
                    }

                    if (points.size > 1) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val p0 = points[i - 1]; val p1 = points[i]
                                val cx = (p0.x + p1.x) / 2
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    points.forEach { point ->
                        drawCircle(color = Color.White, radius = 5f, center = point)
                        drawCircle(color = lineColor, radius = 3.5f, center = point)
                    }

                    val avgY = size.height - ((average.toFloat() - minY) / (maxY - minY)) * size.height
                    drawLine(
                        color = Color(0xFFFF9800).copy(alpha = 0.7f),
                        start = Offset(0f, avgY),
                        end = Offset(size.width, avgY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "Promedio: ${"%.1f".format(average)} / 10",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sin datos")
                }
            }
        }
    }
}

fun generateWeeklySummary(currentEntries: List<Entry>, previousEntries: List<Entry>): String {
    if (currentEntries.isEmpty()) return "Registra datos para obtener un resumen."

    val avgMood = currentEntries.map { moodToValue(it.mood) }.average()
    val avgEnergy = currentEntries.map { it.energy }.average()
    val avgStress = currentEntries.map { it.stress }.average()
    val avgSleep = currentEntries.filter { it.sleepHours != null }.map { it.sleepHours!! }.average()

    val sb = StringBuilder()
    sb.append("En los últimos 7 días, tu ánimo promedio fue de ${"%.1f".format(avgMood)}/5.0. ")
    sb.append("Tu energía media fue de ${"%.1f".format(avgEnergy)}/10 y el estrés de ${"%.1f".format(avgStress)}/10. ")

    if (avgSleep.isNaN()) {
        sb.append("No registraste horas de sueño; añadirlas ayuda a encontrar correlaciones. ")
    } else {
        sb.append("Dormiste en promedio ${"%.1f".format(avgSleep)} horas. ")
    }

    if (previousEntries.isNotEmpty()) {
        val prevAvgMood = previousEntries.map { moodToValue(it.mood) }.average()
        val prevAvgEnergy = previousEntries.map { it.energy }.average()
        val prevAvgStress = previousEntries.map { it.stress }.average()

        sb.append("\n\nComparado con la semana anterior: ")
        if (avgMood > prevAvgMood) sb.append("tu ánimo mejoró (${"%.1f".format(prevAvgMood)} → ${"%.1f".format(avgMood)}). ")
        else if (avgMood < prevAvgMood) sb.append("tu ánimo bajó (${"%.1f".format(prevAvgMood)} → ${"%.1f".format(avgMood)}). ")
        else sb.append("tu ánimo se mantuvo igual. ")

        if (avgEnergy > prevAvgEnergy) sb.append("Tu energía aumentó. ")
        else if (avgEnergy < prevAvgEnergy) sb.append("Tu energía disminuyó. ")
        else sb.append("Tu energía no cambió. ")

        if (avgStress > prevAvgStress) sb.append("El estrés subió. ")
        else if (avgStress < prevAvgStress) sb.append("El estrés bajó. ")
        else sb.append("El estrés se mantuvo. ")
    }

    return sb.toString()
}

fun generateInsights(entries: List<Entry>): List<String> {
    if (entries.size < 3) return listOf("Registra al menos 3 días para recibir consejos personalizados.")

    val insights = mutableListOf<String>()
    val avgStress = entries.map { it.stress }.average()
    if (avgStress > 7.0) {
        insights.add("Tu estrés ha sido alto (promedio ${"%.1f".format(avgStress)}/10). Prueba el ejercicio de respiración 4-7-8 en la sección Ejercicios.")
    }
    val lowMoodDays = entries.count { it.mood == Mood.LOW || it.mood == Mood.AWFUL }
    if (lowMoodDays >= 3) {
        insights.add("Has tenido varios días con ánimo bajo. Escribir en un diario de gratitud puede ayudarte a cambiar la perspectiva.")
    }
    val lowSleepDays = entries.count { it.sleepHours != null && it.sleepHours!! < 6.0 }
    if (lowSleepDays >= 3) {
        insights.add("Has dormido menos de 6 horas varios días. Intenta establecer una rutina de sueño más constante.")
    }
    if (entries.none { it.noteText?.contains("ejercicio", true) == true }) {
        insights.add("¿Hace cuánto no haces un ejercicio de estiramiento? ¡Tu cuerpo te lo agradecerá!")
    }
    val first = moodToValue(entries.first().mood)
    val last = moodToValue(entries.last().mood)
    if (last > first) {
        insights.add("¡Tu ánimo está mejorando! Sigue con tus buenos hábitos.")
    } else if (last < first) {
        insights.add("Parece que tu ánimo está bajando. No te preocupes, es normal tener altibajos. Cuida tu descanso y habla con alguien si lo necesitas.")
    }
    if (insights.isEmpty()) {
        insights.add("Sigue registrando tus estados de ánimo y sueño para obtener consejos más personalizados.")
    }
    return insights
}

fun moodToValue(mood: Mood): Int = when (mood) {
    Mood.GREAT -> 5
    Mood.GOOD -> 4
    Mood.NEUTRAL -> 3
    Mood.LOW -> 2
    Mood.AWFUL -> 1
}

fun exportAndShare(context: android.content.Context, storage: StorageService) {
    val csv = storage.exportToCsv()
    try {
        val cacheDir = File(context.cacheDir, "exports")
        cacheDir.mkdirs()
        val file = File(cacheDir, "moodmate_export_${System.currentTimeMillis()}.csv")
        file.writeText(csv)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

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