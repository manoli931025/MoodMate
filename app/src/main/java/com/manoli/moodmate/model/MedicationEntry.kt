package com.manoli.moodmate.model

import java.util.Date

data class MedicationEntry(
    val id: String,
    val timestamp: Date,
    val type: MedType,        // MEDICATION o THERAPY
    val name: String,          // nombre de la medicina o tipo de terapia
    val dose: String? = null,  // ej. "50mg", solo para medicación
    val note: String? = null
)

enum class MedType { MEDICATION, THERAPY }