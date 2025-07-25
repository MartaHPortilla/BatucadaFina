package com.martahp.batucadafina.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.martahp.batucadafina.databinding.ActivityFrequencyChartTipsBinding
import com.martahp.batucadafina.utils.NoteData
import com.martahp.batucadafina.model.entities.NoteFrequencyEntry
import com.martahp.batucadafina.ui.adapter.FrequencyChartAdapter

class FrequencyChartTipsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFrequencyChartTipsBinding
    private lateinit var frequencyAdapter: FrequencyChartAdapter

    // La lista de datos que se mostrarán en el RecyclerView, los recogemos de NoteData
    private val frequencyChartData: List<NoteFrequencyEntry> by lazy {
        NoteData.getFrequencyChartEntries()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFrequencyChartTipsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureToolbar()
        setupRecyclerView()
    }

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbarFrequencyChart)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarFrequencyChart.setNavigationOnClickListener {
            finish() // Volver a la pantalla anterior (TipsActivity)
        }
    }

    private fun setupRecyclerView() {
        frequencyAdapter = FrequencyChartAdapter()

        binding.recyclerViewFrequencyChart.apply {
            layoutManager = LinearLayoutManager(this@FrequencyChartTipsActivity)
            adapter = frequencyAdapter
        }

        frequencyAdapter.submitList(frequencyChartData)
    }
}