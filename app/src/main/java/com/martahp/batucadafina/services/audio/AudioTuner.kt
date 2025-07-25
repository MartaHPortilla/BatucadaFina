package com.martahp.batucadafina.services.audio

import com.martahp.batucadafina.utils.NoteData
import kotlin.math.abs
import kotlin.math.log2




/**
 * clase de utilidad diseñada para la detección de notas musicales y el análisis de frecuencias.
 *
 * Proporciona funcionalidades para identificar la nota musical más cercana a una frecuencia dada
 * y calcular la diferencia en cents entre una frecuencia objetivo y la frecuencia de referencia de una nota.
 * La clase cubre un amplio rango de notas musicales desde la octava 1 hasta la octava 6 (C1 a C6).
 *
 * La clase contiene un mapa predefinido de notas con sus frecuencias.
 *
 * Ejemplo de uso:
 *
 * ```
 * val tuner = TunerGem()
 * val (note, frequency) = tuner.getClosestNote(442.0) // Encuentra la nota más cercana a 442 Hz
 * println("Nota más cercana: $note, Frecuencia: $frequency") // Salida: Nota más cercana: A4, Frecuencia: 440.0
 * val cents = tuner.calculateCentsDifference(442.0, 440.0) // Calcula la diferencia en cents entre 442 Hz y 440Hz
 * println("Diferencia en cents: $cents") // Salida: Diferencia en cents: 7.824054131467183
 * ```
 *
 * @property notes Un mapa que contiene los nombres de las notas musicales (ej., "C4", "G#5") como claves y sus frecuencias correspondientes (en Hz) como valores.
 *                 Las notas están definidas desde la octava 1 hasta la octava 6.
 *
 */

//hemos ampliado el rango de la octava 1 a la octava 6
class AudioTuner {

    //definimos una variable privada para el mapa de notas, obtenido de NoteData
    private val notesFrequencies: Map<String, Double> = NoteData.getNotesForTuner()

    /**
     * Encuentra la nota musical más cercana y su frecuencia a una frecuencia dada.
     *
     */
    /**
     * Busca en la tabla y devuelve la nota estándar más cercana a una frecuencia dada.
     * @param frequency La frecuencia detectada en Hz.
     * @return Un par (Pair) que contiene el nombre de la nota más cercana y su frecuencia exacta.
     */
    fun getClosestNote(frequency: Double): Pair<String, Double> {
        if (frequency <= 0) return Pair("-", 0.0)

        // La lógica de búsqueda no cambia, solo la fuente de 'noteFrequencies'.
        val closestEntry = notesFrequencies.entries.minByOrNull { abs(it.value - frequency) }

        return closestEntry?.let { Pair(it.key, it.value) } ?: Pair("-", 0.0)
    }

    /**
     * Calcula la diferencia en cents entre una frecuencia objetivo y la frecuencia de referencia de una nota.
     *
     * Los cents son una unidad logarítmica para medir intervalos musicales muy pequeños. 100 cents equivalen a un semitono.
     *
     * @param targetFrequency La frecuencia detectada (en Hz).
     * @param closestNoteFrequency La frecuencia de referencia de la nota más cercana (en Hz).
     * @return La diferencia en cents (un valor positivo indica que la frecuencia objetivo es más aguda, negativo más grave).
     */

    //añadido para mejorar la precisión de la frecuencia detectada mediante el cálculo de la diferencia en cents
    fun calculateCentsDifference(targetFrequency: Double, closestNoteFrequency: Double): Double {
        if (targetFrequency <= 0.0 || closestNoteFrequency <= 0.0) {
            return 0.0 // Si alguna frecuencia es 0, la diferencia es 0 cents
        }
        // Fórmula para convertir la diferencia de frecuencias a cents
        return 1200 * kotlin.math.log2(targetFrequency / closestNoteFrequency)
    }

    /**
     * Devuelve el nombre de la nota estándar más cercana a una frecuencia dada.
     * Es ideal para saber el nombre de una frecuencia objetivo.
     *
     * @param frequency La frecuencia en Hz.
     * @return El nombre de la nota más cercana (ej. "A#4 | La#4").
     */
    /**
     * Devuelve el nombre de la nota estándar más cercana a una frecuencia dada.
     * @param frequency La frecuencia en Hz.
     * @return El nombre de la nota más cercana (ej. "A4 | La4").
     */
    fun getNoteNameForFrequency(frequency: Double): String {
        if (frequency <= 0) return "-"

        // Usamos la misma lógica que antes, ahora funcionará porque 'noteFrequencies' existe.
        val closestEntry = notesFrequencies.entries.minByOrNull { abs(it.value - frequency) }

        return closestEntry?.key ?: "-"
    }
}