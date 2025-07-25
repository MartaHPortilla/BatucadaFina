package com.martahp.batucadafina.ui.viewmodel

sealed class UpdateUsernameUiState {
    object Success : UpdateUsernameUiState()
    object NewUserNameTaken : UpdateUsernameUiState()
    data class Error(val message: String) : UpdateUsernameUiState()
    object CurrentUserNotFound : UpdateUsernameUiState()

}