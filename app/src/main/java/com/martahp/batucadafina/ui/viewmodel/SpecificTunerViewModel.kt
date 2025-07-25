package com.martahp.batucadafina.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.martahp.batucadafina.services.audio.AudioTuner

/**
 * Enum para representar de forma clara y segura el estado de la afinación
 * y comunicárselo a la Activity.
 */
enum class TuningDirection {
    TUNE_UP,    //la nota es muy grave, hay que apretar (subir tono)
    TUNE_DOWN,  //la nota es muy aguda, hay que aflojar (bajar tono)
    IN_TUNE,    //la nota está afinada dentro de un umbral aceptable
    IDLE        //estado inicial, sin procesar
}

/**
 * ViewModel para la pantalla de afinación específica.
 * Este ViewModel conoce la nota y frecuencia objetivo y calcula la desviación
 * con respecto a ese objetivo fijo.
 */
class SpecificTunerViewModel : ViewModel() {

    private val tuner = AudioTuner()
    private val TAG = "SpecificTunerViewModel"

    //variables de estado y objetivo
    private var targetNoteName: String = ""
    private var targetFrequency: Double = 0.0

    //variables para el promedio de frecuencias y estabilidad al mostrar en la UI
    private val frequencyHistory = mutableListOf<Double>()
    private var lastStableDirection: TuningDirection? = null
    private var directionDetectionCount: Int = 0

    companion object {
        private const val HISTORY_SIZE = 3 //mismo que en BasicTunerViewModel
        private const val DIRECTION_STABILITY_THRESHOLD = 3 //umbral para estabilizar las opciones "apretar" y "aflojar"
    }

    //livedata de la nota objetivo que se mostrará en pantalla
    private val _noteName = MutableLiveData<String>()
    val noteName: LiveData<String> get() = _noteName

    //liveData para la nota detectada
    private val _detectedNoteName = MutableLiveData<String>()
    val detectedNoteName: LiveData<String> get() = _detectedNoteName

    //nueva LiveData que indicará la dirección de afinación.
    private val _tuningDirection = MutableLiveData<TuningDirection>()
    val tuningDirection: LiveData<TuningDirection> get() = _tuningDirection

    //liveData para los cents de desviación y la rotación de la aguja.
    private val _centsDeviation = MutableLiveData<Double>()
    val centsDeviation: LiveData<Double> get() = _centsDeviation

    //liveData para mostrar mensajes de estado (opcional, pero consistente con tu otro ViewModel).
    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> get() = _statusMessage

    /**
     * Inicializa el ViewModel con la nota y frecuencia objetivo.
     * Debe ser llamado desde la Activity después de crear el ViewModel.
     */
    fun setTargetFrequency(noteName: String, frequency: Double) {
        this.targetFrequency = frequency
        this.targetNoteName = noteName
        Log.d(TAG, "Nota objetivo establecida: $targetNoteName a $targetFrequency Hz")
        resetDisplayData()
    }

    /**
     * Procesa la frecuencia cruda detectada y la compara con la frecuencia OBJETIVO.
     * @param detectedFrequency La frecuencia detectada por el AudioRecorder.
     */
    fun processNewFrequency(detectedFrequency: Double) {
        //si no hay una frecuencia válida o no se ha establecido un objetivo, no hacemos nada.
        if (detectedFrequency <= 0.0 || targetFrequency <= 0.0) {
            Log.w(
                TAG,
                "Frecuencia detectada no válida o cero: $detectedFrequency. Reseteando datos de display."
            )
            resetDisplayData()
            return
        }

        //lógica de promediado de frecuencias
        frequencyHistory.add(detectedFrequency)
        if (frequencyHistory.size > HISTORY_SIZE) {
            frequencyHistory.removeAt(0) //mantenemos solo las últimas HISTORY_SIZE lecturas
        }
        if (frequencyHistory.size < HISTORY_SIZE) {
            Log.d(TAG, "Esperando más lecturas para promediar. Actual: ${frequencyHistory.size}/${HISTORY_SIZE}")
            return
        }
        val averagedFrequency = frequencyHistory.average()
        Log.d(TAG, "Frecuencia Original: ${String.format("%.2f", detectedFrequency)} Hz, Promediada: ${String.format("%.2f", averagedFrequency)} Hz, Historial: $frequencyHistory")

        //nota detectada
        // obtenemos la nota más cercana a la frecuencia que estamos escuchando
        val (closestNoteName, _) = tuner.getClosestNote(averagedFrequency)
        //lo mostramos en la UI
        _detectedNoteName.postValue("Detectando: $closestNoteName")

        //calculamos la diferencia en cents contra la frecuencia objetivo.
        val cents = tuner.calculateCentsDifference(averagedFrequency, targetFrequency)

        // Determina la dirección de la afinación basándose en la desviación.
        val currentTuningDirection = when {
            cents > 5.0 -> TuningDirection.TUNE_DOWN  //muy agudo -> aflojar
            cents < -5.0 -> TuningDirection.TUNE_UP   //muy grave -> apretar
            else -> TuningDirection.IN_TUNE           //dentro del umbral de +/- 5 cents
        }

        //estabilidad de dirección
        if (currentTuningDirection == lastStableDirection) {
            directionDetectionCount++
        } else {
            lastStableDirection = currentTuningDirection
            directionDetectionCount = 1
        }

        Log.d(TAG, "Dirección Potencial: $currentTuningDirection (Conteo: $directionDetectionCount/$DIRECTION_STABILITY_THRESHOLD)")


        //si hemos detectado una dirección estable, actualizamos los LiveData
        if (directionDetectionCount >= DIRECTION_STABILITY_THRESHOLD) {
            _centsDeviation.postValue(cents)
            _tuningDirection.postValue(currentTuningDirection)
            Log.d(TAG, "UI ACTUALIZADA -> Dirección: $currentTuningDirection, Cents: %.1f".format(cents))

        }
    }

    /**
     * Resetea los valores de LiveData a su estado inicial, mostrando la nota objetivo.
     */
    fun resetDisplayData() {
        _noteName.postValue(targetNoteName)
        _detectedNoteName.postValue("")
        _centsDeviation.postValue(0.0)
        _tuningDirection.postValue(TuningDirection.IDLE)
        _statusMessage.postValue("Listo para afinar")
        frequencyHistory.clear()
        lastStableDirection = null
        directionDetectionCount = 0
        Log.d(TAG, "Datos de display reseteados.")
    }

    /**
     * Actualiza mensajes de estado desde la Activity.
     */
    fun updateStatus(message: String) {
        _statusMessage.postValue(message)
    }
}