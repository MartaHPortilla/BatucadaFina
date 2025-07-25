package com.martahp.batucadafina.utils

import com.martahp.batucadafina.model.entities.NoteFrequencyEntry

/**
 * Objeto singleton
 * Clase de datos que contiene información sobre las notas musicales y sus frecuencias correspondientes.
 *
 */
object NoteData {

    //creamos un mapa de notas con sus respectivas frecuencias. La clave es el nombre de la nota y el valor es la frecuencia en Hz
    private val notesToFrequenciesMap: Map<String, Double> = mapOf(
        //octava 0
        "C0 | Do0" to 16.35,
        "C#0 | Do#0" to 17.32,
        "Db0 | Reb0" to 17.32,
        "D0 | Re0" to 18.35,
        "D#0 | Re#0" to 19.45,
        "Eb0 | Mib0" to 19.45,
        "E0 | Mi0" to 20.60,
        "F0 | Fa0" to 21.83,
        "F#0 | Fa#0" to 23.12,
        "Gb0 | Solb0" to 23.12,
        "G0 | Sol0" to 24.50,
        "G#0 | Sol#0" to 25.96,
        "Ab0 | Lab0" to 25.96,
        "A0 | La0" to 27.50,
        "A#0 | La#0" to 29.14,
        "Bb0 | Sib0" to 29.14,
        "B0 | Si0" to 30.87,

        //octava 1
        "C1 | Do1" to 32.70,
        "C#1 | Do#1" to 34.65,
        "Db1 | Reb1" to 34.65,
        "D1 | Re1" to 36.71,
        "D#1 | Re#1" to 38.89,
        "Eb1 | Mib1" to 38.89,
        "E1 | Mi1" to 41.20,
        "F1 | Fa1" to 43.65,
        "F#1 | Fa#1" to 46.25,
        "Gb1 | Solb1" to 46.25,
        "G1 | Sol1" to 49.00,
        "G#1 | Sol#1" to 51.91,
        "Ab1 | Lab1" to 51.91,
        "A1 | La1" to 55.00,
        "A#1 | La#1" to 58.27,
        "Bb1 | Sib1" to 58.27,
        "B1 | Si1" to 61.74,

        //octava 2
        "C2 | Do2" to 65.41,
        "C#2 | Do#2" to 69.30,
        "Db2 | Reb2" to 69.30,
        "D2 | Re2" to 73.42,
        "D#2 | Re#2" to 77.78,
        "Eb2 | Mib2" to 77.78,
        "E2 | Mi2" to 82.41,
        "F2 | Fa2" to 87.31,
        "F#2 | Fa#2" to 92.50,
        "Gb2 | Solb2" to 92.50,
        "G2 | Sol2" to 98.00,
        "G#2 | Sol#2" to 103.83,
        "Ab2 | Lab2" to 103.83,
        "A2 | La2" to 110.00,
        "A#2 | La#2" to 116.54,
        "Bb2 | Sib2" to 116.54,
        "B2 | Si2" to 123.47,

        //octava 3
        "C3 | Do3" to 130.81,
        "C#3 | Do#3" to 138.59,
        "Db3 | Reb3" to 138.59,
        "D3 | Re3" to 146.83,
        "D#3 | Re#3" to 155.56,
        "Eb3 | Mib3" to 155.56,
        "E3 | Mi3" to 164.81,
        "F3 | Fa3" to 174.61,
        "F#3 | Fa#3" to 185.00,
        "Gb3 | Solb3" to 185.00,
        "G3 | Sol3" to 196.00,
        "G#3 | Sol#3" to 207.65,
        "Ab3 | Lab3" to 207.65,
        "A3 | La3" to 220.00,
        "A#3 | La#3" to 233.08,
        "Bb3 | Sib3" to 233.08,
        "B3 | Si3" to 246.94,

        //octava 4 (Octava Central)
        "C4 | Do4" to 261.63, //Do central
        "C#4 | Do#4" to 277.18,
        "Db4 | Reb4" to 277.18,
        "D4 | Re4" to 293.66,
        "D#4 | Re#4" to 311.13,
        "Eb4 | Mib4" to 311.13,
        "E4 | Mi4" to 329.63,
        "F4 | Fa4" to 349.23,
        "F#4 | Fa#4" to 369.99,
        "Gb4 | Solb4" to 369.99,
        "G4 | Sol4" to 392.00,
        "G#4 | Sol#4" to 415.30,
        "Ab4 | Lab4" to 415.30,
        "A4 | La4 (A440)" to 440.00, //La central
        "A#4 | La#4" to 466.16,
        "Bb4 | Sib4" to 466.16,
        "B4 | Si4" to 493.88,

        //octava 5
        "C5 | Do5" to 523.25,
        "C#5 | Do#5" to 554.37,
        "Db5 | Reb5" to 554.37,
        "D5 | Re5" to 587.33,
        "D#5 | Re#5" to 622.25,
        "Eb5 | Mib5" to 622.25,
        "E5 | Mi5" to 659.25,
        "F5 | Fa5" to 698.46,
        "F#5 | Fa#5" to 739.99,
        "Gb5 | Solb5" to 739.99,
        "G5 | Sol5" to 783.99,
        "G#5 | Sol#5" to 830.61,
        "Ab5 | Lab5" to 830.61,
        "A5 | La5" to 880.00,
        "A#5 | La#5" to 932.33,
        "Bb5 | Sib5" to 932.33,
        "B5 | Si5" to 987.77,

        //octava 6
        "C6 | Do6" to 1046.50,
        "C#6 | Do#6" to 1108.73,
        "Db6 | Reb6" to 1108.73,
        "D6 | Re6" to 1174.66,
        "D#6 | Re#6" to 1244.51,
        "Eb6 | Mib6" to 1244.51,
        "E6 | Mi6" to 1318.51,
        "F6 | Fa6" to 1396.91,
        "F#6 | Fa#6" to 1479.98,
        "Gb6 | Solb6" to 1479.98,
        "G6 | Sol6" to 1567.98,
        "G#6 | Sol#6" to 1661.22,
        "Ab6 | Lab6" to 1661.22,
        "A6 | La6" to 1760.00,
        "A#6 | La#6" to 1864.66,
        "Bb6 | Sib6" to 1864.66,
        "B6 | Si6" to 1975.53,

        //octava 7
        "C7 | Do7" to 2093.00,
        "C#7 | Do#7" to 2217.46,
        "Db7 | Reb7" to 2217.46,
        "D7 | Re7" to 2349.32,
        "D#7 | Re#7" to 2489.02,
        "Eb7 | Mib7" to 2489.02,
        "E7 | Mi7" to 2637.02,
        "F7 | Fa7" to 2793.83,
        "F#7 | Fa#7" to 2959.96,
        "Gb7 | Solb7" to 2959.96,
        "G7 | Sol7" to 3135.96,
        "G#7 | Sol#7" to 3322.44,
        "Ab7 | Lab7" to 3322.44,
        "A7 | La7" to 3520.00,
        "A#7 | La#7" to 3729.31,
        "Bb7 | Sib7" to 3729.31,
        "B7 | Si7" to 3951.07,

        //octava 8 (normalmente el límite superior de un piano)
        "C8 | Do8" to 4186.01,
        "C#8 | Do#8" to 4434.92,
        "Db8 | Reb8" to 4434.92,
        "D8 | Re8" to 4698.63,
        "D#8 | Re#8" to 4978.03,
        "Eb8 | Mib8" to 4978.03,
        "E8 | Mi8" to 5274.04,
        "F8 | Fa8" to 5587.65,
        "F#8 | Fa#8" to 5919.91,
        "Gb8 | Solb8" to 5919.91,
        "G8 | Sol8" to 6271.93,
        "G#8 | Sol#8" to 6644.88,
        "Ab8 | Lab8" to 6644.88,
        "A8 | La8" to 7040.00,
        "A#8 | La#8" to 7458.62,
        "Bb8 | Sib8" to 7458.62,
        "B8 | Si8" to 7902.13
    )

    /**
     * Obtiene la lista de notas y sus frecuencias correspondientes.
     * Utilizada para AudioTuner
     */
    fun getNotesForTuner(): Map<String, Double> {
        return notesToFrequenciesMap
    }

    /**
     * Para FrequencyChartActivity: Devuelve la lista de objetos NoteFrequencyEntry.
     * El campo 'note' en NoteFrequencyEntry será la clave completa "NotaEN | NotaES".
     */
    fun getFrequencyChartEntries(): List<NoteFrequencyEntry> {
        return notesToFrequenciesMap.map { (note, frequency) ->
            NoteFrequencyEntry(note = note, frequency = frequency)
        }.sortedBy { it.frequency } // Ordenar por frecuencia para la tabla
    }

}

