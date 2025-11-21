package br.iots.aqualab.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import br.iots.aqualab.R
import br.iots.aqualab.databinding.FragmentInicioResearcherBinding
import br.iots.aqualab.model.LeituraSensor
import br.iots.aqualab.ui.adapter.SensorStatusAdapter
import br.iots.aqualab.ui.viewmodel.AuthUIState
import br.iots.aqualab.ui.viewmodel.DashboardUIState
import br.iots.aqualab.ui.viewmodel.DashboardViewModel
import br.iots.aqualab.ui.viewmodel.LoginViewModel
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class InicioResearcher : Fragment() {

    private var _binding: FragmentInicioResearcherBinding? = null
    private val binding get() = _binding!!

    private val loginViewModel: LoginViewModel by activityViewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()

    private lateinit var sensorStatusAdapter: SensorStatusAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioResearcherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupWelcomeMessageObserver()
        setupDashboardObserver()

        dashboardViewModel.loadDashboardData()

        if (loginViewModel.loginState.value !is AuthUIState.Success) {
            loginViewModel.checkIfUserIsLoggedIn()
        }
    }

    private fun setupRecyclerView() {
        sensorStatusAdapter = SensorStatusAdapter(emptyList())
        binding.recyclerViewSensorStatus.adapter = sensorStatusAdapter
    }

    private fun setupWelcomeMessageObserver() {
        loginViewModel.loginState.observe(viewLifecycleOwner) { state ->
            if (state is AuthUIState.Success) {
                val userProfile = state.userProfile
                binding.boasVindas.text = "Olá, ${userProfile.displayName}!"
            }
        }
    }

    /**
     * Observa o estado do DashboardViewModel para preencher os cards com dados dinâmicos.
     */
    private fun setupDashboardObserver() {
        dashboardViewModel.dashboardState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DashboardUIState.Loading -> {
                    binding.valorSensorDestaque.text = "Carregando..."
                }
                is DashboardUIState.Success -> {
                    state.ultimaLeitura?.let {
                        updateFeaturedSensorCard(it)
                    }
                    binding.nomeSensorLocal.text = "Sensor de Temp. - ${state.pontoDestaque.nome}"
                    binding.itemAnaliseSensor.text = "Sensor de Temp. - ${state.pontoDestaque.nome}"
                    binding.labelStatusSensores.text = "Status dos Sensores - ${state.pontoDestaque.nome}"

                    updateLineChart(state.leiturasRecentes)
                    updateSensorStatusList(state.leiturasRecentes)
                }
                is DashboardUIState.Error -> {
                    binding.valorSensorDestaque.text = "Erro ao carregar"
                }
                is DashboardUIState.Empty -> {
                    binding.valorSensorDestaque.text = "Nenhum ponto de coleta"
                }
            }
        }
    }

    private fun updateFeaturedSensorCard(ultimaLeitura: LeituraSensor) {
        val leituraTemperatura = if (ultimaLeitura.sensorId == "temperatura") {
            ultimaLeitura
        } else {
            dashboardViewModel.dashboardState.value?.let { state ->
                if (state is DashboardUIState.Success) {
                    state.leiturasRecentes.firstOrNull { it.sensorId == "temperatura" }
                } else null
            }
        }

        leituraTemperatura?.let {
            val valorFormatado = String.format("%.1f °C", it.valor)
            binding.valorSensorDestaque.text = valorFormatado
        } ?: run {
            binding.valorSensorDestaque.text = "Sem dados de Temp."
        }
    }

    /**
     * GRÁFICO APRIMORADO
     */
    private fun updateLineChart(leituras: List<LeituraSensor>) {
        if (leituras.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            return
        }

        val leiturasTemperatura = leituras
            .filter { it.sensorId == "temperatura" }
            .reversed()

        val entries = ArrayList<Entry>()
        leiturasTemperatura.forEachIndexed { index, leitura ->
            leitura.valor?.let { valor ->
                entries.add(Entry(index.toFloat(), valor.toFloat()))
            }
        }

        val dataSet = LineDataSet(entries, "Temperatura (°C)").apply {
            color = ContextCompat.getColor(requireContext(), R.color.blue)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.black)

            lineWidth = 2.5f
            valueTextSize = 9f
            setDrawCircles(true)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.blue))
            circleHoleColor = ContextCompat.getColor(requireContext(), R.color.blue)
            circleRadius = 4f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(false)
        }

        val lineData = LineData(dataSet)

        binding.lineChart.apply {
            data = lineData

            // Remove descrição padrão
            description.isEnabled = false

            // animação
            animateXY(700, 700)

            // Eixos
            axisRight.isEnabled = false

            xAxis.apply {
                setDrawGridLines(false)
                granularity = 1f
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                textSize = 10f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridLineWidth = 0.3f
                textSize = 10f
            }

            legend.isEnabled = true
            legend.textSize = 12f

            invalidate()
        }
    }

    private fun updateSensorStatusList(leituras: List<LeituraSensor>) {
        val ultimasLeiturasPorSensor = leituras
            .groupBy { it.sensorId }
            .map { it.value.first() }
            .sortedBy { it.sensorId }

        sensorStatusAdapter.updateData(ultimasLeiturasPorSensor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
