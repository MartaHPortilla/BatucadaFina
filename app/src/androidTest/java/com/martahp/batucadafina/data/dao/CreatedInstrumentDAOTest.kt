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
import com.google.common.truth.Truth.assertThat

@RunWith(AndroidJUnit4::class)
class CreatedInstrumentDAOTest {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var userDao: UserDAO
    private lateinit var createdInstrumentDao: CreatedInstrumentDAO
    private lateinit var context: Context

    // Datos de prueba Usuario
    private val USER_1_NAME = "userOwner1"
    private val USER_1_PASS = "pass1"
    private var USER_1_ID: Long = -1L

    // Datos de prueba Instrumentos Creados
    private val INSTR_C1_NAME = "Mi Repi"
    private val INSTR_C1_FREQ = 523.25
    private val INSTR_C1_INFO = "Aro madera"
    private val INSTR_C1_PHOTOPATH = "/path/to/repi.jpg" // NUEVO
    private var INSTR_C1_ID: Long = -1L

    private val INSTR_C2_NAME = "Mi Surdo"
    private val INSTR_C2_FREQ = 87.31
    private val INSTR_C2_INFO = "22 pulgadas"
    private val INSTR_C2_PHOTOPATH = "/path/to/surdo.png" // NUEVO
    private var INSTR_C2_ID: Long = -1L

    private val INSTR_C3_NAME = "Mi Pandeiro" // Para el Usuario 2
    private val INSTR_C3_FREQ = 400.0
    private val INSTR_C3_INFO = "Piel animal"
    private val INSTR_C3_PHOTOPATH = null // Ejemplo con photoPath null

    private val USER_2_NAME = "userOwner2" // Para el test de getAll...
    private val USER_2_PASS = "pass2"


    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DbConstants.DATABASE_NAME)
        dbHelper = DatabaseHelper(context)
        userDao = UserDAO(context)
        createdInstrumentDao = CreatedInstrumentDAO(context)

        val addResultUser = userDao.addUser(USER_1_NAME, USER_1_PASS, "william.henry.harrison@example.com")
        assertThat(addResultUser).isEqualTo(1)
        val user = userDao.getUserProfileByName(USER_1_NAME)
        assertThat(user).isNotNull()
        USER_1_ID = user!!.id
        assertThat(USER_1_ID).isGreaterThan(0L)

        // CAMBIO: Añadir photoPath al crear instrumentos en setup
        val instr1Id = createdInstrumentDao.addCreatedInstrument(USER_1_ID, INSTR_C1_NAME, INSTR_C1_FREQ, INSTR_C1_INFO, INSTR_C1_PHOTOPATH)
        assertThat(instr1Id).isGreaterThan(0L)
        INSTR_C1_ID = instr1Id

        val instr2Id = createdInstrumentDao.addCreatedInstrument(USER_1_ID, INSTR_C2_NAME, INSTR_C2_FREQ, INSTR_C2_INFO, INSTR_C2_PHOTOPATH)
        assertThat(instr2Id).isGreaterThan(0L)
        INSTR_C2_ID = instr2Id
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        dbHelper.close()
    }

    @Test
    @Throws(Exception::class)
    fun addAndGetCreatedInstrument_returnsCorrectInstrument() {
        // Arrange
        val testName = "Mi Agogo"
        val testFreq = 600.0
        val testInfo = "Doble campana"
        val testPhotoPath = "/path/to/agogo.gif"

        // Act (Add)
        val newInstrumentId = createdInstrumentDao.addCreatedInstrument(
            USER_1_ID, testName, testFreq, testInfo, testPhotoPath // CAMBIO: Pasar photoPath
        )
        assertThat(newInstrumentId).isGreaterThan(0L)

        // Act (Get)
        val retrievedInstrument = createdInstrumentDao.getCreatedInstrument(newInstrumentId)

        // Assert (Get)
        assertThat(retrievedInstrument).isNotNull()
        assertThat(retrievedInstrument?.id).isEqualTo(newInstrumentId)
        assertThat(retrievedInstrument?.userId).isEqualTo(USER_1_ID)
        assertThat(retrievedInstrument?.name).isEqualTo(testName)
        assertThat(retrievedInstrument?.frequency).isEqualTo(testFreq)
        assertThat(retrievedInstrument?.information).isEqualTo(testInfo)
        assertThat(retrievedInstrument?.photoPath).isEqualTo(testPhotoPath) // CAMBIO: Verificar photoPath
    }

    @Test
    @Throws(Exception::class)
    fun getCreatedInstrument_nonExistentId_returnsNull() {
        val nonExistentId = 999L
        val retrievedInstrument = createdInstrumentDao.getCreatedInstrument(nonExistentId)
        assertThat(retrievedInstrument).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun getAllCreatedInstrumentsForUser_returnsOnlyUserInstruments() {
        // Arrange: Usuario 1 e instrumentos 1 y 2 ya creados en @Before
        // Añadir Usuario 2
        val user2Id = userDao.addUser(USER_2_NAME, USER_2_PASS, "james.madison@examplepetstore.com").let {
            if (it == 1) userDao.getUserProfileByName(USER_2_NAME)?.id else null
        }
        assertThat(user2Id).isNotNull()
        // Añadir instrumento para Usuario 2 (CAMBIO: con photoPath)
        createdInstrumentDao.addCreatedInstrument(user2Id!!, INSTR_C3_NAME, INSTR_C3_FREQ, INSTR_C3_INFO, INSTR_C3_PHOTOPATH)

        // Act: Obtener instrumentos SOLO para Usuario 1
        val user1Instruments = createdInstrumentDao.getAllCreatedInstrumentsForUser(USER_1_ID)

        // Assert
        assertThat(user1Instruments).isNotNull()
        assertThat(user1Instruments).hasSize(2)
        // Verificar que los instrumentos son los correctos y tienen su photoPath
        val instr1 = user1Instruments.find { it.id == INSTR_C1_ID }
        val instr2 = user1Instruments.find { it.id == INSTR_C2_ID }
        assertThat(instr1).isNotNull()
        assertThat(instr2).isNotNull()
        assertThat(instr1?.name).isEqualTo(INSTR_C1_NAME)
        assertThat(instr1?.photoPath).isEqualTo(INSTR_C1_PHOTOPATH)
        assertThat(instr2?.name).isEqualTo(INSTR_C2_NAME)
        assertThat(instr2?.photoPath).isEqualTo(INSTR_C2_PHOTOPATH)
    }

    @Test
    @Throws(Exception::class)
    fun getAllCreatedInstrumentsForUser_userWithNoInstruments_returnsEmptyList() {
        // Arrange: Crear un nuevo usuario sin instrumentos
        val user3Id = userDao.addUser("user3", "pass3", "emailizamccardlejohson@altostrat.com").let {
            if (it == 1) userDao.getUserProfileByName("user3")?.id else null
        }
        assertThat(user3Id).isNotNull()

        // Act
        val user3Instruments = createdInstrumentDao.getAllCreatedInstrumentsForUser(user3Id!!)

        // Assert
        assertThat(user3Instruments).isNotNull()
        assertThat(user3Instruments).isEmpty()
    }

    @Test
    @Throws(Exception::class)
    fun updateCreatedInstrument_validUpdate_updatesDataCorrectly() {
        // Arrange: INSTR_C1_ID ya existe de @Before
        val newName = "Repi Editado Master"
        val newFreq = 550.0
        val newInfo = "Info Super Editada"
        val newPhotoPath = "/path/to/repi_editado.jpg"

        // Act
        val success = createdInstrumentDao.updateCreatedInstrument(
            INSTR_C1_ID, newName, newFreq, newInfo, newPhotoPath // CAMBIO: Pasar newPhotoPath
        )

        // Assert
        assertThat(success).isTrue()
        val updatedInstrument = createdInstrumentDao.getCreatedInstrument(INSTR_C1_ID)
        assertThat(updatedInstrument).isNotNull()
        assertThat(updatedInstrument?.name).isEqualTo(newName)
        assertThat(updatedInstrument?.frequency).isEqualTo(newFreq)
        assertThat(updatedInstrument?.information).isEqualTo(newInfo)
        assertThat(updatedInstrument?.photoPath).isEqualTo(newPhotoPath) // CAMBIO: Verificar photoPath
        assertThat(updatedInstrument?.userId).isEqualTo(USER_1_ID)
    }

    @Test
    @Throws(Exception::class)
    fun updateCreatedInstrument_canUpdatePhotoPath_andSetToNull() { // Test renombrado y enfocado
        // Arrange: INSTR_C1_ID existe con INSTR_C1_PHOTOPATH
        val updatedInstrumentInitially = createdInstrumentDao.getCreatedInstrument(INSTR_C1_ID)
        assertThat(updatedInstrumentInitially?.photoPath).isEqualTo(INSTR_C1_PHOTOPATH)

        // Act 1: Actualizar a un nuevo path
        val newPath = "/path/to/another.jpg"
        var success = createdInstrumentDao.updateCreatedInstrument(INSTR_C1_ID, INSTR_C1_NAME, INSTR_C1_FREQ, INSTR_C1_INFO, newPath)
        // Assert 1
        assertThat(success).isTrue()
        var updatedInstrument = createdInstrumentDao.getCreatedInstrument(INSTR_C1_ID)
        assertThat(updatedInstrument?.photoPath).isEqualTo(newPath)

        // Act 2: Actualizar a null
        success = createdInstrumentDao.updateCreatedInstrument(INSTR_C1_ID, INSTR_C1_NAME, INSTR_C1_FREQ, INSTR_C1_INFO, null)
        // Assert 2
        assertThat(success).isTrue()
        updatedInstrument = createdInstrumentDao.getCreatedInstrument(INSTR_C1_ID)
        assertThat(updatedInstrument?.photoPath).isNull()
    }


    @Test
    @Throws(Exception::class)
    fun updateCreatedInstrument_nonExistentId_returnsFalse() {
        val invalidId = 997L
        // CAMBIO: Pasar photoPath (puede ser null)
        val success = createdInstrumentDao.updateCreatedInstrument(invalidId, "Error", 1.0, null, null)
        assertThat(success).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun deleteCreatedInstrument_existingInstrument_ById_deletesSuccessfullyAndReturnsTrue() {
        // Arrange: INSTR_C1_ID existe
        assertThat(createdInstrumentDao.getCreatedInstrument(INSTR_C1_ID)).isNotNull()

        // Act
        val success = createdInstrumentDao.deleteCreatedInstrumentById(INSTR_C1_ID)

        // Assert
        assertThat(success).isTrue()
        assertThat(createdInstrumentDao.getCreatedInstrument(INSTR_C1_ID)).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun deleteCreatedInstrument_nonExistentInstrument_ById_returnsFalse() {
        val invalidId = 996L
        val success = createdInstrumentDao.deleteCreatedInstrumentById(invalidId)
        assertThat(success).isFalse()
    }

}