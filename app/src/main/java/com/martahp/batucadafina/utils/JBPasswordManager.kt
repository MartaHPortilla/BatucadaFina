package com.martahp.batucadafina.utils

import org.mindrot.jbcrypt.BCrypt // para hashear y verificar contraseñas
import android.util.Log


/**
 * Objeto Singleton para gestionar el hasheo y verificación de contraseñas usando jBCrypt.
 */
object JBPasswordManager {

    private const val LOG_TAG = "JBPasswordManager"

    /**
     * Genera un hash seguro de la contraseña usando bcrypt con una sal generada automáticamente.
     *
     * @param password La contraseña en texto plano a hashear.
     * @return El hash resultante (String) que incluye la sal y parámetros. Listo para guardar en la BD.
     */
    fun hashPassword(password: String): String {
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        Log.d(LOG_TAG, "Contraseña hasheada exitosamente.") // Log genérico sin datos sensibles
        return hashedPassword
    }

    /**
     * Verifica si una contraseña en texto plano coincide con un hash previamente generado.
     *
     * @param plainPassword La contraseña en texto plano introducida por el usuario.
     * @param storedHash El hash completo (que incluye la sal) recuperado de la base de datos.
     * @return true si la contraseña coincide con el hash, false en caso contrario.
     */
    fun verifyPassword(plainPassword: String, storedHash: String): Boolean {
        var passwordMatch = false
        try {
            //BCrypt.checkpw() extrae la sal del storedHash, hashea plainPassword con esa sal y compara los resultados de forma segura.
            passwordMatch = BCrypt.checkpw(plainPassword, storedHash)
        } catch (e: IllegalArgumentException) {
            //Esto puede ocurrir si el storedHash no tiene el formato esperado por jBCrypt
            Log.e(LOG_TAG, "Error al verificar contraseña: El hash almacenado ('$storedHash') no tiene un formato válido.", e)
            passwordMatch = false
        } catch (e: Exception) {
            //captura otras posibles excepciones inesperadas
            Log.e(LOG_TAG, "Error inesperado durante la verificación de contraseña.", e)
            passwordMatch = false
        }
        Log.d(LOG_TAG, "Verificación de contraseña completada. Coincidencia: $passwordMatch")
        return passwordMatch
    }
}