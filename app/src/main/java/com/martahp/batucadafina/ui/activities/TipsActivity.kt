package com.martahp.batucadafina.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.martahp.batucadafina.R
import com.martahp.batucadafina.databinding.ActivityTipsBinding
import com.martahp.batucadafina.model.entities.TipEntry
import com.martahp.batucadafina.ui.adapter.TipsAdapter

class TipsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTipsBinding
    private lateinit var tipsAdapter: TipsAdapter
    val TAG = "TipsActivity"

    //aquí definimos la lista de tips, su descripción, icono y actividad asociada (a dónde nos lleva)
    //podríamos hacerlo con viewmodel? Realmente no es necesario, ya que son items fijos, que no van a variar
    val tipsList = listOf(
        TipEntry("Cómo Afinar un Tambor", "Guía paso a paso con imágenes.", R.drawable.ic_placeholder, HowToTuneTipsActivity::class.java),
        TipEntry("Tabla de Frecuencias", "Correspondencia de notas y frecuencias.", R.drawable.ic_placeholder, FrequencyChartTipsActivity::class.java)
        //TODO: añadir más si nos da tiempo
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTipsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureToolbar()
        configureRecyclerView()

    }

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbarTips)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbarTips.setNavigationOnClickListener {
            finish() //volvemos atrás
        }
    }

    private fun configureRecyclerView() {
        tipsAdapter = TipsAdapter { clickedTip ->
            val intent = Intent(this, clickedTip.targetActivityClass)
            startActivity(intent)
        }

        binding.recyclerViewTips.apply {
            layoutManager = LinearLayoutManager(this@TipsActivity)
            adapter = tipsAdapter
        }

        //enviamos la lista de tips al adapter. Importante!
        tipsAdapter.submitList(tipsList)



    }


}
