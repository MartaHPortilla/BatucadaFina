package com.martahp.batucadafina.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.martahp.batucadafina.data.dao.CreatedInstrumentDAO
import com.martahp.batucadafina.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException

class CreateInstrumentViewModel(application: Application) : AndroidViewModel(application) {

    private val createdInstrumentDao = CreatedInstrumentDAO(application.applicationContext)
    private val TAG = "CreateInstrumentVM"

    //livedatas: para guardar instrumento, carga y mensajes de error
    private val _instrumentSaveResult = MutableLiveData<Event<Boolean>>()
    val instrumentSaveResult: LiveData<Event<Boolean>> get() = _instrumentSaveResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> get() = _errorMessage

    /**
     * Guarda un nuevo instrumento creado por el usuario.
     * Si se proporciona una imageUri, la copia al almacenamiento interno antes de guardarla en la BD.
     */
    fun saveNewInstrument(
        userId: Long,
        name: String,
        frequency: Double?,
        info: String?,
        imageUri: Uri?
    ) {
        _isLoading.value = true
        Log.d(TAG, "Guardando nuevo instrumento para userId: $userId, nombre: $name")

        viewModelScope.launch {
            var finalPhotoPath: String? = null
            var operationSuccess = false

            try {
                // 1. Si hay una imageUri, copiarla al almacenamiento interno
                if (imageUri != null) {
                    finalPhotoPath = withContext(Dispatchers.IO) {
                        Log.d(TAG, "Copiando imagen desde URI: $imageUri")
                        try {
                            val inputStream: InputStream? =
                                getApplication<Application>().contentResolver.openInputStream(imageUri)
                            val fileName =
                                "instrument_img_${userId}_${System.currentTimeMillis()}.jpg"
                            val outputDir = File(
                                getApplication<Application>().filesDir,
                                "created_instrument_images_"
                            )
                            if (!outputDir.exists()) {
                                outputDir.mkdirs()
                            }
                            val outputFile = File(outputDir, fileName)
                            val outputStream = FileOutputStream(outputFile)

                            inputStream?.copyTo(outputStream)
                            inputStream?.close()
                            outputStream.close()
                            Log.d(TAG, "Imagen copiada a: ${outputFile.absolutePath}")
                            outputFile.absolutePath //variable.absolutePath devuelve la ruta absoluta de un archivo o directorio
                        } catch (e: IOException) {
                            Log.e(TAG, "Error al copiar imagen: ${e.message}", e)
                            null //null si la copia falla
                        }
                    }
                    if (finalPhotoPath == null) {
                        // Si la copia falló, emitir error y no continuar con el guardado en BD
                        _errorMessage.value = Event("Error al procesar la imagen seleccionada.")
                        //no ponemos operationSuccess a true
                    }
                }

                // 2. Si no hubo error con la imagen (o no había imagen), proceder a guardar en BD
                //Solo intentamos guardar si la copia de imagen (si la hubo) fue exitosa
                //  o si no había imagen que copiar.
                //Cuidado con imageUri, que no es finalPhotopath
                if (imageUri == null || finalPhotoPath != null) {
                    val newInstrumentId = withContext(Dispatchers.IO) {
                        Log.d(
                            TAG,
                            "Guardando instrumento en BD. Nombre: $name, Path: $finalPhotoPath"
                        )
                        createdInstrumentDao.addCreatedInstrument(
                            userId = userId,
                            name = name,
                            //incluso si el valor frequency es null, debemos asegurarnos de que tiene un formato de valor válido
                            frequency = frequency
                                ?: 0.0,
                            info = info?.ifEmpty { null },
                            photoPath = finalPhotoPath
                        )
                    }
                    operationSuccess = newInstrumentId > 0L // Éxito si el ID es válido
                    if (operationSuccess) {
                        Log.d(TAG, "Instrumento guardado con ID: $newInstrumentId")
                    } else {
                        Log.e(TAG, "Error al guardar instrumento en la BD.")
                        _errorMessage.value =
                            Event("No se pudo guardar el instrumento en la base de datos. DAO devolvió ID inválido.")
                    }
                }
                //si finalPhotoPath es null y imageUri no lo era, significa que la copia de imagen falló, y operationSuccess seguirá siendo false.

            } catch (e: Exception) {
                Log.e(TAG, "Excepción general al guardar instrumento: ${e.message}", e)
                _errorMessage.value = Event("Error inesperado al guardar el instrumento.")
                operationSuccess = false
            } finally {
                _instrumentSaveResult.value = Event(operationSuccess)
                _isLoading.value = false
                Log.d(
                    TAG,
                    "Proceso de guardado de instrumento finalizado. Éxito: $operationSuccess"
                )
            }
        }
    }
}