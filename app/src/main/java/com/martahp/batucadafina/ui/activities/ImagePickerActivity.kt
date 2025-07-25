package com.martahp.batucadafina.ui.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider //para la URI de la imagen
import com.bumptech.glide.Glide
import com.martahp.batucadafina.R
import com.martahp.batucadafina.databinding.ActivityImagePickerBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImagePickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePickerBinding
    private val TAG = "ImagePickerActivity"

    private var selectedImageUri: Uri? =
        null //creamos una variable para guardar la URI de la imagen seleccionada/tomada
    private var currentPhotoUriByCamera: Uri? =
        null //para guardar la URI de la foto tomada por la cámara

    //Lanzamos los ActivityResultLauncher para manejar los resultados de galería y cámara

    //seleccionar desde la galería
    private val galleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    Log.d(TAG, "Imagen de galería seleccionada: $uri")
                    selectedImageUri = uri // Guardar la URI seleccionada
                    showPreview(uri)
                    binding.buttonConfirmSelection.isEnabled = true // Habilitar botón Aceptar
                }
            } else {
                Log.d(TAG, "Selección de galería cancelada o fallida.")
            }
        }

    //tomar foto con la cámara
    private val cameraLauncher: ActivityResultLauncher<Uri> =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoUriByCamera?.let { uri ->
                    Log.d(TAG, "Foto tomada y guardada en: $uri")
                    selectedImageUri = uri // Guardar la URI de la foto tomada
                    showPreview(uri)
                    binding.buttonConfirmSelection.isEnabled = true // Habilitar botón Aceptar
                }
            } else {
                Log.d(TAG, "Captura de foto cancelada o fallida.")
                currentPhotoUriByCamera = null // Limpiar si falló
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureToolbar()
        configureListeners()
    }

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbarImagePicker)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbarImagePicker.setNavigationOnClickListener {
            setResult(Activity.RESULT_CANCELED) // Devolvemos cancelado si se pulsa atrás
            finish()
        }
    }

    private fun configureListeners() {
        binding.buttonOpenGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*" //solo imágenes
            }
            // Verificar si hay una app para manejar el intent
            if (intent.resolveActivity(packageManager) != null) {
                galleryLauncher.launch(intent)
            } else {
                Toast.makeText(this, "No se encontró aplicación de galería.", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.buttonTakePhoto.setOnClickListener {
            currentPhotoUriByCamera = createImageUri() //URI para guardar la foto
            val uriToLaunch = currentPhotoUriByCamera //creamos variable no mutable local para guardar la uri y asegurarnos bien de que no va a ser nula
            if (uriToLaunch != null) {
                Log.d(TAG, "Listener de cámara: Intentando tomar foto en: $uriToLaunch")
                cameraLauncher.launch(uriToLaunch) //aqui la variable uriToLaunch no es nula
            } else { //si createImageUri devuelve null, no se pudo crear la URI
                Toast.makeText(
                    this,
                    "Error al crear el espacio de almacenamiento.",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "Listener de cámara: No se pudo crear URI para guardar la foto.")
            }
        }

        binding.buttonConfirmSelection.setOnClickListener {
            selectedImageUri?.let { uri ->
                val resultIntent = Intent()
                resultIntent.data = uri //poner la URI en el intent de resultado
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } ?: run {
                //esto no debería pasar si el botón solo se habilita cuando hay una imagen
                Toast.makeText(this, "No se ha seleccionado ninguna imagen.", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.buttonCancelSelection.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    /**
     * Muestra la imagen seleccionada en el ImageView de previsualización.
     */
    private fun showPreview(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_error)
            .into(binding.imageViewPreview)
    }

    /**
     * Crea una URI temporal para guardar la imagen tomada por la cámara.
     * La imagen se guarda en el directorio caché de la app.
     */
    private fun createImageUri(): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir: File? = getExternalFilesDir("camera_cache")

        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }

        return try {
            val imageFile = File.createTempFile(
                imageFileName,  //prefijo
                ".jpg", //sufijo
                storageDir    //directorio
            )
            FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                imageFile
            )
        } catch (ex: IOException) {
            Log.e(TAG, "Error creando archivo de imagen temporal", ex)
            Toast.makeText(this, "Error preparando la cámara", Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }
}



