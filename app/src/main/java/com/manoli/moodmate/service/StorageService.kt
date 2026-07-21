package com.manoli.moodmate.service

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.manoli.moodmate.model.Entry
import com.manoli.moodmate.model.Mood
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
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

    fun deleteEntry(entryId: String) {
        val entries = loadEntries()
        entries.removeAll { it.id == entryId }
        saveEntries(entries)
    }

    fun updateEntry(updatedEntry: Entry) {
        val entries = loadEntries()
        val index = entries.indexOfFirst { it.id == updatedEntry.id }
        if (index != -1) {
            entries[index] = updatedEntry
            saveEntries(entries)
        }
    }

    fun deleteAllData() {
        val file = File(context.filesDir, fileName)
        if (file.exists()) file.delete()
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

    fun backupToJson(prefs: SharedPreferences): String {
        val entries = loadEntries()
        val root = JSONObject()
        val entriesArray = JSONArray()
        for (entry in entries) {
            val entryJson = JSONObject()
            entryJson.put("id", entry.id)
            entryJson.put("timestamp", entry.timestamp.time)
            entryJson.put("mood", entry.mood.name)
            entryJson.put("energy", entry.energy)
            entryJson.put("stress", entry.stress)
            entry.noteText?.let { entryJson.put("noteText", it) }
            entry.voiceNotePath?.let { entryJson.put("voiceNotePath", it) }
            entry.sleepHours?.let { entryJson.put("sleepHours", it) }
            entriesArray.put(entryJson)
        }
        root.put("entries", entriesArray)

        val prefsJson = JSONObject()
        prefsJson.put("reminder_enabled", prefs.getBoolean("reminder_enabled", true))
        prefsJson.put("reminder_hour", prefs.getInt("reminder_hour", 20))
        prefsJson.put("reminder_minute", prefs.getInt("reminder_minute", 0))
        prefsJson.put("dark_mode", prefs.getBoolean("dark_mode", false))
        prefsJson.put("lock_enabled", prefs.getBoolean("lock_enabled", true))
        prefsJson.put("lock_type", prefs.getString("lock_type", "system") ?: "system")
        prefsJson.put("onboarding_completed", prefs.getBoolean("onboarding_completed", true))
        prefsJson.put("exercise_streak", prefs.getInt("exercise_streak", 0))
        prefsJson.put("last_exercise_date", prefs.getString("last_exercise_date", "") ?: "")
        root.put("preferences", prefsJson)

        return root.toString(2)
    }

    fun restoreFromJson(jsonString: String, prefs: SharedPreferences): Int {
        val root = JSONObject(jsonString)
        val entriesArray = root.getJSONArray("entries")
        val restoredEntries = mutableListOf<Entry>()
        for (i in 0 until entriesArray.length()) {
            val entryJson = entriesArray.getJSONObject(i)
            val entry = Entry(
                id = entryJson.getString("id"),
                timestamp = Date(entryJson.getLong("timestamp")),
                mood = Mood.valueOf(entryJson.getString("mood")),
                energy = entryJson.getInt("energy"),
                stress = entryJson.getInt("stress"),
                noteText = entryJson.optString("noteText", null),
                voiceNotePath = entryJson.optString("voiceNotePath", null),
                sleepHours = if (entryJson.has("sleepHours")) entryJson.getDouble("sleepHours") else null
            )
            restoredEntries.add(entry)
        }
        saveEntries(restoredEntries)

        val prefsJson = root.getJSONObject("preferences")
        val editor = prefs.edit()
        for (key in prefsJson.keys()) {
            when (val value = prefsJson.get(key)) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is String -> editor.putString(key, value)
            }
        }
        editor.apply()
        return restoredEntries.size
    }
}