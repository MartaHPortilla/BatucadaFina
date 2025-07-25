package com.martahp.batucadafina.ui.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.martahp.batucadafina.R
import com.martahp.batucadafina.databinding.ListItemHeaderBinding
import com.martahp.batucadafina.databinding.ListItemInstrumentBinding
import com.martahp.batucadafina.model.entities.CreatedInstrument
import com.martahp.batucadafina.databinding.DialogDeleteInstrumentBinding



// Constantes para los tipos de vista. Aunque usamos el mismo layout para predefinidos y creados, deben diferenciarse
private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_PREDEFINED = 1
private const val VIEW_TYPE_CREATED = 2

class InstrumentsAdapter (
    // Lambdas para manejar clicks
    private val onItemClick: (item: DisplayableItemSealed) -> Unit,
    private val onFavoriteClick: (item: DisplayableItemSealed) -> Unit, //maneja el click en el icono de favorito
//    private val onTuneClick: ((item: DisplayableItemSealed) -> Unit)? = null,
//    private val onDeleteClick: ((instrument: CreatedInstrument) -> Unit)? = null
    ) : ListAdapter<DisplayableItemSealed, RecyclerView.ViewHolder>(InstrumentDiffCallback()) {

    // --- ViewHolder para Títulos ---
    class HeaderViewHolder(private val binding: ListItemHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(headerItem: DisplayableItemSealed.HeaderItem) {
            binding.textViewHeaderTitle.text = headerItem.title
        }

        // Métod estático para inflar
        companion object {
            fun from(parent: ViewGroup): HeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemHeaderBinding.inflate(layoutInflater, parent, false)
                return HeaderViewHolder(binding)
            }
        }
    }

    // --- ViewHolder para Instrumentos (Predefinidos y Creados) -- eliminamos tune y delete, ya que van en el menú contextual
    class InstrumentViewHolder(
        private val binding: ListItemInstrumentBinding,
        private val onItemClickLambda: (item: DisplayableItemSealed) -> Unit,
        private val onFavoriteClickLambda: (item: DisplayableItemSealed) -> Unit
//        private val onTuneClickLambda: ((item: DisplayableItemSealed) -> Unit)?,
//        private val onDeleteInstrumentClickLambda: ((instrument: CreatedInstrument) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("DiscouragedApi")
        fun bind(displayableItem: DisplayableItemSealed) { // cambiamos: el métod recibe DisplayableItemSealed completo
            // Configurar el click en toda la vista del item
            binding.root.setOnClickListener {
                onItemClickLambda(displayableItem) // aquí es donde se pasa el wrapper
            }

            // configuración del icono y el nombre en función del tipo de instrumento
            var instrumentName: String = ""
            var instrumentInfo: String? = ""
            var isFavoriteState: Boolean = false
            val context = binding.root.context // necesario para obtener el contexto y Glide
            //placeholder para el icono del instrumento (todo)
            Glide.with(context).clear(binding.imageViewItemInstrumentIcon) // limpiamos la vista con Glide


            when (displayableItem) {
                is DisplayableItemSealed.PredefinedInstrumentItem -> {
                    val instrument = displayableItem.instrument
                    val iconResName = instrument.iconResName
                    val frequency = instrument.frequency.let { "Frecuencia: $it Hz" } //establecemos la presentación de la frecuencia para el campo info

                    instrumentName = instrument.name
                    instrumentInfo = "${instrument.information ?: "No hay información disponible."} ($frequency)".trim() //la información podría ser solo la frecuencia
                    isFavoriteState = displayableItem.isFavorite // recuperamos el estado favorito

                    // lógica para el icono de instrumento predefinido
                    if (iconResName.isNotBlank()) {
                        val iconResId = context.resources.getIdentifier(
                            iconResName,
                            "drawable",
                            context.packageName
                        )
                        if (iconResId != 0) {
                            binding.imageViewItemInstrumentIcon.setImageResource(iconResId)
                        } else {
                            Log.w("InstrumentsAdapter", "No se encontró el recurso de imagen: $iconResName")
                            binding.imageViewItemInstrumentIcon.setImageResource(R.drawable.ic_default_instrument)
                        }

                    }
                }

                is DisplayableItemSealed.CreatedInstrumentItem -> {
                    val instrument = displayableItem.instrument
                    val frequency = instrument.frequency.let { "Frecuencia: $it Hz" } //establecemos la presentación de la frecuencia para el campo info
                    val photoPath = instrument.photoPath

                    instrumentName = instrument.name
                    instrumentInfo = "${instrument.information ?: "No hay información disponible."} ($frequency)".trim() //la información podría ser solo la frecuencia
                    isFavoriteState = displayableItem.isFavorite // recuperamos el estado favorito

                    if (!photoPath.isNullOrEmpty()) {
                        Glide.with(context)
                            .load(photoPath) // Puede ser una ruta de archivo
                            .placeholder(R.drawable.ic_placeholder) // Drawable mientras carga
                            .error(R.drawable.ic_error)       // Drawable si hay error
                            .circleCrop() // Opcional, si quieres imágenes circulares
                            .into(binding.imageViewItemInstrumentIcon)
                    } else {
                        binding.imageViewItemInstrumentIcon.setImageResource(R.drawable.ic_default_instrument) // Imagen por defecto si no hay foto
                    }

                }

                is DisplayableItemSealed.HeaderItem -> {
                    // No hacer nada si es un header
                    return
                }
            }

            // Configurar el nombre del instrumento
            binding.textViewItemInstrumentName.text = instrumentName
            // Configurar la información del instrumento
            binding.textViewItemInstrumentInfo.text = instrumentInfo

            //configurar el icono de favorito y el click
            if (isFavoriteState) {
                binding.imageButtonItemFavorite.setImageResource(R.drawable.ic_star_favorite)
            } else {
                binding.imageButtonItemFavorite.setImageResource(R.drawable.ic_star)
            }
            binding.imageButtonItemFavorite.setOnClickListener {
                onFavoriteClickLambda(displayableItem) // llamamos a la función lambda con el wrapper
            }
        }

        // Métod estático para inflar, incluimos las funciones lambda (onItemClick y onFavoriteClick)
        companion object {
            fun from(
                parent: ViewGroup,
                onItemClick: (item: DisplayableItemSealed) -> Unit,
                onFavoriteClick: (item: DisplayableItemSealed) -> Unit,
//                onTuneClick: ((item: DisplayableItemSealed) -> Unit)?,
//                onDeleteClick: ((instrument: CreatedInstrument) -> Unit)?
            ): InstrumentViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemInstrumentBinding.inflate(layoutInflater, parent, false)
                return InstrumentViewHolder(binding, onItemClick, onFavoriteClick) // pasamos las funciones lambda
            }
        }
    }

    // --- Implementación de ListAdapter ---

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DisplayableItemSealed.HeaderItem -> VIEW_TYPE_HEADER
            is DisplayableItemSealed.PredefinedInstrumentItem -> VIEW_TYPE_PREDEFINED
            is DisplayableItemSealed.CreatedInstrumentItem -> VIEW_TYPE_CREATED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder.from(parent)
            VIEW_TYPE_PREDEFINED, VIEW_TYPE_CREATED -> InstrumentViewHolder.from(
                parent,
                onItemClick,
                onFavoriteClick
            ) // pasamos las funciones lambda
            else -> throw ClassCastException("Vista desconocida: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) // Obtenemos el DisplayableItem
        when (holder) {
            is HeaderViewHolder -> {
                if (item is DisplayableItemSealed.HeaderItem) { // Comprobación segura
                    holder.bind(item)
                }
            }

            is InstrumentViewHolder -> {
                if (item is DisplayableItemSealed.PredefinedInstrumentItem || item is DisplayableItemSealed.CreatedInstrumentItem) { // Comprobación segura
                    holder.bind(item)
                }
            }
        }
    }
}

    // --- DiffUtil Callback ---
    // Esta clase es necesaria para ListAdapter ya que le dice al adaptador cómo saber si dos items son el mismo
    // Permite animaciones y actualizaciones eficientes
// Para que ListAdapter sepa cómo calcular diferencias y animar cambios
    class InstrumentDiffCallback : DiffUtil.ItemCallback<DisplayableItemSealed>() {
        override fun areItemsTheSame(
            oldItem: DisplayableItemSealed,
            newItem: DisplayableItemSealed
        ): Boolean {
            // Comprobar si es el mismo item (basado en ID o identificador único)
            return when {
                oldItem is DisplayableItemSealed.HeaderItem && newItem is DisplayableItemSealed.HeaderItem ->
                    oldItem.title == newItem.title // Los headers son iguales si su título es igual
                oldItem is DisplayableItemSealed.PredefinedInstrumentItem && newItem is DisplayableItemSealed.PredefinedInstrumentItem ->
                    oldItem.instrument.id == newItem.instrument.id // Mismo ID predefinido
                oldItem is DisplayableItemSealed.CreatedInstrumentItem && newItem is DisplayableItemSealed.CreatedInstrumentItem ->
                    oldItem.instrument.id == newItem.instrument.id // Mismo ID creado
                else -> false // Tipos diferentes, no son el mismo item
            }
        }

        override fun areContentsTheSame(
            oldItem: DisplayableItemSealed,
            newItem: DisplayableItemSealed
        ): Boolean {
            // Comprobar si el contenido del item ha cambiado (basado en igualdad de data class)
            return oldItem == newItem // Data classes implementan equals()
        }
    }
