package com.manoli.moodmate.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import com.manoli.moodmate.ui.theme.ThemeManager
import com.manoli.moodmate.util.scheduleReminderAt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("moodmate_prefs", Context.MODE_PRIVATE)

    var reminderEnabled by remember { mutableStateOf(prefs.getBoolean("reminder_enabled", true)) }
    val savedHour = prefs.getInt("reminder_hour", 20)
    val savedMinute = prefs.getInt("reminder_minute", 0)

    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(savedHour) }
    var selectedMinute by remember { mutableStateOf(savedMinute) }

    // Permisos de notificación
    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Necesitas habilitar las notificaciones para recibir recordatorios", Toast.LENGTH_LONG).show()
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = savedHour,
            initialMinute = savedMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Elige la hora del recordatorio") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { TimePicker(state = timePickerState) }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                    prefs.edit().putInt("reminder_hour", selectedHour).apply()
                    prefs.edit().putInt("reminder_minute", selectedMinute).apply()
                    if (reminderEnabled) scheduleReminderAt(context, selectedHour, selectedMinute)
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } }
        )
    }

    var lockEnabled by remember { mutableStateOf(prefs.getBoolean("lock_enabled", true)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Sección: Recordatorio
            SectionCard(title = "Recordatorio diario") {
                SettingSwitch(
                    title = "Activar recordatorio",
                    checked = reminderEnabled,
                    onCheckedChange = { newValue ->
                        reminderEnabled = newValue
                        prefs.edit().putBoolean("reminder_enabled", newValue).apply()
                        if (newValue) {
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            scheduleReminderAt(context, selectedHour, selectedMinute)
                        } else {
                            WorkManager.getInstance(context).cancelUniqueWork("daily_reminder")
                        }
                    }
                )
                SettingRow(
                    title = "Hora",
                    subtitle = String.format("%02d:%02d", selectedHour, selectedMinute),
                    onClick = { if (reminderEnabled) showTimePicker = true },
                    enabled = reminderEnabled
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Botón para probar el recordatorio
                Button(
                    onClick = {
                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            scheduleReminderAt(context, -1, -1) // señal para programar en 1 minuto
                            Toast.makeText(context, "Recibirás una notificación de prueba en aproximadamente 1 minuto", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = reminderEnabled
                ) {
                    Text("Probar ahora")
                }

                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "⚠️ Las notificaciones están desactivadas. Púlsame para ir a los ajustes.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.clickable {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección: Apariencia
            SectionCard(title = "Apariencia") {
                SettingSwitch(
                    title = "Modo oscuro",
                    checked = ThemeManager.isDark,
                    onCheckedChange = { newValue ->
                        ThemeManager.isDark = newValue
                        prefs.edit().putBoolean("dark_mode", newValue).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección: Seguridad
            SectionCard(title = "Seguridad") {
                SettingSwitch(
                    title = "Bloquear acceso",
                    checked = lockEnabled,
                    onCheckedChange = { newValue ->
                        lockEnabled = newValue
                        prefs.edit().putBoolean("lock_enabled", newValue).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección: Datos
            SectionCard(title = "Datos") {
                SettingRow(
                    title = "Reiniciar bienvenida",
                    subtitle = "↺",
                    onClick = {
                        prefs.edit().putBoolean("onboarding_completed", false).apply()
                        Toast.makeText(context, "El onboarding se mostrará la próxima vez que abras la app.", Toast.LENGTH_SHORT).show()
                    }
                )
                SettingRow(
                    title = "Acerca de",
                    subtitle = "ℹ️",
                    onClick = onNavigateToAbout
                )
            }
        }
    }
}

// ── Componentes auxiliares ─────────────────────────────
@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingRow(title: String, subtitle: String, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge)
    }
}