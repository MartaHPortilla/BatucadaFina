package com.martahp.batucadafina.ui.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.martahp.batucadafina.R
import com.martahp.batucadafina.databinding.ActivityEditInstrumentBinding
import com.martahp.batucadafina.model.entities.CreatedInstrument
import com.martahp.batucadafina.ui.viewmodel.EditInstrumentViewModel

class EditInstrumentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditInstrumentBinding
    private val viewModel: EditInstrumentViewModel by viewModels()

    private var currentInstrumentId: Long = -1L
    private var currentOwnerUserId: Long = -1L //userId del propietario del instrumento
    private var selectedImageUri: Uri? = null //nueva imagen seleccionada
    private var existingPhotoPath: String? = null //path de la foto actual
    private var deleteCurrentImageFlag: Boolean = false //eliminar foto actual?

    private val TAG = "EditInstrumentActivity"

    companion object {
        const val EXTRA_INSTRUMENT_ID = "com.martahp.batucadafina.INSTRUMENT_ID"
        const val EXTRA_USER_ID = "com.martahp.batucadafina.USER_ID" //id del usuario logueado
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    deleteCurrentImageFlag = false //no queremos borrar la foto actual
                    Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .placeholder(R.drawable.ic_default_instrument)
                        .into(binding.imageViewInstrumentIconEdit)
                    Log.d(TAG, "Nueva imagen seleccionada para instrumento: $uri")
                }
            } else {
                Log.d(TAG, "Selección de imagen cancelada.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditInstrumentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarEditInstrument)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarEditInstrument.setNavigationOnClickListener {
            finish()
        }

        currentInstrumentId = intent.getLongExtra(EXTRA_INSTRUMENT_ID, -1L)
        currentOwnerUserId = intent.getLongExtra(EXTRA_USER_ID, -1L)

        if (currentInstrumentId == -1L || currentOwnerUserId == -1L) {
            Toast.makeText(this, "Error: Instrumento o usuario no especificado.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "No se recibió instrumentId o userId para editar.")
            finish()
            return
        }

        configureListeners()
        observeViewModel()

        viewModel.loadInstrumentDetails(currentInstrumentId, currentOwnerUserId)
    }

    private fun configureListeners() {
        binding.buttonSelectInstrumentIconEdit.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            try {
                pickImageLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir la galería: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error al lanzar galería", e)
            }
        }

        //botón para eliminar la foto actual
        // binding.buttonDeleteCurrentImage.setOnClickListener {
        //     deleteCurrentImageFlag = true
        //     selectedImageUri = null // Anular cualquier selección nueva
        //     binding.imageViewInstrumentIconEdit.setImageResource(R.drawable.ic_default_instrument)
        //     Toast.makeText(this, "La imagen actual se eliminará al guardar.", Toast.LENGTH_SHORT).show()
        // }

        binding.buttonUpdateInstrument.setOnClickListener {
            val name = binding.editTextInstrumentName.text.toString().trim()
            val freqStr = binding.editTextInstrumentFrequency.text.toString().trim()
            val info = binding.editTextInstrumentInfo.text.toString().trim()

            if (name.isEmpty()) {
                binding.textInputLayoutInstrumentName.error = "El nombre es obligatorio"
                return@setOnClickListener
            } else {
                binding.textInputLayoutInstrumentName.error = null
            }

            val frequency = freqStr.toDoubleOrNull()

            viewModel.updateInstrument(
                instrumentId = currentInstrumentId,
                userId = currentOwnerUserId,
                name = name,
                frequency = frequency,
                info = info,
                newImageUri = selectedImageUri,
                deleteCurrentImage = deleteCurrentImageFlag
            )
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "isLoading (EditInstrument): $isLoading")
            binding.buttonUpdateInstrument.isEnabled = !isLoading
            binding.buttonSelectInstrumentIconEdit.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.instrumentToEdit.observe(this) { instrument ->
            if (instrument != null) {
                binding.editTextInstrumentName.setText(instrument.name)
                binding.editTextInstrumentFrequency.setText(instrument.frequency?.takeIf { it > 0.0 }?.toString() ?: "")
                binding.editTextInstrumentInfo.setText(instrument.information ?: "")
                existingPhotoPath = instrument.photoPath //guardar path actual de la foto
                selectedImageUri = null //reseteamos la selección de imagen
                deleteCurrentImageFlag = false //reseteamos el flag de eliminación

                Glide.with(this)
                    .load(instrument.photoPath)
                    .placeholder(R.drawable.ic_default_instrument)
                    .error(R.drawable.ic_default_instrument)
                    .circleCrop()
                    .into(binding.imageViewInstrumentIconEdit)
            } else {
                Toast.makeText(this, "No se pudieron cargar los datos del instrumento.", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        viewModel.updateResult.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { success ->
                if (success) {
                    Toast.makeText(this, "Instrumento actualizado con éxito.", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK) //indicamos cambios en la activity para refrescar la lista
                    finish()
                } else {
                    Toast.makeText(this, "No se pudo actualizar el instrumento.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}