package com.martahp.batucadafina.data

//creamos un objeto singleton para las constantes de la base de datos
object DbConstants {

    // Constantes generales de la BD
    const val DATABASE_VERSION = 1 // Versión en desarrollo del esquema de la BD
    const val DATABASE_NAME = "batucadafina.dbfortuner"

    // Constantes de tags del log
    const val TAG_HELPER = "DatabaseHelper" // TAG para logs del Helper
    const val TAG_USER_DAO = "UserDAO"
    const val TAG_PREDEF_INSTR_DAO = "PredefInstrumentDAO"
    const val TAG_CREATED_INSTR_DAO = "CreatedInstrumentDAO"
    const val TAG_PREDEF_FAV_DAO = "PredefFavDAO"
    const val TAG_CREATED_FAV_DAO = "CreatedFavDAO"

    // Tabla Users
    object UserTable {
        const val TABLE_NAME = "users"
        const val COL_ID = "id"
        const val COL_USERNAME = "username"
        const val COL_EMAIL = "email" // Nueva columna para el correo electrónico
        const val COL_PASSWORD = "password"
        const val COL_PHOTO_PATH = "photo_path" // ruta/uri de la imagen

    }

    // Tabla Instrumentos Predefinidos
    object PredefinedInstrumentTable {
        const val TABLE_NAME = "predefined_instruments"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_FREQUENCY = "frequency"
        const val COL_INFO = "information"
        const val COL_ICON_RES_NAME = "icon_res_name" // Nombre del recurso de la imagen para predefinidos
    }

    // Tabla Instrumentos Creados
    object CreatedInstrumentTable {
        const val TABLE_NAME = "created_instruments"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_FREQUENCY = "frequency"
        const val COL_INFO = "information"
        const val COL_USER_ID = "user_id"
        const val COL_PHOTO_PATH = "photo_path"
    }

    // Tabla Favoritos Predefinidos
    object PredefinedFavoriteTable {
        const val TABLE_NAME = "favorite_predefined_instruments"
        const val COL_USER_ID = "user_id"
        const val COL_INSTRUMENT_ID = "predefined_instrument_id"
    }

    // Tabla Favoritos Creados
    object CreatedFavoriteTable {
        const val TABLE_NAME = "favorite_created_instruments"
        const val COL_USER_ID = "user_id"
        const val COL_INSTRUMENT_ID = "created_instrument_id"
    }

}