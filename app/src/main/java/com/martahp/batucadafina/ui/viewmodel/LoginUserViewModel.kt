package com.martahp.batucadafina.ui.viewmodel

import android.app.Application // Necesario para usar AndroidViewModel si necesita contexto
import android.util.Log
import androidx.lifecycle.AndroidViewModel // Clase base para ViewModels que requieren contexto de aplicación
import androidx.lifecycle.LiveData // Clase para datos que pueden ser observados
import androidx.lifecycle.MutableLiveData // Clase para datos que pueden ser observados y modificados desde el ViewModel
import androidx.lifecycle.viewModelScope // Ámbito de corrutinas para ViewModel, ligado al ciclo de vida del ViewModel
import com.martahp.batucadafina.data.dao.UserDAO
import com.martahp.batucadafina.utils.Event
import com.martahp.batucadafina.utils.SessionManager // Necesario para gestionar la sesión del usuario con SharedPreferences
import kotlinx.coroutines.Dispatchers // Para poder cambiar de hilo
import kotlinx.coroutines.launch // Para lanzar corrutinas de tipo "fire and forget"
import kotlinx.coroutines.withContext // Para cambiar de contexto de un hilo a otro

class LoginUserViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = UserDAO(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)

    private val TAG = "LoginUserViewModel"

    //--- livedata ---
    private val _loginResult = MutableLiveData<Event<LoginResultSealed>>()
    val loginResult: LiveData<Event<LoginResultSealed>> get() = _loginResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading


    //--- funciones ---

    fun loginUser(username: String, plainPassword: String, rememberMe: Boolean) {
        Log.d(TAG, "loginUser() llamado con username: $username, rememberMe: $rememberMe")
        _isLoading.value = true
        viewModelScope.launch {
            Log.d(TAG, "Iniciando coroutine para loginUser: $username")
            val result : LoginResultSealed = withContext(Dispatchers.IO) {
                Log.d(TAG, "Dentro de IO thread, llamando a userDao.checkUserCredentials...")
                try {
                    val credentialsMatch = userDao.checkUserCredentials(username, plainPassword)
                    if (credentialsMatch) {
                        val user = userDao.getUserProfileByName(username)
                        if (user != null) {
                            if (rememberMe) {
                                sessionManager.saveLoginSession(username)
                                Log.d(TAG, "Credenciales válidas. Guardando sesión para $username.")
                            } else {
                                Log.d(TAG, "Credenciales válidas. No se guarda sesión para $username.")
                            }
                            LoginResultSealed.Success(username)
                        } else {
                            // Caso raro: las credenciales coinciden pero no se encuentra el perfil
                            Log.e(TAG, "Credenciales correctas pero no se encontró el perfil para: $username")
                            LoginResultSealed.Error("Error inesperado al obtener datos del usuario.")
                        }
                    } else {
                        Log.d(TAG, "Credenciales inválidas para: $username")
                        LoginResultSealed.InvalidCredentials
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Excepción en userDao.checkUserCredentials o userDao.getUserProfileByName: $e")
                    LoginResultSealed.Error("Error al intentar iniciar sesión: ${e.message}")
                }
            }
            Log.d(TAG, "Resultado de checkUserCredentials: $result. Actualizando LiveData.")
            _loginResult.value = Event(result)
            _isLoading.value = false
        }

    }
}
