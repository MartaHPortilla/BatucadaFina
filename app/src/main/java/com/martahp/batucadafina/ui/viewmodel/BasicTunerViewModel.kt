package com.martahp.batucadafina.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel //no hace falta importar AndroidViewModel porque no necesitamos context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.martahp.batucadafina.services.audio.AudioProcessor
import com.martahp.batucadafina.services.audio.AudioTuner
import java.util.Locale //para String.format

class BasicTunerViewModel :
    ViewModel() { //no hereda de AndroidViewModel porque no necesitamos context

    //instanciamos AudioProcessor y AudioTuner
    private val audioProcessor = AudioProcessor()
    private val tuner = AudioTuner()
    private val TAG = "BasicTunerViewModel"

    //variables para promedio de frecuencias y estabilidad al mostrar en la UI
    private val frequencyHistory = mutableListOf<Double>()
    private var potentialNote: String? = null //nota candidata que estamos evaluando
    private var noteDetectionCount: Int = 0 //cuántas veces seguidas hemos detectado la potential

    companion object {
        private const val HISTORY_SIZE = 3
        private const val NOTE_STABILITY_THRESHOLD = 3 // Se necesitan 3 detecciones consecutivas para cambiar la nota en la UI
        //private const val UPDATE_DEBOUNCE_MS: Long = 150 //para no actualizar UI más rápido que x ms -->debouncing
    }

   // private var lastUiUpdateTime: Long = 0 //para el debounce de la UI

    //livedata
    private val _noteName = MutableLiveData<String>()
    val noteName: LiveData<String> get() = _noteName

    private val _rawFrequencyString = MutableLiveData<String>()
    val rawFrequencyString: LiveData<String> get() = _rawFrequencyString

    //convertimos la frecuencia y la desviación a string para la UI
    private val _detectedFrequencyString = MutableLiveData<String>()
    val detectedFrequencyString: LiveData<String> get() = _detectedFrequencyString

    private val _centsDeviationString = MutableLiveData<String>()
    val centsDeviationString: LiveData<String> get() = _centsDeviationString

    //ángulo de rotación para la aguja
    private val _needleRotation = MutableLiveData<Float>()
    val needleRotation: LiveData<Float> get() = _needleRotation

    //mensaje de estado para la UI
    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> get() = _statusMessage

    //inicializamos los livedata con sus valores por defecto
    init {
        resetDisplayData()
    }

    /**
     * Procesa la frecuencia cruda detectada por el AudioRecorder.
     * Calcula la nota, los cents y actualiza los LiveData correspondientes.
     *
     * @param detectedFrequency La frecuencia cruda en Hz ya procesada por AudioProcessor.
     */

    fun processNewFrequency(detectedFrequency: Double) { //usaremos postValue para actualizar los LiveData en el hilo principal
        if (detectedFrequency <= 0) {
            Log.w(
                TAG,
                "Frecuencia detectada no válida o cero: $detectedFrequency. Reseteando datos de display."
            )
            resetDisplayData() //resetear si la frecuencia es inválida (mediante postV).
            return
        }
        //actualizamos el LiveData con la frecuencia cruda
        _rawFrequencyString.postValue(String.format(Locale.US, "Cruda: %.2f Hz", detectedFrequency))


        //lógica de promediado de frecuencias
        frequencyHistory.add(detectedFrequency)
        if (frequencyHistory.size > HISTORY_SIZE) {
            frequencyHistory.removeAt(0) //mantenemos solo las últimas HISTORY_SIZE lecturas
        }
        //solo calcula el promedio si tenemos suficientes lecturas en el historial para evitar que una sola lectura "salte" mucho al principio.
        //esperamos a tener HISTORY_SIZE lecturas --> 3
        if (frequencyHistory.size < HISTORY_SIZE) {
            Log.d(
                TAG,
                "Esperando más lecturas para promediar. Actual: ${frequencyHistory.size}/${HISTORY_SIZE}"
            )
            return
        }
        val averagedFrequency = frequencyHistory.average()

        //incluimos log para ver el historial de frecuencias: de original a promediada
        Log.d(
            TAG,
            "Frecuencia Original: ${
                String.format(
                    Locale.US,
                    "%.2f",
                    detectedFrequency
                )
            } Hz, Promediada: ${
                String.format(
                    Locale.US,
                    "%.2f",
                    averagedFrequency
                )
            } Hz, Historial: $frequencyHistory"
        )

//        //debouncing para la actualización de la UI: evita actualizar la UI demasiado rápido --> eliminamos en favor de estabilizador de nota
//         val currentTime = System.currentTimeMillis()
//         if (currentTime - lastUiUpdateTime < UPDATE_DEBOUNCE_MS) {
//              Log.v(TAG, "Debouncing UI update.")
//              return
//         }
//         lastUiUpdateTime = currentTime

        // lógica para la estabilización de la nota

        //obtenemos la nota más cercana y su frecuencia de referencia con averageFrequency
        val (closestNoteName, closestNoteFrequency) = tuner.getClosestNote(averagedFrequency)

        if (closestNoteName == potentialNote) {
            //si detectamos la misma nota que la vez anterior, incrementamos el contador.
            noteDetectionCount++
        } else {
            //hemos detectado una nota diferente. La marcamos como la nueva "potencial"
            //y reseteamos su contador a 1.
            potentialNote = closestNoteName
            noteDetectionCount = 1
            return //hay que salir para esperar que la nueva nota se estabilice
        }

        Log.d(
            TAG,
            "Nota Potencial: $potentialNote (Conteo: $noteDetectionCount/$NOTE_STABILITY_THRESHOLD)"
        )

        //-----calculamos la diferencia en cents y la rotación de la aguja----

        //---- ángulo de rotación para la aguja
        /*
    Calculamos cuánto debe girar la aguja del afinador en la pantalla (ángulo de rotación)
    Cents responde a la desviación de la nota más cercana
    Sabemos que 50 cents es medio semitono / 100 cents es un semitono
    CoerceIn establece el límite en el ángulo de rotación, para que la aguja no se mueva más allá de los límites
    En este caso:
    - 0.9f es un factor de escala. Determina cuántos grados rota la aguja por cada cent de desviación
    - Se puede ajustar: si queremos que la aguja sea más sensible a los cambios más pequeños de frecuencia,
        habría que aumentar este número (por ejemplo, doblando el factor de escala -1.8f-, hacemos que 25 cents supongan 45 grados de rotación)
     */
        if (noteDetectionCount >= NOTE_STABILITY_THRESHOLD) {

            //safe call para evitar NullPointerException
            potentialNote?.let { stableNote ->
                val cents = tuner.calculateCentsDifference(averagedFrequency, closestNoteFrequency)
                val rotation = (cents * 0.9f).coerceIn(-45.0, 45.0).toFloat() //entre -45 y 45 grados es óptimo en este caso
                //actualizamos los LiveData usando postValue porque se llama desde el hilo de AudioRecorder
                _noteName.postValue(stableNote)
                _detectedFrequencyString.postValue(
                    String.format(
                        Locale.US,
                        "%.2f Hz",
                        averagedFrequency
                    )
                )
                _centsDeviationString.postValue(String.format(Locale.US, "%.1f cents", cents))
                _needleRotation.postValue(rotation)

                Log.d(TAG, "UI ACTUALIZADA -> Nota: $stableNote, Freq: %.2f, Cents: %.1f".format(averagedFrequency, cents))
            }

        } //else
        //si no hemos detectado la misma nota las veces que marca NOTE_STABILITY_THRESHOLD no hacemos nada con la UI
        //simplemente esperamos a la siguiente lectura hasta que encuentre una nota estable
    }

    /**
     * Resetea los valores de LiveData a su estado inicial.
     */
    fun resetDisplayData() { //usamos post por si se ejecuta desde un hilo diferente al principal
        _noteName.postValue("-")
        _detectedFrequencyString.postValue("0.00 Hz")
        _centsDeviationString.postValue("0.0 cents")
        _needleRotation.postValue(0f)
        _rawFrequencyString.postValue("Cruda: 0.00 Hz")
        frequencyHistory.clear() //limpiar el historial de frecuencias también cuando se resetea el display
        noteDetectionCount = 0 //reseteamos el contador de detección de nota
        potentialNote = null //reseteamos la nota candidata
        Log.d(TAG, "Datos de display reseteados.")
    }

    /**
     * Actualiza mensajes de estado desde la Activity.
     */
    fun updateStatus(message: String) {
        _statusMessage.value = message
    }
}