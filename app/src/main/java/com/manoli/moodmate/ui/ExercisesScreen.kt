package com.manoli.moodmate.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

data class Exercise(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String
)

val exercisesList = listOf(
    Exercise("breathing", "Respiración 4-7-8", "Inhala 4s, mantén 7s, exhala 8s. Repite 4 veces.", "🌬️"),
    Exercise("gratitude", "Diario de gratitud", "Escribe 3 cosas por las que estás agradecido hoy.", "📓"),
    Exercise("stretch", "Estiramiento de 2 minutos", "Estira cuello, hombros y espalda suavemente.", "🤸"),
    Exercise("affirmation", "Afirmación positiva", "Repite: 'Soy capaz y merezco bienestar'.", "💬")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("moodmate_prefs", Context.MODE_PRIVATE)

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val completedTodayKey = "completed_exercises_$today"
    val savedSet = prefs.getStringSet(completedTodayKey, emptySet()) ?: emptySet()
    var completedExercises by remember { mutableStateOf(savedSet) }

    var streak by remember { mutableStateOf(prefs.getInt("exercise_streak", 0)) }
    val lastCompletedDate = prefs.getString("last_exercise_date", null)

    LaunchedEffect(today) {
        if (lastCompletedDate != today) {
            prefs.edit().remove(completedTodayKey).apply()
            completedExercises = emptySet()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ejercicios", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
            // Tarjeta de racha
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏆", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Racha actual", fontWeight = FontWeight.SemiBold)
                        Text("$streak día(s) consecutivo(s)")
                        if (completedExercises.isNotEmpty()) {
                            Text(
                                "✅ Hoy completaste ${completedExercises.size} ejercicio(s)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de ejercicios
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(exercisesList) { exercise ->
                    val isCompleted = exercise.id in completedExercises
                    ExerciseCard(
                        exercise = exercise,
                        enabled = !isCompleted,
                        onComplete = {
                            val newSet = completedExercises + exercise.id
                            completedExercises = newSet
                            prefs.edit()
                                .putStringSet(completedTodayKey, newSet)
                                .putString("last_exercise_date", today)
                                .apply()

                            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(Date(System.currentTimeMillis() - 86400000))
                            val newStreak = if (lastCompletedDate == yesterday) streak + 1 else 1
                            prefs.edit().putInt("exercise_streak", newStreak).apply()
                            streak = newStreak
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseCard(exercise: Exercise, enabled: Boolean, onComplete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = exercise.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = exercise.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(text = exercise.description, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onComplete,
                enabled = enabled,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Hecho")
            }
        }
    }
}