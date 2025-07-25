package com.martahp.batucadafina.ui.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.martahp.batucadafina.R
import com.martahp.batucadafina.databinding.ActivityCreateInstrumentBinding
import com.martahp.batucadafina.ui.viewmodel.CreateInstrumentViewModel

class CreateInstrumentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateInstrumentBinding
    private val viewModel: CreateInstrumentViewModel by viewModels()

    private var currentUserId: Long = -1L
    private var selectedImageUri: Uri? = null
    private val TAG = "CreateInstrumentActivity"

    companion object {
        const val EXTRA_USER_ID = "com.martahp.batucadafina.CREATE_INSTR_USER_ID"
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    // Mostrar vista previa
                    Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .placeholder(R.drawable.ic_default_instrument)
                        .into(binding.imageViewInstrumentIconCreate)
                    Log.d(TAG, "Imagen seleccionada para instrumento: $uri")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateInstrumentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = intent.getLongExtra(EXTRA_USER_ID, -1L)
        if (currentUserId == -1L) {
            Toast.makeText(this, "Error: Usuario no especificado.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "No se recibió userId para crear el instrumento.")
            finish()
            return
        }
        configureToolbar()
        configureListeners()
        observeViewModel()
    }

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbarCreateInstrument)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarCreateInstrument.setNavigationOnClickListener {
            finish()
        }
    }

    private fun configureListeners() {
        binding.buttonSelectInstrumentIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            try {
                pickImageLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir la galería: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                Log.e(TAG, "Error al lanzar galería", e)
            }
        }

        binding.buttonSaveInstrument.setOnClickListener {
            val name = binding.editTextInstrumentName.text.toString().trim()
            val freqStr = binding.editTextInstrumentFrequency.text.toString().trim()
            val info = binding.editTextInstrumentInfo.text.toString().trim()
            // La URI de la imagen ya está en selectedImageUri

            if (name.isEmpty()) {
                binding.textInputLayoutInstrumentName.error = "El nombre es obligatorio"
                return@setOnClickListener
            } else {
                binding.textInputLayoutInstrumentName.error = null //limpiamos el error
            }

            val frequency = freqStr.toDoubleOrNull() //null si no es un número válido

            viewModel.saveNewInstrument(currentUserId, name, frequency, info, selectedImageUri)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "isLoading: $isLoading")
            binding.buttonSaveInstrument.isEnabled = !isLoading ///deshabilitamos el boton de guardar si esta cargando
            binding.buttonSelectInstrumentIcon.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.instrumentSaveResult.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { success ->
                if (success) {
                    Toast.makeText(this, "Instrumento guardado con éxito.", Toast.LENGTH_SHORT)
                        .show()
                    setResult(Activity.RESULT_OK) //para devolver el resultado a la actividad anterior //TODO: realmente debe devolverlo a las activities donde aparecen los instrumentos ¿?
                    finish() //cerramos y volvemos a la activity anterior
                } else {
                    Toast.makeText(this, "No se pudo guardar el instrumento.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}