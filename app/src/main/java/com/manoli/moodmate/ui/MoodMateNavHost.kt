package com.manoli.moodmate.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MoodMateNavHost(
    onStartImport: () -> Unit = {},
    onFinishImport: () -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "checkin") {
        composable("checkin") {
            CheckinScreen(
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToStats = { navController.navigate("stats") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToExercises = { navController.navigate("exercises") },
                onNavigateToAchievements = { navController.navigate("achievements") },
                onNavigateToMedication = { navController.navigate("medication") },
                onNavigateToJournal = { navController.navigate("journal") }
            )
        }
        composable("history") {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable("stats") {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable("exercises") {
            ExercisesScreen(onBack = { navController.popBackStack() })
        }
        composable("achievements") {
            AchievementsScreen(onBack = { navController.popBackStack() })
        }
        composable("medication") {
            MedicationScreen(onBack = { navController.popBackStack() })
        }
        composable("journal") {
            JournalScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate("about") },
                onImportCompleted = {
                    navController.navigate("checkin") {
                        popUpTo("checkin") { inclusive = true }
                    }
                },
                onStartImport = onStartImport,
                onFinishImport = onFinishImport
            )
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}