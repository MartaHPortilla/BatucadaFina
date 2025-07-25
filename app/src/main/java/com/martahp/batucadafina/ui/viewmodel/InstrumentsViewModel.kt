package com.martahp.batucadafina.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.martahp.batucadafina.data.dao.CreatedInstrumentDAO
import com.martahp.batucadafina.data.dao.PredefinedInstrumentDAO
import com.martahp.batucadafina.data.dao.CreatedFavDAO
import com.martahp.batucadafina.data.dao.PredefinedFavDAO
import com.martahp.batucadafina.model.entities.CreatedInstrument
import com.martahp.batucadafina.ui.adapter.DisplayableItemSealed
import com.martahp.batucadafina.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstrumentsViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "InstrumentsViewModel"

    //instancias de los DAOS
    private val predefinedInstrumentDao = PredefinedInstrumentDAO(application.applicationContext)
    private val createdInstrumentDao = CreatedInstrumentDAO(application.applicationContext)

    //incluimos los favoritos
    private val predefinedFavDao = PredefinedFavDAO(application.applicationContext)
    private val createdFavDao = CreatedFavDAO(application.applicationContext)

    //livedata único para los instrumentos
    private val _displayableItems = MutableLiveData<List<DisplayableItemSealed>>()
    val displayableItems: LiveData<List<DisplayableItemSealed>> get() = _displayableItems

    // LiveData para el resultado de la eliminación. Boolean para éxito/fallo, String para el mensaje.
    private val _instrumentDeletedResult = MutableLiveData<Event<Pair<Boolean, String>>>()
    val instrumentDeletedResult: LiveData<Event<Pair<Boolean, String>>> get() = _instrumentDeletedResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> get() = _errorMessage


    //función para cargar los instrumentos predefinidos y creados y determinar si son favoritos para construir la lista displayable
    fun loadInstruments(userId: Long) {
        _isLoading.value = true
        Log.d(TAG, "Iniciando coroutine para cargar instrumentos de $userId...")

        viewModelScope.launch {
            try {
                // Obtenemos todos los datos en un hilo separado
                val predefinedInstrumentsDeferred = async(Dispatchers.IO) {
                    Log.d(TAG, "Cargando instrumentos predefinidos en IO thread...")
                    predefinedInstrumentDao.getAllPredefinedInstruments()
                }
                val createdInstrumentsDeferred = async(Dispatchers.IO) {
                    Log.d(TAG, "Cargando instrumentos creados para userId $userId en IO thread...")
                    createdInstrumentDao.getAllCreatedInstrumentsForUser(userId)
                }

                        // Obtenemos también los favoritos en un hilo separado
                val favPredefIdsDeferred = async(Dispatchers.IO) {
                    predefinedFavDao.getFavoritePredefinedInstrumentIdsForUser(userId)
                }
                val favCreatedIdsDeferred = async(Dispatchers.IO) {
                    createdFavDao.getFavoriteCreatedInstrumentIdsForUser(userId)
                }


                // Esperamos a que ambas consultas terminen y obtener resultados
                val predefinedList = predefinedInstrumentsDeferred.await()
                val createdList = createdInstrumentsDeferred.await()
                val favPredefinedIds = favPredefIdsDeferred.await().toSet() // Convertir a Set para mayor facilidad en la busqueda
                val favCreatedIds = favCreatedIdsDeferred.await().toSet()

                // Construir la lista displayable
                val displayableList = mutableListOf<DisplayableItemSealed>()

                if (predefinedList.isNotEmpty()) {
                    displayableList.add(DisplayableItemSealed.HeaderItem("Instrumentos Predefinidos"))
                    predefinedList.forEach { instrument ->
                        displayableList.add(DisplayableItemSealed.PredefinedInstrumentItem(
                            instrument = instrument,
                            isFavorite = favPredefinedIds.contains(instrument.id)
                        ))
                    }
                }

                if (createdList.isNotEmpty()) {
                    displayableList.add(DisplayableItemSealed.HeaderItem("Mis Instrumentos Creados"))
                    createdList.forEach { instrument ->
                        displayableList.add(DisplayableItemSealed.CreatedInstrumentItem(
                            instrument = instrument,
                            isFavorite = favCreatedIds.contains(instrument.id)
                        ))
                    }
                }

                if (displayableList.isEmpty()) {
                    displayableList.add(DisplayableItemSealed.HeaderItem("No hay instrumentos para mostrar."))
                }

                _displayableItems.value = displayableList //atualizar el LiveData con la lista combinada
                Log.d(TAG, "Lista combinada de DisplayableItems creada (${displayableList.size} items).")

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar instrumentos para el usuario $userId", e)
                _errorMessage.value = Event("Error al cargar instrumentos: ${e.message}")
                _displayableItems.value = listOf(DisplayableItemSealed.HeaderItem("Error al cargar"))
            } finally {
                _isLoading.value = false
                Log.d(TAG, "Coroutine para cargar instrumentos de $userId finalizada.")
            }
        }
    }

    fun toggleFavoriteStatus(item: DisplayableItemSealed, userId: Long) {
        viewModelScope.launch {
            val currentIsFavorite: Boolean
            val currentInstrumentId: Long

            // determinamos el tipo
            when (item) {
                is DisplayableItemSealed.PredefinedInstrumentItem -> {
                    currentIsFavorite = item.isFavorite
                    currentInstrumentId = item.id
                    Log.d(TAG, "Cambiando estado favorito para instrumento predefinido con ID: $currentInstrumentId a ${!currentIsFavorite} para el usuario $userId")
                    withContext(Dispatchers.IO) {
                        predefinedFavDao.setPredefinedFavoriteStatus(userId, currentInstrumentId, !currentIsFavorite)
                    }
                }

                is DisplayableItemSealed.CreatedInstrumentItem -> {
                    currentIsFavorite = item.isFavorite
                    currentInstrumentId = item.id
                    Log.d(TAG, "Cambiando favorito Creado ID: $currentInstrumentId a ${!currentIsFavorite} para User $userId")
                    withContext(Dispatchers.IO) {
                        createdFavDao.setCreatedFavoriteStatus(userId, currentInstrumentId, !currentIsFavorite)
                    }
                }
                is DisplayableItemSealed.HeaderItem -> {
                    // No hacer nada si se clica un header
                    return@launch
                }
            }
            // Volver a cargar todos los datos para refrescar la lista con el estado de favorito actualizado
            //solo se ejecuta si no es un HeaderItem
            loadInstruments(userId)

        }

    }


    fun deleteCreatedInstrument(instrument: CreatedInstrument, userIdToReload: Long) {
        _isLoading.value = true
        Log.d(TAG, "InstrumentsVM: Intentando eliminar instrumento: ${instrument.name} (ID: ${instrument.id})")
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                createdInstrumentDao.deleteCreatedInstrumentById(instrument.id)
            }
            if (success) {
                Log.d(TAG, "InstrumentsVM: Instrumento ${instrument.name} eliminado. Recargando lista completa.")
                _instrumentDeletedResult.value = Event(Pair(true, "Instrumento '${instrument.name}' eliminado con éxito."))
                loadInstruments(userIdToReload) // Recargar la lista completa de instrumentos
            } else {
                Log.e(TAG, "InstrumentsVM: Error al eliminar instrumento ${instrument.name} de la BD.")
                _instrumentDeletedResult.value = Event(Pair(false, "Error al eliminar el instrumento '${instrument.name}'."))
                _isLoading.value = false
            }
        }
    }


}