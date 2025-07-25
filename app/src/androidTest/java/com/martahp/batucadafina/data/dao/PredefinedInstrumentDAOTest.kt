package com.martahp.batucadafina.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import org.junit.Test
import java.io.IOException
import android.content.Context
import com.martahp.batucadafina.data.DatabaseHelper
import com.martahp.batucadafina.data.DbConstants
import com.martahp.batucadafina.model.entities.PredefinedInstrument
import com.martahp.batucadafina.model.entities.User
import com.google.common.truth.Truth.assertThat

@RunWith(AndroidJUnit4::class)
class PredefinedInstrumentDAOTest {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var predefinedInstrumentDao: PredefinedInstrumentDAO
    private lateinit var context: Context

    // --- Constantes para datos de prueba (deben coincidir con DBH.generatePredefinedInstruments)
    private val EXPECTED_INITIAL_COUNT = 6 //numero de instrumentos predefinidos en DBHelper

    private val FUNDO_1_NAME = "Fundo 1"
    private val FUNDO_1_FREQ = 73.41
    private val FUNDO_1_INFO = "Fundo de Primera. 20-22 pulgadas. Se afina en D2 (Re2)"
    private val FUNDO_1_ICON_RES_NAME = "ic_f1"

    private val FUNDO_2_NAME = "Fundo 2"
    private val FUNDO_2_FREQ = 92.49
    private val FUNDO_2_INFO = "Fundo de Segunda. 18-20 pulgadas. Se afina en F#2 (Fa#2)"
    private val FUNDO_2_ICON_RES_NAME = "ic_f2"

    private val FUNDO_3_NAME = "Fundo 3"
    private val FUNDO_3_FREQ = 146.83
    private val FUNDO_3_INFO = "Fundo de Tercera. 16-18 pulgadas. Se afina en D3 (Re3)"
    private val FUNDO_3_ICON_RES_NAME = "ic_f3"

    private val DIAPASON_NAME = "Diapason"
    private val DIAPASON_FREQ = 440.00
    private val DIAPASON_INFO = "Nota A4 (La4)"
    private val DIAPASON_ICON_RES_NAME = "ic_diapason"

    private val REPENIQUE_NAME = "Repenique"
    private val REPENIQUE_FREQ = 587.33
    private val REPENIQUE_INFO = "Repique o replicante. 10-12 pulgadas. Se afina en D2 (Re5)"
    private val REPENIQUE_ICON_RES_NAME = "ic_repenique"

    private val CAIXA_NAME = "Caixa"
    private val CAIXA_FREQ = 587.33
    private val CAIXA_INFO = "Caja (relleno). Se afina en D2 (Re5)"
    private val CAIXA_ICON_RES_NAME = "ic_caixa"


    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DbConstants.DATABASE_NAME) // Asegura DB limpia
        dbHelper = DatabaseHelper(context) // Esto llama a onCreate y generatePredefinedInstruments
        predefinedInstrumentDao = PredefinedInstrumentDAO(context)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        dbHelper.close()
    }

    @Test
    @Throws(Exception::class)
    fun getPredefinedInstrument_existingId_returnsCorrectInstrument() {
        //Arrange: Datos insertados en @Before. Asumimos que el primer ID es 1L.
        val targetId = 1L
        //Act
        val retrievedInstrument =
            predefinedInstrumentDao.getPredefinedInstrument(targetId)
        //Assert
        assertThat(retrievedInstrument).isNotNull()
        assertThat(retrievedInstrument?.id).isEqualTo(targetId)
        assertThat(retrievedInstrument?.name).isEqualTo(FUNDO_1_NAME)
        assertThat(retrievedInstrument?.frequency).isEqualTo(FUNDO_1_FREQ)
        assertThat(retrievedInstrument?.information).isEqualTo(FUNDO_1_INFO)
        assertThat(retrievedInstrument?.iconResName).isEqualTo(FUNDO_1_ICON_RES_NAME)
    }

    @Test
    @Throws(Exception::class)
    fun getPredefinedInstrument_nonExistentId_returnsNull() {
        // Arrange
        val nonExistentId = 999L
        // Act
        val retrievedInstrument = predefinedInstrumentDao.getPredefinedInstrument(nonExistentId)
        // Assert
        assertThat(retrievedInstrument).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun getAllPredefinedInstruments_returnsAllInitiallyPopulatedInstruments() {
        // Arrange: Datos insertados en @Before

        // Act
        val instruments = predefinedInstrumentDao.getAllPredefinedInstruments()

        // Assert
        assertThat(instruments).isNotNull()
        assertThat(instruments).hasSize(EXPECTED_INITIAL_COUNT)

        // Verificar contenido de cada instrumento (el DAO ordena por nombre ASC)
        val fundo1 = instruments.find { it.name == FUNDO_1_NAME }
        val fundo2 = instruments.find { it.name == FUNDO_2_NAME }
        val fundo3 = instruments.find { it.name == FUNDO_3_NAME }
        val diapason = instruments.find { it.name == DIAPASON_NAME }
        val repenique = instruments.find { it.name == REPENIQUE_NAME }
        val caixa = instruments.find { it.name == CAIXA_NAME }

        assertThat(fundo1).isNotNull()
        assertThat(fundo1?.id).isEqualTo(1L)
        assertThat(fundo1?.name).isEqualTo(FUNDO_1_NAME)
        assertThat(fundo1?.frequency).isEqualTo(FUNDO_1_FREQ)
        assertThat(fundo1?.information).isEqualTo(FUNDO_1_INFO)
        assertThat(fundo1?.iconResName).isEqualTo(FUNDO_1_ICON_RES_NAME)

        assertThat(fundo2).isNotNull()
        assertThat(fundo2?.id).isEqualTo(2L) //  2
        assertThat(fundo2?.name).isEqualTo(FUNDO_2_NAME)
        assertThat(fundo2?.frequency).isEqualTo(FUNDO_2_FREQ)
        assertThat(fundo2?.information).isEqualTo(FUNDO_2_INFO)
        assertThat(fundo2?.iconResName).isEqualTo(FUNDO_2_ICON_RES_NAME)

        assertThat(fundo3).isNotNull()
        assertThat(fundo3?.id).isEqualTo(3L) //3
        assertThat(fundo3?.name).isEqualTo(FUNDO_3_NAME)
        assertThat(fundo3?.frequency).isEqualTo(FUNDO_3_FREQ)
        assertThat(fundo3?.information).isEqualTo(FUNDO_3_INFO)
        assertThat(fundo3?.iconResName).isEqualTo(FUNDO_3_ICON_RES_NAME) // CAMBIO

        assertThat(diapason).isNotNull()
        assertThat(diapason?.id).isEqualTo(4L) //
        assertThat(diapason?.name).isEqualTo(DIAPASON_NAME)
        assertThat(diapason?.frequency).isEqualTo(DIAPASON_FREQ)
        assertThat(diapason?.information).isEqualTo(DIAPASON_INFO)
        assertThat(diapason?.iconResName).isEqualTo(DIAPASON_ICON_RES_NAME)

        assertThat(repenique).isNotNull()
        assertThat(repenique?.id).isEqualTo(5L) //
        assertThat(repenique?.name).isEqualTo(REPENIQUE_NAME)
        assertThat(repenique?.frequency).isEqualTo(REPENIQUE_FREQ)
        assertThat(repenique?.information).isEqualTo(REPENIQUE_INFO)

        assertThat(caixa).isNotNull()
        assertThat(caixa?.id).isEqualTo(6L) //
        assertThat(caixa?.name).isEqualTo(CAIXA_NAME)
        assertThat(caixa?.frequency).isEqualTo(CAIXA_FREQ)
        assertThat(caixa?.information).isEqualTo(CAIXA_INFO)


        //verificar el orden de los nombres
        val instrumentNames = instruments.map { it.name }
        assertThat(instrumentNames).containsExactly(CAIXA_NAME, DIAPASON_NAME, FUNDO_1_NAME, FUNDO_2_NAME, FUNDO_3_NAME, REPENIQUE_NAME).inOrder()
    }

    @Test
    @Throws(Exception::class)
    fun getAllPredefinedInstruments_whenDbIsEmpty_returnsEmptyList() {
        // Arrange: Borrar los datos predefinidos insertados en @Before
        val db = dbHelper.writableDatabase
        db.delete(DbConstants.PredefinedInstrumentTable.TABLE_NAME, null, null) // Borrar todas las filas
        db.close()

        // Act
        val instruments = predefinedInstrumentDao.getAllPredefinedInstruments()

        // Assert
        assertThat(instruments).isNotNull()
        assertThat(instruments).isEmpty()
    }
}