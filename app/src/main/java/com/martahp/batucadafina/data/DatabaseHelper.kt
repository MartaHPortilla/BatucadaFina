package com.martahp.batucadafina.data

// Importar SOLO el objeto de constantes centralizado
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * Gestiona la creación y actualización (migración) del esquema de la base de datos SQLite.
 * NO contiene lógica CRUD pública, esta se delega a los DAOs.
 * Utiliza constantes definidas en DbConstants.
 */
class DatabaseHelper(context: Context) :
// Usa constantes de DbConstants en el constructor
    SQLiteOpenHelper(context, DbConstants.DATABASE_NAME, null, DbConstants.DATABASE_VERSION) {


    /**
     * Se llama SOLO la primera vez que se crea la base de datos.
     * Define la estructura COMPLETA para la VERSIÓN 1 usando constantes de DbConstants.
     */
    override fun onCreate(db: SQLiteDatabase?) {
        Log.i(
            DbConstants.TAG_HELPER,
            "onCreate: INICIO: Creando base de datos v${DbConstants.DATABASE_VERSION}..."
        )
        if (db == null) {
            Log.e(DbConstants.TAG_HELPER, "onCreate: db es null, no se pueden crear tablas.")
            return
        }
        try {
            Log.d(DbConstants.TAG_HELPER, "OnCreate: INICIO. Dentro del try antes de crear tablas...")
            // --- CREACION DE TABLAS ---
            val CREATE_USERS_TABLE =
                ("CREATE TABLE ${DbConstants.UserTable.TABLE_NAME} ("
                        + "${DbConstants.UserTable.COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "${DbConstants.UserTable.COL_USERNAME} TEXT UNIQUE NOT NULL,"
                        + "${DbConstants.UserTable.COL_EMAIL} TEXT UNIQUE NOT NULL DEFAULT 'noemail@provided.com'," //añadimos para recuperar contraseña
                        + "${DbConstants.UserTable.COL_PASSWORD} TEXT NOT NULL,"
                        + "${DbConstants.UserTable.COL_PHOTO_PATH} TEXT NULL)")
            db.execSQL(CREATE_USERS_TABLE)
            Log.d(DbConstants.TAG_HELPER, "Tabla ${DbConstants.UserTable.TABLE_NAME} creada.")

            val CREATE_PREDEFINED_INSTRUMENTS_TABLE =
                ("CREATE TABLE ${DbConstants.PredefinedInstrumentTable.TABLE_NAME} ("
                        + "${DbConstants.PredefinedInstrumentTable.COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "${DbConstants.PredefinedInstrumentTable.COL_NAME} TEXT NOT NULL,"
                        + "${DbConstants.PredefinedInstrumentTable.COL_FREQUENCY} REAL,"
                        + "${DbConstants.PredefinedInstrumentTable.COL_INFO} TEXT,"
                        + "${DbConstants.PredefinedInstrumentTable.COL_ICON_RES_NAME} TEXT NOT NULL)") // Nombre del recurso de la imagen para predefinidos
            db.execSQL(CREATE_PREDEFINED_INSTRUMENTS_TABLE)
            Log.d(
                DbConstants.TAG_HELPER,
                "Tabla ${DbConstants.PredefinedInstrumentTable.TABLE_NAME} creada."
            )

            val CREATE_CREATED_INSTRUMENTS_TABLE =
                ("CREATE TABLE ${DbConstants.CreatedInstrumentTable.TABLE_NAME} ("
                        + "${DbConstants.CreatedInstrumentTable.COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "${DbConstants.CreatedInstrumentTable.COL_NAME} TEXT NOT NULL,"
                        + "${DbConstants.CreatedInstrumentTable.COL_FREQUENCY} REAL,"
                        + "${DbConstants.CreatedInstrumentTable.COL_INFO} TEXT,"
                        + "${DbConstants.CreatedInstrumentTable.COL_USER_ID} INTEGER NOT NULL,"
                        + "${DbConstants.CreatedInstrumentTable.COL_PHOTO_PATH} TEXT NULL," // ruta/uri de la imagen
                        + "FOREIGN KEY(${DbConstants.CreatedInstrumentTable.COL_USER_ID}) REFERENCES ${DbConstants.UserTable.TABLE_NAME}(${DbConstants.UserTable.COL_ID}) ON DELETE CASCADE)")
            db.execSQL(CREATE_CREATED_INSTRUMENTS_TABLE)
            Log.d(
                DbConstants.TAG_HELPER,
                "Tabla ${DbConstants.CreatedInstrumentTable.TABLE_NAME} creada."
            )

            val CREATE_FAVORITE_PREDEFINED_TABLE =
                ("CREATE TABLE ${DbConstants.PredefinedFavoriteTable.TABLE_NAME} ("
                        + "${DbConstants.PredefinedFavoriteTable.COL_USER_ID} INTEGER NOT NULL,"
                        + "${DbConstants.PredefinedFavoriteTable.COL_INSTRUMENT_ID} INTEGER NOT NULL,"
                        + "PRIMARY KEY (${DbConstants.PredefinedFavoriteTable.COL_USER_ID}, ${DbConstants.PredefinedFavoriteTable.COL_INSTRUMENT_ID})," // PK correcta
                        + "FOREIGN KEY(${DbConstants.PredefinedFavoriteTable.COL_USER_ID}) REFERENCES ${DbConstants.UserTable.TABLE_NAME}(${DbConstants.UserTable.COL_ID}) ON DELETE CASCADE," // FK correcta
                        + "FOREIGN KEY(${DbConstants.PredefinedFavoriteTable.COL_INSTRUMENT_ID}) REFERENCES ${DbConstants.PredefinedInstrumentTable.TABLE_NAME}(${DbConstants.PredefinedInstrumentTable.COL_ID}) ON DELETE CASCADE)") // FK correcta
            db.execSQL(CREATE_FAVORITE_PREDEFINED_TABLE)
            Log.d(
                DbConstants.TAG_HELPER,
                "Tabla ${DbConstants.PredefinedFavoriteTable.TABLE_NAME} creada."
            )

            val CREATE_FAVORITE_CREATED_TABLE =
                ("CREATE TABLE ${DbConstants.CreatedFavoriteTable.TABLE_NAME} ("
                        + "${DbConstants.CreatedFavoriteTable.COL_USER_ID} INTEGER NOT NULL,"
                        + "${DbConstants.CreatedFavoriteTable.COL_INSTRUMENT_ID} INTEGER NOT NULL,"
                        + "PRIMARY KEY (${DbConstants.CreatedFavoriteTable.COL_USER_ID}, ${DbConstants.CreatedFavoriteTable.COL_INSTRUMENT_ID})," // PK correcta
                        + "FOREIGN KEY(${DbConstants.CreatedFavoriteTable.COL_USER_ID}) REFERENCES ${DbConstants.UserTable.TABLE_NAME}(${DbConstants.UserTable.COL_ID}) ON DELETE CASCADE," // FK correcta
                        + "FOREIGN KEY(${DbConstants.CreatedFavoriteTable.COL_INSTRUMENT_ID}) REFERENCES ${DbConstants.CreatedInstrumentTable.TABLE_NAME}(${DbConstants.CreatedInstrumentTable.COL_ID}) ON DELETE CASCADE)") // FK correcta
            db.execSQL(CREATE_FAVORITE_CREATED_TABLE)
            Log.d(
                DbConstants.TAG_HELPER,
                "Tabla ${DbConstants.CreatedFavoriteTable.TABLE_NAME} creada."
            )

            Log.d(
                DbConstants.TAG_HELPER,
                "onCreate: Creación de tablas finalizada. Poblando instrumentos predefinidos..."
            )
            generatePredefinedInstruments(db) // Llamada a poblar datos iniciales
            Log.i(DbConstants.TAG_HELPER, "onCreate: Base de datos creada y poblada exitosamente.")

        } catch (e: SQLiteException) {
            Log.e(DbConstants.TAG_HELPER, "Error al crear las tablas en onCreate", e)
        } catch (e: Exception) {
            Log.e(DbConstants.TAG_HELPER, "onCreate: ERROR General Exception al crear las tablas.", e)
            }
    }

    /**
     * Se llama CUANDO la versión de la BD en el dispositivo es MENOR que DATABASE_VERSION.
     * Maneja las migraciones de esquema.
     */
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.w(
            DbConstants.TAG_HELPER,
            "onUpgrade: Actualizando base de datos de v$oldVersion a v$newVersion."
        )
        if (db == null) {
            Log.e(DbConstants.TAG_HELPER, "onUpgrade: db es null, no se puede migrar.")
            return
        }
//        if (oldVersion < 2) {
//            Log.i(DbConstants.TAG_HELPER, "Migrando a versión 2 de la base de datos...")
//            try {
//                Log.d(DbConstants.TAG_HELPER, "Intentando migrar a versión 2...")
//            } catch (e: SQLiteException) {
//                Log.e(
//                    DbConstants.TAG_HELPER,
//                    "Error al migrar a versión 2 de la base de datos (${DbConstants.UserTable.COL_EMAIL})",
//                    e
//                )
//            }
//        }
        Log.i(DbConstants.TAG_HELPER, "onUpgrade: Migración de base de datos completada: versión $oldVersion a $newVersion.")
    }

    /**
     * Función privada que inserta los instrumentos predefinidos
     * durante la creación inicial de la base de datos (en onCreate).
     */
    private fun generatePredefinedInstruments(db: SQLiteDatabase?) {
        if (db == null) {
            Log.e(DbConstants.TAG_HELPER, "generatePredefinedInstruments: db es null.")
            return
        }
        val initialInstruments = listOf(
            PredefinedInstrumentData("Fundo 1", 73.41, "Fundo de Primera. 20-22 pulgadas. Se afina en D2 (Re2)", "ic_f1"),
            PredefinedInstrumentData("Fundo 2", 92.49, "Fundo de Segunda. 18-20 pulgadas. Se afina en F#2 (Fa#2)", "ic_f2"),
            PredefinedInstrumentData("Fundo 3", 146.83, "Fundo de Tercera. 16-18 pulgadas. Se afina en D3 (Re3)", "ic_f3"),
            PredefinedInstrumentData("Diapason", 440.00, "Nota A4 (La4)", "ic_diapason"),
            PredefinedInstrumentData("Repenique", 587.33, "Repique o replicante. 10-12 pulgadas. Se afina en D2 (Re5)", "ic_repenique"),
            PredefinedInstrumentData("Caixa", 587.33, "Caja (relleno). Se afina en D2 (Re5)", "ic_caixa")
            )

        Log.d(
            DbConstants.TAG_HELPER,
            "Insertando ${initialInstruments.size} instrumentos predefinidos..."
        )
        initialInstruments.forEach { instrumentData ->
            val values = ContentValues().apply {
                // Usa las constantes de DbConstants
                put(DbConstants.PredefinedInstrumentTable.COL_NAME, instrumentData.name)
                put(DbConstants.PredefinedInstrumentTable.COL_FREQUENCY, instrumentData.frequency)
                put(DbConstants.PredefinedInstrumentTable.COL_INFO, instrumentData.information)
                put(DbConstants.PredefinedInstrumentTable.COL_ICON_RES_NAME, instrumentData.iconResName)
            }
            // Usa la constante de DbConstants
            val id = db.insert(DbConstants.PredefinedInstrumentTable.TABLE_NAME, null, values)
            if (id == -1L) {
                Log.e(
                    DbConstants.TAG_HELPER,
                    "Error al insertar instrumento predefinido: ${instrumentData.name}"
                )
            } else {
                Log.d(
                    DbConstants.TAG_HELPER,
                    "Instrumento predefinido insertado: ${instrumentData.name} con ID $id"
                )
            }
        }
        Log.d(DbConstants.TAG_HELPER, "Población de instrumentos predefinidos completada.")
    }

    // Clase auxiliar privada para los datos de los instrumentos predefinidos
    private data class PredefinedInstrumentData(
        val name: String,
        val frequency: Double,
        val information: String,
        val iconResName: String
    )


}