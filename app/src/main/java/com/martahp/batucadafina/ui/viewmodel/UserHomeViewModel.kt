package com.martahp.batucadafina.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel // Clase base si necesitamos el contexto de Application
import androidx.lifecycle.LiveData // Para exponer datos observables de solo lectura
import androidx.lifecycle.MutableLiveData // Para poder modificar los datos desde el ViewModel
import androidx.lifecycle.viewModelScope // Para lanzar corutinas ligadas al ViewModel
import com.martahp.batucadafina.data.dao.CreatedFavDAO
import com.martahp.batucadafina.data.dao.UserDAO // El DAO para interactuar con datos de usuario
import com.martahp.batucadafina.data.dao.PredefinedInstrumentDAO
import com.martahp.batucadafina.data.dao.CreatedInstrumentDAO
import com.martahp.batucadafina.data.dao.PredefinedFavDAO
import com.martahp.batucadafina.model.entities.User
import com.martahp.batucadafina.model.entities.PredefinedInstrument
import com.martahp.batucadafina.model.entities.CreatedInstrument
import com.martahp.batucadafina.utils.Event
import com.martahp.batucadafina.utils.SessionManager
import kotlinx.coroutines.Dispatchers // Para elegir el hilo (IO para BD)
import kotlinx.coroutines.launch // Para iniciar corutinas
import kotlinx.coroutines.withContext // Para cambiar de hilo

class UserHomeViewModel(application: Application) : AndroidViewModel(application) {

    // Instanciamos los DAOs para acceder a las tablas de la BD
    private val userDao = UserDAO(application.applicationContext)
    private val predefinedInstrumentDao = PredefinedInstrumentDAO(application.applicationContext)
    private val createdInstrumentDao = CreatedInstrumentDAO(application.applicationContext)
    private val predefinedFavDao = PredefinedFavDAO(application.applicationContext)
    private val createdFavDao = CreatedFavDAO(application.applicationContext)

    private val sessionManager = SessionManager(application.applicationContext)


    private val TAG = "UserHomeViewModel"

    // --- LiveData ---

    private val _userProfile = MutableLiveData<User?>() // User puede ser null si no se encuentra
    val userProfile: LiveData<User?> get() = _userProfile

    private val _predefinedFavs = MutableLiveData<List<PredefinedInstrument>>()
    val predefinedFavs: LiveData<List<PredefinedInstrument>> get() = _predefinedFavs

    private val _createdFavs = MutableLiveData<List<CreatedInstrument>>()
    val createdFavs: LiveData<List<CreatedInstrument>> get() = _createdFavs

    // LiveData para  creados
    private val _createdInstruments = MutableLiveData<List<CreatedInstrument>>()
    val createdInstruments: LiveData<List<CreatedInstrument>> get() = _createdInstruments

    private val _logoutEvent = MutableLiveData<Event<Unit>>()
    val logoutEvent: LiveData<Event<Unit>> get() = _logoutEvent


    //todo: // LiveData para estados de carga o errores
    //    // private val _isLoading = MutableLiveData<Boolean>()
    //    // val isLoading: LiveData<Boolean> get() = _isLoading
    //    // private val _errorMessage = MutableLiveData<Event<String>>()
    //    // val errorMessage: LiveData<Event<String>> get() = _errorMessage


    // --- Funciones para cargar los datos obtenidos de la BD a través de LiveData ---

    /**
     * Carga el perfil del usuario desde la BD. La Activity llamará a esta función.
     * @param username El nombre del usuario a buscar.
     */
    fun loadUserProfile(username: String) {
        viewModelScope.launch {
            Log.d(TAG, "Iniciando coroutine para cargar perfil de: $username")
            val userResult = withContext(Dispatchers.IO) {
                Log.d(TAG, "Dentro de IO thread, llamando a userDao.getUserProfile...")
                userDao.getUserProfileByName(username) // Devuelve User?
            }
            _userProfile.value = userResult
            if (userResult != null) {
                Log.d(TAG, "Perfil cargado para: ${userResult.username} (ID: ${userResult.id})")
            } else {
                Log.w(TAG, "No se encontró perfil para: $username")
            }
        }
    }

    /**
     * Carga las listas de instrumentos favoritos (predefinidos y creados) para un usuario.
     * @param userId El ID del usuario.
     */
    fun loadFavorites(userId: Long) {
        viewModelScope.launch {
            Log.d(TAG, "Iniciando coroutine para cargar favoritos de User ID: $userId")

            // Cargar Favoritos Predefinidos
            val predefFavs = withContext(Dispatchers.IO) {
                Log.d(TAG, "Cargando favoritos predefinidos (IO Thread)...")
                predefinedFavDao.getFavoritePredefinedInstrumentsForUser(userId)
            }
            _predefinedFavs.value = predefFavs // Actualizar LiveData
            Log.d(TAG, "Favoritos predefinidos cargados (${predefFavs.size} encontrados).")

            // Cargar Favoritos Creados
            val createdFavs = withContext(Dispatchers.IO) {
                Log.d(TAG, "Cargando favoritos creados (IO Thread)...")
                createdFavDao.getFavoriteCreatedInstrumentsForUser(userId)
            }
            _createdFavs.value = createdFavs // Actualizar LiveData
            Log.d(TAG, "Favoritos creados cargados (${createdFavs.size} encontrados).")
        }
    }

    /**
     * Carga la lista de todos los instrumentos creados por un usuario.
     * @param userId El ID del usuario.
     */
    fun loadAllCreatedInstruments(userId: Long) {
        viewModelScope.launch {
            Log.d(TAG, "Iniciando coroutine para cargar todos los instrumentos creados de User ID: $userId")
            val allCreated = withContext(Dispatchers.IO) {
                Log.d(TAG, "Cargando todos los instrumentos creados (IO Thread)...")
                createdInstrumentDao.getAllCreatedInstrumentsForUser(userId)
            }
            _createdInstruments.value = allCreated // Actualizar LiveData
            Log.d(TAG, "Todos los instrumentos creados cargados (${allCreated.size} encontrados).")
        }
    }
    fun logoutUserAccountRequest() {
        Log.d(TAG, "Logout User Account: solicitud de cierre de sesión recibida")
        sessionManager.clearLoginSession()
        _logoutEvent.value = Event(Unit) //enviamos el evento para que la activity lo maneje
    }


}