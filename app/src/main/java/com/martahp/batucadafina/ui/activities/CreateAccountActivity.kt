package com.martahp.batucadafina.ui.activities

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Email
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.martahp.batucadafina.databinding.ActivityCreateAccountBinding
import androidx.activity.viewModels
import com.martahp.batucadafina.ui.viewmodel.CreateAccountViewModel
import com.martahp.batucadafina.ui.viewmodel.RegistrationResultSealed

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAccountBinding
    private val viewModel: CreateAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureToolbar()
        configureListeners()
        observeViewModel()
    }

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbarCreateAccount)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) //para mostrar la flecha
        supportActionBar?.setDisplayShowHomeEnabled(true) //para hacer la flecha clicable
        //configuramos el listener de la flecha
        binding.toolbarCreateAccount.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    /**
     * Configurar listeners para los botones y el enlace de inicio de sesión.
     */
    private fun configureListeners() {

        //botón de creación de cuenta
        binding.buttonCreateAccount.setOnClickListener {
            // 1. Obtener los datos de los EditText
            val username = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            val email = binding.editTextEmail.text.toString().trim()
            val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()

            // 2. Validaciones necesarias
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    "Por favor, completa usuario y contraseña.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener // Detiene la ejecución si hay campos vacíos
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.registerUser(username, password, email)

        }

        //botón (textview) de inicio de sesión
        binding.textViewSignInLink.setOnClickListener {
            val intent = Intent(this, LoginUserActivity::class.java)
            startActivity(intent)
            finish() // Cierra esta Activity (CreateAccountActivity) para evitar un loop estúpido
        }
    }

    /**
     * Configurar observadores de LiveData del ViewModel.
     */
    private fun observeViewModel() { //aquí también hacemos cambios por la sealed class añadida en el directorio del vm
        viewModel.registrationResult.observe(this) { event -> //ahora es de tipo Event<RegistrationResult>
            event.getContentIfNotProcessed()?.let { result -> //result es RegistrationResult
                when (result) { //ahora usamos el when en función del tipo de resultado, no el número entero que devuelve el DAO
                    is RegistrationResultSealed.Success -> {
                        Toast.makeText(this, "¡Usuario registrado con éxito!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, UserHomeActivity::class.java) // viajamos a UserHomeActivity porque ha sido exitoso el registro
                        // Opcional: Limpiar el stack para que no vuelva aquí al dar atrás desde UserHome
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Para hacer que la nueva actividad sea la única en el stack
                        intent.putExtra(UserHomeActivity.EXTRA_USERNAME, result.username) //para que reconozca username: reestructuración de código (lo vemos en la memoria)
                        startActivity(intent)
                        finish()
                    }

                    is RegistrationResultSealed.UserAlreadyExists -> {
                        Toast.makeText(this, "El nombre de usuario ya está en uso.", Toast.LENGTH_LONG).show() // No navegamos, el usuario debe elegir otro nombre
                    }

                    is RegistrationResultSealed.Error -> {
                        Toast.makeText(
                            this,
                            "Error: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        // No navegamos. El usuario debe intentar de nuevo.
                    }
                }
            }
        }
    }
}

