package com.manoli.moodmate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.manoli.moodmate.MainActivity
import com.manoli.moodmate.R
import com.manoli.moodmate.model.Mood
import com.manoli.moodmate.service.StorageService

class MoodMateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Obtener último check‑in
            val storage = StorageService(context)
            val entries = storage.loadEntries()
            if (entries.isNotEmpty()) {
                val last = entries.last()
                val emoji = when (last.mood) {
                    Mood.GREAT -> "😄"
                    Mood.GOOD -> "🙂"
                    Mood.NEUTRAL -> "😐"
                    Mood.LOW -> "😔"
                    Mood.AWFUL -> "😢"
                }
                views.setTextViewText(R.id.widget_mood_emoji, emoji)
                views.setTextViewText(R.id.widget_energy, "⚡ ${last.energy}/10")
                views.setTextViewText(R.id.widget_stress, "🧘 ${last.stress}/10")

                // Mostrar horas de sueño si existen
                if (last.sleepHours != null) {
                    views.setTextViewText(R.id.widget_sleep, "💤 ${last.sleepHours}h")
                } else {
                    views.setTextViewText(R.id.widget_sleep, "")
                }

                // Mostrar nota truncada si existe
                if (!last.noteText.isNullOrBlank()) {
                    views.setTextViewText(R.id.widget_note, last.noteText)
                } else {
                    views.setTextViewText(R.id.widget_note, "")
                }
            } else {
                views.setTextViewText(R.id.widget_mood_emoji, "🧠")
                views.setTextViewText(R.id.widget_energy, "Sin datos")
                views.setTextViewText(R.id.widget_stress, "")
                views.setTextViewText(R.id.widget_sleep, "")
                views.setTextViewText(R.id.widget_note, "")
            }

            // Configurar el clic para abrir la app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}