package com.martahp.batucadafina.ui.viewmodel

import android.app.Application // Necesario para usar AndroidViewModel si necesita contexto
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.martahp.batucadafina.data.dao.UserDAO
import com.martahp.batucadafina.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = UserDAO(application.applicationContext)
    private val TAG = "CreateAccountViewModel"

    // --- Livedata para resultados --- cambiada por Sealed Class
    //
    //
    // mutable privado. Solo el ViewModel puede modificarlo. aquí es donde el vm escribe el resultado de la operación
    // guarda un dato de tipo Event<Int> (ver clase Event). INT es el código de resultado de userDao.addUser (1, 0, -1)
    // _registrationResult es una convención para las backing properties (propiedades de respaldo, privadas y mutables)
    // Usamos Event<Int> para que el resultado (mensaje/navegación) se consuma una sola vez --> cambiado a <RegistrationResult>
    private val _registrationResult = MutableLiveData<Event<RegistrationResultSealed>>()

    // Inmutable público: esto es lo que la Activity observará --> también cambiado a tipo <RegistrationResult>
    // a través de la variable registrationResult, que contiene un Event<RegistrationResult>,
    // observamos _registrationResult, pero no se puede modificar
    val registrationResult: LiveData<Event<RegistrationResultSealed>> get() = _registrationResult // Versión pública inmutable


    // --- Funciones ---

    /**
     * Función llamada desde la Activity cuando el usuario intenta registrarse.
     * Recibe el nombre de usuario y la contraseña desde la Activity.
     * Realiza la operación de añadir usuario en segundo plano.
     */
    fun registerUser(username: String, plainPassword: String, email: String) {
        // Lanzar una coroutine en el viewModelScope (o ámbito del viewmodel: se cancela si el ViewModel se destruye)
        viewModelScope.launch {
            Log.d(TAG, "Iniciando coroutine para registrar usuario: $username")
            // Cambiar al hilo de IO (Input/Output) para operaciones de BD/disco/red
            // Esto lo estamos haciendo en el hilo secundario
            // Hemos cambiado la lógica por RegistrationResult
            val result: RegistrationResultSealed = withContext(Dispatchers.IO) {
                Log.d(TAG, "Dentro de IO thread, llamando a userDao.addUser...")
                // Llamar a la función del DAO que añade un usuario a la BD
                // userDao.addUser devuelve 0, 1, -1
                try {
                    // Llamamos al DAO para añadir el usuario. Este devolverá 1, 0, -1
                    when (val daoResult = userDao.addUser(username, plainPassword, email)) {
                        1 -> RegistrationResultSealed.Success(username)
                        0 -> RegistrationResultSealed.UserAlreadyExists
                        else -> RegistrationResultSealed.Error("Error al añadir al usuario con código: $daoResult")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Excepción en userDao.addUser: $e")
                    RegistrationResultSealed.Error("Excepción: ${e.message}")
                }
            } // fin hilo secundario. El resultado vuelve al hilo principal con result.
            // Volvemos al hilo principal. Actualizamos LiveData
            Log.d(TAG, "Resultado de addUser: $result. Actualizando LiveData.")
            // Actualiza el MutableLiveData con el resultado obtenido del DAO.
            // Envolvemos el 'result' (Int) en un 'Event' para que sea un evento de un solo uso.
            // Usamos value porque estamos en el hilo principal. Si no, usamos postValue.
            _registrationResult.value = Event(result)
        }
    }
}

