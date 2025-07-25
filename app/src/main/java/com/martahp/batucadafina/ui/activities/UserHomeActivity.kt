package com.martahp.batucadafina.ui.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.martahp.batucadafina.R
import com.martahp.batucadafina.databinding.ActivityUserHomeBinding
import com.martahp.batucadafina.databinding.DialogLogoutBinding
import com.martahp.batucadafina.ui.viewmodel.UserHomeViewModel

class UserHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserHomeBinding
    private val viewModel: UserHomeViewModel by viewModels()
    private var userID: Long? = null // para poder obtener el ID del usuario y enviar datos a las demás Activities
    private var currentUsername: String? = null
    private val TAG = "UserHomeActivity"

    // --- Activity Result Launcher para SettingsActivity ---
    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "Resultado recibido de SettingsActivity, resultCode: ${result.resultCode}")
            if (this.currentUsername != null) { //nos aseguramos de que tenemos el username
                val newUsernameFromSettings = result.data?.getStringExtra(SettingsActivity.EXTRA_USERNAME)
                if (result.resultCode == Activity.RESULT_OK && newUsernameFromSettings != null) {
                    Log.d(TAG, "SettingsActivity devolvió RESULT_OK. Username podría haber cambiado a: $newUsernameFromSettings.")
                    this.currentUsername = newUsernameFromSettings //actualizamos el username
                } else {
                    Log.d(TAG, "SettingsActivity cerrada o sin cambio de nombre explícito. Recargando perfil con username actual: ${this.currentUsername}")
                }
                //forzamos el recargado del perfil
                viewModel.loadUserProfile(this.currentUsername!!)
            } else {
                Log.w(TAG, "currentUsername es null en UserHomeActivity después de volver de Settings. No se puede recargar el perfil.")
            }
        }

    //constante para poder enviar el username a través de un intent
    companion object {
        const val EXTRA_USERNAME = "com.martahp.batucadafina.USERNAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Lógica Inicial ---
        // Intentar obtener el username pasado desde la Activity anterior (Login/Registro)
        val usernameIntent = intent.getStringExtra(EXTRA_USERNAME)

        // Comprobación de seguridad: ¿Recibimos el username?
        if (usernameIntent == null) {
            // Si no, es un error -> Log, Toast y volver a la pantalla inicial/login
            Log.e("UserHomeActivity", "No se recibió username. Volviendo a Login/Main.")
            Toast.makeText(this, "Error: Sesión no válida.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, MainActivity::class.java) // O LoginUserActivity
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Cerrar esta Activity para que no quede en la pila
            return // Salir de onCreate para evitar ejecutar el resto
        }

        // Si SÍ tenemos username, le pedimos al ViewModel que cargue su perfil
        this.currentUsername = usernameIntent
        viewModel.loadUserProfile(this.currentUsername!!)

        // Llamamos a funciones separadas para configurar listeners y observadores
        configureToolbar()
        configureListeners()
        observeViewModel()
    }

    // --- Métodos de Configuración ---

    //en esta ocasión la toolbar tendrá un botón de retroceso con el que se cierra la sesion y volvemos a MainActivity
    private fun configureToolbar() {
        setSupportActionBar(binding.toolbarUserHome)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbarUserHome.setNavigationOnClickListener {
            showLogoutConfirmationDialog()

        }
    }

    // Configura los OnClickListeners para los botones
    private fun configureListeners() {
        binding.buttonBasicTuner.setOnClickListener {
            val intent = Intent(this, BasicTunerActivity::class.java)
            startActivity(intent) // Inicia la Activity del afinador básico
        }

        binding.buttonTips.setOnClickListener {
            val intent = Intent(this, TipsActivity::class.java)
            startActivity(intent)
        }
        binding.buttonSettings.setOnClickListener {
            if (this.currentUsername != null && this.userID != null && this.userID != -1L) {
                val intent = Intent(this, SettingsActivity::class.java)
                intent.putExtra(SettingsActivity.EXTRA_USERNAME, this.currentUsername)
                intent.putExtra(SettingsActivity.EXTRA_USER_ID, this.userID)
                settingsLauncher.launch(intent)
            } else {
                Toast.makeText(this, "Información de usuario no disponible.", Toast.LENGTH_SHORT).show()
                Log.e("UserHomeActivity", "Usuario o ID null al intentar ir a SettingsActivity.")
            }
        }

        binding.buttonTuneFavorites.setOnClickListener {
            if (this.userID != null && this.userID != -1L) { // 'this.userID' es la variable miembro para guardar el ID del usuario
                val intent = Intent(this, TuneFavoritesActivity::class.java)
                // const de TuneFavoritesActivity para la clave del extra
                intent.putExtra(TuneFavoritesActivity.EXTRA_USER_ID, this.userID)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Información de usuario no disponible.", Toast.LENGTH_SHORT).show()
                Log.e("UserHomeActivity", "UserID es null al intentar ir a TuneFavoritesActivity.")
            }
        }
        binding.buttonInstruments.setOnClickListener {
            if (this.userID != null) {
                val intent = Intent(this, InstrumentsActivity::class.java)
                intent.putExtra(InstrumentsActivity.EXTRA_USER_ID, this.userID)
                startActivity(intent)
            } else {
                Log.e("UserHomeActivity", "No se recibió userID para ir a InstrumentsActivity.")
                Toast.makeText(this, "Error: información de usuario no cargada.", Toast.LENGTH_LONG).show()
            }
        }
        binding.buttonCreateInstrument.setOnClickListener {
            if (this.userID != null && this.userID != -1L) {
                val intent = Intent(this, CreateInstrumentActivity::class.java)
                intent.putExtra(CreateInstrumentActivity.EXTRA_USER_ID, this.userID)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Información de usuario no disponible.", Toast.LENGTH_SHORT).show()
                Log.e("UserHomeActivity", "UserID es null al intentar ir a CreateInstrumentActivity.")
            }
        }
    }

    // Configuracion de los observadores de LiveData del ViewModel
    private fun observeViewModel() {
        // Observar el LiveData 'userProfile' del ViewModel
        viewModel.userProfile.observe(this) { user ->
            if (user != null) {
                // Actualizar currentUsername por si acaso cambió y el perfil se recargó
                this.currentUsername = user.username
                this.userID = user.id
                Log.d("UserHomeActivity", "Perfil observado y actualizado. User ID: ${this.userID}, Usuario: ${this.currentUsername}")
                binding.textViewUserName.text = user.username
                //cargar la foto de perfil con Glide
                Glide.with(this)
                    .load(user.profilePhotoPath)
                    .placeholder(R.drawable.ic_default_user_avatar)
                    .error(R.drawable.ic_default_user_avatar)
                    .circleCrop()
                    .into(binding.imageViewProfilePicUserHome)
            } else {
                binding.textViewUserName.text = "Usuario" //por defecto si usuario es null
                this.currentUsername = null //por defecto si usuario es null
                this.userID = null
                binding.imageViewProfilePicUserHome.setImageResource(R.drawable.ic_default_user_avatar) //placeholder si el user es null

                Log.w(TAG, "El perfil de usuario es null en el observador. Forzando logout.")
                Toast.makeText(this, "Error: No se pudo cargar el perfil de usuario. Volviendo al inicio", Toast.LENGTH_LONG).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finishAffinity()
            }

        }

        viewModel.logoutEvent.observe(this) { event ->
            event.getContentIfNotProcessed()?.let {
                Log.i(TAG, "Evento de logout recibido. Cerrando sesión y volviendo a MainActivity.")
                Toast.makeText(this, "Has cerrado sesión.", Toast.LENGTH_SHORT).show()

                // Este es el código que navega a la pantalla principal y limpia la pila de actividades
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finishAffinity() // Cierra esta actividad y todas las que estuvieran antes
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        val localCurrentUsername = this.currentUsername
        if (localCurrentUsername == null) {
            Toast.makeText(this, "Error de sesión.", Toast.LENGTH_SHORT).show()
            return
        }

        // Inflamos el layout personalizado del diálogo
        val dialogBinding = DialogLogoutBinding.inflate(LayoutInflater.from(this))
        val builder = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        // Botón de cancelar
        dialogBinding.buttonDialogCancelLogout.setOnClickListener {
            builder.dismiss()
        }

        // Botón de confirmar cierre de sesión
        dialogBinding.buttonDialogConfirmLogout.setOnClickListener {
            Log.d(TAG, "Cerrando sesión para: $localCurrentUsername")
            viewModel.logoutUserAccountRequest()
            builder.dismiss()
        }
        builder.show()
    }
}