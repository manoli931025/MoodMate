package com.manoli.moodmate.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    var isDark by mutableStateOf(false)
    var currentScheme by mutableStateOf(ThemeScheme.CLASSIC)
}

enum class ThemeScheme {
    CLASSIC, NATURE, OCEAN, LAVENDER, SUNSET, CARBON
}