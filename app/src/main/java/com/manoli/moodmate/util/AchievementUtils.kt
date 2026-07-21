package com.manoli.moodmate.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.manoli.moodmate.R

object AchievementUtils {

    private const val CHANNEL_ID = "achievements"
    private var lastNotificationId = 2000

    fun checkAndNotify(context: Context) {
        createChannel(context)

        val achievements = listOf(
            Triple("Principiante", "🌱") { val s = com.manoli.moodmate.service.StorageService(context); s.loadEntries().isNotEmpty() },
            Triple("Constante", "📅") { val p = context.getSharedPreferences("moodmate_prefs", Context.MODE_PRIVATE); p.getInt("checkin_streak", 0) >= 7 },
            Triple("Diario de gratitud", "📓") { countCompleted(context, "gratitude") >= 3 },
            Triple("Buen descanso", "😴") { val s = com.manoli.moodmate.service.StorageService(context); s.loadEntries().count { it.sleepHours != null && it.sleepHours!! > 7.0 } >= 10 },
            Triple("Maestro del ánimo", "🧠") { val s = com.manoli.moodmate.service.StorageService(context); s.loadEntries().size >= 30 },
            Triple("Guerrero del estrés", "🧘") { val s = com.manoli.moodmate.service.StorageService(context); val e = s.getEntriesLastDays(7); e.size >= 3 && e.map { it.stress }.average() < 4.0 },
            Triple("Energía máxima", "⚡") { val s = com.manoli.moodmate.service.StorageService(context); val e = s.getEntriesLastDays(7); e.size >= 3 && e.map { it.energy }.average() > 8.0 }
        )

        val prefs = context.getSharedPreferences("moodmate_prefs", Context.MODE_PRIVATE)
        val unlocked = prefs.getStringSet("unlocked_achievements", emptySet())?.toMutableSet() ?: mutableSetOf()

        for ((name, emoji, condition) in achievements) {
            if (name in unlocked) continue
            if (condition()) {
                unlocked.add(name)
                prefs.edit().putStringSet("unlocked_achievements", unlocked).apply()
                showNotification(context, name, emoji)
            }
        }
    }

    private fun showNotification(context: Context, name: String, emoji: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mood_placeholder)
            .setContentTitle("¡Logro desbloqueado! $emoji")
            .setContentText("Has conseguido: $name")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(lastNotificationId++, notification)
        } catch (e: SecurityException) {
            // permiso no concedido, ignorar
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Logros",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notificaciones de logros desbloqueados" }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun countCompleted(context: Context, exerciseId: String): Int {
        val prefs = context.getSharedPreferences("moodmate_prefs", Context.MODE_PRIVATE)
        var total = 0
        for (key in prefs.all.keys) {
            if (key.startsWith("completed_exercises_")) {
                val set = prefs.getStringSet(key, emptySet()) ?: emptySet()
                if (set.contains(exerciseId)) total++
            }
        }
        return total
    }
}