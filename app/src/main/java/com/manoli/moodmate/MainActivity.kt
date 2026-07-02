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
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.*
import com.manoli.moodmate.ui.MoodMateNavHost
import com.manoli.moodmate.ui.OnboardingScreen
import com.manoli.moodmate.ui.SplashScreen
import com.manoli.moodmate.ui.theme.MoodMateTheme
import com.manoli.moodmate.ui.theme.ThemeManager
import com.manoli.moodmate.util.scheduleReminderAt
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val isAuthenticated = mutableStateOf(false)
    private val showSplash = mutableStateOf(true)
    private val showOnboarding = mutableStateOf(false)
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var lockScreenLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("moodmate_prefs", MODE_PRIVATE)
        ThemeManager.isDark = prefs.getBoolean("dark_mode", false)

        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        showOnboarding.value = !onboardingCompleted

        val lockEnabled = prefs.getBoolean("lock_enabled", true)
        if (!lockEnabled) {
            isAuthenticated.value = true
        }

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

        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        val lockNow = getSharedPreferences("moodmate_prefs", MODE_PRIVATE)
                            .getBoolean("lock_enabled", true)
                        if (!lockNow) {
                            isAuthenticated.value = true
                        } else if (!showSplash.value && !showOnboarding.value && !isAuthenticated.value) {
                            launchLockScreen()
                        }
                    }
                    Lifecycle.Event.ON_STOP -> {
                        isAuthenticated.value = false
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
                            if (showOnboarding.value) {
                                // mostrar onboarding
                            } else {
                                if (!isAuthenticated.value) {
                                    val lockNow = getSharedPreferences("moodmate_prefs", MODE_PRIVATE)
                                        .getBoolean("lock_enabled", true)
                                    if (lockNow) {
                                        launchLockScreen()
                                    } else {
                                        isAuthenticated.value = true
                                    }
                                }
                            }
                        })
                    }
                    showOnboarding.value -> {
                        OnboardingScreen(onFinish = {
                            prefs.edit().putBoolean("onboarding_completed", true).apply()
                            showOnboarding.value = false
                            if (!isAuthenticated.value) {
                                val lockNow = getSharedPreferences("moodmate_prefs", MODE_PRIVATE)
                                    .getBoolean("lock_enabled", true)
                                if (lockNow) {
                                    launchLockScreen()
                                } else {
                                    isAuthenticated.value = true
                                }
                            }
                        })
                    }
                    else -> {
                        if (isAuthenticated.value) {
                            MoodMateNavHost()
                        }
                    }
                }
            }
        }

        // Programar recordatorio si está activado
        val reminderEnabled = prefs.getBoolean("reminder_enabled", true)
        if (reminderEnabled) {
            val hour = prefs.getInt("reminder_hour", 20)
            val minute = prefs.getInt("reminder_minute", 0)

            // Solicitar permiso de notificación en Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
                }
            }

            // Programar el trabajo único con REPLACE para asegurar que se ejecute
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

    private fun launchLockScreen() {
        if (keyguardManager.isKeyguardSecure) {
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