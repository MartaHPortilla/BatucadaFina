package com.martahp.batucadafina.ui.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.martahp.batucadafina.R
import com.martahp.batucadafina.databinding.ActivitySpecificTunerBinding
import com.martahp.batucadafina.services.audio.AudioProcessor
import com.martahp.batucadafina.services.audio.AudioRecorder
import com.martahp.batucadafina.ui.viewmodel.SpecificTunerViewModel
import com.martahp.batucadafina.ui.viewmodel.TuningDirection
import com.martahp.batucadafina.utils.AudioPermissionManager
import java.util.Locale

class SpecificTunerActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpecificTunerBinding
    private val viewModel: SpecificTunerViewModel by viewModels()

    private lateinit var audioRecorder: AudioRecorder
    private val audioProcessor = AudioProcessor()
    private lateinit var audioPermissionManager: AudioPermissionManager

    private var isCurrentlyRecording = false
    private val TAG = "SpecificTunerActivity"

    //variables específicas para esta activity
    private var targetNoteName: String? = null
    private var targetFrequency: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpecificTunerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetNoteName = intent.getStringExtra(EXTRA_NOTE_NAME) ?: "-"
        val targetFrequency = intent.getDoubleExtra(EXTRA_FREQUENCY, 0.0)

        // Comprobación de seguridad
        if (targetNoteName == null || targetFrequency == 0.0) {
            Toast.makeText(this, "Error: No se ha especificado un instrumento para afinar.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        //inicializamos el ViewModel con la nota y frecuencia objetivo
        viewModel.setTargetFrequency(targetNoteName!!, targetFrequency)

        //configuramos el AudioRecorder y procesador de audio
        audioRecorder = AudioRecorder(audioProcessor, { detectedFrequency ->
            viewModel.processNewFrequency(detectedFrequency)
        }, this)

        audioPermissionManager = AudioPermissionManager(this)
        configureAudioPermissionLauncher()

        //llamamos a las funciones de configuración, en este caso pasamos las variables para la toolbar
        configureToolbar(targetNoteName!!, targetFrequency)
        configureListeners()
        observeViewModel()
        configureNeedlePivot()

        // 5. Pedir permiso al iniciar
        audioPermissionManager.askForPermission()
    }

    private fun configureNeedlePivot() {
        binding.needleImageView.post {
            val viewWidth = binding.needleImageView.width
            val viewHeight = binding.needleImageView.height

            if (viewWidth > 0 && viewHeight > 0) {
                binding.needleImageView.pivotX = viewWidth / 2f
                binding.needleImageView.pivotY = viewHeight.toFloat()
                Log.d(TAG, "configureNeedlePivot(): Pivot configurado a X=${binding.needleImageView.pivotX}, Y=${binding.needleImageView.pivotY}")
            } else {
                Log.w(TAG, "configureNeedlePivot(): No se pudieron obtener dimensiones para needleImageView.")
            }
        }
    }

    private fun configureAudioPermissionLauncher() {
        audioPermissionManager.setupPermissionLauncher { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Permiso de audio concedido.")
                binding.recordButton.isEnabled = true
                binding.statusTextView.text = "Status: Listo para afinar"
            } else {
                Log.d(TAG, "Permiso de audio denegado.")
                binding.recordButton.isEnabled = false
                binding.statusTextView.text = "Status: Permiso denegado"
                Snackbar.make(binding.root, "El permiso de audio es necesario para usar el afinador.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun configureToolbar(name: String, frequency: Double) {
        setSupportActionBar(binding.toolbarSpecificTuner)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Afinar: %s - %.2fHz".format(name, frequency)
        binding.toolbarSpecificTuner.setNavigationOnClickListener {
            finish()
        }
    }

    private fun configureListeners() {
        binding.recordButton.setOnClickListener {
            toggleRecording()
        }
    }

    private fun observeViewModel() {

        //observador nota objetivo
        viewModel.noteName.observe(this) { targetNote ->
            binding.targetNoteTextView.text = "Objetivo: $targetNote"
        }

        //observador para la nota DETECTADA
        viewModel.detectedNoteName.observe(this) { detectedNote ->
            binding.detectedNoteTextView.text = detectedNote
        }
        //observador para los cents
        viewModel.centsDeviation.observe(this) { cents ->
            binding.centsTextView.text = String.format(Locale.US, "%.1f cents", cents)
            binding.needleImageView.rotation = (cents * 0.9f).coerceIn(-45.0, 45.0).toFloat()
        }

        // Observador para los indicadores visuales
        viewModel.tuningDirection.observe(this) { direction ->
            when (direction) {
                TuningDirection.TUNE_UP -> {
                    val color = ContextCompat.getColor(this, R.color.tuner_yellow_indicator)

                    binding.dialScaleImageView.setImageResource(R.drawable.dial_scale_yellow)
                    binding.tuningIndicatorTextView.visibility = View.VISIBLE
                    binding.tuningIndicatorTextView.text = "APRETAR ▲"

                    binding.tuningIndicatorTextView.setTextColor(color)
                    binding.detectedNoteTextView.setTextColor(color)

                }
                TuningDirection.TUNE_DOWN -> {
                    val color = ContextCompat.getColor(this, R.color.tuner_red_indicator)

                    binding.dialScaleImageView.setImageResource(R.drawable.dial_scale_red)
                    binding.tuningIndicatorTextView.visibility = View.VISIBLE
                    binding.tuningIndicatorTextView.text = "AFLOJAR ▼"

                    binding.tuningIndicatorTextView.setTextColor(color)
                    binding.detectedNoteTextView.setTextColor(color)
                }
                TuningDirection.IN_TUNE -> {
                    val color = ContextCompat.getColor(this, R.color.tuner_green_indicator)

                    binding.dialScaleImageView.setImageResource(R.drawable.dial_scale_green)
                    binding.tuningIndicatorTextView.visibility = View.VISIBLE
                    binding.tuningIndicatorTextView.text = "¡AFINADO!"

                    binding.tuningIndicatorTextView.setTextColor(color)
                    binding.detectedNoteTextView.setTextColor(color)
                }
                TuningDirection.IDLE -> {
                    binding.dialScaleImageView.setImageResource(R.drawable.last_dial_scale_allcolor)
                    binding.tuningIndicatorTextView.visibility = View.INVISIBLE
                    binding.detectedNoteTextView.text = ""
                }
            }
        }

        // Se mantiene el statusTextView por consistencia, aunque el indicador principal ahora es más visual
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
            viewModel.updateStatus("Afinando...")
            audioRecorder.startRecording()
            binding.recordButton.text = "Detener Afinación"
            isCurrentlyRecording = true
            Log.d(TAG, "startRecording() llamado.")
        }
    }

    private fun stopRecording() {
        if (isCurrentlyRecording) {
            audioRecorder.stopRecording()
            viewModel.resetDisplayData()
            viewModel.updateStatus("Listo para afinar")
            binding.recordButton.text = "Iniciar Afinación"
            isCurrentlyRecording = false
            Log.d(TAG, "stopRecording() llamado.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy(): Deteniendo grabación si está activa.")
        if (::audioRecorder.isInitialized && isCurrentlyRecording) {
            audioRecorder.stopRecording()
        }
    }

    // Companion object para las claves de los extras del Intent
    companion object {
        const val EXTRA_NOTE_NAME = "EXTRA_NOTE_NAME"
        const val EXTRA_FREQUENCY = "EXTRA_FREQUENCY"
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 201
    }
}