package com.martahp.batucadafina.ui.viewmodel

sealed class UpdatePasswordUiState {
    object Success : UpdatePasswordUiState()
    object CurrentPasswordIncorrect : UpdatePasswordUiState()
    data class Error(val message: String) : UpdatePasswordUiState()
}