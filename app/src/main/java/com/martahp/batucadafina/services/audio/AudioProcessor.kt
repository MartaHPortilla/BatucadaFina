package com.martahp.batucadafina.services.audio

import android.util.Log
import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Esta clase se encarga del procesamiento de audio, incluyendo la aplicación
 * de la ventana de Hamming, la realización de la FFT y la búsqueda del pico de frecuencia.
 * //TODO: añadir comentarios sobre HPS
 * Pertenece a la capa de servicios.
 * Experto en frecuencias que analiza el sonido
 */

class AudioProcessor(val windowSize: Int = 4096) { //Hemos subido de 2048 a 4096

    //countOneBits es una función que cuenta el número de bits que están en 1 en la representación binaria de un número entero.
    //Esos numeros serán potencias de 2
    //Aporta más eficiencia al algoritmo FFT
    init {
        require(windowSize > 0 && windowSize.countOneBits() == 1) { "El tamaño de la ventana debe ser un número positivo y una potencia de 2." }
    }

    val TAG = "AudioProcessor"

    fun processAudio(audioData: ShortArray, sampleRate: Int): Double {
        if (audioData.size < windowSize) {
            Log.w(TAG, "Datos de audio insuficientes (${audioData.size}) para el tamaño de ventana ($windowSize).")
            return 0.0
        }

        val windowedData = applyHammingWindow(audioData) //aplicamos la ventana de Hamming
        val fftData = performFFT(windowedData) //fftData tiene [Re0, Im0, Re1, Im1, ...]

        //cambios: llamamos a la nueva función para encontrar el pico de frecuencia con HPS
        val peakIndex = findPeakFrequencyIndexWithHPS(fftData, sampleRate)

        //si no encuentra un pico válido, devolvemos 0
        if (peakIndex <= 0) {
            return 0.0
        }
        //la interpolación necesita al menos 3 bins para funcionar (el bin del pico y sus dos vecinos)
        if (peakIndex <= 0 || peakIndex >= (windowSize / 2) - 1) {
            return (peakIndex.toDouble() * sampleRate) / windowSize
        }
        //--- INTERPOLACIÓN DEL PICO ---
        //obtenemos magnitudes de los 3 bins relevantes (pico y sus vecinos)
        //ftData es [Re0, Im0, Re1, Im1, ...]
        //magnitud de M[k] = sqrt(fftData[2k]^2 + fftData[2k+1]^2)

        val alpha_real = fftData[2 * (peakIndex - 1)]
        val alpha_imag = fftData[2 * (peakIndex - 1) + 1]
        val alpha_mag = sqrt(alpha_real * alpha_real + alpha_imag * alpha_imag)
        val beta_real = fftData[2 * peakIndex]
        val beta_imag = fftData[2 * peakIndex + 1]
        val beta_mag = sqrt(beta_real * beta_real + beta_imag * beta_imag) //esta es maxMagnitude
        val gamma_real = fftData[2 * (peakIndex + 1)]
        val gamma_imag = fftData[2 * (peakIndex + 1) + 1]
        val gamma_mag = sqrt(gamma_real * gamma_real + gamma_imag * gamma_imag)

        //comprobamos que el denominador no sea cero o muy pequeño para evitar división por cero
        val denominator = alpha_mag - 2 * beta_mag + gamma_mag
        if (abs(denominator) < 1e-6) { // Umbral pequeño para evitar división por cero
            Log.w(TAG, "Denominador para interpolación de pico es casi cero. Devolviendo frecuencia del bin.")
            return (peakIndex.toDouble() * sampleRate) / windowSize
        }

        val p = 0.5 * (alpha_mag - gamma_mag) / denominator

        //asegurarnos de que p está en un rango razonable (a veces puede dar valores extraños si el pico no es "bonito")
        if (p < -1.0 || p > 1.0) { //rango un poco más amplio que -0.5 a 0.5 por si acaso
            Log.w(TAG, "Desplazamiento de interpolación 'p' fuera de rango esperado ($p). Devolviendo frecuencia del bin.")
            return (peakIndex.toDouble() * sampleRate) / windowSize
        }
        val interpolatedPeakIndex = peakIndex.toDouble() + p
        val refinedFrequency = (interpolatedPeakIndex * sampleRate) / windowSize

        Log.d(TAG, "Frecuencia Original del Bin $peakIndex: ${(peakIndex.toDouble() * sampleRate) / windowSize} Hz")
        Log.d(TAG, "Magnitudes para Interpolación: Alpha($alpha_mag), Beta($beta_mag), Gamma($gamma_mag)")
        Log.d(TAG, "Desplazamiento Interpolado (p): $p")
        Log.d(TAG, "Frecuencia Refinada: $refinedFrequency Hz")
        return refinedFrequency
    }

    /**
     * Aplica la ventana de Hamming a los datos de audio.
     * @param data Los datos de audio en formato Short.
     * @return Array de doubles que contiene los datos de audio con la ventana de Hamming aplicada.
     */
    private fun applyHammingWindow(data: ShortArray): DoubleArray {
        val windowedData = DoubleArray(windowSize)
        val dataSize = data.size
        val start = maxOf(0, dataSize - windowSize)
        for (i in 0 until windowSize) {
            val dataIndex = start + i
            val windowValue = 0.54 - 0.46 * cos(2 * Math.PI * i / (windowSize - 1))
            windowedData[i] = if (dataIndex < dataSize) data[dataIndex] * windowValue else 0.0
        }
        return windowedData
    }

    /**
     * Realiza la FFT en los datos de audio.
     */
    private fun performFFT(data: DoubleArray): DoubleArray {
        val fft = DoubleFFT_1D(windowSize.toLong())
        val fftData = data.copyOf()
        fft.realForward(fftData)
        return fftData
    }

    private fun findPeakFrequencyIndexWithHPS(fftData: DoubleArray, sampleRate: Int): Int {

        //preparamos el espectro. La FFT produce un espectro simétrico, por lo que solo necesitamos la mitad
        val spectrumSize = windowSize / 2

        //convierte el resultado de la FFT (Real0, Imaginario0, Real1, Imaginario1, ...) a un array de magnitudes (absoluto)
        //la magnitud de M[k] = sqrt(fftData[2k]^2 + fftData[2k+1]^2) --> raiz de la suma de los cuadrados de los componentes real e imaginario
        val magnitudes = DoubleArray(spectrumSize) { i ->
            val real = fftData[2 * i]
            val imaginary = fftData[2 * i + 1]
            sqrt(real * real + imaginary * imaginary)
        }

        //llamamos a la función de ponderación que aisla las frecuencias de los armónicos agudos
        applyFrequencyWeighting(magnitudes)

        //creamos el espectro HPS empezando con las magnitudes originales (copia) y lo iremos multiplicando por sus versiones comprimidas
        val hpsSpectrum = magnitudes.copyOf()
        val numHarmonics = 5 //el número de armónicos que queremos encontrar //todo: subimos a 5

        //multiplicamos el espectro por sus versiones comprimidas
        for (h in 2..numHarmonics) { //h es
            for (i in 0 until spectrumSize / h) {
                hpsSpectrum[i] *= magnitudes[i * h] //multiplicamos la magnitud del espectro original por la del armónico correspondiente
                //esta es la clave: si la frecuencia de i es fundamental, será reforzada por todos los armónicos, dando un producto muy alto
                //pero si i es solo un armónico, el producto será más pequeño, siendo menos significativo en comparación con el producto del fundamental
            }
        }

        //buscamos el pico en el hpsSpectrum resultante
        var maxMagnitude = 0.0
        var peakIndex = 0
        val fundamentalMinHz = 60.0
        val fundamentalMaxHz = 1200.0 //todo: ajustar si es necesario
        val MIN_HPS_PEAK_MAGNITUDE = 1e18 //umbral para el valor del HPS, Es un productop de 10^n //todo: ajustado de 1e9 1e18

        //recorremos el espectro HPS (ya calculado) para encontrar el pico de mayor magnitud
        for (i in 1 until spectrumSize) { // Empezar desde 1 para ignorar DC (dc es el bin de energía 0hz)
            val currentFreq = (i.toDouble() * sampleRate) / windowSize
            if (currentFreq in fundamentalMinHz..fundamentalMaxHz) { //si está dentro del rango y supera la magnitud máxima hasta ahora actualizamos
                if (hpsSpectrum[i] > maxMagnitude) {
                    maxMagnitude = hpsSpectrum[i]
                    peakIndex = i
                }
            }
        }

        //nos aseguramos de que el pico encontrado sea su ficientemente significativo
        if (maxMagnitude < MIN_HPS_PEAK_MAGNITUDE) {
            Log.d(TAG, "Pico HPS encontrado (Mag: $maxMagnitude) no supera el umbral. No se detectó nota clara.")
            return 0
        }

        //si el pico es válido, lo mostramos
        val detectedPeakFreq = (peakIndex.toDouble() * sampleRate) / windowSize
        Log.d(TAG, "Pico HPS encontrado en Bin $peakIndex, Freq: ${String.format("%.2f", detectedPeakFreq)} Hz, Mag HPS: $maxMagnitude")

        //indice del bin donde se ha encontrado el pico de frecuencia fundamental
        return peakIndex
    }

    //nueva función para aplicar ponderación a las frecuencias


    /**
     * Aplica una ponderación al espectro de magnitudes para dar más importancia
     * a las frecuencias bajas, ayudando a discriminar la fundamental de los armónicos.
     * @param magnitudes El array de magnitudes del espectro FFT que será modificado.
     */
    private fun applyFrequencyWeighting(magnitudes: DoubleArray) {
        val spectrumSize = magnitudes.size
        for (i in 0 until spectrumSize) {
            // Fórmula de ponderación: Disminuye linealmente.
            // El factor va de 1.0 (en 0 Hz) a 0.5 (en la frecuencia más alta).
            val weight = 1.0 - (i.toDouble() / spectrumSize.toDouble()) * 0.5
            magnitudes[i] *= weight
        }
    }


    //esta es la función anterior para encontrar el pico / SIN HPS
    private fun findPeakFrequencyIndex(fftData: DoubleArray, sampleRate: Int): Int {
        var maxMagnitude = 0.0
        var peakIndex = 0
        val fundamentalMinHz = 60.0 //ignoramos picos por debajo de una frecuencia mínima razonable para instrumentos
        val fundamentalMaxHz = 1200.0 //y muy agudos si no son de interés
        val MIN_PEAK_MAGNITUDE_THRESHOLD = 100.0 //todo: ajustar


        Log.d(TAG, "findPeakFrequencyIndex: --- Espectro FFT ---")
        for (i in 1 until windowSize / 2) {
            val real = fftData[2 * i]
            val imaginary = fftData[2 * i + 1]
            val magnitude = sqrt(real * real + imaginary * imaginary)
            val currentFreq = (i.toDouble() * sampleRate) / windowSize

//            //DEBUG: se muestra solo si la magnitud es significativa para no llenar el logcat
//            if (magnitude > 50) { //todo: ajustar para ver el log
//                Log.d("AudioProcessor", "Bin $i: Freq ${String.format("%.2f", currentFreq)} Hz, Mag ${String.format("%.2f", magnitude)}")
//            }

            //aplicamos un rango de frecuencias para encontrar la frecuencia dominante
            if (currentFreq in fundamentalMinHz..fundamentalMaxHz) {
                if (magnitude > maxMagnitude) {
                    maxMagnitude = magnitude
                    peakIndex = i
                }
            }
        }
        if (maxMagnitude < MIN_PEAK_MAGNITUDE_THRESHOLD) {
            Log.d(TAG, "Pico encontrado con magnitud $maxMagnitude no supera umbral $MIN_PEAK_MAGNITUDE_THRESHOLD. Devolviendo peakIndex 0.")
            return 0
        }

        return peakIndex
    }
}
