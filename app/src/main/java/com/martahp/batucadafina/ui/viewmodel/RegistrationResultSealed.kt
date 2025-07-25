package com.martahp.batucadafina.ui.viewmodel

sealed class RegistrationResultSealed {

    // Clase que representa el resultado de un registro exitoso
    data class Success(val username: String) : RegistrationResultSealed()
    // Objeto que representa el caso de que el usuario ya exista
    data object UserAlreadyExists : RegistrationResultSealed()
    // Clase que representa cualquier otro error con mensaje
    data class Error(val message: String) : RegistrationResultSealed()


}