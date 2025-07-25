package com.martahp.batucadafina.data.dao

// Imports para el Runner y Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import android.content.Context

// Imports de JUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat // Importar Truth

// Imports de tu código y constantes
import com.martahp.batucadafina.data.DatabaseHelper
import com.martahp.batucadafina.data.DbConstants // Importar constantes
import java.io.IOException // Para @Throws en tearDown


@RunWith(AndroidJUnit4::class) // ¡IMPORTANTE! Añadir el Runner de Android
class UserDAOTest {

    // Declarar las variables miembro que usaremos en los tests
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var userDao: UserDAO // el DAO que vamos a probar
    private lateinit var context: Context // Necesario para el DAO

    // --- Constantes para datos de prueba ---
    private val USER_1_NAME = "usuario1"
    private val USER_1_PASS = "pass1"
    private val USER_2_NAME = "usuario2"
    private val USER_2_PASS = "pass2"
    private val NON_EXISTENT_USER = "noExiste"
    private val PHOTO_PATH_1 = "/data/test/photo1.jpg"
    private val EMAIL_1 = "pruebamail@test.com"
    private val EMAIL_2 = "pruebamail2@test.com"

    @Before // Anotación para el mtodo de configuración
    fun setUp() {
        // --- Implementación de la Configuración ---
        // 1. Obtener el contexto real de la app instrumentada
        context = ApplicationProvider.getApplicationContext<Context>()

        // 2. Borrar la base de datos ANTES de cada test.
        //    Esto asegura que empezamos con un estado limpio y que
        //    DatabaseHelper ejecutará su mtodo onCreate().
        context.deleteDatabase(DbConstants.DATABASE_NAME)

        // 3. Crear una nueva instancia del Helper (ejecutará onCreate)
        dbHelper = DatabaseHelper(context)

        // 4. Crear una instancia del DAO que vamos a probar
        userDao = UserDAO(context)
        // --- Fin de la Configuración ---
    }

    @After // Anotación para el mtodo de limpieza
    @Throws(IOException::class) // Buena práctica
    fun tearDown() {
        // --- Implementación de la Limpieza ---
        // 1. Cerrar la conexión a la base de datos para liberar recursos
        dbHelper.close()
        // --- Fin de la Limpieza ---
    }

    // --- Tests para Añadir un Usuario (addUser) ---

    @Test
    @Throws(Exception::class)
    fun addUser_newUser_returnsSuccessAndUserExists() {
        // Arrange: Datos listos

        // Act: Añadir usuario nuevo
        val result = userDao.addUser(USER_1_NAME, USER_1_PASS, EMAIL_1)

        // Assert: Verificar resultado y existencia
        assertThat(result).isEqualTo(1) // Código 1 = éxito
        val retrievedUser = userDao.getUserProfileByName(USER_1_NAME)
        assertThat(retrievedUser).isNotNull()
        assertThat(retrievedUser?.username).isEqualTo(USER_1_NAME)
    }

    @Test
    @Throws(Exception::class)
    fun addUser_existingUser_returnsAlreadyExistsCode() {
        //arrange:añadir un usuario primero
        userDao.addUser(USER_1_NAME, USER_1_PASS, EMAIL_1)

        //act: intentar añadir el mismo usuario de nuevo
        val result = userDao.addUser(USER_1_NAME, USER_2_PASS, EMAIL_2)

        //assert: verificar resultado
        assertThat(result).isEqualTo(0) // Código 0 = ya existe
    }


    // --- Tests para comprobar si un usuario existe (checkUserExists) ---

    @Test
    @Throws(Exception::class)
    fun checkUserExists_existingUser_returnsTrue() {
        // Arrange: Añadir usuario
        userDao.addUser(USER_1_NAME, USER_1_PASS, EMAIL_1)

        // Act: Comprobar existencia
        val exists = userDao.checkUserExists(USER_1_NAME)

        // Assert: Verificar
        assertThat(exists).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun checkUserExists_nonExistentUser_returnsFalse() {
        // Arrange: No añadir el usuario

        // Act: Comprobar existencia
        val exists = userDao.checkUserExists(NON_EXISTENT_USER)

        // Assert: Verificar
        assertThat(exists).isFalse()
    }

    // --- Tests para verificar credenciales (checkUserCredentials) ---

    @Test
    @Throws(Exception::class)
    fun checkUserCredentials_validCredentials_returnsTrue() {
        // Arrange: Añadir usuario
        userDao.addUser(USER_1_NAME, USER_1_PASS, EMAIL_1)

        // Act: Comprobar credenciales correctas
        val matches = userDao.checkUserCredentials(USER_1_NAME, USER_1_PASS)

        // Assert: Verificar
        assertThat(matches).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun checkUserCredentials_validUserInvalidPassword_returnsFalse() {
        // Arrange: Añadir usuario
        userDao.addUser(USER_1_NAME, USER_1_PASS, EMAIL_1)

        // Act: Comprobar credenciales con contraseña INCORRECTA
        val matches = userDao.checkUserCredentials(USER_1_NAME, "passInvalida")

        // Assert: Verificar
        assertThat(matches).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun checkUserCredentials_invalidUser_returnsFalse() {
        // Arrange: No añadir el usuario

        // Act: Comprobar credenciales de usuario inexistente
        val matches = userDao.checkUserCredentials(NON_EXISTENT_USER, "cualquierPass")

        // Assert: Verificar
        assertThat(matches).isFalse()
    }

    // --- Tests para actualizar el nombre de usuario (updateUsername) ---

    @Test
    @Throws(Exception::class)
    fun updateUsername_validUpdate_returnsSuccessAndUpdatesUsername() {
        // Arrange: Añadir usuario original
        userDao.addUser(USER_1_NAME, USER_1_PASS, EMAIL_1)
        val newUsername = "usuario1_modificado"

        // Act: Actualizar username
        val result = userDao.updateUsername(USER_1_NAME, newUsername)

        // Assert: Verificar resultado y cambio
        assertThat(result).isEqualTo(1) // Código 1 = éxito
        val oldUser = userDao.getUserProfileByName(USER_1_NAME)
        val newUser = userDao.getUserProfileByName(newUsername)
        assertThat(oldUser).isNull() // El perfil con el nombre antiguo ya no debería existir
        assertThat(newUser).isNotNull() // El perfil con el nombre nuevo sí
        assertThat(newUser?.username).isEqualTo(newUsername)
    }

    @Test
    @Throws(Exception::class)
    fun updateUsername_newUserAlreadyExists_returnsAlreadyExistsCode() {
        // Arrange: Añadir dos usuarios
        userDao.addUser(USER_1_NAME, USER_1_PASS, EMAIL_1)
        userDao.addUser(USER_2_NAME, USER_2_PASS, EMAIL_2)

        // Act: Intentar cambiar USER_1 al nombre de USER_2
        val result = userDao.updateUsername(USER_1_NAME, USER_2_NAME)

        // Assert: Verificar resultado
        assertThat(result).isEqualTo(0) // Código 0 = nuevo username ya existe
        // Verificar que el usuario original no cambió
        val originalUser = userDao.getUserProfileByName(USER_1_NAME)
        assertThat(originalUser).isNotNull()
        assertThat(originalUser?.username).isEqualTo(USER_1_NAME)
    }

    @Test
    @Throws(Exception::class)
    fun updateUsername_oldUserDoesNotExist_returnsErrorCode() {
        // Arrange: No añadir el usuario original
        val newUsername = "usuario_nuevo"

        // Act: Intentar actualizar un usuario inexistente
        val result = userDao.updateUsername(NON_EXISTENT_USER, newUsername)

        // Assert: Verificar resultado
        assertThat(result).isLessThan(1) // Esperamos 0 o -1 (no éxito)
        val user = userDao.getUserProfileByName(newUsername)
        assertThat(user).isNull() // El nuevo usuario no debería haberse creado
    }

    // --- Tests para actualizar la contraseña (updatePassword) ---

    @Test
    @Throws(Exception::class)
    fun updatePassword_validUser_updatesPasswordSuccessfully() {
        // Arrange: Añadir usuario
        userDao.addUser(USER_1_NAME, USER_1_PASS, EMAIL_1)
        val newPassword = "nuevaPasswordSegura"

        // Act: Actualizar contraseña
        val success = userDao.updatePassword(USER_1_NAME, newPassword)

        // Assert: Verificar que la actualización fue exitosa y la nueva contraseña funciona
        assertThat(success).isTrue()
        val credentialsMatch = userDao.checkUserCredentials(USER_1_NAME, newPassword)
        assertThat(credentialsMatch).isTrue()
        // Verificar que la contraseña antigua ya NO funciona
        val oldCredentialsMatch = userDao.checkUserCredentials(USER_1_NAME, USER_1_PASS)
        assertThat(oldCredentialsMatch).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun updatePassword_nonExistentUser_returnsFalse() {
        // Arrange: No añadir usuario

        // Act: Intentar actualizar contraseña de usuario inexistente
        val success = userDao.updatePassword(NON_EXISTENT_USER, "passAleatoria")

        // Assert: Verificar que la operación falló
        assertThat(success).isFalse()
    }

    // --- Tests para obtener el perfil de un usuario (getUserProfile) ---

    @Test
    @Throws(Exception::class)
    fun getUserProfile_nonExistentUser_returnsNullByName() {
        // Arrange: No añadir usuario

        // Act: Intentar obtener perfil
        val retrievedUser = userDao.getUserProfileByName(NON_EXISTENT_USER)

        // Assert: Verificar que es nulo
        assertThat(retrievedUser).isNull()
    }

    // el caso de éxito de getUserProfile está en addUser (addUserAndGetUserProfile)

    // --- Tests para actualizar la foto de perfil (updateUserProfilePicPath) ---

    @Test
    @Throws(Exception::class)
    fun updateUserProfilePicPath_validUser_updatesPathCorrectly() {
        // Arrange: Añadir usuario y obtener su ID
        userDao.addUser(USER_1_NAME, USER_1_PASS, EMAIL_1)
        val user = userDao.getUserProfileByName(USER_1_NAME)
        assertThat(user).isNotNull()
        val userId = user!!.id // Sabemos que no es nulo por la aserción anterior

        // Act: Actualizar path de la foto
        val success = userDao.updateUserProfilePicPath(userId, PHOTO_PATH_1)

        // Assert: Verificar éxito y que el path se guardó
        assertThat(success).isTrue()
        val updatedUser = userDao.getUserProfileByName(USER_1_NAME)
        assertThat(updatedUser).isNotNull()
        assertThat(updatedUser?.profilePhotoPath).isEqualTo(PHOTO_PATH_1)
    }

    @Test
    @Throws(Exception::class)
    fun updateUserProfilePicPath_setPathToNull_updatesPathToNull() {
        // Arrange: Añadir usuario, ponerle foto y obtener su ID
        userDao.addUser(USER_1_NAME, USER_1_PASS, EMAIL_1)
        val user = userDao.getUserProfileByName(USER_1_NAME)
        assertThat(user).isNotNull()
        val userId = user!!.id
        userDao.updateUserProfilePicPath(userId, PHOTO_PATH_1) // Poner foto inicial

        // Act: Actualizar path a null (quitar foto)
        val success = userDao.updateUserProfilePicPath(userId, null)

        // Assert: Verificar éxito y que el path es null
        assertThat(success).isTrue()
        val updatedUser = userDao.getUserProfileByName(USER_1_NAME)
        assertThat(updatedUser).isNotNull()
        assertThat(updatedUser?.profilePhotoPath).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun updateUserProfilePicPath_invalidUserId_returnsFalse() {
        // Arrange: ID de usuario inválido
        val invalidUserId = 999L

        // Act: Intentar actualizar path para ID inválido
        val success = userDao.updateUserProfilePicPath(invalidUserId, PHOTO_PATH_1)

        // Assert: Verificar que falló
        assertThat(success).isFalse()
    }

    // --- Tests para eliminar un usuario (deleteUser) ---

    @Test
    @Throws(Exception::class)
    fun deleteUser_existingUser_deletesUserAndReturnsTrue() {
        // Arrange: Añadir usuario
        userDao.addUser(USER_1_NAME, USER_1_PASS, EMAIL_1)
        // Verificar que existe antes de borrar
        assertThat(userDao.checkUserExists(USER_1_NAME)).isTrue()

        // Act: Borrar usuario
        val success = userDao.deleteUser(USER_1_NAME)

        // Assert: Verificar que se borró y ya no existe
        assertThat(success).isTrue()
        assertThat(userDao.checkUserExists(USER_1_NAME)).isFalse()
        assertThat(userDao.getUserProfileByName(USER_1_NAME)).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun deleteUser_nonExistentUser_returnsFalse() {
        // Arrange: Asegurarse de que el usuario no existe
        assertThat(userDao.checkUserExists(NON_EXISTENT_USER)).isFalse()

        // Act: Intentar borrar usuario inexistente
        val success = userDao.deleteUser(NON_EXISTENT_USER)

        // Assert: Verificar que falló
        assertThat(success).isFalse()
    }
}