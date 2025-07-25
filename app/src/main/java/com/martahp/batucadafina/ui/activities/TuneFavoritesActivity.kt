package com.martahp.batucadafina.ui.activities

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.martahp.batucadafina.databinding.ActivityTuneFavoritesBinding
import com.martahp.batucadafina.ui.adapter.DisplayableItemSealed
import com.martahp.batucadafina.ui.adapter.InstrumentsAdapter
import com.martahp.batucadafina.ui.viewmodel.TuneFavoritesViewModel
import com.martahp.batucadafina.model.entities.CreatedInstrument
import androidx.appcompat.app.AlertDialog
import com.martahp.batucadafina.databinding.DialogDeleteInstrumentBinding
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.martahp.batucadafina.databinding.DialogInstrumentDetailsBinding
import com.martahp.batucadafina.databinding.DialogInstrumentsOptionsBinding
import java.util.Locale

class TuneFavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTuneFavoritesBinding
    private val viewModel: TuneFavoritesViewModel by viewModels()
    private lateinit var adapter: InstrumentsAdapter
    private val TAG = "TuneFavoritesActivity"
    private var currentUserId: Long = -1L

    companion object {
        const val EXTRA_USER_ID = "com.martahp.batucadafina.USER_ID_TUNE_FAV"
    }

    //ActivityResultLauncher para manejar el resultado de EditInstrumentActivity
    private val editInstrumentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "Resultado recibido de EditInstrumentActivity, resultCode: ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "EditInstrumentActivity devolvió RESULT_OK. Recargando instrumentos favoritos.")
                if (currentUserId != -1L) {
                    viewModel.loadFavoriteInstruments(currentUserId) // Recargar favoritos
                }
            } else {
                Log.d(TAG, "EditInstrumentActivity cerrada o sin cambios guardados.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTuneFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Recuperar el userId del Intent
        currentUserId = intent.getLongExtra(EXTRA_USER_ID, -1L)

        // 2. Comprobar si el userId es válido
        if (currentUserId == -1L) {
            Log.e(TAG, "No se ha recibido un ID de usuario válido.")
            Toast.makeText(this, "Error: No se pudo identificar al usuario.", Toast.LENGTH_LONG)
                .show()
            finish() // Cerrar esta activity si no hay userId
            return   // Salir de onCreate
        }

        // 3. Configurar UI
        configureToolbar()
        configureRecyclerView()
        configureListeners()
        observeViewModel()

        // 4. Pedir al ViewModel que cargue los instrumentos FAVORITOS
        Log.d(TAG, "Solicitando carga de instrumentos FAVORITOS para userId: $currentUserId")
        viewModel.loadFavoriteInstruments(currentUserId) // Llamar a la función específica de este ViewModel
    }

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbarTuneFavorites)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) //para mostrar la flecha
        supportActionBar?.setDisplayShowHomeEnabled(true) //para hacer la flecha clicable
        //configuramos el listener de la flecha
        binding.toolbarTuneFavorites.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun configureRecyclerView() {
        adapter = InstrumentsAdapter(
            onItemClick = { displayableItemClicked ->
                showInstrumentOptionsDialog(displayableItemClicked)
            },
            onFavoriteClick = { displayableItemClicked ->
                //lógica que hace que se cambie el estado de favorito, desapareciendo el item de la lista automáticamente
                if (currentUserId != -1L) {
                    viewModel.toggleFavoriteStatus(displayableItemClicked, currentUserId)
                } else {
                    Log.e(TAG, "UserID es inválido al intentar cambiar favorito.")
                    Toast.makeText(this, "Error al procesar favorito.", Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.recyclerViewInstruments.apply {
            layoutManager = LinearLayoutManager(this@TuneFavoritesActivity)
            this.adapter = this@TuneFavoritesActivity.adapter
        }
        Log.d(TAG, "RecyclerView de Favoritos configurado.")
    }

    private fun configureListeners() {
        //necesario?
    }

    private fun observeViewModel() {
        // Observar la lista combinada de DisplayableItemSealed de favoritos
        viewModel.displayableItems.observe(this) { favoriteItemsList ->
            Log.d(
                TAG,
                "Lista de favoritos (DisplayableItems) recibida: ${favoriteItemsList.size} items"
            )
            // Pasa la lista directamente al adapter
            adapter.submitList(favoriteItemsList)
        }

        // --- observar instrumentDeletedResult ---
        viewModel.instrumentDeletedResult.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { (success, message) ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                if (!success) {
                    Log.e(TAG, "Falló la eliminación del instrumento en TuneFavorites: $message")
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "Estado de carga (Favoritos): $isLoading")
        }

        viewModel.errorMessage.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { message ->
                Log.e(TAG, "Error del TuneFavoritesViewModel: $message")
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    //añadimos funcion para mostrar el dialogo de opciones personalizado
    private fun showInstrumentOptionsDialog(item: DisplayableItemSealed) {
        //inflamos el layout
        val dialogBinding = DialogInstrumentsOptionsBinding.inflate(LayoutInflater.from(this))
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        val instrumentNameForTitle: String
        val instrumentInfo: String
        val instrumentFrequency: Double?

        when (item) {
            is DisplayableItemSealed.PredefinedInstrumentItem -> {
                instrumentNameForTitle = item.instrument.name
                instrumentInfo = item.instrument.information ?: "No hay información disponible."
                instrumentFrequency = item.instrument.frequency
            }
            is DisplayableItemSealed.CreatedInstrumentItem -> {
                instrumentNameForTitle = item.instrument.name
                instrumentInfo = item.instrument.information ?: "No hay información disponible."
                instrumentFrequency = item.instrument.frequency
            }
            is DisplayableItemSealed.HeaderItem -> {
                Log.d(TAG, "No se muestran opciones para HeaderItem.")
                return
            }
        }
        dialogBinding.dialogInstrumentOptionsTitle.text = instrumentNameForTitle

        //detalles siempre para todos los instrumentos
        dialogBinding.optionDetails.visibility = View.VISIBLE
        dialogBinding.optionDetails.setOnClickListener {
            Log.d(TAG, "Opción Menú: Detalles - $instrumentNameForTitle")
            showInstrumentDetailsDialog(instrumentNameForTitle, instrumentFrequency, instrumentInfo)
            alertDialog.dismiss()
        }

        //afinar siempre visible en esta activity
        dialogBinding.optionTune.visibility = View.VISIBLE
        dialogBinding.optionTune.setOnClickListener {
            Log.d(TAG, "Opción Menú: Afinar - $instrumentNameForTitle")
            handleTuneAction(item) //llama a la función para manejar el click e ir a SpecificTunerActivity
            alertDialog.dismiss()
        }

        //modificar y eliminar solo para instrumentos creados
        if (item is DisplayableItemSealed.CreatedInstrumentItem) {
            val createdInstrument = item.instrument

            dialogBinding.optionModify.visibility = View.VISIBLE //modificar
            dialogBinding.optionModify.setOnClickListener {
                Log.d(TAG, "Opción Menú: Modificar - ${createdInstrument.name}")
                val intent = Intent(this, EditInstrumentActivity::class.java)
                intent.putExtra(EditInstrumentActivity.EXTRA_USER_ID, createdInstrument.id)
                intent.putExtra(EditInstrumentActivity.EXTRA_INSTRUMENT_ID, currentUserId)
                editInstrumentLauncher.launch(intent)
                alertDialog.dismiss()
            }

            dialogBinding.optionDelete.visibility = View.VISIBLE //eliminar
            dialogBinding.optionDelete.setOnClickListener {
                Log.d(TAG, "Opción Menú: Eliminar - ${createdInstrument.name}")
                showDeleteInstrumentDialog(createdInstrument)
                alertDialog.dismiss() // Cerrar el diálogo de opciones
            }
        } else {
            dialogBinding.optionModify.visibility = View.GONE
            dialogBinding.optionDelete.visibility = View.GONE
        }

        //cancelar
        dialogBinding.buttonDialogOptionsCancel.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    //diálogo de confirmación para eliminar instrumento favorito
    private fun showDeleteInstrumentDialog(instrument: CreatedInstrument) {
        if (currentUserId == -1L) {
            Toast.makeText(
                this,
                "Error de usuario, no se puede procesar la acción.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialogBinding = DialogDeleteInstrumentBinding.inflate(LayoutInflater.from(this))

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.buttonDialogCancelInstrument.setOnClickListener {
            alertDialog.dismiss()
        }
        dialogBinding.buttonDialogDeleteInstrument.setOnClickListener {
            Log.d(TAG, "Confirmada eliminación para: ${instrument.name}")
            viewModel.deleteCreatedInstrument(instrument, currentUserId)
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    //función para manejar el click en el botón "Afinar"
    private fun handleTuneAction(item: DisplayableItemSealed) {
        var name: String? = null
        var frequency: Double? = null

        when (item) {
            is DisplayableItemSealed.PredefinedInstrumentItem -> {
                name = item.instrument.name
                frequency = item.instrument.frequency
            }

            is DisplayableItemSealed.CreatedInstrumentItem -> {
                name = item.instrument.name
                frequency = item.instrument.frequency
            }

            else -> {
                Log.w(TAG, "handleTuneAction llamado con un tipo de item no afinable.")
                return
            }
        }

        if (name != null && frequency != null && frequency > 0) {
             val intent = Intent(this, SpecificTunerActivity::class.java)
            intent.putExtra(SpecificTunerActivity.EXTRA_NOTE_NAME, name)
            intent.putExtra(SpecificTunerActivity.EXTRA_FREQUENCY, frequency)
             startActivity(intent)
        } else {
            Log.w(TAG, "handleTuneAction: name o frequency son nulos.")
            Toast.makeText(this, "No se pueden obtener los datos para afinar este instrumento.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInstrumentDetailsDialog(
        instrumentName: String,
        frequency: Double?,
        instrumentInfo: String
    ) {
        //inflamos el layout
        val dialogBinding = DialogInstrumentDetailsBinding.inflate(LayoutInflater.from(this))

        //preparamos el contenido del diálogo con los datos del instrumento
        val frequencyText =
            frequency?.let { String.format(Locale.US, "%.2f Hz", it) } ?: "No especificada"
        val detailsInstrumentToFullText = """
        Nombre: $instrumentName
        Frecuencia: $frequencyText
        Información adicional: $instrumentInfo
    """.trimIndent() //trimIndent() para eliminar espacios al principio y al final

        //preparamos el contenido del diálogo con los datos del instrumento para mostrarlos en los textview
        dialogBinding.dialogTitleInstrumentDetails.text = "Detalles"
        dialogBinding.dialogTextInstrumentName.text = instrumentName
        dialogBinding.dialogTextInstrumentInfo.text = instrumentInfo
        dialogBinding.dialogTextInstrumentFrequency.text = frequencyText
        //ocultamos frecuencia si no está disponible
        if (frequency == null) {
            dialogBinding.layoutInstrumentFrequencyDetails.visibility = View.GONE
        } else {
            dialogBinding.layoutInstrumentFrequencyDetails.visibility = View.VISIBLE
        }
        //creamos el diálogo
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true) // Permitir cerrar tocando fuera o con botón atrás
            .create()

        // 4. Configurar listeners para los botones/textos DENTRO del layout del diálogo
        dialogBinding.textViewCopyInfo.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("InstrumentInfo", detailsInstrumentToFullText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Información copiada al portapapeles", Toast.LENGTH_SHORT).show()
        }

        dialogBinding.buttonDialogDetailsClose.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.show()

    }

}