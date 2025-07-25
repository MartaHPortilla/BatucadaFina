package com.martahp.batucadafina.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.martahp.batucadafina.databinding.ListItemTipBinding // ViewBinding para tu list_item_tip.xml
import com.martahp.batucadafina.model.entities.TipEntry

class TipsAdapter(
    private val onItemClick: (tipEntry: TipEntry) -> Unit
) : ListAdapter<TipEntry, TipsAdapter.TipViewHolder>(TipDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        return TipViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClick)
    }

    class TipViewHolder(private val binding: ListItemTipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tipEntry: TipEntry, onItemClick: (tipEntry: TipEntry) -> Unit) {
            binding.textViewItemTipName.text = tipEntry.title
            binding.textViewItemTipInfo.text = tipEntry.description ?: ""
            binding.textViewItemTipInfo.visibility = if (tipEntry.description.isNullOrEmpty()) View.GONE else View.VISIBLE

            binding.ImageViewItemTipIcon.setImageResource(tipEntry.iconResId)

            binding.root.setOnClickListener {
                onItemClick(tipEntry)
            }
        }

        companion object {
            fun from(parent: ViewGroup): TipViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemTipBinding.inflate(layoutInflater, parent, false)
                return TipViewHolder(binding)
            }
        }
    }

    class TipDiffCallback : DiffUtil.ItemCallback<TipEntry>() {
        override fun areItemsTheSame(oldItem: TipEntry, newItem: TipEntry): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: TipEntry, newItem: TipEntry): Boolean {
            return oldItem == newItem //data class compara todos los campos
        }
    }
}