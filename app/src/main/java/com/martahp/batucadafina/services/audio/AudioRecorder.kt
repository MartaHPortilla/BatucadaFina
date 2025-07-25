package com.martahp.batucadafina.services.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import android.content.Context // Import Context
import androidx.core.content.ContextCompat // Import ContextCompat
import android.content.pm.PackageManager // Import PackageManager
import kotlin.math.abs

/**
 * Esta clase se encarga de la grabación de audio usando AudioRecord.
 * Pertenece a la capa de servicios.
 * Máquina que graba el sonido
 */

class AudioRecorder (
    private val audioProcessor: AudioProcessor,
    private val onFrequencyDetectedCallback: (detectedFrequency: Double) -> Unit, //callback para comunicar la frecuencia a la clase que está usando AudioRecorder
    private val context: Context
){
    private val TAG: String = "AudioRecorder"
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    //calculamos el tamaño del buffer en función del tamaño de la ventana de FFT
    // AudioRecord.getMinBufferSize devuelve el tamaño en BYTES.
    // Si AudioProcessor.windowSize es el número de MUESTRAS (Shorts) para la FFT,
    // y audioFormat es ENCODING_PCM_16BIT (2 bytes por Short),
    // entonces el bufferSize en bytes para AudioRecord debe ser al menos audioProcessor.windowSize * 2.
    private val bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).let { minBytes ->
        val desiredBytesForProcessor = audioProcessor.windowSize * 2 // Bytes necesarios para windowSize shorts
        val calculatedMinBufferSize = if (minBytes == AudioRecord.ERROR_BAD_VALUE || minBytes == AudioRecord.ERROR) {
            Log.w(TAG, "getMinBufferSize devolvió error, usando fallback de ${desiredBytesForProcessor * 2} bytes.")
            desiredBytesForProcessor * 2 // Un fallback grande si getMinBufferSize falla
        } else {
            minBytes
        }
        // Asegurar que el buffer sea al menos tan grande como lo que necesita el procesador,
        // y preferiblemente un múltiplo o un poco más grande.
        // Si desiredBytesForProcessor es 4096 shorts * 2 bytes/short = 8192 bytes.
        // Si calculatedMinBufferSize es, por ejemplo, 2048 bytes, necesitamos uno más grande.
        Log.d(TAG, "AudioProcessor windowSize (shorts): ${audioProcessor.windowSize}")
        Log.d(TAG, "Bytes necesarios para procesador (windowSize * 2): $desiredBytesForProcessor")
        Log.d(TAG, "Min buffer size devuelto por sistema (bytes): $calculatedMinBufferSize")

        // Usar el mayor entre el mínimo del sistema (multiplicado por un factor) y lo que necesita el procesador
        maxOf(calculatedMinBufferSize * 2, desiredBytesForProcessor).also {
            Log.d(TAG, "Tamaño final del buffer para AudioRecord (bytes): $it")
        }
    }
    private var audioRecord: AudioRecord? = null  //null porque se inicializa en startRecording
    private val isRecording = AtomicBoolean(false) //AtomicBoolean es una clase que permite acceder a una variable de manera segura desde múltiples hilos
    private var recordingThread: Thread? = null //hacemos una referencia al thread para poder detenerlo si es necesario

        fun startRecording() {
        if (isRecording.get()) { //evitar que se inicie una grabación si ya está grabando
            Log.w(TAG, "Ya está grabando.")
            return
        }
            // **Check for RECORD_AUDIO permission before initializing AudioRecord**
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permiso RECORD_AUDIO no concedido. No se puede iniciar la grabación.")
                onFrequencyDetectedCallback(0.0) //pasamos una frecuencia inválida
                return
            }

        try {
            audioRecord = AudioRecord(
                AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSizeInBytes
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Error al inicializar AudioRecord, estado: ${audioRecord?.state}")
                audioRecord?.release()
                audioRecord = null
                onFrequencyDetectedCallback(0.0) //pasamos una frecuencia inválida para indicar fallo
                return
            }

            audioRecord?.startRecording()
            isRecording.set(true)
            Log.d(TAG, "Grabación iniciada")

            recordingThread = Thread {
                val bufferShorts = ShortArray(audioProcessor.windowSize) //el tamaño debe coincidir con el tamaño de la ventana
                //umbral de amplitud para la detección de frecuencia: si la amplitud supera este valor, se considera una frecuencia válida
                val AMPLITUDE_THRESHOLD = 1000.0 //TODO: ajustar este valor para ver qué amplitud es correcta

                while (isRecording.get()) {
                    val readResult = audioRecord?.read(bufferShorts, 0, bufferShorts.size) ?: 0
                    if (readResult == audioProcessor.windowSize) { // Solo procesar si leímos un buffer completo
                        var maxAmplitudeInShort = 0
                        // Optimización: no es necesario iterar tod el buffer si readResult es menor,
                        // pero si esperamos leer siempre bufferShorts.size, esta iteración está bien.
                        for (i in 0 until readResult) { // O hasta bufferShorts.size
                            val currentAmplitude = abs(bufferShorts[i].toInt())
                            if (currentAmplitude > maxAmplitudeInShort) {
                                maxAmplitudeInShort = currentAmplitude
                            }
                        }
                        //verificamos si la amplitud supera el umbral, en cuyo caso procesamos el audio
                        if (maxAmplitudeInShort.toDouble() > AMPLITUDE_THRESHOLD) {
                            Log.d(TAG, "Amplitud ($maxAmplitudeInShort) superó umbral ($AMPLITUDE_THRESHOLD). Procesando...")
                            //val audioDataToProcess = bufferShorts.copyOfRange(0, readResult)
                            val frequency = audioProcessor.processAudio(bufferShorts, sampleRate)
                            if (frequency > 0) {
                                onFrequencyDetectedCallback(frequency)
                                Log.d(TAG, "Frecuencia enviada: $frequency Hz")
                            } else {
                                Log.d(TAG, "Frecuencia procesada no válida o cero: $frequency")
                            }
                        } else {
                            Log.d(TAG, "Amplitud ($maxAmplitudeInShort) por debajo del umbral.")
                        }

                        try {
                            Thread.sleep(50) //TODO: ajustar este valor para que la grabación no sea tan rápida
                        } catch (e: InterruptedException) {
                            Log.w(TAG, "Hilo de grabación interrumpido durante sleep.")
                            Thread.currentThread().interrupt()
                            isRecording.set(false) // Salir del bucle
                        }
                    } else if (readResult > 0 && readResult < audioProcessor.windowSize) {
                        Log.w(TAG, "Lectura parcial del buffer: $readResult shorts de ${audioProcessor.windowSize}. Descartando.")
                        // No procesar buffers parciales si tu FFT espera un tamaño fijo.
                    }
                    else if (readResult < 0) { //codigo de error
                        Log.e(TAG, "Error al leer datos de audio: $readResult")
                        isRecording.set(false) //paramos el bucle en caso de error grave
                        onFrequencyDetectedCallback(0.0)
                        break
                    }
                }
                Log.d(TAG, "Hilo de grabación finalizado.")
            }
            recordingThread?.start()

        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar grabación", e)
            audioRecord?.release()
            audioRecord = null
            isRecording.set(false)
            onFrequencyDetectedCallback(0.0)

        }
    }


    fun stopRecording() {
        if (!isRecording.get()) {
            return
        }
        isRecording.set(false)

        //si el hilo de grabación no está nulo, lo interrumpemos
        recordingThread?.interrupt()
        recordingThread = null

        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error al detener AudioRecord (stop): ${e.message}")
        }
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error al liberar recursos en AudioRecord (release): ${e.message}")
        }
        audioRecord = null
        Log.d(TAG, "Grabación detenida y recursos liberados.")
    }
}
