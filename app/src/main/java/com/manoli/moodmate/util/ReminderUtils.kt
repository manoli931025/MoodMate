package com.manoli.moodmate.util

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.manoli.moodmate.worker.DailyReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

fun scheduleReminderAt(context: Context, hour: Int, minute: Int) {
    WorkManager.getInstance(context).cancelUniqueWork("daily_reminder")

    val now = Calendar.getInstance()
    val next = Calendar.getInstance().apply {
        if (hour == -1 && minute == -1) {
            // Modo prueba: programar en 1 minuto
            add(Calendar.MINUTE, 1)
        } else {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }
    val delay = next.timeInMillis - now.timeInMillis

    val reminderRequest = OneTimeWorkRequestBuilder<DailyReminderWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .addTag("daily_reminder")
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork(
            "daily_reminder",
            ExistingWorkPolicy.REPLACE,
            reminderRequest
        )
}