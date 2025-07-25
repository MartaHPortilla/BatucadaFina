package com.martahp.batucadafina.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import org.junit.Test
import java.io.IOException
import android.content.Context
import com.martahp.batucadafina.data.dao.UserDAO
import com.martahp.batucadafina.data.dao.PredefinedInstrumentDAO
import com.google.common.truth.Truth.assertThat

@RunWith(AndroidJUnit4::class)
class DatabaseHelperTest {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var context: Context
    private lateinit var userDao: UserDAO
    private lateinit var predefinedInstrumentDao: PredefinedInstrumentDAO

    // Datos prueba
    private val DIAPASON_NAME = "Diapason"
    private val REPENIQUE_NAME = "Repenique"
    private val CAIXA_NAME = "Caixa"
    private val PREDEF_1_NAME = "Fundo 1"
    private val PREDEF_2_NAME = "Fundo 2"
    private val PREDEF_3_NAME = "Fundo 3"

    private val DIAPASON_ICON_RES_NAME = "ic_diapason"
    private val REPENIQUE_ICON_RES_NAME = "ic_repenique"
    private val CAIXA_ICON_RES_NAME = "ic_caixa"
    private val PREDEF_1_ICON_RES_NAME = "ic_f1"
    private val PREDEF_2_ICON_RES_NAME = "ic_f2"
    private val PREDEF_3_ICON_RES_NAME = "ic_f3"

    private val EXPECTED_PREDEF_COUNT = 6


    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DbConstants.DATABASE_NAME)
        dbHelper = DatabaseHelper(context)
        userDao = UserDAO(context)
        predefinedInstrumentDao = PredefinedInstrumentDAO(context)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        dbHelper.close()
    }

    @Test
    @Throws(Exception::class)
    fun onCreate_databaseCreated_tablesAndInitialDataExist() {
        // Arrange: La BD se crea y puebla en @Before

        // Act: Usar DAOs para consultar el estado post-onCreate
        val allPredefined = predefinedInstrumentDao.getAllPredefinedInstruments()
        val userAddResult = userDao.addUser(
            "testOnCreateHelper", "testpass", "test@mail.test"
        )

        // Assert:
        // 1. Verificar población inicial de instrumentos predefinidos
        assertThat(allPredefined).isNotNull()
        assertThat(allPredefined).hasSize(EXPECTED_PREDEF_COUNT)

        // Verificar nombres, orden y AHORA TAMBIÉN iconResName
        val names = allPredefined.map { it.name }
        assertThat(names).containsExactly(CAIXA_NAME,
            DIAPASON_NAME,
            PREDEF_1_NAME,
            PREDEF_2_NAME,
            PREDEF_3_NAME,
            REPENIQUE_NAME
        ).inOrder()

        // Verificación de iconos
        val instrument1 = allPredefined.find { it.name == PREDEF_1_NAME }
        val instrument2 = allPredefined.find { it.name == PREDEF_2_NAME }
        val instrument3 = allPredefined.find { it.name == PREDEF_3_NAME }

        assertThat(instrument1).isNotNull()
        assertThat(instrument1?.iconResName).isEqualTo(PREDEF_1_ICON_RES_NAME)

        assertThat(instrument2).isNotNull()
        assertThat(instrument2?.iconResName).isEqualTo(PREDEF_2_ICON_RES_NAME)

        assertThat(instrument3).isNotNull()
        assertThat(instrument3?.iconResName).isEqualTo(PREDEF_3_ICON_RES_NAME)

        // 2. Verificar que otras tablas parecen haberse creado correctamente
        assertThat(userAddResult).isEqualTo(1)
        val testUser = userDao.getUserProfileByName("testOnCreateHelper")
        assertThat(testUser).isNotNull()
        assertThat(testUser?.username).isEqualTo("testOnCreateHelper")
    }
}