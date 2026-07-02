package com.manoli.moodmate.service

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.manoli.moodmate.model.Entry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.*

class StorageService(private val context: Context) {
    private val gson = Gson()
    private val fileName = "entries.json"

    fun loadEntries(): MutableList<Entry> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return mutableListOf()
        val json = file.readText()
        val type = object : TypeToken<List<Entry>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveEntries(entries: List<Entry>) {
        val file = File(context.filesDir, fileName)
        val json = gson.toJson(entries)
        file.writeText(json)
    }

    fun addEntry(entry: Entry) {
        val entries = loadEntries()
        entries.add(entry)
        saveEntries(entries)
    }

    fun getEntriesByDate(date: Date): List<Entry> {
        val calendar = Calendar.getInstance()
        return loadEntries().filter { entry ->
            calendar.time = entry.timestamp
            val entryYear = calendar.get(Calendar.YEAR)
            val entryDay = calendar.get(Calendar.DAY_OF_YEAR)
            calendar.time = date
            entryYear == calendar.get(Calendar.YEAR) &&
            entryDay == calendar.get(Calendar.DAY_OF_YEAR)
        }
    }

    fun getEntriesForMonth(year: Int, month: Int): List<Entry> {
        val calendar = Calendar.getInstance()
        return loadEntries().filter { entry ->
            calendar.time = entry.timestamp
            calendar.get(Calendar.YEAR) == year &&
            calendar.get(Calendar.MONTH) == month
        }
    }

    fun getEntriesLastDays(days: Int): List<Entry> {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startDate = calendar.time
        return loadEntries().filter { entry ->
            entry.timestamp >= startDate && entry.timestamp <= today
        }
    }

    fun exportToCsv(): String {
        val entries = loadEntries()
        if (entries.isEmpty()) return "Sin datos"

        val sb = StringBuilder()
        sb.appendLine("Fecha,Ánimo,Energía,Estrés,Nota")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        for (entry in entries.sortedBy { it.timestamp }) {
            sb.appendLine(
                "${dateFormat.format(entry.timestamp)}," +
                "${entry.mood}," +
                "${entry.energy}," +
                "${entry.stress}," +
                "\"${entry.noteText ?: ""}\""
            )
        }
        return sb.toString()
    }
}