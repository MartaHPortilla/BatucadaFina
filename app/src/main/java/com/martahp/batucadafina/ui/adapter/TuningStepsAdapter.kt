package com.martahp.batucadafina.ui.adapter

import android.os.Build
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.martahp.batucadafina.databinding.ListItemTuningStepBinding
import com.martahp.batucadafina.model.entities.TuningStep

class TuningStepsAdapter(
    private val steps: List<TuningStep> //el adapter recibe la lista de pasos directamente
) : RecyclerView.Adapter<TuningStepsAdapter.StepViewHolder>() {

    //viewHolder para cada paso
    class StepViewHolder(private val binding: ListItemTuningStepBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(step: TuningStep) {

            //necesitamos el contexto del binding desde itemView (binding.root)
            val context = itemView.context

            binding.textViewStepTitle.setText(step.titleResId)
            binding.imageViewStepImage.setImageResource(step.imageResId)
            //binding.textViewStepDescription.setText(step.descriptionResId) --> NO FUNCIONA para el formato HTML

            // --- NUEVO ENFOQUE PARA LA DESCRIPCIÓN ---
            // 1. Obtener el CharSequence del recurso.
            //    Esto debería devolver un CharSequence que ya tiene los 'Spans' de formato
            //    si las etiquetas <b> son reconocidas por el sistema de recursos.
            val styledDescriptionCharSequence: CharSequence = context.resources.getText(step.descriptionResId)
            Log.d("AdapterDebug", "Texto desde getText(): '$styledDescriptionCharSequence'")
            Log.d("AdapterDebug", "Tipo de CharSequence: ${styledDescriptionCharSequence::class.java.simpleName}")

            // 2. Asignar directamente el CharSequence al TextView.
            //    TextView está diseñado para manejar CharSequence con Spans.
            binding.textViewStepDescription.text = styledDescriptionCharSequence

        }
    }

    //creamos nuevos ViewHolders (llamado por el layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context) //inflamos el layout del item
        val binding = ListItemTuningStepBinding.inflate(layoutInflater, parent, false) //creamos el binding
        return StepViewHolder(binding) //creamos el ViewHolder mediante el retorno de la función
    }

    //reemplazamos el contenido de una vista (llamado por el layout manager)
    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val currentStep = steps[position] //obtenemos el paso actual de la lista
        holder.bind(currentStep) //llamamos al bind del ViewHolder para asignar los datos
    }

    //devolvemos el tamaño del dataset, o lista de pasos (llamado por el layout manager)
    override fun getItemCount(): Int {
        return steps.size
    }
}