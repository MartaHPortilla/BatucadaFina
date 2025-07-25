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
// import org.junit.Assert.* // Quitar si solo usas Truth

@RunWith(AndroidJUnit4::class)
class CreatedFavDAOTest {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var userDao: UserDAO
    private lateinit var createdInstrumentDao: CreatedInstrumentDAO
    private lateinit var createdFavDao: CreatedFavDAO
    private lateinit var context: Context

    // Datos de prueba Usuario
    private val USER_1_NAME = "favUserCreated1"
    private val USER_1_PASS = "passFavC1"
    private var USER_1_ID: Long = -1L

    // Datos de prueba Instrumentos Creados
    private val INSTR_C1_NAME = "Mi Repi"
    private val INSTR_C1_FREQ = 523.25
    private val INSTR_C1_INFO = "Aro madera"
    // --- AÑADIR CONSTANTE PARA PHOTOPATH ---
    private val INSTR_C1_PHOTOPATH = "/path/to/my_repi.jpg"
    private var INSTR_C1_ID: Long = -1L

    private val INSTR_C2_NAME = "Mi Surdo"
    private val INSTR_C2_FREQ = 87.31
    private val INSTR_C2_INFO = "22 pulgadas"
    // --- AÑADIR CONSTANTE PARA PHOTOPATH ---
    private val INSTR_C2_PHOTOPATH = "/path/to/my_surdo.png"
    private var INSTR_C2_ID: Long = -1L

    private val NON_EXISTENT_CREATED_ID = 999L

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DbConstants.DATABASE_NAME)
        dbHelper = DatabaseHelper(context)
        userDao = UserDAO(context)
        createdInstrumentDao = CreatedInstrumentDAO(context)
        createdFavDao = CreatedFavDAO(context)

        val addResultUser = userDao.addUser(USER_1_NAME, USER_1_PASS, "john.mckinley@examplepetstore.com")
        assertThat(addResultUser).isEqualTo(1)
        val user = userDao.getUserProfileByName(USER_1_NAME)
        assertThat(user).isNotNull()
        USER_1_ID = user!!.id
        assertThat(USER_1_ID).isGreaterThan(0L)

        // --- CAMBIO: Añadir photoPath al crear instrumentos ---
        val instr1Id = createdInstrumentDao.addCreatedInstrument(
            USER_1_ID, INSTR_C1_NAME, INSTR_C1_FREQ, INSTR_C1_INFO, INSTR_C1_PHOTOPATH // Pasar photoPath
        )
        assertThat(instr1Id).isGreaterThan(0L)
        INSTR_C1_ID = instr1Id

        val instr2Id = createdInstrumentDao.addCreatedInstrument(
            USER_1_ID, INSTR_C2_NAME, INSTR_C2_FREQ, INSTR_C2_INFO, INSTR_C2_PHOTOPATH // Pasar photoPath
        )
        assertThat(instr2Id).isGreaterThan(0L)
        INSTR_C2_ID = instr2Id
        // --- FIN CAMBIO ---
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        dbHelper.close()
    }

    // --- Tests para setCreatedFavoriteStatus y isCreatedFavorite (sin cambios necesarios aquí para photoPath) ---
    @Test
    @Throws(Exception::class)
    fun setAndCheckCreatedFavoriteStatus_markAndUnmark_updatesCorrectly() {
        assertThat(createdFavDao.isCreatedFavorite(USER_1_ID, INSTR_C1_ID)).isFalse()
        createdFavDao.setCreatedFavoriteStatus(USER_1_ID, INSTR_C1_ID, true)
        assertThat(createdFavDao.isCreatedFavorite(USER_1_ID, INSTR_C1_ID)).isTrue()
        createdFavDao.setCreatedFavoriteStatus(USER_1_ID, INSTR_C1_ID, false)
        assertThat(createdFavDao.isCreatedFavorite(USER_1_ID, INSTR_C1_ID)).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun isCreatedFavorite_nonExistentFavorite_returnsFalse() {
        val isFav = createdFavDao.isCreatedFavorite(USER_1_ID, INSTR_C2_ID) // INSTR_C2_ID no se marcó
        assertThat(isFav).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun isCreatedFavorite_nonExistentUserOrInstrument_returnsFalse() {
        val invalidUserId = 998L
        assertThat(createdFavDao.isCreatedFavorite(invalidUserId, INSTR_C1_ID)).isFalse()
        assertThat(createdFavDao.isCreatedFavorite(USER_1_ID, NON_EXISTENT_CREATED_ID)).isFalse()
    }

    // --- Test para getFavoriteCreatedInstrumentIdsForUser (sin cambios necesarios aquí para photoPath) ---
    @Test
    @Throws(Exception::class)
    fun getFavoriteCreatedInstrumentIds_userWithFavorites_returnsCorrectIds() {
        createdFavDao.setCreatedFavoriteStatus(USER_1_ID, INSTR_C2_ID, true)
        createdFavDao.setCreatedFavoriteStatus(USER_1_ID, INSTR_C1_ID, true)
        val favInstrumentsId = createdFavDao.getFavoriteCreatedInstrumentIdsForUser(USER_1_ID)
        assertThat(favInstrumentsId).isNotNull()
        assertThat(favInstrumentsId).hasSize(2)
        assertThat(favInstrumentsId).containsExactly(INSTR_C1_ID, INSTR_C2_ID)
    }

    @Test
    @Throws(Exception::class)
    fun getFavoriteCreatedInstrumentIds_userWithNoFavorites_returnsEmptyList() {
        val favoriteIds = createdFavDao.getFavoriteCreatedInstrumentIdsForUser(USER_1_ID)
        assertThat(favoriteIds).isNotNull()
        assertThat(favoriteIds).isEmpty()
    }

    // --- Test para getFavoriteCreatedInstrumentsForUser (JOIN) ---
    @Test
    @Throws(Exception::class)
    fun getFavoriteCreatedInstruments_userWithFavorites_returnsCorrectInstrumentsOrdered() {
        // Arrange
        createdFavDao.setCreatedFavoriteStatus(USER_1_ID, INSTR_C2_ID, true) // Surdo
        createdFavDao.setCreatedFavoriteStatus(USER_1_ID, INSTR_C1_ID, true) // Repi

        // Act
        val favoriteInstruments = createdFavDao.getFavoriteCreatedInstrumentsForUser(USER_1_ID)

        // Assert
        assertThat(favoriteInstruments).isNotNull()
        assertThat(favoriteInstruments).hasSize(2)

        val names = favoriteInstruments.map { it.name }
        val ids = favoriteInstruments.map { it.id }
        // CAMBIO: Mapear también los photoPaths
        val photoPaths = favoriteInstruments.map { it.photoPath }

        // Orden por nombre: "Mi Repi" (INSTR_C1) antes de "Mi Surdo" (INSTR_C2)
        assertThat(names).containsExactly(INSTR_C1_NAME, INSTR_C2_NAME).inOrder()
        assertThat(ids).containsExactly(INSTR_C1_ID, INSTR_C2_ID).inOrder()
        // CAMBIO: Verificar photoPaths en el orden correcto
        assertThat(photoPaths).containsExactly(INSTR_C1_PHOTOPATH, INSTR_C2_PHOTOPATH).inOrder()

        // Detalles del primer instrumento (debería ser INSTR_C1_NAME)
        assertThat(favoriteInstruments[0].name).isEqualTo(INSTR_C1_NAME)
        assertThat(favoriteInstruments[0].id).isEqualTo(INSTR_C1_ID)
        assertThat(favoriteInstruments[0].userId).isEqualTo(USER_1_ID)
        assertThat(favoriteInstruments[0].photoPath).isEqualTo(INSTR_C1_PHOTOPATH) // CAMBIO

        // Detalles del segundo instrumento (debería ser INSTR_C2_NAME)
        assertThat(favoriteInstruments[1].name).isEqualTo(INSTR_C2_NAME)
        assertThat(favoriteInstruments[1].id).isEqualTo(INSTR_C2_ID)
        assertThat(favoriteInstruments[1].userId).isEqualTo(USER_1_ID)
        assertThat(favoriteInstruments[1].photoPath).isEqualTo(INSTR_C2_PHOTOPATH) // CAMBIO
    }

    @Test
    @Throws(Exception::class)
    fun getFavoriteCreatedInstruments_userWithNoFavorites_returnsEmptyList() {
        val favoriteInstruments = createdFavDao.getFavoriteCreatedInstrumentsForUser(USER_1_ID)
        assertThat(favoriteInstruments).isNotNull()
        assertThat(favoriteInstruments).isEmpty()
    }
}