package com.martahp.batucadafina.ui.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.martahp.batucadafina.databinding.ActivityBasicTunerBinding
import com.martahp.batucadafina.services.audio.AudioProcessor
import com.martahp.batucadafina.services.audio.AudioRecorder
import com.martahp.batucadafina.ui.viewmodel.BasicTunerViewModel
import com.martahp.batucadafina.utils.AudioPermissionManager

class BasicTunerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBasicTunerBinding
    private val viewModel: BasicTunerViewModel by viewModels()

    private lateinit var audioRecorder: AudioRecorder
    private val audioProcessor = AudioProcessor()
    private lateinit var audioPermissionManager: AudioPermissionManager

    private var isCurrentlyRecording = false
    private val TAG = "BasicTunerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBasicTunerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioRecorder = AudioRecorder(
            audioProcessor,
            { detectedFrequency ->
                //llamada al ViewModel para procesar la nueva frecuencia
                viewModel.processNewFrequency(detectedFrequency)
            },
            this
        )

        audioPermissionManager = AudioPermissionManager(this)
        configureAudioPermissionLauncher() //función que configura el launcher de permisos

        configureToolbar()
        configureListeners()
        observeViewModel()
        configureNeedlePivot()

        //pedimos permiso al iniciar (si no se ha concedido)
        audioPermissionManager.askForPermission()
    }

    private fun configureNeedlePivot() {
        binding.needleImageView.post {
            val viewWidth = binding.needleImageView.width
            val viewHeight = binding.needleImageView.height

            if (viewWidth > 0 && viewHeight > 0) {
                //hemos configurado los drawables para que el pivote esté en el centro
                binding.needleImageView.pivotX = viewWidth / 2f //esto es para que el pivote esté en el centro
                binding.needleImageView.pivotY = viewHeight.toFloat() //esto es para que el pivote esté en la parte inferior
                Log.d(TAG, "configureNeedlePivot(): Pivot configurado a X=${binding.needleImageView.pivotX}, Y=${binding.needleImageView.pivotY} (width=$viewWidth, height=$viewHeight)")
            } else {
                Log.w(TAG, "configureNeedlePivot(): No se pudieron obtener dimensiones válidas para needleImageView (width=$viewWidth, height=$viewHeight). El pivote podría no estar bien configurado.")
            }
        }
    }

    private fun configureAudioPermissionLauncher() {
        audioPermissionManager.setupPermissionLauncher { isGranted: Boolean ->
            if (isGranted) {
                viewModel.updateStatus("Permiso concedido. Listo.")
                Log.d(TAG, "Permiso de audio concedido.")
                binding.recordButton.isEnabled = true
            } else {
                Log.d(TAG, "Permiso de audio denegado por el usuario")
                viewModel.updateStatus("Permiso denegado. Grabación deshabilitada.")
                binding.recordButton.isEnabled = false
                Snackbar.make(
                    binding.root, //binding.root para encontrar el layout principal
                    "Permiso de audio denegado. La grabación no está disponible.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbarBasicTuner)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Afinador Básico"
        binding.toolbarBasicTuner.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun configureListeners() {
        binding.recordButton.setOnClickListener {
            viewModel.updateStatus("Botón presionado") //el vm actualizará el estado en start/stop
            toggleRecording()
        }
    }

    private fun observeViewModel() {
        viewModel.noteName.observe(this) { note ->
            binding.noteTextView.text =
                "Nota: $note" //recordemos que el ViewModel da el String formateado
        }
        viewModel.detectedFrequencyString.observe(this) { freqString ->
            binding.frequencyTextView.text = freqString
        }
        viewModel.rawFrequencyString.observe(this) { rawFreqString ->
            if (rawFreqString.isNullOrEmpty()) {
                binding.rawFrequencyTextView.visibility = View.GONE
            } else {
                binding.rawFrequencyTextView.visibility = View.VISIBLE
                binding.rawFrequencyTextView.text = rawFreqString
            }
        }
        viewModel.centsDeviationString.observe(this) { centsString ->
            binding.centsTextView.text = centsString
        }
        viewModel.needleRotation.observe(this) { rotationAngle ->
            binding.needleImageView.rotation = rotationAngle
        }
        viewModel.statusMessage.observe(this) { status ->
            binding.statusTextView.text = "Status: $status"
        }
    }

    private fun toggleRecording() {
        if (!isCurrentlyRecording) {
            audioPermissionManager.askForPermission() //si no tiene permiso, el callback de askForPermission manejará el no poder grabar
            if (binding.recordButton.isEnabled) { //solo si el botón está habilitado (lo que implica permiso)
                startRecording()
            }
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        if (!isCurrentlyRecording) {
            viewModel.updateStatus("Iniciando grabación...")
            audioRecorder.startRecording()
            viewModel.updateStatus("Grabando...")
            binding.recordButton.text = "Detener Grabación"
            isCurrentlyRecording = true
            Log.d(TAG, "startRecording() llamado.")
        }
    }

    private fun stopRecording() {
        if (isCurrentlyRecording) {
            viewModel.updateStatus("Deteniendo grabación...")
            audioRecorder.stopRecording()
            viewModel.resetDisplayData()
            viewModel.updateStatus("Grabación detenida. Listo.")
            binding.recordButton.text = "Iniciar Grabación"
            isCurrentlyRecording = false
            Log.d(TAG, "stopRecording() llamado.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy(): Deteniendo grabación si está activa.")
        audioRecorder.stopRecording() //importante liberar recursos de AudioRecorder
    }
}