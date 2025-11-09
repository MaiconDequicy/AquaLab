package br.iots.aqualab.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import br.iots.aqualab.R
import br.iots.aqualab.databinding.FragmentInicioResearcherBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class InicioResearcher : Fragment() {

    private var _binding: FragmentInicioResearcherBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInicioResearcherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLineChart()
    }

    private fun setupLineChart() {

        val entries = ArrayList<Entry>()
        entries.add(Entry(0f, 22.5f))
        entries.add(Entry(1f, 23.1f))
        entries.add(Entry(2f, 24.0f))
        entries.add(Entry(3f, 23.8f))
        entries.add(Entry(4f, 25.1f))
        entries.add(Entry(5f, 24.5f))

        val dataSet = LineDataSet(entries, "Temperatura (°C)")

        dataSet.color = ContextCompat.getColor(requireContext(), R.color.blue) // Cor da linha
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.black) // Cor do texto dos valores
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.blue)) // Cor dos pontos
        dataSet.setDrawCircles(true) // Desenhar os pontos
        dataSet.lineWidth = 2f // Espessura da linha
        dataSet.valueTextSize = 10f // Tamanho do texto dos valores
        dataSet.setDrawValues(true) // Mostrar os valores em cada ponto

        val lineData = LineData(dataSet)

        binding.lineChart.apply {
            data = lineData
            description.isEnabled = false
            legend.isEnabled = true
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}