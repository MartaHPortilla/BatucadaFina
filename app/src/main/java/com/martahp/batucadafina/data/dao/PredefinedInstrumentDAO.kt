package com.martahp.batucadafina.data.dao

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.Cursor
import android.util.Log
import com.martahp.batucadafina.data.DatabaseHelper
import com.martahp.batucadafina.data.DbConstants.TAG_PREDEF_INSTR_DAO
import com.martahp.batucadafina.data.DbConstants.PredefinedInstrumentTable.TABLE_NAME
import com.martahp.batucadafina.data.DbConstants.PredefinedInstrumentTable.COL_ID
import com.martahp.batucadafina.data.DbConstants.PredefinedInstrumentTable.COL_NAME
import com.martahp.batucadafina.data.DbConstants.PredefinedInstrumentTable.COL_FREQUENCY
import com.martahp.batucadafina.data.DbConstants.PredefinedInstrumentTable.COL_ICON_RES_NAME
import com.martahp.batucadafina.data.DbConstants.PredefinedInstrumentTable.COL_INFO
import com.martahp.batucadafina.model.entities.PredefinedInstrument

/**
 * DAO para gestionar las operaciones de lectura de la entidad PredefinedInstrument.
 */
class PredefinedInstrumentDAO(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    // --- CRUD PARA INSTRUMENTOS PREDEFINIDOS ---

    /** Obtiene un instrumento predefinido por su ID.
     * Retorna un objeto PredefinedInstrument o null si no se encuentra.*/
    fun getPredefinedInstrument(id: Long): PredefinedInstrument? {

        //abrir base de datos para lectura
        val db = dbHelper.readableDatabase
        var cursor: android.database.Cursor? = null //fuera para cerrar
        var predefinedInstrument: PredefinedInstrument? = null

        Log.d(TAG_PREDEF_INSTR_DAO, "Obteniendo instrumento predefinido con ID: $id")

        //bloque try catch para obtener los datos
        try {
            val columns = arrayOf(
                COL_ID,
                COL_NAME,
                COL_FREQUENCY,
                COL_INFO,
                COL_ICON_RES_NAME //añadimos el nombre del recurso de la imagen
            ) // Columnas a seleccionar
            val selection = "$COL_ID = ?" // Cláusula WHERE
            val selectionArgs = arrayOf(id.toString()) // Valores para la cláusula WHERE

            cursor = db.query(
                TABLE_NAME,
                columns,
                selection,
                selectionArgs,
                null,
                null,
                null
            )

            // Verificar si se encontraron resultados
            if (cursor != null && cursor.moveToFirst()) { // propuesta: if (cursor.moveToFirst()) rechazada por ser más seguro poner el null explicitamente
                Log.d(TAG_PREDEF_INSTR_DAO, "Instrumento predefinido encontrado con ID: $id")

                //otener los índices de las columnas
                val idColIndex = cursor.getColumnIndexOrThrow(COL_ID)
                val nameColIndex = cursor.getColumnIndexOrThrow(COL_NAME)
                val freqColIndex = cursor.getColumnIndexOrThrow(COL_FREQUENCY)
                val infoColIndex = cursor.getColumnIndexOrThrow(COL_INFO)
                val iconResNameColIndex = cursor.getColumnIndexOrThrow(COL_ICON_RES_NAME) // índice de la columna con el nombre del recurso de la imagen

                // Obtener los valores de las columnas creando valores default si no existen
                val idValue = cursor.getLong(idColIndex) // PK, asumimos no nulo
                val nameValue = cursor.getString(nameColIndex)
                    ?: "Nombre por defecto" // Proporciona un default si name es NULL en la BD
                val frequencyValue =
                    if (cursor.isNull(freqColIndex)) 0.0 else cursor.getDouble(freqColIndex) // Proporciona un default si frequency es NULL en la BD
                val infoValue: String? =
                    if (cursor.isNull(infoColIndex)) null else cursor.getString(infoColIndex) // Puede ser NULL
                val iconResNameValue = cursor.getString(iconResNameColIndex) ?: "ic_default_instrument" // Nombre del recurso de la imagen con default por seguridad

                // Crear el objeto PredefinedInstrument
                predefinedInstrument =
                    PredefinedInstrument(idValue, nameValue, frequencyValue, infoValue, iconResNameValue) // añadimos el nombre del recurso de la imagen

            } else {
                Log.d(TAG_PREDEF_INSTR_DAO, "No se encontraron resultados para el ID: $id")
            }
        } catch (e: Exception) {
            Log.e(TAG_PREDEF_INSTR_DAO, "Error al obtener instrumento predefinido con ID: $id", e)
        } finally {
            cursor?.close() // Asegúrate de cerrar el cursor en todos los casos
            db.close()
            Log.d(TAG_PREDEF_INSTR_DAO, "Base de datos cerrada.")
        }
        return predefinedInstrument
    }

    /** Obtiene todos los instrumentos predefinidos. */
    fun getAllPredefinedInstruments(): List<PredefinedInstrument> {

        //creamos una lista de instrumentos que vamos a devolver y abrimos la base de datos para lectura
        val instruments = mutableListOf<PredefinedInstrument>()
        val db = dbHelper.readableDatabase

        var cursor: android.database.Cursor? = null //fuera para cerrar

        Log.d(TAG_PREDEF_INSTR_DAO, "Obteniendo todos los instrumentos predefinidos")

        try {
            //define las columnas a seleccionar
            val columns =
                arrayOf(COL_ID, COL_NAME, COL_FREQUENCY, COL_INFO, COL_ICON_RES_NAME) //añadimos el nombre del recurso de la imagen
            val orderByAsc = "$COL_NAME ASC" //ordenar por nombre ascendente

            //consulta a la BD
            cursor =
                db.query(TABLE_NAME, columns, null, null, null, null, orderByAsc)

            if (cursor != null) { //si el cursor no es nulo obtenemos los índices de las columnas
                val idColIndex = cursor.getColumnIndexOrThrow(COL_ID)
                val nameColIndex = cursor.getColumnIndexOrThrow(COL_NAME)
                val freqColIndex = cursor.getColumnIndexOrThrow(COL_FREQUENCY)
                val infoColIndex = cursor.getColumnIndexOrThrow(COL_INFO)
                val iconResNameColIndex = cursor.getColumnIndexOrThrow(COL_ICON_RES_NAME) // índice de la columna con el nombre del recurso de la imagen

                //recorremos el cursor y obtenemos los valores de las columnas para cada instrumento
                while (cursor.moveToNext()) {
                    val idValue = cursor.getLong(idColIndex)
                    val nameValue = cursor.getString(nameColIndex) ?: "Nombre por defecto"
                    val frequencyValue = if (cursor.isNull(freqColIndex)) {
                        0.0
                    } else {
                        cursor.getDouble(freqColIndex)
                    }
                    val infoValue = if (cursor.isNull(infoColIndex)) {
                        null
                    } else {
                        cursor.getString(infoColIndex)
                    }
                    val iconResNameValue = cursor.getString(iconResNameColIndex) ?: "ic_default_instrument" // Nombre del recurso de la imagen con default por seguridad

                    //creamos un objeto PredefinedInstrument y lo añadimos a la lista
                    val instrument =
                        PredefinedInstrument(idValue, nameValue, frequencyValue, infoValue, iconResNameValue) // añadimos el nombre del recurso de la imagen
                    instruments.add(instrument)
                }
                Log.d(TAG_PREDEF_INSTR_DAO, "Instrumentos predefinidos obtenidos exitosamente.")
            } else {
                Log.d(TAG_PREDEF_INSTR_DAO, "No se encontraron instrumentos predefinidos. Cursor nulo.")
            }
        } catch (e: Exception) {
            Log.e(TAG_PREDEF_INSTR_DAO, "Error al obtener todos los instrumentos predefinidos", e)
        } finally {
            cursor?.close()
            db.close()
            Log.d(TAG_PREDEF_INSTR_DAO, "Base de datos cerrada.")
        }
        return instruments
    }

}