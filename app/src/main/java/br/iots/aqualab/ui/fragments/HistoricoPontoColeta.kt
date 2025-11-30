package br.iots.aqualab.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import br.iots.aqualab.R
import br.iots.aqualab.ui.viewmodel.HistoricoViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.chip.Chip

class HistoricoPontoColeta : Fragment() {

    private val viewModel: HistoricoViewModel by viewModels()

    private var idPonto: String? = null
    private var nomePonto: String? = null

    private lateinit var lineChart: LineChart
    private lateinit var chip24h: Chip
    private lateinit var chip7d: Chip
    private lateinit var chip30d: Chip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            nomePonto = it.getString("nome_ponto")
            idPonto = it.getString("id_ponto")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_historico_ponto_coleta, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupChartStyle()
        setupObservers(view)
        setupListeners()

        if (idPonto != null) {
            viewModel.carregarDados(idPonto!!, "24h")
        } else {
        }
    }

    private fun initViews(view: View) {
        view.findViewById<TextView>(R.id.textViewLabelTitulo).text = "Histórico - ${nomePonto ?: ""}"

        view.findViewById<View>(R.id.toolbarTelaHistoricoPontosSensor).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        lineChart = view.findViewById(R.id.lineChart)
        chip24h = view.findViewById(R.id.chip24Horas)
        chip7d = view.findViewById(R.id.chip7Dias)
        chip30d = view.findViewById(R.id.chip30Dias)
    }

    private fun setupObservers(view: View) {
        viewModel.ultimoPH.observe(viewLifecycleOwner) { ph ->
            view.findViewById<TextView>(R.id.tvValorPH).text = String.format("%.1f", ph)
        }

        viewModel.ultimaTemp.observe(viewLifecycleOwner) { temp ->
            view.findViewById<TextView>(R.id.tvValorTemp).text = "${temp.toInt()}°C"
        }

        viewModel.qualidadeAtual.observe(viewLifecycleOwner) { qualidade ->
            val tvQualidade = view.findViewById<TextView>(R.id.tvStatusQualidade)
            tvQualidade.text = qualidade

            val indicador = view.findViewById<View>(R.id.viewIndicadorQualidade)

            if (qualidade == "Boa") {
                indicador.setBackgroundResource(R.drawable.circle_green)
            } else {
                indicador.setBackgroundResource(R.drawable.circle_orange) // ou red
            }
        }

        viewModel.dadosGrafico.observe(viewLifecycleOwner) { entries ->
            updateChartData(entries)
        }
    }

    private fun setupListeners() {
        chip24h.setOnClickListener {
            atualizarChips("24h")
            idPonto?.let { id -> viewModel.carregarDados(id, "24h") }
        }
        chip7d.setOnClickListener {
            atualizarChips("7d")
            idPonto?.let { id -> viewModel.carregarDados(id, "7d") }
        }
        chip30d.setOnClickListener {
            atualizarChips("30d")
            idPonto?.let { id -> viewModel.carregarDados(id, "30d") }
        }
    }

    private fun atualizarChips(selected: String) {
        chip24h.isChecked = selected == "24h"
        chip7d.isChecked = selected == "7d"
        chip30d.isChecked = selected == "30d"
    }

    private fun setupChartStyle() {
        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setTouchEnabled(true)
            animateX(1000)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.textColor = Color.GRAY

            axisRight.isEnabled = false
            axisLeft.textColor = Color.GRAY
            axisLeft.setDrawGridLines(true)
        }
    }

    private fun updateChartData(entries: List<Entry>) {
        if (entries.isEmpty()) {
            lineChart.clear()
            return
        }

        val dataSet = LineDataSet(entries, "pH").apply {
            color = Color.parseColor("#1976D2") // Azul
            valueTextColor = Color.BLACK
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(Color.parseColor("#1976D2"))
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#BBDEFB")
        }

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }
}