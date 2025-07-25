package com.martahp.batucadafina.ui.activities

import android.app.Activity
import android.content.Intent // Para la navegación si es necesaria después
import android.os.Bundle
import android.util.Log
import android.view.View // Para View.VISIBLE / View.GONE
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.martahp.batucadafina.databinding.ActivityForgotPasswordBinding
import com.martahp.batucadafina.ui.viewmodel.ForgotPasswordViewModel
import com.martahp.batucadafina.model.entities.User // Para el tipo en userForPasswordReset

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()

    //guardamos el username del usuario cuyo email fue verificado
    private var verifiedUsernameForReset: String? = null
    private val TAG = "ForgotPasswordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarForgotPassword)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarForgotPassword.setNavigationOnClickListener {
            finish()
        }

        setupInitialUIState()
        configureListeners()
        observeViewModel()
    }

    private fun setupInitialUIState() {
        binding.groupVerifyEmail.visibility = View.VISIBLE
        binding.groupResetPassword.visibility = View.GONE
        binding.forgotPasswordProgressBar.visibility = View.GONE
    }

    private fun configureListeners() {
        binding.buttonVerifyEmail.setOnClickListener {
            val email = binding.editTextForgotEmail.text.toString().trim()
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.textInputLayoutForgotEmail.error = "Ingresa un email válido"
                return@setOnClickListener
            }
            binding.textInputLayoutForgotEmail.error = null
            Log.d(TAG, "Verificando email: $email")
            viewModel.verifyEmailForPasswordReset(email) // Llamar al ViewModel
        }

        binding.buttonResetPassword.setOnClickListener {
            val newPassword = binding.editTextNewPasswordForgot.text.toString()
            val confirmNewPassword = binding.editTextConfirmNewPasswordForgot.text.toString()


            if (newPassword != confirmNewPassword) {
                binding.textInputLayoutConfirmNewPasswordForgot.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }
            binding.textInputLayoutConfirmNewPasswordForgot.error = null

            if (verifiedUsernameForReset != null) {
                Log.d(TAG, "Intentando restablecer contraseña para: $verifiedUsernameForReset")
                viewModel.setNewPasswordForUser(verifiedUsernameForReset!!, newPassword)
            } else {
                Log.e(TAG, "Error: verifiedUsernameForReset es null al intentar restablecer contraseña.")
                Toast.makeText(this, "Error inesperado. Intenta verificar tu email de nuevo.", Toast.LENGTH_LONG).show()
                setupInitialUIState() // Volver al estado de pedir email
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "isLoading: $isLoading")
            binding.forgotPasswordProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonVerifyEmail.isEnabled = !isLoading
            binding.buttonResetPassword.isEnabled = !isLoading
            binding.editTextForgotEmail.isEnabled = !isLoading
            binding.editTextNewPasswordForgot.isEnabled = !isLoading
            binding.editTextConfirmNewPasswordForgot.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.userForPasswordReset.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { user ->
                if (user != null) {
                    this.verifiedUsernameForReset = user.username // Guardar el username para usarlo al resetear
                    Toast.makeText(this, "Email verificado. Ingresa tu nueva contraseña.", Toast.LENGTH_SHORT).show()
                    // Cambiar la visibilidad de los grupos
                    binding.groupVerifyEmail.visibility = View.GONE
                    binding.groupResetPassword.visibility = View.VISIBLE
                } else {
                    binding.textInputLayoutForgotEmail.error = "Email no registrado"
                }
            }
        }

        viewModel.passwordResetResult.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { success ->
                if (success) {
                    Toast.makeText(this, "Contraseña restablecida con éxito. Ya puedes iniciar sesión.", Toast.LENGTH_LONG).show()
                    finish()
                } else {

                    Toast.makeText(this, "No se pudo restablecer la contraseña. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}