package com.manoli.moodmate.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.manoli.moodmate.MainActivity
import com.manoli.moodmate.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("moodmate_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("reminder_enabled", true)
        if (!enabled) {
            // Si se desactivó mientras el trabajo estaba pendiente, cancelar futuros trabajos
            WorkManager.getInstance(applicationContext).cancelUniqueWork("daily_reminder")
            return Result.success()
        }

        createNotificationChannel()
        showNotification()
        scheduleNextReminder()
        return Result.success()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recordatorio diario",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Canal para el recordatorio de check‑in"
        }
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun showNotification() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("¿Cómo te sientes hoy?")
            .setContentText("Tómate un momento para registrar tu estado de ánimo.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun scheduleNextReminder() {
        val prefs = applicationContext.getSharedPreferences("moodmate_prefs", Context.MODE_PRIVATE)
        val hour = prefs.getInt("reminder_hour", 20)
        val minute = prefs.getInt("reminder_minute", 0)

        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = next.timeInMillis - now.timeInMillis

        val nextRequest = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("daily_reminder")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "daily_reminder",
                ExistingWorkPolicy.REPLACE,
                nextRequest
            )
    }

    companion object {
        const val CHANNEL_ID = "daily_reminder"
        const val NOTIFICATION_ID = 1001
    }
}