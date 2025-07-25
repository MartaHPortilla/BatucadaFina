package com.martahp.batucadafina.data.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import com.martahp.batucadafina.data.DatabaseHelper
import com.martahp.batucadafina.data.DbConstants
import com.martahp.batucadafina.data.DbConstants.TAG_CREATED_INSTR_DAO
import com.martahp.batucadafina.data.DbConstants.CreatedInstrumentTable.TABLE_NAME
import com.martahp.batucadafina.data.DbConstants.CreatedInstrumentTable.COL_ID
import com.martahp.batucadafina.data.DbConstants.CreatedInstrumentTable.COL_NAME
import com.martahp.batucadafina.data.DbConstants.CreatedInstrumentTable.COL_FREQUENCY
import com.martahp.batucadafina.data.DbConstants.CreatedInstrumentTable.COL_INFO
import com.martahp.batucadafina.data.DbConstants.CreatedInstrumentTable.COL_USER_ID
import com.martahp.batucadafina.data.DbConstants.CreatedInstrumentTable.COL_PHOTO_PATH
import com.martahp.batucadafina.model.entities.CreatedInstrument

/**
 * DAO para gestionar las operaciones CRUD de la entidad CreatedInstrument.
 */
class CreatedInstrumentDAO(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    /**
     * Añade un instrumento creado por un usuario específico a la base de datos.
     */
    fun addCreatedInstrument(userId: Long, name: String, frequency: Double, info: String?, photoPath: String?): Long {
        val db = dbHelper.writableDatabase
        var newId: Long

        val values = ContentValues().apply {
            put(COL_USER_ID, userId)
            put(COL_NAME, name)
            put(COL_FREQUENCY, frequency)
            if (info != null) {
                put(COL_INFO, info)
            } else {
                putNull(COL_INFO)
            }
            if (photoPath != null) {
                put(COL_PHOTO_PATH, photoPath)
            } else {
                putNull(COL_PHOTO_PATH)

            }
        }

        try {
            newId = db.insert(TABLE_NAME, null, values)
            if (newId == -1L) {
                Log.e(TAG_CREATED_INSTR_DAO, "Error al insertar instrumento creado para usuario ID: $userId, nombre: $name")
            } else {
                Log.d(TAG_CREATED_INSTR_DAO, "Instrumento creado insertado con ID: $newId para usuario ID: $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG_CREATED_INSTR_DAO, "Excepción al insertar instrumento creado para usuario ID: $userId", e)
            newId = -1
        } finally {
            db.close()
            Log.d(TAG_CREATED_INSTR_DAO, "Recursos cerrados para addCreatedInstrument.")
        }
        return newId
    }

    /**
     * Obtiene un instrumento creado por su ID.
     */
    fun getCreatedInstrument(id: Long): CreatedInstrument? {
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null
        var createdInstrument: CreatedInstrument? = null

        Log.d(TAG_CREATED_INSTR_DAO, "Buscando instrumento creado con ID: $id")

        try {
            val columns = arrayOf(COL_ID, COL_NAME, COL_FREQUENCY, COL_INFO, COL_USER_ID, COL_PHOTO_PATH)
            val selection = "$COL_ID = ?"
            val selectionArgs = arrayOf(id.toString())

            cursor = db.query(
                TABLE_NAME,
                columns, selection, selectionArgs,
                null, null, null
            )

            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG_CREATED_INSTR_DAO, "Instrumento creado encontrado con ID: $id")

                val idColIndex = cursor.getColumnIndexOrThrow(COL_ID)
                val nameColIndex = cursor.getColumnIndexOrThrow(COL_NAME)
                val freqColIndex = cursor.getColumnIndexOrThrow(COL_FREQUENCY)
                val infoColIndex = cursor.getColumnIndexOrThrow(COL_INFO)
                val userIdColIndex = cursor.getColumnIndexOrThrow(COL_USER_ID)
                val photoPathColIndex = cursor.getColumnIndexOrThrow(COL_PHOTO_PATH)

                val idValue = cursor.getLong(idColIndex)
                val nameValue = cursor.getString(nameColIndex) ?: "Nombre por defecto"
                val frequencyValue = if (cursor.isNull(freqColIndex)) 0.0 else cursor.getDouble(freqColIndex)
                val infoValue = if (cursor.isNull(infoColIndex)) null else cursor.getString(infoColIndex)
                val userIdValue = cursor.getLong(userIdColIndex)
                val photoPathValue = if (cursor.isNull(photoPathColIndex)) null else cursor.getString(photoPathColIndex)

                createdInstrument = CreatedInstrument(idValue, nameValue, frequencyValue, infoValue, userIdValue, photoPathValue)
            } else {
                Log.d(TAG_CREATED_INSTR_DAO, "No se encontraron resultados para instrumento creado con ID: $id (o cursor es null)")
            }
        } catch (e: Exception) {
            Log.e(TAG_CREATED_INSTR_DAO, "Error al obtener instrumento creado con ID: $id", e)
        } finally {
            cursor?.close()
            db.close()
            Log.d(TAG_CREATED_INSTR_DAO, "Recursos cerrados para getCreatedInstrument ID: $id")
        }
        return createdInstrument
    }

    /**
     * Obtiene todos los instrumentos creados por un usuario específico.
     */
    fun getAllCreatedInstrumentsForUser(userId: Long): List<CreatedInstrument> {
        val instruments = mutableListOf<CreatedInstrument>()
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null

        Log.d(TAG_CREATED_INSTR_DAO, "Obteniendo instrumentos creados por usuario ID: $userId")

        try {
            val columns = arrayOf(COL_ID, COL_NAME, COL_FREQUENCY, COL_INFO, COL_USER_ID, COL_PHOTO_PATH)
            val selection = "$COL_USER_ID = ?"
            val selectionArgs = arrayOf(userId.toString())
            val orderBy = "$COL_NAME ASC"

            cursor = db.query(
                TABLE_NAME,
                columns, selection, selectionArgs,
                null, null, orderBy
            )

            if (cursor != null) {
                val idColIndex = cursor.getColumnIndexOrThrow(COL_ID)
                val nameColIndex = cursor.getColumnIndexOrThrow(COL_NAME)
                val freqColIndex = cursor.getColumnIndexOrThrow(COL_FREQUENCY)
                val infoColIndex = cursor.getColumnIndexOrThrow(COL_INFO)
                val userIdColIndex = cursor.getColumnIndexOrThrow(COL_USER_ID)
                val photoPathColIndex = cursor.getColumnIndexOrThrow(COL_PHOTO_PATH)

                while (cursor.moveToNext()) {
                    val idValue = cursor.getLong(idColIndex)
                    val nameValue = cursor.getString(nameColIndex) ?: "Nombre por defecto"
                    val frequencyValue = if (cursor.isNull(freqColIndex)) 0.0 else cursor.getDouble(freqColIndex)
                    val infoValue = if (cursor.isNull(infoColIndex)) null else cursor.getString(infoColIndex)
                    val userIdValue = cursor.getLong(userIdColIndex)
                    val photoPathValue = if (cursor.isNull(photoPathColIndex)) null else cursor.getString(photoPathColIndex)

                    val instrument = CreatedInstrument(idValue, nameValue, frequencyValue, infoValue, userIdValue, photoPathValue)
                    instruments.add(instrument)
                }
                Log.d(TAG_CREATED_INSTR_DAO, "Se encontraron ${instruments.size} instrumentos creados para el usuario ID: $userId.")
            } else {
                Log.d(TAG_CREATED_INSTR_DAO, "El cursor es null al obtener instrumentos creados para el usuario ID: $userId.")
            }
        } catch (e: Exception) {
            Log.e(TAG_CREATED_INSTR_DAO, "Error al obtener instrumentos creados para usuario ID: $userId", e)
        } finally {
            cursor?.close()
            db.close()
            Log.d(TAG_CREATED_INSTR_DAO, "Recursos cerrados para getAllCreatedInstrumentsForUser ID: $userId.")
        }
        return instruments
    }

    /**
     * Actualiza los datos de un instrumento creado existente.
     */
    fun updateCreatedInstrument(instrumentId: Long, name: String, frequency: Double, info: String?, photoPath: String?): Boolean {
        val db = dbHelper.writableDatabase
        var count = 0

        Log.d(TAG_CREATED_INSTR_DAO, "Actualizando instrumento creado con ID: $instrumentId")

        val values = ContentValues().apply {
            put(COL_NAME, name)
            put(COL_FREQUENCY, frequency)
            if (info != null) {
                put(COL_INFO, info)
            } else {
                putNull(COL_INFO)
            }
            if (photoPath != null) {
                put(COL_PHOTO_PATH, photoPath)
            } else {
                putNull(COL_PHOTO_PATH)
            }
        }
        val whereClause = "$COL_ID = ?"
        val whereArgs = arrayOf(instrumentId.toString())

        try {
            // Usar constante importada
            count = db.update(TABLE_NAME, values, whereClause, whereArgs)
            if (count == 1) {
                Log.d(TAG_CREATED_INSTR_DAO, "Instrumento creado ID $instrumentId actualizado exitosamente.")
            } else if (count == 0) {
                Log.w(TAG_CREATED_INSTR_DAO, "No se encontró instrumento creado con ID $instrumentId para actualizar.")
            } else {
                Log.w(TAG_CREATED_INSTR_DAO, "Se actualizaron $count filas para instrumento creado ID $instrumentId (inesperado).")
            }
        } catch (e: Exception) {
            Log.e(TAG_CREATED_INSTR_DAO, "Error al actualizar instrumento creado con ID: $instrumentId", e)
            count = -1
        } finally {
            db.close()
            Log.d(TAG_CREATED_INSTR_DAO, "Recursos cerrados para updateCreatedInstrument ID: $instrumentId")
        }
        return count == 1
    }

    /**
     * Elimina un instrumento creado de la base de datos por su ID.
     */
    fun deleteCreatedInstrumentById(instrumentId: Long): Boolean {
        val db = dbHelper.writableDatabase
        var count = 0

        Log.d(TAG_CREATED_INSTR_DAO, "Intentando eliminar instrumento creado con ID: $instrumentId")

        val whereClause = "$COL_ID = ?"
        val whereArgs = arrayOf(instrumentId.toString())

        try{
            count = db.delete(TABLE_NAME, whereClause, whereArgs)
            if (count == 1) {
                Log.d(TAG_CREATED_INSTR_DAO, "Instrumento creado ID $instrumentId eliminado exitosamente.")
            } else if (count == 0) {
                Log.w(TAG_CREATED_INSTR_DAO, "No se encontró instrumento creado con ID $instrumentId para eliminar.")
            } else {
                Log.w(TAG_CREATED_INSTR_DAO, "Se eliminaron $count filas para instrumento creado ID $instrumentId (inesperado).")
            }
        } catch (e: Exception) {
            Log.e(TAG_CREATED_INSTR_DAO, "Error al eliminar instrumento creado con ID: $instrumentId", e)
            count = -1
        } finally {
            db.close()
            Log.d(TAG_CREATED_INSTR_DAO, "Recursos cerrados para deleteCreatedInstrument ID: $instrumentId")
        }
        return count == 1
    }


    /**
     * Obtiene un instrumento creado específico por su ID y el ID del usuario propietario.
     * Devuelve null si no se encuentra o si el instrumento no pertenece al usuario.
     */
    fun getCreatedInstrumentByIdAndUser(instrumentId: Long, userId: Long): CreatedInstrument? {
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null
        var instrument: CreatedInstrument? = null
        Log.d(TAG_CREATED_INSTR_DAO, "Buscando instrumento creado con ID: $instrumentId para usuario ID: $userId")

        try {
            val columns = arrayOf(
                COL_ID,
                COL_NAME,
                COL_FREQUENCY,
                COL_INFO,
                COL_USER_ID,
                COL_PHOTO_PATH
            )
            // Cláusula WHERE para buscar por ID de instrumento Y ID de usuario
            val selection = "$COL_ID = ? AND $COL_USER_ID = ?"
            val selectionArgs = arrayOf(instrumentId.toString(), userId.toString())

            cursor = db.query(
                TABLE_NAME,
                columns,
                selection,
                selectionArgs,
                null, null, null, "1"
            )

            if (cursor != null && cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME))
                val frequency = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_FREQUENCY))
                val info = cursor.getString(cursor.getColumnIndexOrThrow(COL_INFO))
                val ownerUserId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_ID))
                val photoPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO_PATH))

                // Doble verificación de que el instrumento realmente pertenece al usuario (aunque la query ya lo filtra)
                if (ownerUserId == userId) {
                    instrument = CreatedInstrument(id, name, frequency, info, ownerUserId, photoPath)
                    Log.d(TAG_CREATED_INSTR_DAO, "Instrumento creado encontrado: ${instrument.name}")
                } else {
                    Log.w(TAG_CREATED_INSTR_DAO, "Instrumento ID $instrumentId encontrado pero no pertenece al usuario ID $userId.")
                }
            } else {
                Log.d(TAG_CREATED_INSTR_DAO, "No se encontró instrumento creado con ID: $instrumentId para usuario ID: $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG_CREATED_INSTR_DAO, "Error al obtener instrumento creado por ID y Usuario.", e)
        } finally {
            cursor?.close()
            db.close()
        }
        return instrument
    }

    /**
     * Actualiza los datos de un instrumento creado existente.
     * Se identifica el instrumento por su 'id' dentro del objeto 'instrument'.
     * Solo se actualizan nombre, frecuencia, información y photoPath. El userId no debería cambiar.
     *
     * @param instrument El objeto CreatedInstrument con los datos actualizados (incluyendo su ID original).
     * @return true si la actualización fue exitosa (1 fila afectada), false en caso contrario.
     */
    fun updateCreatedInstrument(instrument: CreatedInstrument): Boolean {
        val db = dbHelper.writableDatabase
        Log.d(TAG_CREATED_INSTR_DAO, "Actualizando instrumento creado con ID: ${instrument.id}")

        val values = ContentValues().apply {
            put(COL_NAME, instrument.name)
            put(COL_FREQUENCY, instrument.frequency)
            put(COL_INFO, instrument.information)
            put(COL_PHOTO_PATH, instrument.photoPath)
            // No actualizamos COL_USER_ID ni COL_ID aquí (COL_ID se usa en el WHERE)
        }

        // Cláusula WHERE para actualizar solo el instrumento con el ID correcto
        // Y (opcional pero recomendado para seguridad) que pertenezca al usuario correcto
        val selection = "${COL_ID} = ? AND ${COL_USER_ID} = ?"
        val selectionArgs = arrayOf(instrument.id.toString(), instrument.userId.toString())
        // Si instrument.userId no estuviera disponible aquí, tendrías que pasarlo como parámetro.

        var rowsAffected = 0
        try {
            rowsAffected = db.update(/* table = */ TABLE_NAME, /* values = */
                values, /* whereClause = */
                selection, /* whereArgs = */
                selectionArgs)
            if (rowsAffected == 1) {
                Log.d(TAG_CREATED_INSTR_DAO, "Instrumento creado ID ${instrument.id} actualizado exitosamente.")
            } else if (rowsAffected == 0) {
                Log.w(TAG_CREATED_INSTR_DAO, "No se encontró instrumento creado con ID ${instrument.id} (o no pertenece al usuario) para actualizar.")
            } else {
                Log.w(TAG_CREATED_INSTR_DAO, "Se actualizaron $rowsAffected filas para instrumento ID ${instrument.id} (inesperado).")
            }
        } catch (e: Exception) {
            Log.e(TAG_CREATED_INSTR_DAO, "Error al actualizar instrumento creado ID: ${instrument.id}", e)
            rowsAffected = -1 // Indicar error
        } finally {
            db.close()
        }
        return rowsAffected == 1
    }
}