package com.manoli.moodmate.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest

@Composable
fun LockScreen(onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("moodmate_prefs", Context.MODE_PRIVATE)

    val savedPinHash = prefs.getString("pin_hash", null)
    val isPinSet = savedPinHash != null

    var creatingPin by remember { mutableStateOf(!isPinSet) }

    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(if (creatingPin) "create" else "verify") }

    val maxLength = 4

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (creatingPin) {
            when (step) {
                "create" -> {
                    Text("Crea un PIN de 4 dígitos", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(24.dp))
                    PinDisplay(enteredPin, maxLength)
                    Spacer(modifier = Modifier.height(24.dp))
                    NumberPad(
                        onNumberClick = { number ->
                            if (enteredPin.length < maxLength) {
                                enteredPin += number
                                if (enteredPin.length == maxLength) {
                                    step = "confirm"
                                }
                            }
                        },
                        onDeleteClick = {
                            if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                        }
                    )
                }
                "confirm" -> {
                    Text("Confirma tu PIN", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(24.dp))
                    PinDisplay(confirmPin, maxLength)
                    Spacer(modifier = Modifier.height(24.dp))
                    NumberPad(
                        onNumberClick = { number ->
                            if (confirmPin.length < maxLength) {
                                confirmPin += number
                                if (confirmPin.length == maxLength) {
                                    if (confirmPin == enteredPin) {
                                        val hash = hashPin(confirmPin)
                                        prefs.edit().putString("pin_hash", hash).apply()
                                        prefs.edit().putBoolean("lock_enabled", true).apply()
                                        prefs.edit().putString("lock_type", "app_pin").apply()
                                        Toast.makeText(context, "PIN creado correctamente", Toast.LENGTH_SHORT).show()
                                        onAuthenticated()
                                    } else {
                                        Toast.makeText(context, "Los PIN no coinciden", Toast.LENGTH_SHORT).show()
                                        confirmPin = ""
                                        enteredPin = ""
                                        step = "create"
                                    }
                                }
                            }
                        },
                        onDeleteClick = {
                            if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                        }
                    )
                }
            }
        } else {
            Text("Ingresa tu PIN", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))
            PinDisplay(enteredPin, maxLength)
            Spacer(modifier = Modifier.height(24.dp))
            NumberPad(
                onNumberClick = { number ->
                    if (enteredPin.length < maxLength) {
                        enteredPin += number
                        if (enteredPin.length == maxLength) {
                            val inputHash = hashPin(enteredPin)
                            if (inputHash == savedPinHash) {
                                onAuthenticated()
                            } else {
                                Toast.makeText(context, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                                enteredPin = ""
                            }
                        }
                    }
                },
                onDeleteClick = {
                    if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                }
            )
        }
    }
}

@Composable
fun PinDisplay(pin: String, maxLength: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(maxLength) { index ->
            val filled = index < pin.length
            Surface(
                shape = CircleShape,
                color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(16.dp)
            ) {}
        }
    }
}

@Composable
fun NumberPad(onNumberClick: (String) -> Unit, onDeleteClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { label ->
                    if (label == "") {
                        Spacer(modifier = Modifier.size(64.dp))
                    } else {
                        Button(
                            onClick = {
                                if (label == "⌫") onDeleteClick() else onNumberClick(label)
                            },
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = label,
                                fontSize = if (label == "⌫") 18.sp else 24.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

fun hashPin(pin: String): String {
    val salt = "MoodMateSalt2024"
    val input = pin + salt
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(input.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}