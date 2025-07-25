package com.martahp.batucadafina.model.entities

data class CreatedInstrument(
    val id: Long,
    val name: String,
    val frequency: Double,
    val information: String?, // Puede ser null si no se proporciona
    val userId: Long,// Relación con el usuario al que pertenece el instrumento
    val photoPath: String? // Ruta/URI de la imagen OPCIONAL
)
