package com.martahp.batucadafina.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.martahp.batucadafina.data.dao.CreatedInstrumentDAO
import com.martahp.batucadafina.model.entities.CreatedInstrument
import com.martahp.batucadafina.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException

class EditInstrumentViewModel(application: Application) : AndroidViewModel(application) {

    private val createdInstrumentDao = CreatedInstrumentDAO(application.applicationContext)
    private val TAG = "EditInstrumentVM"

    //livedatas
    private val _instrumentToEdit = MutableLiveData<CreatedInstrument?>()
    val instrumentToEdit: LiveData<CreatedInstrument?> get() = _instrumentToEdit
    private val _updateResult = MutableLiveData<Event<Boolean>>()
    val updateResult: LiveData<Event<Boolean>> get() = _updateResult
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> get() = _errorMessage

    private var originalPhotoPath: String? = null //para ver si cambia
    private var currentInstrumentOwnerId: Long = -1L //para actualización segura

    /**
     * Carga los detalles de un instrumento creado para su edición.
     */
    fun loadInstrumentDetails(instrumentId: Long, userId: Long) {
        _isLoading.value = true
        this.currentInstrumentOwnerId = userId
        Log.d(TAG, "Cargando detalles para instrumento ID: $instrumentId, usuario ID: $userId")
        viewModelScope.launch {
            val instrument = withContext(Dispatchers.IO) {
                createdInstrumentDao.getCreatedInstrumentByIdAndUser(instrumentId, userId)
            }
            if (instrument != null) {
                _instrumentToEdit.postValue(instrument)
                originalPhotoPath = instrument.photoPath //el path original
                Log.d(TAG, "Instrumento cargado: ${instrument.name}")
            } else {
                _instrumentToEdit.postValue(null)
                _errorMessage.postValue(Event("No se pudo cargar el instrumento para editar o no tienes permiso."))
                Log.w(TAG, "Instrumento no encontrado o no pertenece al usuario. ID: $instrumentId")
            }
            _isLoading.postValue(false)
        }
    }

    /**
     * Actualiza un instrumento existente.
     * Si se proporciona una newImageUri, copia la nueva imagen y actualiza el photoPath.
     * Si newImageUri es null pero se desea eliminar la foto existente, se debe pasar un empty string o null explícito a photoPathToSave.
     */
    fun updateInstrument(
        instrumentId: Long, //id del instrumento
        userId: Long,       //id del usuario propietario del instrumento
        name: String,
        frequency: Double?,
        info: String?,
        newImageUri: Uri?, //nueva imagen seleccionada (puede ser null)
        deleteCurrentImage: Boolean //flag para indicar si se debe borrar la foto actual y no poner una nueva
    ) {
        if (currentInstrumentOwnerId == -1L) {
            _errorMessage.value = Event("Error: ID de propietario de instrumento no establecido.")
            _updateResult.value = Event(false)
            return
        }

        _isLoading.value = true
        Log.d(TAG, "Iniciando actualización de instrumento ID: $instrumentId")

        viewModelScope.launch {
            var photoPathToSave = originalPhotoPath //mantener original por defecto
            var copySuccess = true //asumimos true si no hay nueva imagen

            if (deleteCurrentImage) {
                photoPathToSave = null //para borrar la foto actual
                Log.d(TAG, "Se solicitó eliminar la imagen actual.")
                if (originalPhotoPath != null) {
                    withContext(Dispatchers.IO) {
                        try {
                            File(originalPhotoPath!!).delete()
                            Log.d(TAG, "Archivo de imagen antiguo eliminado: $originalPhotoPath")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al eliminar archivo de imagen antiguo: $originalPhotoPath", e)
                        }
                    }
                }
                photoPathToSave = null //para borrar la referencia a la foto en la BD
            } else if (newImageUri != null) { //si se seleccionó una nueva imagen
                Log.d(TAG, "Copiando nueva imagen desde URI: $newImageUri")
                photoPathToSave = withContext(Dispatchers.IO) {
                    try {
                        val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(newImageUri)
                        val fileName = "instr_img_${userId}_${System.currentTimeMillis()}.jpg"
                        val outputDir = File(getApplication<Application>().filesDir, "created_instrument_images")

                        if (!outputDir.exists()) { outputDir.mkdirs() }

                        val outputFile = File(outputDir, fileName)
                        val outputStream = FileOutputStream(outputFile)

                        inputStream?.copyTo(outputStream)
                        inputStream?.close()
                        outputStream.close()
                        Log.d(TAG, "Nueva imagen copiada a: ${outputFile.absolutePath}")

                        //borramos la imagen antigua si se sube una nueva
                        if (originalPhotoPath != null && originalPhotoPath != outputFile.absolutePath) {
                            File(originalPhotoPath!!).delete()
                            Log.d(TAG, "Archivo de imagen antiguo (reemplazado) eliminado: $originalPhotoPath")
                        }

                        outputFile.absolutePath
                    } catch (e: IOException) {
                        Log.e(TAG, "Error al copiar nueva imagen: ${e.message}", e)
                        copySuccess = false
                        null //null si la copia falla
                    }
                }
                if (!copySuccess) {
                    _errorMessage.postValue(Event("Error al copiar la nueva imagen."))
                }
            }

            var updateDbSuccess = false
            if (copySuccess) { // Solo intentar actualizar BD si la gestión de imagen fue bien
                val instrumentToUpdate = CreatedInstrument(
                    id = instrumentId,
                    userId = currentInstrumentOwnerId, // Usar el ID del propietario original
                    name = name,
                    frequency = frequency ?: 0.0, // Default si es null
                    information = info?.ifEmpty { null }, // Null si está vacío
                    photoPath = photoPathToSave // El path nuevo, el original, o null si se borró
                )

                updateDbSuccess = withContext(Dispatchers.IO) {
                    Log.d(TAG, "Actualizando instrumento en BD. ID: $instrumentId, Nuevo Path: $photoPathToSave")
                    createdInstrumentDao.updateCreatedInstrument(instrumentToUpdate)
                }

                if (updateDbSuccess) {
                    Log.d(TAG, "Instrumento ID $instrumentId actualizado con éxito en la BD.")
                    // Es importante actualizar originalPhotoPath si la foto cambió y se guardó
                    originalPhotoPath = photoPathToSave
                } else {
                    Log.e(TAG, "Error al actualizar instrumento ID $instrumentId en la BD.")
                    _errorMessage.postValue(Event("No se pudo actualizar el instrumento en la base de datos."))
                }
            }
            // El resultado final depende de si la copia/borrado de imagen Y la actualización de BD fueron exitosos
            _updateResult.postValue(Event(copySuccess && updateDbSuccess))
            _isLoading.postValue(false)
            Log.d(TAG, "Proceso de actualización de instrumento finalizado. Éxito general: ${copySuccess && updateDbSuccess}")
        }
    }
}