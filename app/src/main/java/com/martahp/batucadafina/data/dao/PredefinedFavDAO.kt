package com.martahp.batucadafina.data.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.martahp.batucadafina.data.DatabaseHelper
import com.martahp.batucadafina.data.DbConstants.TAG_PREDEF_FAV_DAO
import com.martahp.batucadafina.data.DbConstants.PredefinedInstrumentTable
import com.martahp.batucadafina.data.DbConstants.PredefinedFavoriteTable

import com.martahp.batucadafina.model.entities.PredefinedInstrument

/**
 * DAO para gestionar las operaciones relacionadas con los favoritos
 * de los Instrumentos Predefinidos.
 */
class PredefinedFavDAO(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    // --------------------------------------------------------------------
    // AQUÍ PEGAREMOS LAS FUNCIONES DE FAVORITOS PREDEFINIDOS
    // --------------------------------------------------------------------


    /**
     * Marca o desmarca un instrumento predefinido como favorito para un usuario.
     * @param userId El ID del usuario.
     * @param predefinedInstrumentId El ID del instrumento predefinido.
     * @param isFavorite true para marcar como favorito, false para desmarcar.
     */
    fun setPredefinedFavoriteStatus(userId: Long, predefinedInstrumentId: Long, isFavorite: Boolean) {
        // 1. Obtener BD en modo escritura
        val db = dbHelper.writableDatabase
        // 2. Log inicial (Usar TAG)
        Log.d(TAG_PREDEF_FAV_DAO, "Estableciendo estado favorito=$isFavorite para User $userId - Predef $predefinedInstrumentId")

        try {
            if (isFavorite) {
                // 3a. Marcar como favorito: Preparar valores
                val values = ContentValues().apply {
                    put(PredefinedFavoriteTable.COL_USER_ID, userId)
                    put(PredefinedFavoriteTable.COL_INSTRUMENT_ID, predefinedInstrumentId)
                }
                // 3b. Insertar usando CONFLICT_IGNORE (no falla si ya existe)
                db.insertWithOnConflict(PredefinedFavoriteTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
                Log.d(TAG_PREDEF_FAV_DAO, "Relación favorito predefinido insertada/ignorada.")
            } else {
                // 4a. Desmarcar como favorito: Preparar condiciones WHERE
                val whereClause = "${PredefinedFavoriteTable.COL_USER_ID} = ? AND ${PredefinedFavoriteTable.COL_INSTRUMENT_ID} = ?"
                val whereArgs = arrayOf(userId.toString(), predefinedInstrumentId.toString())
                // 4b. Ejecutar borrado
                val deletedRows = db.delete(PredefinedFavoriteTable.TABLE_NAME, whereClause, whereArgs)
                Log.d(TAG_PREDEF_FAV_DAO, "Relación favorito predefinido eliminada (filas afectadas: $deletedRows).")
            }
        } catch (e: Exception) {
            // 5. Manejo de errores
            Log.e(TAG_PREDEF_FAV_DAO, "Error al establecer estado favorito predefinido", e)
        }
        finally {
            // 6. Cerrar DB siempre
            db.close()
            Log.d(TAG_PREDEF_FAV_DAO, "Recursos cerrados para setPredefinedFavoriteStatus.")
        }
    }

    /**
     * Comprueba si un instrumento predefinido específico es favorito para un usuario específico.
     * @param userId El ID del usuario.
     * @param predefinedInstrumentId El ID del instrumento predefinido.
     * @return true si es favorito, false si no lo es o si hay error.
     */
    fun isPredefinedFavorite(userId: Long, predefinedInstrumentId: Long): Boolean {
        // 1. Obtener BD legible
        val db = dbHelper.readableDatabase // Usar dbHelper
        var cursor: Cursor? = null
        var isFavorite = false

        // 2. Log inicial
        Log.d(TAG_PREDEF_FAV_DAO, "Comprobando si Predef $predefinedInstrumentId es favorito para User $userId")

        try {
            // 3. Definir columnas (solo necesitamos saber si existe)
            val columns = arrayOf(PredefinedFavoriteTable.COL_USER_ID)
            // 4. Definir WHERE para buscar la combinación exacta
            val selection = "${PredefinedFavoriteTable.COL_USER_ID} = ? AND ${PredefinedFavoriteTable.COL_INSTRUMENT_ID} = ?"
            val selectionArgs = arrayOf(userId.toString(), predefinedInstrumentId.toString())

            // 5. Ejecutar consulta con LIMIT 1 (optimización)
            cursor = db.query(
                PredefinedFavoriteTable.TABLE_NAME, columns, selection, selectionArgs,
                null, null, "1" // LIMIT 1
            )

            // 6. Comprobar si se encontró alguna fila
            isFavorite = (cursor != null && cursor.count > 0)
            Log.d(TAG_PREDEF_FAV_DAO, "Resultado comprobación favorito: $isFavorite")

        } catch (e: Exception) {
            // 7. Manejo de errores
            Log.e(TAG_PREDEF_FAV_DAO, "Error comprobando si Predef $predefinedInstrumentId es favorito para User $userId", e)
            isFavorite = false // marcar como falso en caso de error
        } finally {
            // 8. Cerrar recursos
            cursor?.close()
            db.close()
            Log.d(TAG_PREDEF_FAV_DAO, "Recursos cerrados para isPredefinedFavorite.")
        }
        // 9. Devolver resultado
        return isFavorite
    }

    /**
     * Obtiene la lista de IDs de instrumentos predefinidos marcados como favoritos
     * por un usuario específico.
     */
    fun getFavoritePredefinedInstrumentIdsForUser(userId: Long): List<Long> {
        // 1. Inicializar lista y obtener BD
        val favoriteIds = mutableListOf<Long>()
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null

        // 2. Log inicial (Usar TAG)
        Log.d(TAG_PREDEF_FAV_DAO, "Obteniendo IDs de predefinidos favoritos para User ID: $userId")

        try {
            // 3. Definir columna a obtener (solo el ID del instrumento)
            val columns = arrayOf(PredefinedFavoriteTable.COL_INSTRUMENT_ID)
            // 4. Definir filtro WHERE por userId
            val selection = "${PredefinedFavoriteTable.COL_USER_ID} = ?"
            val selectionArgs = arrayOf(userId.toString())

            // 5. Ejecutar consulta
            cursor = db.query(
                PredefinedFavoriteTable.TABLE_NAME, columns, selection, selectionArgs,
                null, null, null
            )

            // 6. Procesar cursor si no es nulo
            if (cursor != null) {
                // Obtener índice antes del bucle
                val instrumentIdColIndex = cursor.getColumnIndexOrThrow(PredefinedFavoriteTable.COL_INSTRUMENT_ID)
                // 7. Recorrer filas
                while (cursor.moveToNext()) {
                    // 8. Extraer ID y añadir a lista
                    val instrumentId = cursor.getLong(instrumentIdColIndex)
                    favoriteIds.add(instrumentId)
                }
                Log.d(TAG_PREDEF_FAV_DAO, "Se encontraron ${favoriteIds.size} IDs de predefinidos favoritos para User ID: $userId.")
            } else {
                Log.d(TAG_PREDEF_FAV_DAO, "Cursor es null al obtener IDs de predefinidos favoritos para User ID: $userId.")
            }
        } catch (e: Exception) {
            // 9. Manejo de errores
            Log.e(TAG_PREDEF_FAV_DAO, "Error al obtener IDs de predefinidos favoritos para User ID: $userId", e)
        } finally {
            // 10. Cerrar recursos
            cursor?.close()
            db.close()
            Log.d(TAG_PREDEF_FAV_DAO, "Recursos cerrados para getFavoritePredefinedInstrumentIdsForUser ID: $userId.")
        }
        // 11. Devolver lista de IDs
        return favoriteIds
    }

    /**
     * Obtiene la lista completa de objetos PredefinedInstrument marcados como favoritos
     * por un usuario específico. Requiere un JOIN entre tablas.
     */
    fun getFavoritePredefinedInstrumentsForUser(userId: Long): List<PredefinedInstrument> {
        // 1. Inicializar lista y obtener BD
        val instruments = mutableListOf<PredefinedInstrument>()
        val db = dbHelper.readableDatabase // Usar dbHelper
        var cursor: Cursor? = null

        // 2. Log inicial (Usar TAG)
        Log.d(TAG_PREDEF_FAV_DAO, "Obteniendo objetos PredefinedInstrument favoritos para User ID: $userId")

        // 3. Construir la consulta SQL con JOIN
        val sqlQuery = """
            SELECT P.${PredefinedInstrumentTable.COL_ID}, 
                    P.${PredefinedInstrumentTable.COL_NAME}, 
                    P.${PredefinedInstrumentTable.COL_FREQUENCY}, 
                    P.${PredefinedInstrumentTable.COL_INFO},
                    P.${PredefinedInstrumentTable.COL_ICON_RES_NAME}
            FROM ${PredefinedInstrumentTable.TABLE_NAME} P
            JOIN ${PredefinedFavoriteTable.TABLE_NAME} F 
            ON P.${PredefinedInstrumentTable.COL_ID} = F.${PredefinedFavoriteTable.COL_INSTRUMENT_ID}
            WHERE F.${PredefinedFavoriteTable.COL_USER_ID} = ?
            ORDER BY P.${PredefinedInstrumentTable.COL_NAME} ASC
        """.trimIndent()
        // 4. Definir argumentos para el WHERE
        val selectionArgs = arrayOf(userId.toString())

        try {
            // 5. Ejecutar consulta rawQuery
            cursor = db.rawQuery(sqlQuery, selectionArgs)

            // 6. Procesar cursor
            if (cursor != null) {
                // Obtener índices antes del bucle
                val idColIndex = cursor.getColumnIndexOrThrow(PredefinedInstrumentTable.COL_ID)
                val nameColIndex = cursor.getColumnIndexOrThrow(PredefinedInstrumentTable.COL_NAME)
                val freqColIndex = cursor.getColumnIndexOrThrow(PredefinedInstrumentTable.COL_FREQUENCY)
                val infoColIndex = cursor.getColumnIndexOrThrow(PredefinedInstrumentTable.COL_INFO)
                val iconColIndex = cursor.getColumnIndexOrThrow(PredefinedInstrumentTable.COL_ICON_RES_NAME)

                // 7. Recorrer filas
                while (cursor.moveToNext()) {
                    // 8. Extraer valores (con manejo de nulos/defaults)
                    val idValue = cursor.getLong(idColIndex)
                    val nameValue = cursor.getString(nameColIndex) ?: "Nombre por defecto"
                    val frequencyValue = if (cursor.isNull(freqColIndex)) 0.0 else cursor.getDouble(freqColIndex)
                    val infoValue = if (cursor.isNull(infoColIndex)) null else cursor.getString(infoColIndex)
                    val iconValue = cursor.getString(iconColIndex) ?: "ic_default_instrument"

                    // 9. Crear objeto y añadir a lista
                    val instrument = PredefinedInstrument(idValue, nameValue, frequencyValue, infoValue, iconValue)
                    instruments.add(instrument)
                }
                Log.d(TAG_PREDEF_FAV_DAO, "Se obtuvieron ${instruments.size} objetos PredefinedInstrument favoritos para User ID: $userId.")
            } else {
                Log.d(TAG_PREDEF_FAV_DAO, "Cursor es null al obtener objetos PredefinedInstrument favoritos para User ID: $userId.")
            }
        } catch (e: Exception) {
            // 10. Manejo de errores
            Log.e(TAG_PREDEF_FAV_DAO, "Error al obtener objetos PredefinedInstrument favoritos para User ID: $userId", e)
        } finally {
            // 11. Cerrar recursos
            cursor?.close()
            db.close()
            Log.d(TAG_PREDEF_FAV_DAO, "Recursos cerrados para getFavoritePredefinedInstrumentsForUser ID: $userId.")
        }
        // 12. Devolver lista
        return instruments
    }

}