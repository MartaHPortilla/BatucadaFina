package com.martahp.batucadafina.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.martahp.batucadafina.R
import com.martahp.batucadafina.databinding.ActivityHowToTuneTipsBinding
import com.martahp.batucadafina.model.entities.TuningStep
import com.martahp.batucadafina.ui.adapter.TuningStepsAdapter

class HowToTuneTipsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHowToTuneTipsBinding
    private lateinit var tuningStepsAdapter: TuningStepsAdapter
    private val TAG = "HowToTuneTipsActivity"

    // variable que contiene los pasos de la guía de afinación
    private val tuningGuideSteps: List<TuningStep> = listOf(
        TuningStep(
            titleResId = R.string.tuning_step1_title,
            descriptionResId = R.string.tuning_step1_desc,
            imageResId = R.drawable.step1_pic
        ),
        TuningStep(
            titleResId = R.string.tuning_step2_title,
            descriptionResId = R.string.tuning_step2_desc,
            imageResId = R.drawable.step2_pic
        ),
        TuningStep(
            titleResId = R.string.tuning_step3_title,
            descriptionResId = R.string.tuning_step3_desc,
            imageResId = R.drawable.step3_pic
        ),
        TuningStep(
            titleResId = R.string.tuning_step4_title,
            descriptionResId = R.string.tuning_step4_desc,
            //imageResId = R.drawable.step4_pic
            imageResId = R.drawable.step4_pic
        ),
        TuningStep(
            titleResId = R.string.tuning_step5_title,
            descriptionResId = R.string.tuning_step5_desc,
            imageResId = R.drawable.step5_pic
        ),
        TuningStep(
            titleResId = R.string.tuning_step6_title,
            descriptionResId = R.string.tuning_step6_desc,
            imageResId = R.drawable.step6_pic
        ),
        TuningStep(
            titleResId = R.string.tuning_step7_title,
            descriptionResId = R.string.tuning_step7_desc,
            imageResId = R.drawable.step7_pic
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHowToTuneTipsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureToolbar()
        configureViewPager()
        configureButtons()

    }

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbarHowToTune)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarHowToTune.setNavigationOnClickListener {
            finish()
        }
    }

    private fun configureButtons() {

        //botón paso anterior
        binding.buttonPreviousStep.setOnClickListener {
            val currentItem = binding.viewPagerTuningSteps.currentItem
            if (currentItem > 0) {
                binding.viewPagerTuningSteps.currentItem = currentItem - 1
            }
        }

        //botón paso siguiente
        binding.buttonNextStep.setOnClickListener {
            val currentItem = binding.viewPagerTuningSteps.currentItem
            if (currentItem < tuningGuideSteps.size - 1) {
                binding.viewPagerTuningSteps.currentItem = currentItem + 1
            } else {
                //si es el último paso, cerramos Activity
                Toast.makeText(this, "¡Fin de la guía! Vuelve cuando quieras.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun configureViewPager() {
        tuningStepsAdapter = TuningStepsAdapter(tuningGuideSteps)
        binding.viewPagerTuningSteps.adapter = tuningStepsAdapter

        // Listener para actualizar los botones de navegación cuando cambia la página
        binding.viewPagerTuningSteps.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateNavigationButtonVisibility(position)
            }
        })
        // Llamada inicial para configurar la visibilidad de los botones
        updateNavigationButtonVisibility(0)
    }

    private fun updateNavigationButtonVisibility(currentPosition: Int) {

        //botón anterior: si estamos en el primer paso, se oculta
        if (currentPosition == 0) {
            binding.buttonPreviousStep.visibility = android.view.View.INVISIBLE
        } else {
            binding.buttonPreviousStep.visibility = android.view.View.VISIBLE

        }

        //botón siguiente: si estamos en el último paso, se oculta
        if (currentPosition == tuningStepsAdapter.itemCount -1) {
            binding.buttonNextStep.text = "Finalizar" //cambiamos el texto del botón
        } else {
            binding.buttonNextStep.text = "Siguiente" //es el texto por defecto
        }

    }


}



