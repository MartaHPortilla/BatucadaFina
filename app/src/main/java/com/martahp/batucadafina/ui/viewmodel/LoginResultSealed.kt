package com.martahp.batucadafina.ui.viewmodel

sealed class LoginResultSealed {
    // Caso éxito: Incluye el username logueado
    data class Success(val username: String) : LoginResultSealed()
    // Caso específico: Credenciales inválidas (user o contraseña)
    object InvalidCredentials : LoginResultSealed()
    // Caso genérico de error
    data class Error(val message: String) : LoginResultSealed()
}