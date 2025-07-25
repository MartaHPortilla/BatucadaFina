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
// import org.junit.Assert.* // Puedes quitar esta si solo usas Truth

@RunWith(AndroidJUnit4::class)
class PredefinedFavDAOTest {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var userDao: UserDAO
    private lateinit var predefinedFavDao: PredefinedFavDAO
    private lateinit var context: Context

    private val USER_1_NAME = "favUser1"
    private val USER_1_PASS = "passFav1"
    private var USER_1_ID: Long = -1L

    private val PREDEF_ID_1 = 1L
    private val PREDEF_ID_2 = 2L
    private val PREDEF_ID_3 = 3L
    private val NON_EXISTENT_PREDEF_ID = 999L


    private val PREDEF_NAME_1 = "Fundo 1"
    private val PREDEF_NAME_2 = "Fundo 2"
    private val PREDEF_NAME_3 = "Fundo 3"

    private val PREDEF_NAME_DIAPASON = "Diapason"
    private val PREDEF_NAME_REPENIQUE = "Repenique"
    private val PREDEF_NAME_CAIXA = "Caixa"

    private val PREDEF_ICON_1 = "ic_f1"
    private val PREDEF_ICON_2 = "ic_f2"
    private val PREDEF_ICON_3 = "ic_f3"
    private val PREDEF_ICON_DIAPASON = "ic_diapason"
    private val PREDEF_ICON_REPENIQUE = "ic_repenique"
    private val PREDEF_ICON_CAIXA = "ic_caixa"

    private val PREDEF_FREQ_1 = 73.41
    private val PREDEF_INFO_1 = "Fundo de Primera. 20-22 pulgadas. Se afina en D2 (Re2)"
    private val PREDEF_FREQ_3 = 146.83
    private val PREDEF_INFO_3 = "Fundo de Tercera. 16-18 pulgadas. Se afina en D3 (Re3)"



    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DbConstants.DATABASE_NAME)
        dbHelper = DatabaseHelper(context)
        userDao = UserDAO(context)
        predefinedFavDao = PredefinedFavDAO(context)

        val addResult = userDao.addUser(USER_1_NAME, USER_1_PASS, "william.henry.harrison@example-pet-store.com")
        assertThat(addResult).isEqualTo(1)
        val user = userDao.getUserProfileByName(USER_1_NAME)
        assertThat(user).isNotNull()
        USER_1_ID = user!!.id
        assertThat(USER_1_ID).isGreaterThan(0L)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        dbHelper.close()
    }

    // --- Tests para setPredefinedFavoriteStatus y isPredefinedFavorite (Estos estaban bien) ---
    @Test
    @Throws(Exception::class)
    fun setAndCheckFavoriteStatus_markAndUnmark_updatesCorrectly() {
        assertThat(predefinedFavDao.isPredefinedFavorite(USER_1_ID, PREDEF_ID_1)).isFalse()
        predefinedFavDao.setPredefinedFavoriteStatus(USER_1_ID, PREDEF_ID_1, true)
        assertThat(predefinedFavDao.isPredefinedFavorite(USER_1_ID, PREDEF_ID_1)).isTrue()
        predefinedFavDao.setPredefinedFavoriteStatus(USER_1_ID, PREDEF_ID_1, false)
        assertThat(predefinedFavDao.isPredefinedFavorite(USER_1_ID, PREDEF_ID_1)).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun isPredefinedFavorite_nonExistentFavorite_returnsFalse() {
        val isFav = predefinedFavDao.isPredefinedFavorite(USER_1_ID, PREDEF_ID_2)
        assertThat(isFav).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun isPredefinedFavorite_nonExistentUserOrInstrument_returnsFalse() {
        val invalidUserId = 998L
        assertThat(predefinedFavDao.isPredefinedFavorite(invalidUserId, PREDEF_ID_1)).isFalse()
        assertThat(predefinedFavDao.isPredefinedFavorite(USER_1_ID, NON_EXISTENT_PREDEF_ID)).isFalse()
    }

    // --- Test para getFavoritePredefinedInstrumentIdsForUser (Este estaba bien) ---
    @Test
    @Throws(Exception::class)
    fun getFavoritePredefinedInstrumentIds_userWithFavorites_returnsCorrectIds() {
        predefinedFavDao.setPredefinedFavoriteStatus(USER_1_ID, PREDEF_ID_1, true)
        predefinedFavDao.setPredefinedFavoriteStatus(USER_1_ID, PREDEF_ID_3, true)
        val favoriteIds = predefinedFavDao.getFavoritePredefinedInstrumentIdsForUser(USER_1_ID)
        assertThat(favoriteIds).isNotNull()
        assertThat(favoriteIds).hasSize(2)
        assertThat(favoriteIds).containsExactly(PREDEF_ID_1, PREDEF_ID_3)
    }

    @Test
    @Throws(Exception::class)
    fun getFavoritePredefinedInstrumentIds_userWithNoFavorites_returnsEmptyList() {
        val favoriteIds = predefinedFavDao.getFavoritePredefinedInstrumentIdsForUser(USER_1_ID)
        assertThat(favoriteIds).isNotNull()
        assertThat(favoriteIds).isEmpty()
    }

    // --- Test para getFavoritePredefinedInstrumentsForUser (JOIN) ---
    @Test
    @Throws(Exception::class)
    fun getFavoritePredefinedInstruments_userWithFavorites_returnsCorrectInstrumentsOrdered() {
        // Arrange: Marcar ID 1 y 3 como favoritos
        predefinedFavDao.setPredefinedFavoriteStatus(USER_1_ID, PREDEF_ID_3, true) // Fundo 3, Icono 3
        predefinedFavDao.setPredefinedFavoriteStatus(USER_1_ID, PREDEF_ID_1, true) // Fundo 1, Icono 1
        // Dejar ID 2 sin marcar

        // Act: Obtener la lista de objetos favoritos
        val favoriteInstruments = predefinedFavDao.getFavoritePredefinedInstrumentsForUser(USER_1_ID)

        // Assert: Verificar tamaño y contenido (el DAO debe ordenar por nombre: Fundo 1, Fundo 3)
        assertThat(favoriteInstruments).isNotNull()
        assertThat(favoriteInstruments).hasSize(2)

        // Verificar orden por nombre (Fundo 1 antes que Fundo 3)
        val names = favoriteInstruments.map { it.name }
        assertThat(names).containsExactly(PREDEF_NAME_1, PREDEF_NAME_3).inOrder()

        // Verificar IDs correspondientes (deben coincidir con el orden de nombres)
        val ids = favoriteInstruments.map { it.id }
        assertThat(ids).containsExactly(PREDEF_ID_1, PREDEF_ID_3).inOrder()

        // --- AÑADIR VERIFICACIÓN DE iconResName ---
        val iconNames = favoriteInstruments.map { it.iconResName }
        assertThat(iconNames).containsExactly(PREDEF_ICON_1, PREDEF_ICON_3).inOrder()
        // --- FIN AÑADIR ---

        // Verificar algún detalle extra del primer instrumento (debe ser Fundo 1)
        assertThat(favoriteInstruments[0].name).isEqualTo(PREDEF_NAME_1)
        assertThat(favoriteInstruments[0].id).isEqualTo(PREDEF_ID_1)
        assertThat(favoriteInstruments[0].frequency).isEqualTo(PREDEF_FREQ_1) // Añadido para más detalle
        assertThat(favoriteInstruments[0].information).isEqualTo(PREDEF_INFO_1) // Añadido para más detalle
        assertThat(favoriteInstruments[0].iconResName).isEqualTo(PREDEF_ICON_1) // AÑADIDO

        // Verificar algún detalle extra del segundo instrumento (debe ser Fundo 3)
        assertThat(favoriteInstruments[1].name).isEqualTo(PREDEF_NAME_3)
        assertThat(favoriteInstruments[1].id).isEqualTo(PREDEF_ID_3)
        assertThat(favoriteInstruments[1].frequency).isEqualTo(PREDEF_FREQ_3) // Añadido para más detalle
        assertThat(favoriteInstruments[1].information).isEqualTo(PREDEF_INFO_3) // Añadido para más detalle
        assertThat(favoriteInstruments[1].iconResName).isEqualTo(PREDEF_ICON_3) // AÑADIDO
    }

    @Test
    @Throws(Exception::class)
    fun getFavoritePredefinedInstruments_userWithNoFavorites_returnsEmptyList() {
        val favoriteInstruments = predefinedFavDao.getFavoritePredefinedInstrumentsForUser(USER_1_ID)
        assertThat(favoriteInstruments).isNotNull()
        assertThat(favoriteInstruments).isEmpty()
    }
}