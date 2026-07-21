package com.manoli.moodmate.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.core.content.FileProvider
import androidx.work.WorkManager
import com.manoli.moodmate.service.StorageService
import com.manoli.moodmate.ui.theme.ThemeManager
import com.manoli.moodmate.util.scheduleReminderAt
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onImportCompleted: () -> Unit = {},
    onStartImport: () -> Unit = {},
    onFinishImport: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("moodmate_prefs", Context.MODE_PRIVATE)
    val storage = StorageService(context)

    var reminderEnabled by remember { mutableStateOf(prefs.getBoolean("reminder_enabled", true)) }
    val savedHour = prefs.getInt("reminder_hour", 20)
    val savedMinute = prefs.getInt("reminder_minute", 0)

    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(savedHour) }
    var selectedMinute by remember { mutableStateOf(savedMinute) }

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

    // ── Launcher de importación (con manejo completo) ──
    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            // Usuario canceló, finalizamos la importación
            onFinishImport()
            return@rememberLauncherForActivityResult
        }

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null || bytes.isEmpty()) {
                showErrorDialog(context, "El archivo está vacío o no se pudo leer.")
                onFinishImport()
                return@rememberLauncherForActivityResult
            }

            val json = String(bytes, Charsets.UTF_8)
            val count = storage.restoreFromJson(json, prefs)
            Toast.makeText(context, "Se restauraron $count entradas. Listo.", Toast.LENGTH_LONG).show()
            onImportCompleted()
        } catch (e: Exception) {
            showErrorDialog(context, "Error al restaurar: ${e.message}")
        } finally {
            // Tanto en éxito como en error, finalizamos la importación
            onFinishImport()
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
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { TimePicker(state = timePickerState) } },
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
    val lockType = prefs.getString("lock_type", "system") ?: "system"
    var selectedLockType by remember { mutableStateOf(lockType) }

    var showResetDialog by remember { mutableStateOf(false) }
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reiniciar todos los datos") },
            text = { Text("¿Estás seguro? Se eliminarán todas tus entradas, ejercicios y preferencias. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    storage.deleteAllData()
                    prefs.edit().clear().apply()
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }) { Text("Sí, eliminar todo") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancelar") } }
        )
    }

    var showImportDialog by remember { mutableStateOf(false) }
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar copia de seguridad") },
            text = { Text("¿Estás seguro? Se reemplazarán todos tus datos actuales por los del archivo de respaldo.") },
            confirmButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    // Indicar que se inicia la importación (no se perderá autenticación)
                    onStartImport()
                    openFileLauncher.launch(arrayOf("application/json"))
                }) { Text("Importar") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancelar") }
            }
        )
    }

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
                .verticalScroll(rememberScrollState())
        ) {
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
                Button(
                    onClick = {
                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            scheduleReminderAt(context, -1, -1)
                            Toast.makeText(context, "Recibirás una notificación de prueba en aproximadamente 1 minuto", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = reminderEnabled
                ) { Text("Probar ahora") }

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

            SectionCard(title = "Seguridad") {
                SettingSwitch(
                    title = "Bloquear acceso",
                    checked = lockEnabled,
                    onCheckedChange = { newValue ->
                        lockEnabled = newValue
                        prefs.edit().putBoolean("lock_enabled", newValue).apply()
                    }
                )

                if (lockEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedLockType = if (selectedLockType == "app_pin") "system" else "app_pin"
                                prefs.edit().putString("lock_type", selectedLockType).apply()
                                if (selectedLockType == "app_pin") {
                                    prefs.edit().putBoolean("lock_enabled", true).apply()
                                }
                                Toast.makeText(
                                    context,
                                    "Cambio realizado. Vuelve a abrir la app para aplicar.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tipo de bloqueo")
                        Text(
                            text = if (selectedLockType == "app_pin") "PIN de la app" else "Sistema",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    if (selectedLockType == "app_pin") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                prefs.edit().remove("pin_hash").apply()
                                prefs.edit().putString("lock_type", "app_pin").apply()
                                Toast.makeText(
                                    context,
                                    "La próxima vez que abras la app podrás crear un nuevo PIN.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Cambiar PIN") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionCard(title = "Datos") {
                SettingRow(
                    title = "Exportar copia de seguridad",
                    subtitle = "📤",
                    onClick = { exportBackup(context, storage, prefs) }
                )
                SettingRow(
                    title = "Importar copia de seguridad",
                    subtitle = "📥",
                    onClick = { showImportDialog = true }
                )
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
                SettingRow(
                    title = "Reiniciar todos los datos",
                    subtitle = "⚠️",
                    onClick = { showResetDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Exportación: guarda en filesDir y comparte con FileProvider ──
fun exportBackup(context: Context, storage: StorageService, prefs: SharedPreferences) {
    try {
        val json = storage.backupToJson(prefs)
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val file = File(backupDir, "MoodMate_backup.json")
        file.writeText(json)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Exportar copia de seguridad"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// ── Función para mostrar un diálogo de error ──
fun showErrorDialog(context: Context, message: String) {
    (context as? Activity)?.runOnUiThread {
        android.app.AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Aceptar", null)
            .show()
    }
}

// ── Componentes auxiliares ──
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