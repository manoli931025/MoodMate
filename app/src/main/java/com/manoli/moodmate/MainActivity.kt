package com.manoli.moodmate

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.*
import com.manoli.moodmate.ui.MoodMateNavHost
import com.manoli.moodmate.ui.OnboardingScreen
import com.manoli.moodmate.ui.SplashScreen
import com.manoli.moodmate.ui.LockScreen
import com.manoli.moodmate.ui.theme.MoodMateTheme
import com.manoli.moodmate.ui.theme.ThemeManager
import com.manoli.moodmate.ui.theme.ThemeScheme
import com.manoli.moodmate.util.scheduleReminderAt
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val isAuthenticated = mutableStateOf(false)
    private val showSplash = mutableStateOf(true)
    private val showOnboarding = mutableStateOf(false)
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var lockScreenLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    var isImporting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("moodmate_prefs", MODE_PRIVATE)
        ThemeManager.isDark = prefs.getBoolean("dark_mode", false)

        // Restaurar esquema de color guardado
        val schemeName = prefs.getString("theme_scheme", "CLASSIC") ?: "CLASSIC"
        ThemeManager.currentScheme = try {
            ThemeScheme.valueOf(schemeName)
        } catch (e: Exception) {
            ThemeScheme.CLASSIC
        }

        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        showOnboarding.value = !onboardingCompleted

        val lockEnabled = prefs.getBoolean("lock_enabled", true)
        val lockType = prefs.getString("lock_type", "system") ?: "system"
        val useAppPin = lockEnabled && lockType == "app_pin"
        val useSystemLock = lockEnabled && !useAppPin

        if (useSystemLock) {
            keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            lockScreenLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    isAuthenticated.value = true
                } else {
                    finish()
                }
            }
        }

        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        if (isImporting) return
                        if (!showSplash.value && !showOnboarding.value && !isAuthenticated.value) {
                            requestAuthentication(useSystemLock, useAppPin)
                        }
                    }
                    Lifecycle.Event.ON_STOP -> {
                        if (!isImporting) {
                            isAuthenticated.value = false
                        }
                    }
                    else -> {}
                }
            }
        })

        setContent {
            MoodMateTheme {
                when {
                    showSplash.value -> {
                        SplashScreen(onSplashFinished = {
                            showSplash.value = false
                            if (!showOnboarding.value) {
                                requestAuthentication(useSystemLock, useAppPin)
                            }
                        })
                    }
                    showOnboarding.value -> {
                        OnboardingScreen(onFinish = {
                            prefs.edit().putBoolean("onboarding_completed", true).apply()
                            showOnboarding.value = false
                            requestAuthentication(useSystemLock, useAppPin)
                        })
                    }
                    else -> {
                        if (isAuthenticated.value || isImporting) {
                            MoodMateNavHost(
                                onStartImport = { isImporting = true },
                                onFinishImport = { isImporting = false }
                            )
                        } else if (!lockEnabled) {
                            MoodMateNavHost(
                                onStartImport = { isImporting = true },
                                onFinishImport = { isImporting = false }
                            )
                        } else if (useAppPin) {
                            LockScreen(onAuthenticated = {
                                isAuthenticated.value = true
                            })
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }

        // Recordatorio
        val reminderEnabled = prefs.getBoolean("reminder_enabled", true)
        if (reminderEnabled) {
            val hour = prefs.getInt("reminder_hour", 20)
            val minute = prefs.getInt("reminder_minute", 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
                }
            }
            val delay = calculateDelay(hour, minute)
            WorkManager.getInstance(this).enqueueUniqueWork(
                "daily_reminder",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<com.manoli.moodmate.worker.DailyReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag("daily_reminder")
                    .build()
            )
        } else {
            WorkManager.getInstance(this).cancelUniqueWork("daily_reminder")
        }
    }

    private fun calculateDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return next.timeInMillis - now.timeInMillis
    }

    private fun requestAuthentication(useSystemLock: Boolean, useAppPin: Boolean) {
        if (isAuthenticated.value) return
        if (useSystemLock) {
            if (::keyguardManager.isInitialized && keyguardManager.isKeyguardSecure) {
                val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    "Accede a MoodMate", "Usa tu huella o PIN del dispositivo"
                )
                if (intent != null) {
                    lockScreenLauncher.launch(intent)
                } else {
                    isAuthenticated.value = true
                }
            } else {
                isAuthenticated.value = true
            }
        }
    }
}