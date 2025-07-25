package com.martahp.batucadafina.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.martahp.batucadafina.data.dao.CreatedFavDAO
import com.martahp.batucadafina.data.dao.CreatedInstrumentDAO
import com.martahp.batucadafina.data.dao.PredefinedFavDAO
import com.martahp.batucadafina.ui.adapter.DisplayableItemSealed
import com.martahp.batucadafina.model.entities.CreatedInstrument
import com.martahp.batucadafina.model.entities.PredefinedInstrument
import com.martahp.batucadafina.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TuneFavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "TuneFavoritesViewModel"

    private val predefinedFavDao = PredefinedFavDAO(application.applicationContext)
    private val createdFavDao = CreatedFavDAO(application.applicationContext)
    private val createdInstrumentDao = CreatedInstrumentDAO(application.applicationContext)

    private val _displayableItems = MutableLiveData<List<DisplayableItemSealed>>()
    val displayableItems: LiveData<List<DisplayableItemSealed>> get() = _displayableItems

    private val _instrumentDeletedResult = MutableLiveData<Event<Pair<Boolean, String>>>()
    val instrumentDeletedResult: LiveData<Event<Pair<Boolean, String>>> get() = _instrumentDeletedResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> get() = _errorMessage

    /**
     * Carga SOLO los instrumentos favoritos (predefinidos y creados) del usuario.
     * Construye la lista de DisplayableItemSealed para el RecyclerView.
     */
    fun loadFavoriteInstruments(userId: Long) {

        _isLoading.value = true
        Log.d(TAG, "Iniciando coroutine para cargar instrumentos FAVORITOS del usuario ID: $userId...")

        viewModelScope.launch {
            try {
                //listas de instrumentos favoritos
                val predefinedFavoritesDeferred = async(Dispatchers.IO) {
                    Log.d(TAG, "Cargando favoritos predefinidos en IO thread...")
                    predefinedFavDao.getFavoritePredefinedInstrumentsForUser(userId)
                }
                val createdFavoritesDeferred = async(Dispatchers.IO) {
                    Log.d(TAG, "Cargando favoritos creados para userId $userId en IO thread...")
                    createdFavDao.getFavoriteCreatedInstrumentsForUser(userId)
                }

                //esperar a que ambas consultas terminen
                val predefinedFavList = predefinedFavoritesDeferred.await()
                val createdFavList = createdFavoritesDeferred.await()

                //construimos la lista displayable
                val finalDisplayableList = mutableListOf<DisplayableItemSealed>()

                if (predefinedFavList.isNotEmpty()) {
                    finalDisplayableList.add(DisplayableItemSealed.HeaderItem("Favoritos Predefinidos"))
                    predefinedFavList.forEach { instrument ->
                        //todos los instrumentos aquí son favoritos, así que isFavorite es true
                        finalDisplayableList.add(DisplayableItemSealed.PredefinedInstrumentItem(
                            instrument = instrument,
                            isFavorite = true
                        ))
                    }
                }

                if (createdFavList.isNotEmpty()) {
                    finalDisplayableList.add(DisplayableItemSealed.HeaderItem("Mis Favoritos Creados"))
                    createdFavList.forEach { instrument ->
                        finalDisplayableList.add(DisplayableItemSealed.CreatedInstrumentItem(
                            instrument = instrument,
                            isFavorite = true // también son favoritos
                        ))
                    }
                }

                if (finalDisplayableList.isEmpty()) {
                    finalDisplayableList.add(DisplayableItemSealed.HeaderItem("Añade algún instrumento a tus favoritos para poder visualizarlo aquí."))
                }

                _displayableItems.value = finalDisplayableList
                Log.d(TAG, "Lista de favoritos DisplayableItems creada (${finalDisplayableList.size} items).")

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar instrumentos favoritos para el usuario $userId", e)
                _errorMessage.value = Event("Error al cargar favoritos: ${e.message}")
                // En caso de error, podrías mostrar un header de error en la lista //todo
                _displayableItems.value = listOf(DisplayableItemSealed.HeaderItem("Error al cargar favoritos"))
            } finally {
                _isLoading.value = false // Indicar que la carga ha terminado
                Log.d(TAG, "Coroutine para cargar instrumentos favoritos del usuario $userId finalizada.")
            }
        }
    }

    /**
     * Cambia el estado de favorito de un instrumento.
     * Después de actualizar la BD, vuelve a cargar SOLO los instrumentos favoritos.
     */
    fun toggleFavoriteStatus(item: DisplayableItemSealed, userId: Long) {
        viewModelScope.launch {
            val currentIsFavorite: Boolean //ya sabemos que es true si viene de esta pantalla
            val instrumentId: Long

            when (item) {
                is DisplayableItemSealed.PredefinedInstrumentItem -> {
                    currentIsFavorite = item.isFavorite
                    instrumentId = item.id
                    Log.d(
                        TAG,
                        "Cambiando estado favorito (predef): ID $instrumentId a ${!currentIsFavorite} para User $userId"
                    )
                    withContext(Dispatchers.IO) {
                        predefinedFavDao.setPredefinedFavoriteStatus(
                            userId,
                            instrumentId,
                            !currentIsFavorite
                        )
                    }
                }

                is DisplayableItemSealed.CreatedInstrumentItem -> {
                    currentIsFavorite = item.isFavorite
                    instrumentId = item.id
                    Log.d(
                        TAG,
                        "Cambiando estado favorito (creado): ID $instrumentId a ${!currentIsFavorite} para User $userId"
                    )
                    withContext(Dispatchers.IO) {
                        createdFavDao.setCreatedFavoriteStatus(
                            userId,
                            instrumentId,
                            !currentIsFavorite
                        )
                    }
                }

                is DisplayableItemSealed.HeaderItem -> {
                    return@launch
                }
            }

            //volvemos a cargar la lista de favoritos para refrescar la ui. Si un item se desmarca como favorito, desaparece de la lista
            loadFavoriteInstruments(userId)
        }
    }


    fun deleteCreatedInstrument(instrument: CreatedInstrument, userIdToReload: Long) {
        _isLoading.value = true
        Log.d(TAG, "TuneFavoritesVM: Intentando eliminar instrumento: ${instrument.name} (ID: ${instrument.id})")
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                createdInstrumentDao.deleteCreatedInstrumentById(instrument.id)
            }
            if (success) {
                Log.d(TAG, "TuneFavoritesVM: Instrumento ${instrument.name} eliminado. Recargando lista de favoritos.")
                _instrumentDeletedResult.value = Event(Pair(true, "Instrumento '${instrument.name}' eliminado."))
                loadFavoriteInstruments(userIdToReload) //recargamos lista de favoritos por si el instrumento era favorito
            } else {
                Log.e(TAG, "TuneFavoritesVM: Error al eliminar instrumento ${instrument.name} de la BD.")
                _instrumentDeletedResult.value = Event(Pair(false, "Error al eliminar el instrumento '${instrument.name}'."))
                _isLoading.value = false
            }
        }
    }
}

