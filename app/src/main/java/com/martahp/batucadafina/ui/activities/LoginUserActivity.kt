package com.martahp.batucadafina.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.martahp.batucadafina.databinding.ActivityLoginUserBinding
import androidx.activity.viewModels
import com.martahp.batucadafina.ui.viewmodel.LoginUserViewModel
import android.widget.Toast
import com.martahp.batucadafina.ui.viewmodel.LoginResultSealed

class LoginUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginUserBinding
    private val viewModel: LoginUserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureToolbar()
        configureListeners()
        observeViewModel()

    }

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbarLoginUser)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbarLoginUser.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun configureListeners() {

        // Listener para el botón de inicio de sesión
        binding.buttonLogin.setOnClickListener {
            val username = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString() //no trim() porque es contraseña
            val rememberMe = binding.checkBoxRememberMe.isChecked

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    "Por favor, completa usuario y contraseña.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            viewModel.loginUser(username, password, rememberMe)
        }

        // Listener para el enlace de registro
        binding.textViewSignUpLink.setOnClickListener {
            val intent = Intent(this, CreateAccountActivity::class.java)
            startActivity(intent)
        }

        //listener para "he olvidado mi contraseña"
        binding.textViewForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

    }

    private fun observeViewModel() {

        viewModel.loginResult.observe(this) { event -> // cambio: observa Event<LoginResult>
            event.getContentIfNotProcessed()?.let { result ->
                when (result) {
                    is LoginResultSealed.Success -> {
                        Toast.makeText(this, "Inicio de sesión correcto.", Toast.LENGTH_SHORT).show() // pasa a UserHomeActivity si es correcto
                        val intent = Intent(this, UserHomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        intent.putExtra(UserHomeActivity.EXTRA_USERNAME, result.username)
                        startActivity(intent)
                        finish()
                } is LoginResultSealed.InvalidCredentials -> {
                    Toast.makeText(this, "Usuario o contraseña incorrectos.", Toast.LENGTH_SHORT).show()
                } is LoginResultSealed.Error -> {
                    Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                }
            }
        }
    }
}
