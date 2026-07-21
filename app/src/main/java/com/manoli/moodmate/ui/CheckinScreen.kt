package com.manoli.moodmate.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.manoli.moodmate.R
import com.manoli.moodmate.model.Entry
import com.manoli.moodmate.model.Mood
import com.manoli.moodmate.service.StorageService
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinScreen(
    onNavigateToHistory: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToExercises: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {}
) {
    val context = LocalContext.current
    val storage = StorageService(context)

    var selectedMood by remember { mutableStateOf(Mood.NEUTRAL) }
    var energy by remember { mutableFloatStateOf(5f) }
    var stress by remember { mutableFloatStateOf(5f) }
    var noteText by remember { mutableStateOf("") }
    var sleepHoursText by remember { mutableStateOf("") }

    var showSuccess by remember { mutableStateOf(false) }
    val scaleAnimation = remember { Animatable(0f) }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            scaleAnimation.snapTo(0f)
            scaleAnimation.animateTo(1.2f, animationSpec = tween(300, easing = FastOutSlowInEasing))
            scaleAnimation.animateTo(1f, animationSpec = tween(200))
            delay(1000)
            showSuccess = false
        }
    }

    // Colores del tema (se adaptan a claro/oscuro y al esquema seleccionado)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val backgroundColor = MaterialTheme.colorScheme.background

    // ── Cita diaria ──
    val quote = remember { getDailyQuote(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("MoodMate", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = onNavigateToAchievements) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = "Logros")
                        }
                        IconButton(onClick = onNavigateToExercises) {
                            Icon(Icons.Default.Favorite, contentDescription = "Ejercicios")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                        }
                        IconButton(onClick = onNavigateToStats) {
                            Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Estadísticas")
                        }
                        IconButton(onClick = onNavigateToHistory) {
                            Icon(Icons.Default.DateRange, contentDescription = "Historial")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(backgroundColor)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Cita diaria
                if (quote.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = primaryColor.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💬", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = quote,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Tarjeta de emociones
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "¿Cómo te sientes?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Mood.entries.forEach { mood ->
                                val emoji = when (mood) {
                                    Mood.GREAT -> "😄"
                                    Mood.GOOD -> "🙂"
                                    Mood.NEUTRAL -> "😐"
                                    Mood.LOW -> "😔"
                                    Mood.AWFUL -> "😢"
                                }
                                FilterChip(
                                    selected = selectedMood == mood,
                                    onClick = { selectedMood = mood },
                                    label = { Text(emoji, style = MaterialTheme.typography.headlineMedium) },
                                    modifier = Modifier.size(56.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                                        selectedLabelColor = primaryColor
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta de energía y estrés
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Energía", fontWeight = FontWeight.Medium, color = onSurfaceColor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = energy,
                                onValueChange = { energy = it },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = primaryColor,
                                    activeTrackColor = primaryColor,
                                    inactiveTrackColor = primaryColor.copy(alpha = 0.2f)
                                )
                            )
                            Text(
                                "${energy.toInt()}/10",
                                modifier = Modifier.padding(start = 8.dp),
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Estrés", fontWeight = FontWeight.Medium, color = onSurfaceColor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = stress,
                                onValueChange = { stress = it },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = secondaryColor,
                                    activeTrackColor = secondaryColor,
                                    inactiveTrackColor = secondaryColor.copy(alpha = 0.2f)
                                )
                            )
                            Text(
                                "${stress.toInt()}/10",
                                modifier = Modifier.padding(start = 8.dp),
                                color = secondaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta de notas y sueño
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("¿Qué está pasando? (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                cursorColor = primaryColor
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = sleepHoursText,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    sleepHoursText = newValue
                                }
                            },
                            label = { Text("Horas de sueño (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                cursorColor = primaryColor
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botón Guardar con color sólido y sin degradado
                Button(
                    onClick = {
                        val sleepHours = sleepHoursText.toDoubleOrNull()
                        val entry = Entry(
                            id = UUID.randomUUID().toString(),
                            timestamp = Date(),
                            mood = selectedMood,
                            energy = energy.toInt(),
                            stress = stress.toInt(),
                            noteText = noteText.ifBlank { null },
                            sleepHours = sleepHours
                        )
                        storage.addEntry(entry)
                        Toast.makeText(context, "Check-in guardado", Toast.LENGTH_SHORT).show()
                        showSuccess = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(8.dp, RoundedCornerShape(25.dp)),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Guardar Check-in",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Espacio extra al final para que el botón no quede pegado al borde
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Animación de éxito
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scaleAnimation.value)
                        .clip(CircleShape)
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Guardado",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

// ── Función para obtener la cita del día ──
fun getDailyQuote(context: Context): String {
    val calendar = Calendar.getInstance()
    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
    try {
        val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.quotes)))
        val quotes = reader.readLines().filter { it.isNotBlank() }
        reader.close()
        if (quotes.isEmpty()) return ""
        val index = dayOfYear % quotes.size
        return quotes[index]
    } catch (e: Exception) {
        return "“Vive el momento.”"
    }
}