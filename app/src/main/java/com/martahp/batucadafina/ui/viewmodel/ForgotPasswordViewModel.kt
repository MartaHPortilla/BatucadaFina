package com.martahp.batucadafina.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.martahp.batucadafina.data.dao.UserDAO
import com.martahp.batucadafina.model.entities.User
import com.martahp.batucadafina.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = UserDAO(application.applicationContext)
    private val TAG = "ForgotPasswordVM"


    private val _userForPasswordReset = MutableLiveData<Event<User?>>()
    val userForPasswordReset: LiveData<Event<User?>> get() = _userForPasswordReset


    private val _passwordResetResult = MutableLiveData<Event<Boolean>>()
    val passwordResetResult: LiveData<Event<Boolean>> get() = _passwordResetResult


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> get() = _errorMessage

    /**
     * Verifica si un email existe en la base de datos y obtiene el perfil de usuario asociado.
     * Actualiza _userForPasswordReset LiveData.
     */
    fun verifyEmailForPasswordReset(email: String) {
        _isLoading.value = true
        Log.d(TAG, "Verificando email para restablecer contraseña: $email")
        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) {
                userDao.getUserProfileByEmail(email)
            }
            if (user != null) {
                Log.d(TAG, "Email encontrado para usuario: ${user.username}")
                _userForPasswordReset.value = Event(user)
            } else {
                Log.d(TAG, "Email no encontrado: $email")
                _userForPasswordReset.value = Event(null)
                _errorMessage.value = Event("El correo electrónico no está registrado.")
            }
            _isLoading.value = false
        }
    }

    /**
     * Establece una nueva contraseña para el usuario (identificado por su username).
     * Actualiza _passwordResetResult LiveData.
     */
    fun setNewPasswordForUser(username: String, newPlainPassword: String) {
        _isLoading.value = true
        Log.d(TAG, "Intentando establecer nueva contraseña para: $username")
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                userDao.updatePassword(username, newPlainPassword)
            }
            if (success) {
                Log.d(TAG, "Contraseña actualizada con éxito para: $username")
            } else {
                Log.e(TAG, "Error al actualizar la contraseña para: $username en el DAO.")
                _errorMessage.value = Event("No se pudo actualizar la contraseña. Inténtalo de nuevo.")
            }
            _passwordResetResult.value = Event(success)
            _isLoading.value = false
        }
    }
}