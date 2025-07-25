package com.martahp.batucadafina.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.martahp.batucadafina.databinding.ListItemFrequencyNoteBinding
import com.martahp.batucadafina.model.entities.NoteFrequencyEntry
import java.util.Locale

class FrequencyChartAdapter : ListAdapter<NoteFrequencyEntry, FrequencyChartAdapter.FrequencyViewHolder>(FrequencyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrequencyViewHolder {
        return FrequencyViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: FrequencyViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class FrequencyViewHolder(private val binding: ListItemFrequencyNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NoteFrequencyEntry) {
            binding.textViewNote.text = item.note
            binding.textViewFrequencyHz.text = String.format(Locale.US, "%.2f Hz", item.frequency)
        }

        companion object {
            fun from(parent: ViewGroup): FrequencyViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemFrequencyNoteBinding.inflate(layoutInflater, parent, false)
                return FrequencyViewHolder(binding)
            }
        }
    }

    class FrequencyDiffCallback : DiffUtil.ItemCallback<NoteFrequencyEntry>() {
        override fun areItemsTheSame(oldItem: NoteFrequencyEntry, newItem: NoteFrequencyEntry): Boolean {
            //como los datos son estáticos y no cambian de posición podemos usar el contenido como identificador
            return oldItem.note == newItem.note && oldItem.frequency == newItem.frequency
        }

        override fun areContentsTheSame(oldItem: NoteFrequencyEntry, newItem: NoteFrequencyEntry): Boolean {
            return oldItem == newItem //data class compara todos los campos
        }
    }
}