package com.martahp.batucadafina.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.view.LayoutInflater
import android.widget.Toast
import android.app.Activity //para activity result
import androidx.activity.result.contract.ActivityResultContracts //para el nuevo Activity Result API
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.martahp.batucadafina.R
import com.martahp.batucadafina.databinding.ActivitySettingsBinding
import com.martahp.batucadafina.databinding.DialogChangeUsernameBinding //binding del diálogo
import com.martahp.batucadafina.databinding.DialogChangePasswordBinding //binding del diálogo
import com.martahp.batucadafina.databinding.DialogDeleteAccountBinding //binding del diálogo
import com.martahp.batucadafina.databinding.DialogLogoutBinding //binding del diálogo
import com.martahp.batucadafina.ui.viewmodel.SettingsViewModel
import com.martahp.batucadafina.ui.viewmodel.UpdatePasswordUiState
import com.martahp.batucadafina.ui.viewmodel.UpdateUsernameUiState //hay que importar esta sealed class

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    private var currentUsername: String? = null
    private var currentUserId: Long = -1L
    private val TAG = "SettingsActivity"
    private var profileDataChanged: Boolean = false //flag para saber si se ha cambiado la foto de perfil y se debe actualizar

    companion object {
        const val EXTRA_USERNAME = "com.martahp.batucadafina.SETTINGS_USERNAME"
        const val EXTRA_USER_ID = "com.martahp.batucadafina.SETTINGS_USER_ID"
    }

    // añadimos ActivityResultLauncher para manejar la selección de imagen desde la galería
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { imageUri ->
                    Log.d(TAG, "Imagen seleccionada URI: $imageUri")
                    if (currentUserId != -1L && currentUsername != null) {
                        viewModel.updateUserProfilePhotoPath(imageUri, currentUserId, currentUsername!!)
                    } else {
                        Toast.makeText(this, "Error: Información de usuario no disponible.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.d(TAG, "Selección de imagen fallida.")
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUsername = intent.getStringExtra(EXTRA_USERNAME)
        currentUserId = intent.getLongExtra(EXTRA_USER_ID, -1L)

        if (currentUsername == null || currentUserId == -1L) {
            Toast.makeText(this, "Error al cargar datos de usuario.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        viewModel.loadUserProfile(currentUsername!!)

        configureToolbar()
        configureListeners()
        observeViewModel()
    }

    //TODO: volver a implementar esto
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        finishWithResult()
    }

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbarSettings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbarSettings.setNavigationOnClickListener {
            finishWithResult()
        }
    }

    //añadimos esta función para que actualice los datos del perfil
    private fun finishWithResult() {
        val resultIntent = Intent()
        if (profileDataChanged) {
            resultIntent.putExtra(EXTRA_USERNAME, this.currentUsername) //pasamos el username actualizado
            setResult(RESULT_OK, resultIntent)
            Log.d(TAG, "FinishWithResult: Datos de perfil actualizados. Username: $currentUsername")
        } else {
            setResult(RESULT_CANCELED)
            Log.d(TAG, "FinishWithResult: No se actualizaron datos de perfil. RESULT_CANCELLED")
        }
        finish()
    }

    private fun configureListeners() {

        //llamaremos a la función que muestra el diálogo en cada listener
        binding.frameLayoutProfilePic.setOnClickListener { //podemos hacerlo con imageviewprofilepicsettings también
            selectImageFromGallery()
        }

        binding.optionChangeUsername.setOnClickListener {
            showChangeUsernameDialog()
        }

        binding.optionChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.buttonDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

        binding.optionLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun observeViewModel() {

        viewModel.userProfile.observe(this) { user ->
            if (user != null) {
                if (this.currentUsername != user.username) {
                    // Actualizar currentUsername por si acaso cambió y el perfil se recargó
                    this.currentUsername = user.username
                    Log.d(TAG, "Perfil observado y actualizado. User ID: ${this.currentUserId}, Usuario: ${this.currentUsername}")
                    profileDataChanged = true
                }
                binding.textViewCurrentUsername.text = user.username
                Glide.with(this)
                    .load(user.profilePhotoPath)
                    .placeholder(R.drawable.ic_default_user_avatar)
                    .error(R.drawable.ic_default_user_avatar)
                    .circleCrop()
                    .into(binding.imageViewProfilePicSettings)
            } else {
                binding.textViewCurrentUsername.text = "Usuario no encontrado"
                binding.imageViewProfilePicSettings.setImageResource(R.drawable.ic_default_user_avatar)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            //podemos deshabilitar los botones mientras se carga
            binding.optionChangeUsername.isEnabled = !isLoading
            binding.optionChangePassword.isEnabled = !isLoading
            binding.frameLayoutProfilePic.isEnabled = !isLoading
            binding.optionLogout.isEnabled = !isLoading
            binding.buttonDeleteAccount.isEnabled = !isLoading
            if (isLoading) {
                Log.d("SettingsActivity", "Cargando...")
            } else {
                Log.d("SettingsActivity", "Carga finalizada.")
            }
        }

        // --- Observer para el resultado de actualizar username ---
        viewModel.updateUsernameResult.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { resultState ->
                when (resultState) {
                    is UpdateUsernameUiState.Success -> {
                        profileDataChanged = true //añadimos para cambiar los datos de perfil
                        Toast.makeText(
                            this,
                            "Nombre de usuario actualizado con éxito.",
                            Toast.LENGTH_SHORT
                        ).show()
                        //añadimos la lógica
                    }

                    is UpdateUsernameUiState.NewUserNameTaken -> {
                        Toast.makeText(
                            this,
                            "Ese nombre de usuario ya está en uso. Elige otro.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    is UpdateUsernameUiState.CurrentUserNotFound -> {
                        Toast.makeText(
                            this,
                            "Error: No se encontró el usuario actual para actualizar.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    is UpdateUsernameUiState.Error -> {
                        Toast.makeText(
                            this,
                            "Error al actualizar nombre: ${resultState.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        // --- Observer para el resultado de actualizar password ---
        viewModel.updatePasswordResult.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { resultState ->
                when (resultState) {
                    is UpdatePasswordUiState.Success -> {
                        Toast.makeText(
                            this,
                            "Contraseña actualizada con éxito.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    is UpdatePasswordUiState.CurrentPasswordIncorrect -> {
                        Toast.makeText(
                            this,
                            "La contraseña actual es incorrecta.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    is UpdatePasswordUiState.Error -> {
                        Toast.makeText(
                            this,
                            "Error al actualizar contraseña: ${resultState.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        // --- Observer para el resultado de eliminar cuenta ---
        viewModel.deleteAccountResult.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { success ->
                if (success) {
                    Toast.makeText(this, "Cuenta eliminada exitosamente.", Toast.LENGTH_LONG).show()
                    //vamos a la pantalla de login y limpiamos todas las activities anteriores
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finishAffinity() //esto cierra todas las activities anteriores (pila)
                } else {
                    Toast.makeText(
                        this,
                        "No se pudo eliminar la cuenta. Verifica tu contraseña o inténtalo más tarde.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // --- Observer para el resultado de actualizar foto de perfil ---
        viewModel.updatePhotoResult.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { success ->
                //userProfile (y la foto) se recargará desde el ViewModel, actualizando la imagen.
                if (success) {
                    profileDataChanged = true //añadimos para cambiar los datos de perfil
                    Toast.makeText(this, "Foto de perfil actualizada.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        "Error al actualizar la foto de perfil.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // --- Observer para la solicitud de cierre de sesión ---
        viewModel.logoutEvent.observe(this) { event ->
            event.getContentIfNotProcessed()?.let {
                Log.i(TAG, "Cerrando sesión y volviendo a la pantalla principal.")
                Toast.makeText(this, "Has cerrado sesión.", Toast.LENGTH_SHORT).show()

                val intent = Intent(
                    this,
                    MainActivity::class.java
                ) // vamos a la pantalla de inicio a través de un intent
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finishAffinity() //cerramos pila
            }
        }
    }

    private fun showChangeUsernameDialog() {
        // verificar que tenemos el nombre de usuario actual para pasarlo al ViewModel
        val localCurrentUsername = currentUsername
        if (localCurrentUsername == null) {
            Toast.makeText(this, "No se pueden cargar los datos del usuario.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        //inflar el layout personalizado del diálogo
        val dialogBinding = DialogChangeUsernameBinding.inflate(LayoutInflater.from(this))

        //construimos el AlertDialog
        val builder = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        // Configurar listeners para los botones
        dialogBinding.buttonDialogCancelUsername.setOnClickListener {
            builder.dismiss()
        }

        dialogBinding.buttonDialogUpdateUsername.setOnClickListener { // Usa el ID de tu botón "Actualizar" en el XML
            val newUsername = dialogBinding.dialogEditTextNewUsername.text.toString().trim()

            dialogBinding.dialogTextInputLayoutNewUsername.error = null

            //validaciones
            if (newUsername.isEmpty()) {
                dialogBinding.dialogTextInputLayoutNewUsername.error = "El nuevo nombre no puede estar vacío."
                return@setOnClickListener
            }
            else if (newUsername == currentUsername) {
                dialogBinding.dialogTextInputLayoutNewUsername.error = "El nuevo nombre no puede ser igual al actual."
            } else {
                viewModel.updateUserUsername(localCurrentUsername, newUsername)
                return@setOnClickListener
            }
        }
        builder.show()
    }

    private fun showChangePasswordDialog() {
        val localCurrentUsername = this.currentUsername
        if (localCurrentUsername == null) {
            Toast.makeText(this, "No se pueden cargar los datos del usuario.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        //inflar el layout personalizado del diálogo
        val dialogBinding = DialogChangePasswordBinding.inflate(LayoutInflater.from(this))

        val builder = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        //botón cancelar
        dialogBinding.buttonDialogCancelPassword.setOnClickListener {
            builder.dismiss()
        }
        //botón actualizar
        dialogBinding.buttonDialogUpdatePassword.setOnClickListener {
            val currentPassword = dialogBinding.dialogEditTextCurrentPassword.text.toString()
            val newPassword = dialogBinding.dialogEditTextNewPassword.text.toString()
            val confirmPassword = dialogBinding.dialogEditTextConfirmNewPassword.text.toString()

            var isValid = true

            //validaciones
            if (currentPassword.isEmpty()) {
                dialogBinding.dialogTextInputLayoutCurrentPassword.error =
                    "La contraseña actual es obligatoria"
                isValid = false
            }
            if (newPassword.isEmpty()) {
                dialogBinding.dialogTextInputLayoutNewPassword.error =
                    "La nueva contraseña es obligatoria"
                isValid = false
            }
            if (confirmPassword.isEmpty()) {
                dialogBinding.dialogTextInputLayoutConfirmNewPassword.error =
                    "Confirma la nueva contraseña"
                isValid = false
            }

            if (!isValid) { //si alguna de las validaciones anteriores fallan, no hacemos nada
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                dialogBinding.dialogTextInputLayoutConfirmNewPassword.error =
                    "Las nuevas contraseñas no coinciden"
                return@setOnClickListener
            } else {
                dialogBinding.dialogTextInputLayoutNewPassword.error = null
                dialogBinding.dialogTextInputLayoutConfirmNewPassword.error = null
            }

            if (currentPassword == newPassword) {
                dialogBinding.dialogTextInputLayoutNewPassword.error =
                    "La nueva contraseña no puede ser igual a la actual"
                return@setOnClickListener
            }

            // Si todas las validaciones de la UI pasan:
            Log.d(TAG, "Intentando actualizar contraseña para: $localCurrentUsername")
            viewModel.updateUserPassword(localCurrentUsername, currentPassword, newPassword)
            builder.dismiss() // Cerrar el diálogo
        }
        builder.show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        val localCurrentUsername = this.currentUsername
        if (localCurrentUsername == null) {
            Toast.makeText(this, "Error de sesión.", Toast.LENGTH_SHORT).show()
            return
        }

        //inflamos el layout personalizado
        val dialogBinding = DialogDeleteAccountBinding.inflate(LayoutInflater.from(this))
        val builder = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.buttonDialogCancelAccount.setOnClickListener {
            builder.dismiss()
        }

        dialogBinding.buttonDialogDeleteAccount.setOnClickListener { // Asume este ID en tu XML
            // Limpiar error previo
            dialogBinding.dialogTextInputLayoutCurrentPasswordForDelete.error = null

            val enteredPassword = dialogBinding.dialogEditTextCurrentPasswordForDelete.text.toString()

            if (enteredPassword.isEmpty()) {
                dialogBinding.dialogTextInputLayoutCurrentPasswordForDelete.error = "La contraseña es obligatoria para confirmar."
                // NO llames a alertDialog.dismiss() aquí, el diálogo permanece abierto
            } else {
                Log.d(TAG, "Confirmada eliminación de cuenta para: $localCurrentUsername")
                viewModel.deleteUserAccount(localCurrentUsername, enteredPassword)
                builder.dismiss()
            }
        }
        builder.show()
    }

    private fun showLogoutConfirmationDialog() {
        val localCurrentUsername = this.currentUsername
        if (localCurrentUsername == null) {
            Toast.makeText(this, "Error de sesión.", Toast.LENGTH_SHORT).show()
            return
        }

        //inflamos el layout personalizado
        val dialogBinding = DialogLogoutBinding.inflate(LayoutInflater.from(this))
        val builder = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.buttonDialogCancelLogout.setOnClickListener {
            builder.dismiss()
        }

        dialogBinding.buttonDialogConfirmLogout.setOnClickListener {
            Log.d(TAG, "Cerrando sesión para: $localCurrentUsername")
            viewModel.logoutUserAccountRequest() //llamamos al ViewModel para cerrar sesión
            builder.dismiss()
        }
        builder.show()
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*" // Mostrar solo imágenes
        try {
            pickImageLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir la galería: {e.message}", Toast.LENGTH_LONG)
                .show()
            Log.e(TAG, "Error al lanzar galería", e)
        }
    }
}
