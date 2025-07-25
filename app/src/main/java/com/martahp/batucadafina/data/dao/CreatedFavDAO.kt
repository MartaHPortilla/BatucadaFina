package com.martahp.batucadafina.data.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.martahp.batucadafina.data.DatabaseHelper
import com.martahp.batucadafina.data.DbConstants.TAG_CREATED_FAV_DAO
import com.martahp.batucadafina.data.DbConstants.CreatedFavoriteTable
import com.martahp.batucadafina.data.DbConstants.CreatedInstrumentTable
import com.martahp.batucadafina.model.entities.CreatedInstrument

/**
 * DAO para gestionar las operaciones relacionadas con los favoritos
 * de los Instrumentos Creados por los usuarios.
 */
class CreatedFavDAO(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    /**
     * Marca o desmarca un instrumento creado como favorito para un usuario.
     */
    fun setCreatedFavoriteStatus(userId: Long, createdInstrumentId: Long, isFavorite: Boolean) {
        val db = dbHelper.writableDatabase
        Log.d(TAG_CREATED_FAV_DAO, "Estableciendo estado favorito=$isFavorite para User $userId - Created $createdInstrumentId")

        try {
            if (isFavorite) {
                val values = ContentValues().apply {
                    put(CreatedFavoriteTable.COL_USER_ID, userId)
                    put(CreatedFavoriteTable.COL_INSTRUMENT_ID, createdInstrumentId)
                }
                db.insertWithOnConflict(CreatedFavoriteTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
                Log.d(TAG_CREATED_FAV_DAO, "Relación favorito creado insertada/ignorada.")
            } else {
                val whereClause = "${CreatedFavoriteTable.COL_USER_ID} = ? AND ${CreatedFavoriteTable.COL_INSTRUMENT_ID} = ?"
                val whereArgs = arrayOf(userId.toString(), createdInstrumentId.toString())
                val deletedRows = db.delete(CreatedFavoriteTable.TABLE_NAME, whereClause, whereArgs)
                Log.d(TAG_CREATED_FAV_DAO, "Relación favorito creado eliminada (filas afectadas: $deletedRows).")
            }
        } catch (e: Exception) {
            Log.e(TAG_CREATED_FAV_DAO, "Error al establecer estado favorito creado", e)
        } finally {
            db.close()
            Log.d(TAG_CREATED_FAV_DAO, "Recursos cerrados para setCreatedFavoriteStatus.")
        }
    }

    /**
     * Comprueba si un instrumento creado específico es favorito para un usuario específico.
     */
    fun isCreatedFavorite(userId: Long, createdInstrumentId: Long): Boolean {
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null
        var isFavorite = false

        Log.d(TAG_CREATED_FAV_DAO, "Comprobando si Created $createdInstrumentId es favorito para User $userId")

        try {
            val columns = arrayOf(CreatedFavoriteTable.COL_USER_ID)
            val selection = "${CreatedFavoriteTable.COL_USER_ID} = ? AND ${CreatedFavoriteTable.COL_INSTRUMENT_ID} = ?"
            val selectionArgs = arrayOf(userId.toString(), createdInstrumentId.toString())

            cursor = db.query(
                CreatedFavoriteTable.TABLE_NAME, columns, selection, selectionArgs,
                null, null, "1"
            )

            isFavorite = (cursor != null && cursor.count > 0)
            Log.d(TAG_CREATED_FAV_DAO, "Resultado comprobación favorito creado: $isFavorite")

        } catch (e: Exception) {
            Log.e(TAG_CREATED_FAV_DAO, "Error comprobando si Created $createdInstrumentId es favorito para User $userId", e)
            isFavorite = false
        } finally {
            cursor?.close()
            db.close()
            Log.d(TAG_CREATED_FAV_DAO, "Recursos cerrados para isCreatedFavorite.")
        }
        return isFavorite
    }

    /**
     * Obtiene la lista de IDs de instrumentos CREADOS marcados como favoritos
     * por un usuario específico.
     */
    fun getFavoriteCreatedInstrumentIdsForUser(userId: Long): List<Long> {
        val favoriteIds = mutableListOf<Long>()
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null

        Log.d(TAG_CREATED_FAV_DAO, "Obteniendo IDs de creados favoritos para User ID: $userId")

        try {
            val columns = arrayOf(CreatedFavoriteTable.COL_INSTRUMENT_ID)
            val selection = "${CreatedFavoriteTable.COL_USER_ID} = ?"
            val selectionArgs = arrayOf(userId.toString())

            cursor = db.query(
                CreatedFavoriteTable.TABLE_NAME, columns, selection, selectionArgs,
                null, null, null
            )

            if (cursor != null) {
                val instrumentIdColIndex = cursor.getColumnIndexOrThrow(CreatedFavoriteTable.COL_INSTRUMENT_ID)
                while (cursor.moveToNext()) {
                    val instrumentId = cursor.getLong(instrumentIdColIndex)
                    favoriteIds.add(instrumentId)
                }
                Log.d(TAG_CREATED_FAV_DAO, "Se encontraron ${favoriteIds.size} IDs de creados favoritos para User ID: $userId.")
            } else {
                Log.d(TAG_CREATED_FAV_DAO, "Cursor es null al obtener IDs de creados favoritos para User ID: $userId.")
            }
        } catch (e: Exception) {
            Log.e(TAG_CREATED_FAV_DAO, "Error al obtener IDs de creados favoritos para User ID: $userId", e)
        } finally {
            cursor?.close()
            db.close()
            Log.d(TAG_CREATED_FAV_DAO, "Recursos cerrados para getFavoriteCreatedInstrumentIdsForUser ID: $userId.")
        }
        return favoriteIds
    }

    /**
     * Obtiene la lista completa de objetos CreatedInstrument marcados como favoritos
     * por un usuario específico. Requiere un JOIN entre tablas.
     */
    fun getFavoriteCreatedInstrumentsForUser(userId: Long): List<CreatedInstrument> {
        val instruments = mutableListOf<CreatedInstrument>()
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null

        Log.d(TAG_CREATED_FAV_DAO, "Obteniendo objetos CreatedInstrument favoritos para User ID: $userId")

        val sqlQuery = """
            SELECT C.${CreatedInstrumentTable.COL_ID}, 
                    C.${CreatedInstrumentTable.COL_NAME}, 
                    C.${CreatedInstrumentTable.COL_FREQUENCY}, 
                    C.${CreatedInstrumentTable.COL_INFO}, 
                    C.${CreatedInstrumentTable.COL_USER_ID},
                    C.${CreatedInstrumentTable.COL_PHOTO_PATH}
            FROM ${CreatedInstrumentTable.TABLE_NAME} C
            JOIN ${CreatedFavoriteTable.TABLE_NAME} F 
            ON C.${CreatedInstrumentTable.COL_ID} = F.${CreatedFavoriteTable.COL_INSTRUMENT_ID}
            WHERE F.${CreatedFavoriteTable.COL_USER_ID} = ?
            ORDER BY C.${CreatedInstrumentTable.COL_NAME} ASC
        """.trimIndent()
        val selectionArgs = arrayOf(userId.toString())

        try {
            cursor = db.rawQuery(sqlQuery, selectionArgs)

            if (cursor != null) {
                val idColIndex = cursor.getColumnIndexOrThrow(CreatedInstrumentTable.COL_ID)
                val nameColIndex = cursor.getColumnIndexOrThrow(CreatedInstrumentTable.COL_NAME)
                val freqColIndex = cursor.getColumnIndexOrThrow(CreatedInstrumentTable.COL_FREQUENCY)
                val infoColIndex = cursor.getColumnIndexOrThrow(CreatedInstrumentTable.COL_INFO)
                val userIdColIndex = cursor.getColumnIndexOrThrow(CreatedInstrumentTable.COL_USER_ID)
                val photoPathColIndex = cursor.getColumnIndexOrThrow(CreatedInstrumentTable.COL_PHOTO_PATH)

                while (cursor.moveToNext()) {
                    val idValue = cursor.getLong(idColIndex)
                    val nameValue = cursor.getString(nameColIndex) ?: "Nombre por defecto"
                    val frequencyValue = if (cursor.isNull(freqColIndex)) 0.0 else cursor.getDouble(freqColIndex)
                    val infoValue = if (cursor.isNull(infoColIndex)) null else cursor.getString(infoColIndex)
                    val ownerUserIdValue = cursor.getLong(userIdColIndex)
                    val photoPathValue = if (cursor.isNull(photoPathColIndex)) null else cursor.getString(photoPathColIndex)

                    val instrument = CreatedInstrument(idValue, nameValue, frequencyValue, infoValue, ownerUserIdValue, photoPathValue)
                    instruments.add(instrument)
                }
                Log.d(TAG_CREATED_FAV_DAO, "Se obtuvieron ${instruments.size} objetos CreatedInstrument favoritos para User ID: $userId.")
            } else {
                Log.d(TAG_CREATED_FAV_DAO, "Cursor es null al obtener objetos CreatedInstrument favoritos para User ID: $userId.")
            }
        } catch (e: Exception) {
            Log.e(TAG_CREATED_FAV_DAO, "Error al obtener objetos CreatedInstrument favoritos para User ID: $userId", e)
        } finally {
            cursor?.close()
            db.close()
            Log.d(TAG_CREATED_FAV_DAO, "Recursos cerrados para getFavoriteCreatedInstrumentsForUser ID: $userId.")
        }
        return instruments
    }

}