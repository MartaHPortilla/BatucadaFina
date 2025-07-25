package com.martahp.batucadafina.ui.activities

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.martahp.batucadafina.databinding.ActivityInstrumentsBinding
import com.martahp.batucadafina.model.entities.CreatedInstrument
import com.martahp.batucadafina.model.entities.PredefinedInstrument
import com.martahp.batucadafina.ui.adapter.DisplayableItemSealed
import com.martahp.batucadafina.ui.adapter.InstrumentsAdapter
import com.martahp.batucadafina.ui.viewmodel.InstrumentsViewModel
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.martahp.batucadafina.databinding.DialogDeleteInstrumentBinding
import com.martahp.batucadafina.databinding.DialogInstrumentDetailsBinding
import com.martahp.batucadafina.databinding.DialogInstrumentsOptionsBinding
import java.util.Locale

class InstrumentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstrumentsBinding
    private val viewModel: InstrumentsViewModel by viewModels()
    private lateinit var adapter: InstrumentsAdapter
    private val TAG = "InstrumentsActivity"

    // añadimos variable miembro para guardar el id del usuario
    private var currentUserId: Long = -1L

    companion object {
        const val EXTRA_INSTRUMENT_ID = "com.martahp.batucadafina.INSTRUMENT_ID"
        const val EXTRA_USER_ID = "com.martahp.batucadafina.USER_ID"
    }

    //ActivityResultLauncher para manejar el resultado de EditInstrumentActivity
    private val editInstrumentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "Resultado recibido de EditInstrumentActivity, resultCode: ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                // El instrumento fue modificado, recargamos la lista completa
                Log.d(TAG, "EditInstrumentActivity devolvió RESULT_OK. Recargando instrumentos.")
                if (currentUserId != -1L) {
                    viewModel.loadInstruments(currentUserId)
                }
            } else {
                Log.d(TAG, "EditInstrumentActivity cerrada o sin cambios guardados.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstrumentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // recuperamos el user ID del intent y lo guardamos en la variable miembro
        currentUserId = intent.getLongExtra(EXTRA_USER_ID, -1L)
        // Nos aseguramos de que el ID es válido
        if (currentUserId == -1L) {
            Log.e(TAG, "No se ha recibido un ID de usuario válido.")
            Toast.makeText(this, "No se ha podido identificar al usuario.", Toast.LENGTH_LONG)
                .show()
            finish()
            return
        }

        //configuración del recycler, listeners y observadores
        configureToolbar()
        configureRecyclerView() // primero configuramos el RecyclerView y el Adapter
        configureListeners()
        configureObservers() // luego configuramos los observadores

        // Pedir al ViewModel que cargue los instrumentos para el userId obtenido
        Log.d(TAG, "Solicitando carga de instrumentos para userId: $currentUserId")
        viewModel.loadInstruments(currentUserId)

    }


    // funciones de configuración

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbarInstruments)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) //para mostrar la flecha
        supportActionBar?.setDisplayShowHomeEnabled(true) //para hacer la flecha clicable
        //configuramos el listener de la flecha
        binding.toolbarInstruments.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }


    private fun configureRecyclerView() {
        // creamos y configuramos el adaptador
        adapter = InstrumentsAdapter(
            onItemClick = { displayableItemClicked ->
                showInstrumentOptionsDialog(displayableItemClicked)
            },
            onFavoriteClick = { displayableItemClicked ->
                if (currentUserId != -1L) {
                    viewModel.toggleFavoriteStatus(displayableItemClicked, currentUserId)
                } else {
                    Log.e(
                        TAG,
                        "No se ha recibido un ID de usuario válido para realizar el toggle de favorito."
                    )
                    Toast.makeText(
                        this,
                        "No se ha podido identificar al usuario al modificar favoritos.",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
        )

        binding.recyclerViewInstruments.apply {
            layoutManager = LinearLayoutManager(this@InstrumentsActivity)
            this.adapter = this@InstrumentsActivity.adapter // asignamos el adapter solo una vez
        }
        Log.d(TAG, "RecyclerView configurado (LayoutManager asignado, Adapter asignado).")
    }

    private fun showInstrumentOptionsDialog(item: DisplayableItemSealed) {
        //inflamos el layout
        val dialogBinding = DialogInstrumentsOptionsBinding.inflate(LayoutInflater.from(this))
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true) // Permitir cerrar con botón atrás o tocando fuera
            .create()

        //declaramos variables para nombre, info y frecuencia
        val instrumentNameForTitle: String
        val instrumentInfo: String
        val instrumentFrequency: Double?

        //seteamos los datos del instrumento
        when (item) {
            is DisplayableItemSealed.PredefinedInstrumentItem -> {
                instrumentNameForTitle = item.instrument.name
                instrumentInfo = item.instrument.information ?: "No hay información disponible."
                instrumentFrequency = item.instrument.frequency
            }
            is DisplayableItemSealed.CreatedInstrumentItem -> {
                instrumentNameForTitle = item.instrument.name
                instrumentInfo = item.instrument.information ?: "No hay información disponible."
                instrumentFrequency = item.instrument.frequency // Asumiendo que CreatedInstrument también tiene frecuencia
            }
            is DisplayableItemSealed.HeaderItem -> {
                Log.d(TAG, "No se muestran opciones para HeaderItem.")
                return
            }
        }
        //titulo del diálogo
        dialogBinding.dialogInstrumentOptionsTitle.text = instrumentNameForTitle


        //opción detalles para todos los instrumentos
        dialogBinding.optionDetails.visibility = View.VISIBLE
        dialogBinding.optionDetails.setOnClickListener {
            Log.d(TAG, "Opción Menú: Detalles - $instrumentNameForTitle")
            showInstrumentDetailsDialog(instrumentNameForTitle, instrumentFrequency, instrumentInfo) //llamamos al diálogo con nombre, frecuencia e info como parámetros
            alertDialog.dismiss()
        }
        //modificar y eliminar solo para instrumentos creados
        if (item is DisplayableItemSealed.CreatedInstrumentItem) {
            val createdInstrument = item.instrument

            dialogBinding.optionModify.visibility = View.VISIBLE
            dialogBinding.optionModify.setOnClickListener {
                Log.d(TAG, "Opción Menú: Modificar - ${createdInstrument.name}")
                //navegamos a la pantalla de edición
                val intent =
                    Intent(this, EditInstrumentActivity::class.java)
                intent.putExtra(EditInstrumentActivity.EXTRA_INSTRUMENT_ID, createdInstrument.id)
                intent.putExtra(EditInstrumentActivity.EXTRA_USER_ID, currentUserId)
                editInstrumentLauncher.launch(intent) //para lanzar la activity
                alertDialog.dismiss()
            }

            // Eliminar
            dialogBinding.optionDelete.visibility = View.VISIBLE
            dialogBinding.optionDelete.setOnClickListener {
                Log.d(TAG, "Opción Menú: Eliminar - ${createdInstrument.name}")
                showDeleteInstrumentDialog(createdInstrument)
                alertDialog.dismiss()
            }
        } else {
            //si no es creado, ocultamos los botones de modificar y eliminar
            dialogBinding.optionModify.visibility = View.GONE
            dialogBinding.optionDelete.visibility = View.GONE
        }
        //tampoco necesitamos el botón de afinar en esta activity
        dialogBinding.optionTune.visibility = View.GONE

        //cancelar
        dialogBinding.buttonDialogOptionsCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
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

        // 4. Configurar listeners para los botones/textos DENTRO de tu layout
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
            .setCancelable(true)
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

    private fun configureListeners() {

        //TODO: Configuración de listeners?
    }

    private fun configureObservers() {
        // Observar la lista de instrumentos y headers combinada
        viewModel.displayableItems.observe(this) { displayableList ->
            Log.d(TAG, "Lista de displayable items recibida. Tamaño: ${displayableList.size}")
            adapter.submitList(displayableList) // pasamos la lista al adapter directamente
            displayableList.forEach { item ->
                when (item) {
                    is DisplayableItemSealed.HeaderItem -> Log.d(TAG, "  -> Header: ${item.title}")
                    is DisplayableItemSealed.PredefinedInstrumentItem -> Log.d(
                        TAG,
                        "  -> Predefinido: ${item.instrument.name}, Fav: ${item.isFavorite}"
                    )

                    is DisplayableItemSealed.CreatedInstrumentItem -> Log.d(
                        TAG,
                        "  -> Creado: ${item.instrument.name}, Fav: ${item.isFavorite}"
                    )
                }
            }
        }

        // Observar el resultado de la eliminación de un instrumento
        viewModel.instrumentDeletedResult.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { (success, message) ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                if (!success) {
                    Log.e(TAG, "Falló la eliminación del instrumento: $message")
                }
            }
        }

        //añadimos en este caso observadores de errores y carga
        viewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "Cargando: $isLoading")
        }

        viewModel.errorMessage.observe(this) { event ->
            event.getContentIfNotProcessed()?.let { message ->
                Log.e(TAG, "Error recibido en el viewmodel: $message")
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }


    }
}
