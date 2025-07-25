package com.martahp.batucadafina.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Clase para gestionar la sesión del usuario.
 */
class SessionManager(context: Context) {
    private var sharedPrefs: SharedPreferences = context.getSharedPreferences("BatukAfinaAppPrefs", Context.MODE_PRIVATE)

    companion object {
        const val IS_LOGGED_IN = "is_logged_in"
        const val LOGGED_IN_USERNAME = "logged_in_username"
    }

    /**
     * Guarda la sesión del usuario.
     */
    fun saveLoginSession(username: String) {
        val editor = sharedPrefs.edit()
        editor.putBoolean(IS_LOGGED_IN, true)
        editor.putString(LOGGED_IN_USERNAME, username)
        editor.apply()
    }

    /**
     * Limpia la sesión guardada (para logout).
     */
    fun clearLoginSession() {
        val editor = sharedPrefs.edit()
        editor.putBoolean(IS_LOGGED_IN, false)
        editor.remove(LOGGED_IN_USERNAME)
        editor.apply()
    }

    /**
     * Comprueba si hay una sesión activa guardada.
     */
    fun isLoggedIn(): Boolean {
        return sharedPrefs.getBoolean(IS_LOGGED_IN, false)
    }

    /**
     * Obtiene el nombre de usuario de la sesión guardada.
     * Devuelve null si no hay ninguno.
     */
    fun getLoggedInUsername(): String? {
        return sharedPrefs.getString(LOGGED_IN_USERNAME, null)
    }
}