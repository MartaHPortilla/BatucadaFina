package com.martahp.batucadafina.model.entities

data class NoteFrequencyEntry(
    val note: String, //optamos por formato "A4 | LA", para notación en inglés y español
    val frequency: Double
)
