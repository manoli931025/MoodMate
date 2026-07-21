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
import com.manoli.moodmate.model.Mood
import com.manoli.moodmate.service.StorageService
import java.text.SimpleDateFormat
import java.util.*

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val isUnlocked: (context: Context) -> Boolean,
    val progressText: (context: Context) -> String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val achievements = remember {
        listOf(
            Achievement(
                id = "first_checkin",
                title = "Principiante",
                description = "Registra tu primer check-in",
                emoji = "🌱",
                isUnlocked = { ctx ->
                    val storage = StorageService(ctx)
                    storage.loadEntries().isNotEmpty()
                },
                progressText = { ctx ->
                    val storage = StorageService(ctx)
                    if (storage.loadEntries().isEmpty()) "0/1" else "1/1"
                }
            ),
            Achievement(
                id = "streak_7",
                title = "Constante",
                description = "7 días seguidos registrando",
                emoji = "📅",
                isUnlocked = { ctx ->
                    val prefs = ctx.getSharedPreferences("moodmate_prefs", Context.MODE_PRIVATE)
                    val streak = prefs.getInt("checkin_streak", 0)
                    streak >= 7
                },
                progressText = { ctx ->
                    val prefs = ctx.getSharedPreferences("moodmate_prefs", Context.MODE_PRIVATE)
                    val streak = prefs.getInt("checkin_streak", 0)
                    "${streak}/7"
                }
            ),
            Achievement(
                id = "early_bird",
                title = "Madrugador",
                description = "5 check-ins antes de las 8 a.m.",
                emoji = "🌅",
                isUnlocked = { ctx ->
                    val storage = StorageService(ctx)
                    val entries = storage.loadEntries()
                    val cal = Calendar.getInstance()
                    entries.count { entry ->
                        cal.time = entry.timestamp
                        cal.get(Calendar.HOUR_OF_DAY) < 8
                    } >= 5
                },
                progressText = { ctx ->
                    val storage = StorageService(ctx)
                    val entries = storage.loadEntries()
                    val cal = Calendar.getInstance()
                    val count = entries.count { entry ->
                        cal.time = entry.timestamp
                        cal.get(Calendar.HOUR_OF_DAY) < 8
                    }
                    "${count}/5"
                }
            ),
            Achievement(
                id = "gratitude_3",
                title = "Diario de gratitud",
                description = "Completa 3 ejercicios de gratitud",
                emoji = "📓",
                isUnlocked = { ctx ->
                    val prefs = ctx.getSharedPreferences("moodmate_prefs", Context.MODE_PRIVATE)
                    var total = 0
                    val keys = prefs.all.keys
                    for (key in keys) {
                        if (key.startsWith("completed_exercises_")) {
                            val set = prefs.getStringSet(key, emptySet()) ?: emptySet()
                            if (set.contains("gratitude")) total++
                        }
                    }
                    total >= 3
                },
                progressText = { ctx ->
                    val prefs = ctx.getSharedPreferences("moodmate_prefs", Context.MODE_PRIVATE)
                    var total = 0
                    val keys = prefs.all.keys
                    for (key in keys) {
                        if (key.startsWith("completed_exercises_")) {
                            val set = prefs.getStringSet(key, emptySet()) ?: emptySet()
                            if (set.contains("gratitude")) total++
                        }
                    }
                    "${total}/3"
                }
            ),
            Achievement(
                id = "sleep_10",
                title = "Buen descanso",
                description = "10 días con más de 7 horas de sueño",
                emoji = "😴",
                isUnlocked = { ctx ->
                    val storage = StorageService(ctx)
                    storage.loadEntries().count { it.sleepHours != null && it.sleepHours!! > 7.0 } >= 10
                },
                progressText = { ctx ->
                    val storage = StorageService(ctx)
                    val count = storage.loadEntries().count { it.sleepHours != null && it.sleepHours!! > 7.0 }
                    "${count}/10"
                }
            ),
            Achievement(
                id = "mood_master",
                title = "Maestro del ánimo",
                description = "30 check-ins en total",
                emoji = "🧠",
                isUnlocked = { ctx ->
                    val storage = StorageService(ctx)
                    storage.loadEntries().size >= 30
                },
                progressText = { ctx ->
                    val storage = StorageService(ctx)
                    val total = storage.loadEntries().size
                    "${total}/30"
                }
            ),
            Achievement(
                id = "low_stress_7",
                title = "Guerrero del estrés",
                description = "Promedio de estrés < 4 durante 7 días",
                emoji = "🧘",
                isUnlocked = { ctx ->
                    val storage = StorageService(ctx)
                    val entries = storage.getEntriesLastDays(7)
                    if (entries.size < 3) return@Achievement false
                    entries.map { it.stress }.average() < 4.0
                },
                progressText = { ctx ->
                    val storage = StorageService(ctx)
                    val entries = storage.getEntriesLastDays(7)
                    if (entries.size < 3) "Necesitas más datos"
                    else {
                        val avg = entries.map { it.stress }.average()
                        "Promedio: ${"%.1f".format(avg)} (< 4.0)"
                    }
                }
            ),
            Achievement(
                id = "high_energy_7",
                title = "Energía máxima",
                description = "Promedio de energía > 8 durante 7 días",
                emoji = "⚡",
                isUnlocked = { ctx ->
                    val storage = StorageService(ctx)
                    val entries = storage.getEntriesLastDays(7)
                    if (entries.size < 3) return@Achievement false
                    entries.map { it.energy }.average() > 8.0
                },
                progressText = { ctx ->
                    val storage = StorageService(ctx)
                    val entries = storage.getEntriesLastDays(7)
                    if (entries.size < 3) "Necesitas más datos"
                    else {
                        val avg = entries.map { it.energy }.average()
                        "Promedio: ${"%.1f".format(avg)} (> 8.0)"
                    }
                }
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logros", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(achievements) { achievement ->
                val unlocked = achievement.isUnlocked(context)
                AchievementCard(achievement = achievement, unlocked = unlocked, progressText = achievement.progressText(context))
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, unlocked: Boolean, progressText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = achievement.emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (unlocked) "✅ Desbloqueado" else "🔒 $progressText",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}