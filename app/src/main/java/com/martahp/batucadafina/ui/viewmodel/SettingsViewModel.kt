package com.martahp.batucadafina.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.martahp.batucadafina.data.dao.UserDAO
import com.martahp.batucadafina.model.entities.User
import com.martahp.batucadafina.utils.Event
import com.martahp.batucadafina.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = UserDAO(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)
    private val TAG = "SettingsViewModel"

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> get() = _userProfile

    private val _updateUsernameResult = MutableLiveData<Event<UpdateUsernameUiState>>()
    val updateUsernameResult: LiveData<Event<UpdateUsernameUiState>> get() = _updateUsernameResult

    private val _updatePasswordResult = MutableLiveData<Event<UpdatePasswordUiState>>()
    val updatePasswordResult: LiveData<Event<UpdatePasswordUiState>> get() = _updatePasswordResult

    private val _updatePhotoResult = MutableLiveData<Event<Boolean>>() // true si éxito, false si error
    val updatePhotoResult: LiveData<Event<Boolean>> get() = _updatePhotoResult

    private val _logoutEvent = MutableLiveData<Event<Unit>>() //no hace falta true o false
    val logoutEvent: LiveData<Event<Unit>> get() = _logoutEvent

    private val _deleteAccountResult = MutableLiveData<Event<Boolean>>() // true si éxito, false si error
    val deleteAccountResult: LiveData<Event<Boolean>> get() = _deleteAccountResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> get() = _errorMessage


    /**
     * Carga el perfil del usuario actual usando su nombre de usuario.
     */
    fun loadUserProfile(username: String) {
        _isLoading.value = true
        viewModelScope.launch {
            Log.d(TAG, "Cargando perfil para $username")
            val user = withContext(Dispatchers.IO) {
                userDao.getUserProfileByName(username)
            }
            _userProfile.value = user
            _isLoading.value = false
            if (user == null) {
                Log.w(TAG, "LoadUserProfile: No se encontró perfil para $username")
            } else {
                Log.d(TAG, "LoadUserProfile: Perfil cargado para ${user.username}")
            }
        }
    }

    /**
     * Actualiza el nombre de usuario.
     */
    fun updateUserUsername(currentUsername: String, newUsername: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val resultState: UpdateUsernameUiState = withContext(Dispatchers.IO) {
                //la función updateUsername de UserDAO devolvía 1, 0 o -1
                when (val daoResponse = userDao.updateUsername(currentUsername, newUsername)) {
                    1 -> UpdateUsernameUiState.Success
                    0 -> UpdateUsernameUiState.NewUserNameTaken
                    -1 -> UpdateUsernameUiState.CurrentUserNotFound
                    else -> UpdateUsernameUiState.Error("No se pudo actualizar el nombre (código DAO: $daoResponse).")
                }
            }
            _updateUsernameResult.value = Event(resultState)
            if (resultState is UpdateUsernameUiState.Success) { //si éxito
                loadUserProfile(newUsername) //carga el perfil con el nuevo nombre de usuario si ha cambiado
            } else {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza la contraseña del usuario.
     */
    fun updateUserPassword(username: String, currentPlainPass: String, newPlainPass: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val resultState: UpdatePasswordUiState = withContext(Dispatchers.IO) {
                if (!userDao.checkUserCredentials(username, currentPlainPass)) {
                    UpdatePasswordUiState.CurrentPasswordIncorrect
                } else {
                    if (userDao.updatePassword(username, newPlainPass)) {
                        UpdatePasswordUiState.Success
                    } else {
                        UpdatePasswordUiState.Error("No se pudo actualizar la contraseña.")
                    }
                }
            }
            _updatePasswordResult.value = Event(resultState)
            _isLoading.value = false
        }
    }

    /**
     * Procesa la URI de una imagen seleccionada, la copia al almacenamiento interno,
     * y actualiza el photoPath del usuario en la BD.
     */
    fun updateUserProfilePhotoPath (imageUri: Uri, userId: Long, currentUsername: String) {
        _isLoading.value = true // Indicar que estamos procesando
        viewModelScope.launch {
            Log.d(TAG, "Iniciando coroutine para procesar y guardar imagen de perfil.")
            var success = false
            var newPath: String? = null

            try {
                newPath = withContext(Dispatchers.IO) {
                    //lógica de copia de archivo
                    val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(imageUri)
                    val fileName = "profile_pic_${userId}_${System.currentTimeMillis()}.jpg"
                    val outputDir = File(getApplication<Application>().filesDir, "profile_images")
                    if (!outputDir.exists()) {
                        outputDir.mkdirs()
                    }
                    val outputFile = File(outputDir, fileName)
                    val outputStream = FileOutputStream(outputFile)

                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    Log.d(TAG, "Imagen copiada a: ${outputFile.absolutePath}")
                    outputFile.absolutePath //devuelve la ruta del archivo copiado
                }

                // Si la copia fue exitosa y obtuvimos un path
                if (newPath != null) {
                    success = withContext(Dispatchers.IO) {
                        userDao.updateUserProfilePicPath(userId, newPath)
                    }
                } else {
                    success = false
                    _errorMessage.value = Event("Error al procesar la imagen: No se pudo obtener la ruta.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar o guardar la imagen de perfil", e)
                _errorMessage.value = Event("Error al procesar la imagen: ${e.message}")
                success = false
            } finally {
                _updatePhotoResult.value = Event(success) // Informar del resultado de la actualización de la foto
                if (success) {
                    //si fue bien y el path está guardado en la bd, recargamos el perfil para refrescar la UI
                    loadUserProfile(currentUsername) //esto ya pone isLoading a false al final
                } else {
                    _isLoading.value = false //en este caso sí hay que ponerlo a false
                }
                Log.d(TAG, "Proceso de imagen de perfil finalizado. Éxito: $success")
            }
        }
    }

    /**
     * Elimina la cuenta del usuario.
     */
    fun deleteUserAccount(username: String, currentPlainPass: String) {
        _isLoading.value = true
        viewModelScope.launch {
            var success = false

            try {
                val verificationPassed = withContext(Dispatchers.IO) {
                    userDao.checkUserCredentials(username, currentPlainPass)
                }

                if (verificationPassed) {
                    success = withContext(Dispatchers.IO) {
                        userDao.deleteUser(username)
                    }
                    if(success) {
                        Log.d(TAG, "DeleteUserAccount: Cuenta para $username eliminada definitivamente de la BD.")
                        sessionManager.clearLoginSession() //borramos la sesión
                    } else {
                        //este error es si el DAO.deleteUser() devuelve false
                        Log.e(TAG, "DeleteUserAccount: Error de DAO al eliminar cuenta para $username después de verificar contraseña.")
                        _errorMessage.value = Event("No se pudo eliminar la cuenta de la base de datos.")
                    }
                } else {
                    Log.w(TAG, "DeleteUserAccount: Verificación de contraseña fallida para $username")
                }

            } catch (e: Exception) {
                Log.e(TAG, "DeleteUserAccount: Error al eliminar cuenta para $username", e)
                _errorMessage.value = Event("Error al eliminar la cuenta: ${e.message}")
                success = false
            } finally {
                _deleteAccountResult.value = Event(success) //hay que informar del resultado de la eliminación
                _isLoading.value = false //reseteamos el estado de carga a false
                Log.d(TAG, "DeleteUserAccount: Proceso finalizado. Éxito: $success")

            }
        }
    }

    fun logoutUserAccountRequest() {
        Log.d(TAG, "Logout User Account: solicitud de cierre de sesión recibida")
        sessionManager.clearLoginSession()
        _logoutEvent.value = Event(Unit) //enviamos el evento para que la activity lo maneje
    }
}