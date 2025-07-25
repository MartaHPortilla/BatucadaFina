package com.martahp.batucadafina.model.entities

/**
 * Clase que almacena la configuración para cada instrumento.
 * @param name Nombre del instrumento.
 * @param targetFrequency Frecuencia objetivo del instrumento.
 */

data class InstrumentConfiguration(
    var name: String,
    var targetFrequency: Double
)

