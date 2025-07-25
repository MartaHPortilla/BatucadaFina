package com.martahp.batucadafina.model.entities

data class PredefinedInstrument(
    val id: Long,
    val name: String,
    val frequency: Double,
    val information: String?,
    val iconResName: String // Nombre del recurso de la imagen para predefinidos NO NULO
)
