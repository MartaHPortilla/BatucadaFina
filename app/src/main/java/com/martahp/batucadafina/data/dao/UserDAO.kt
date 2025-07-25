package com.martahp.batucadafina.data.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import com.martahp.batucadafina.data.DatabaseHelper
import com.martahp.batucadafina.data.DbConstants.TAG_USER_DAO
import com.martahp.batucadafina.data.DbConstants.UserTable.COL_ID
import com.martahp.batucadafina.data.DbConstants.UserTable.COL_PASSWORD
import com.martahp.batucadafina.data.DbConstants.UserTable.COL_PHOTO_PATH
import com.martahp.batucadafina.data.DbConstants.UserTable.COL_USERNAME
import com.martahp.batucadafina.data.DbConstants.UserTable.TABLE_NAME
import com.martahp.batucadafina.data.DbConstants.UserTable.COL_EMAIL //añadimos a version 2 bd
import com.martahp.batucadafina.model.entities.User
import com.martahp.batucadafina.utils.JBPasswordManager

/**
 * DAO para gestionar las operaciones CRUD de la entidad User en la base de datos.
 */
class UserDAO(context: Context) {

    /** Instancia del Helper de la base de datos. */
    private val dbHelper = DatabaseHelper(context)

    /**
     * Añade un nuevo usuario, comprobando existencia y hasheando contraseña.
     * Retorna: 1 (éxito), 0 (ya existe), -1 (otro error), -2 (email ya existe)
     */
    fun addUser(username: String, plainPassword: String, email: String): Int { //añadimos email a version 2 bd
        if (checkUserExists(username)) {
            Log.w(TAG_USER_DAO, "Intento de añadir un usuario que ya existe: $username")
            return 0 //error de usuario ya existente
        }

        if (checkEmailExists(email)) {
            Log.w(TAG_USER_DAO, "addUser: El email '$email' ya está registrado.")
            return -2 // Cambiado a -2 (nuevo código de error, email ya registrado)
        }

        val db = dbHelper.writableDatabase
        val hashedPassword = JBPasswordManager.hashPassword(plainPassword)
        val values = ContentValues().apply {
            put(COL_USERNAME, username)
            put(COL_PASSWORD, hashedPassword)
            put(COL_EMAIL, email) //añadimos email a version 2 bd
        }

        try {
            val result = db.insert(TABLE_NAME, null, values)
            if (result == -1L) {
                Log.e(TAG_USER_DAO, "Error desconocido al insertar usuario: $username")
                return -1 // Error de inserción
            } else {
                Log.d(TAG_USER_DAO, "Usuario insertado con ID: $result ($username)")
                return 1 // Éxito
            }
        } catch (e: Exception) {
            Log.e(TAG_USER_DAO, "Excepción al insertar usuario $username", e)
            return -1 // Error de inserción
        } finally {
            db.close() // Ya estaba en finally
            Log.d(TAG_USER_DAO, "Recursos cerrados para addUser.")
        }
    }

    /**
     * Comprueba si un email ya está registrado.
     * Añadido para la versión 2 de la BD.
     */
    private fun checkEmailExists(email: String): Boolean {
        Log.d(TAG_USER_DAO, "DEBUG BD: Intentando obtener readableDatabase para checkEmailExists...")
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null
        var exists = false
        Log.d(TAG_USER_DAO, "Comprobando si existe $email")

        try {
            val columns = arrayOf(COL_ID)
            val selection = "$COL_EMAIL = ?"
            val selectionArgs = arrayOf(email)
            cursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, "1")
            exists = (cursor != null && cursor.count > 0)
        } catch (e: Exception) {
            Log.e(TAG_USER_DAO, "Error en checkEmailExists para email: $email", e)
        } finally {
            cursor?.close()
            db.close()
        }
        return exists
    }

    /** Comprueba si un usuario ya existe por username. */
    fun checkUserExists(username: String): Boolean {
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null // Cursor fuera del try
        var exists = false
        Log.d(TAG_USER_DAO, "Comprobando si existe $username")
        try {
            val selection = "$COL_USERNAME = ?"
            val selectionArgs = arrayOf(username)
            cursor = db.query(TABLE_NAME, arrayOf(COL_ID), selection, selectionArgs, null, null, "1") // LIMIT 1
            exists = (cursor != null && cursor.count > 0)
        } catch (e: Exception) {
            Log.e(TAG_USER_DAO, "Error al comprobar si existe $username", e)
            exists = false
        } finally {
            cursor?.close()
            db.close()
            Log.d(TAG_USER_DAO, "Recursos cerrados para checkUserExists.")
        }
        Log.d(TAG_USER_DAO, "Resultado comprobación $username: $exists")
        return exists
    }

    /** Verifica las credenciales del usuario (username y password plano) usando hash. */
    fun checkUserCredentials(username: String, plainPasswordToCheck: String): Boolean {
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null
        var passwordMatches = false
        Log.d(TAG_USER_DAO, "Iniciando verificación de credenciales para $username")
        try {
            val columns = arrayOf(COL_PASSWORD)
            val selection = "$COL_USERNAME = ?"
            val selectionArgs = arrayOf(username)
            cursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null)

            if (cursor != null && cursor.moveToFirst()) { // Añadido check cursor != null
                val storedHash = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD))
                passwordMatches = JBPasswordManager.verifyPassword(plainPasswordToCheck, storedHash)
                Log.d(TAG_USER_DAO,"Verificando credenciales para $username. Coincidencia de hash: $passwordMatches")
            } else {
                Log.d(TAG_USER_DAO, "Usuario $username no encontrado para verificar credenciales (o cursor null).")
            }
        } catch (e: Exception) {
            Log.e(TAG_USER_DAO, "Error al verificar credenciales para $username", e)
            passwordMatches = false
        } finally {
            cursor?.close()
            db.close()
            Log.d(TAG_USER_DAO, "Recursos cerrados para checkUserCredentials.")
        }
        return passwordMatches
    }

    /** Actualiza el nombre de usuario (username), comprobando disponibilidad. */
    fun updateUsername(oldUsername: String, newUsername: String): Int {
        if (checkUserExists(newUsername)) {
            Log.w(TAG_USER_DAO,"Intento de actualizar a un username que ya existe: $newUsername")
            return 0
        }

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(COL_USERNAME, newUsername)
        }
        val selection = "$COL_USERNAME = ?"
        val selectionArgs = arrayOf(oldUsername)
        var count = -1
        try {
            count = db.update(TABLE_NAME, values, selection, selectionArgs)
            if (count == 1) {
                Log.d(TAG_USER_DAO,"Username actualizado de $oldUsername a $newUsername exitosamente.")
            } else if (count == 0) {
                Log.w(TAG_USER_DAO,"No se encontró al usuario $oldUsername para actualizar username.")
                count = -1 // Considerar error si no se encontró
            } else {
                Log.w(TAG_USER_DAO,"Se actualizaron $count filas para username $oldUsername (inesperado).")
                count = -1 // Considerar error si se actualizan múltiples filas
            }
        } catch (e: Exception) {
            Log.e(TAG_USER_DAO,"Error al actualizar username de $oldUsername a $newUsername", e)
            count = -1
        } finally {
            db.close()
            Log.d(TAG_USER_DAO, "Recursos cerrados para updateUsername.")
        }
        return if (count == 1) 1 else count
    }

    /** Actualiza la contraseña (hasheada) de un usuario. */
    fun updatePassword(username: String, newPlainPassword: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            val newHashedPassword = JBPasswordManager.hashPassword(newPlainPassword)
            put(COL_PASSWORD, newHashedPassword)
        }
        val selection = "$COL_USERNAME = ?"
        val selectionArgs = arrayOf(username)
        var count = 0
        try {
            count = db.update(TABLE_NAME, values, selection, selectionArgs)
            Log.d(TAG_USER_DAO, "Filas afectadas al actualizar contraseña de $username: $count")
        } catch (e: Exception) {
            Log.e(TAG_USER_DAO, "Error al actualizar contraseña de $username", e)
            count = -1
        } finally {
            db.close() // Ya estaba en finally
            Log.d(TAG_USER_DAO, "Recursos cerrados para updatePassword.")
        }
        return count == 1
    }

    /**
     * Obtiene el perfil de un usuario por su email.
     */
    fun getUserProfileByEmail(email: String): User? {
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null
        var user: User? = null
        Log.d(TAG_USER_DAO, "Obteniendo perfil por email: $email")
        try {
            val columns = arrayOf(COL_ID, COL_USERNAME, COL_PHOTO_PATH, COL_EMAIL)
            val selection = "$COL_EMAIL = ?"
            val selectionArgs = arrayOf(email)
            cursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, "1")

            if (cursor != null && cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
                val username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)) ?: "Desconocido"
                val photoPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO_PATH))
                val userEmail = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)) ?: "error@example.com"
                user = User(id, username, userEmail, photoPath) // Asegúrate que User.kt tiene email
            }
        } catch (e: Exception) {
            Log.e(TAG_USER_DAO, "Error al obtener perfil por email: $email", e)
        } finally {
            cursor?.close()
            db.close()
        }
        return user
    }

    /** Obtiene el perfil de un usuario por su username (sin contraseña). */
    fun getUserProfileByName(username: String): User? {
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null
        var user: User? = null
        Log.d(TAG_USER_DAO, "Obteniendo perfil para $username")
        try {
            val columns = arrayOf(COL_ID, COL_USERNAME, COL_PHOTO_PATH, COL_EMAIL) // Añadido COL_EMAIL a version 2 bd
            val selection = "$COL_USERNAME = ?"
            val selectionArgs = arrayOf(username)
            cursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null)

            if (cursor != null && cursor.moveToFirst()) {
                val idColIndex = cursor.getColumnIndexOrThrow(COL_ID)
                val nameColIndex = cursor.getColumnIndexOrThrow(COL_USERNAME)
                val photoPathColIndex = cursor.getColumnIndexOrThrow(COL_PHOTO_PATH)
                val emailColIndex = cursor.getColumnIndexOrThrow(COL_EMAIL) // Añadido a version 2 bd

                val id = cursor.getLong(idColIndex)
                val name = cursor.getString(nameColIndex) ?: "Usuario Desconocido"
                val profilePhotoPath = cursor.getString(photoPathColIndex)
                val email = cursor.getString(emailColIndex) ?: "email@desconocido.com" // Añadido a version 2 bd

                user = User(
                    id = id,
                    username = name,
                    profilePhotoPath = profilePhotoPath,
                    email = email //añadimos para recuperar contraseña a version 2 bd
                )
                Log.d(TAG_USER_DAO, "Perfil obtenido para $username.")
            } else {
                Log.d(TAG_USER_DAO, "Perfil no encontrado para $username (o cursor null).")
            }
        } catch (e: Exception) {
            Log.e(TAG_USER_DAO, "Error al obtener perfil para $username", e)
        } finally {
            cursor?.close() // Cerrar cursor en finally
            db.close()      // Cerrar DB en finally
            Log.d(TAG_USER_DAO, "Recursos cerrados para getUserProfile.")
        }
        return user
    }

    /** Actualiza la ruta de la imagen de perfil por ID de usuario. */
    fun updateUserProfilePicPath(userId: Long, path: String?): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(COL_PHOTO_PATH, path)
        }
        val selection = "$COL_ID = ?"
        val selectionArgs = arrayOf(userId.toString())
        var count = 0
        try {
            count = db.update(TABLE_NAME, values, selection, selectionArgs)
            Log.d(TAG_USER_DAO, "Actualizando path imagen para usuario $userId. Filas afectadas: $count")
        } catch (e: Exception) {
            Log.e(TAG_USER_DAO, "Error al actualizar path imagen para usuario $userId", e)
            count = -1
        } finally {
            db.close() // Mover close a finally
            Log.d(TAG_USER_DAO, "Recursos cerrados para updateUserProfilePicPath.")
        }
        return count == 1
    }

    /** Elimina un usuario por su username. */
    fun deleteUser(username: String): Boolean {
        val db = dbHelper.writableDatabase
        val selection = "$COL_USERNAME = ?"
        val selectionArgs = arrayOf(username)
        var count = 0
        try{
            count = db.delete(TABLE_NAME, selection, selectionArgs)
            Log.d(TAG_USER_DAO, "Filas afectadas al eliminar usuario $username: $count")
        } catch (e: Exception) {
            Log.e(TAG_USER_DAO, "Error al eliminar usuario $username", e)
            count = -1
        } finally {
            db.close() // Mover close a finally
            Log.d(TAG_USER_DAO, "Recursos cerrados para deleteUser.")
        }
        return count == 1
    }
}