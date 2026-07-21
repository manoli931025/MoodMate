package com.manoli.moodmate.model

import java.util.Date

data class JournalEntry(
    val id: String,
    val timestamp: Date,
    val title: String? = null,
    val content: String
)