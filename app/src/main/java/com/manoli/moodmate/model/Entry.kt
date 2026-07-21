package com.manoli.moodmate.model

import java.util.Date

data class Entry(
    val id: String,
    val timestamp: Date,
    val mood: Mood,
    val energy: Int,
    val stress: Int,
    val noteText: String? = null,
    val voiceNotePath: String? = null,
    val tags: List<String>? = null,
    val sleepHours: Double? = null,
    val dreamText: String? = null          // <-- nuevo campo
)

enum class Mood {
    GREAT, GOOD, NEUTRAL, LOW, AWFUL
}