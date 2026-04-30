package br.iots.aqualab.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import br.iots.aqualab.R
import br.iots.aqualab.constants.WaterQualityConstants
import br.iots.aqualab.constants.WaterUsageStandards
import br.iots.aqualab.databinding.FragmentInicioResearcherBinding
import br.iots.aqualab.model.LeituraSensor
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.repository.PontoColetaRepository
import br.iots.aqualab.ui.adapter.SensorStatusAdapter
import br.iots.aqualab.ui.components.TimestampAxisFormatter
import br.iots.aqualab.ui.viewmodel.AuthUIState
import br.iots.aqualab.ui.viewmodel.DashboardUIState
import br.iots.aqualab.ui.viewmodel.DashboardViewModel
import br.iots.aqualab.ui.viewmodel.LoginViewModel
import br.iots.aqualab.utils.CsvExporter
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch

class InicioResearcher : Fragment() {

    private var _binding: FragmentInicioResearcherBinding? = null
    private val binding get() = _binding!!

    private val loginViewModel: LoginViewModel by activityViewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()

    private lateinit var sensorStatusAdapter: SensorStatusAdapter

    // Seleção dinâmica de sensor e ponto de coleta
    private var selectedSensorType: WaterQualityConstants.SensorType = WaterQualityConstants.SensorType.TEMPERATURA
    private var selectedPontoColeta: PontoColeta? = null
    private val repository = PontoColetaRepository()
    private var allPontos: List<PontoColeta> = emptyList()

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
        setupClickListeners()
        loadAvailablePoints()

        dashboardViewModel.loadDashboardData()

        if (loginViewModel.loginState.value !is AuthUIState.Success) {
            loginViewModel.checkIfUserIsLoggedIn()
        }
    }

    private fun loadAvailablePoints() {
        lifecycleScope.launch {
            val result = repository.getTodosPontosAcessiveis()
            allPontos = result.getOrNull() ?: emptyList()
        }
    }

    private fun setupClickListeners() {
        // Clique no card de sensor de destaque para trocar sensor
        binding.cardDestaque.setOnClickListener {
            showSensorSelectionDialog()
        }

        // Clique longo no card para trocar ponto de coleta
        binding.cardDestaque.setOnLongClickListener {
            showPontoColetaSelectionDialog()
            true
        }

        // Clique no nome do sensor/local para trocar
        binding.nomeSensorLocal.setOnClickListener {
            showPontoColetaSelectionDialog()
        }

        // Clique no gráfico para trocar sensor
        binding.lineChart.setOnClickListener {
            showSensorSelectionDialog()
        }

        // Botão de compartilhar dados
        binding.btnShareData.setOnClickListener {
            exportAndShareData()
        }

        // Botão de legenda de qualidade
        binding.btnLegenda.setOnClickListener {
            showWaterStandardsDialog()
        }
    }

    private fun showSensorSelectionDialog() {
        val sensorTypes = WaterQualityConstants.SensorType.values()
        val sensorNames = sensorTypes.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Selecionar Sensor de Destaque")
            .setItems(sensorNames) { dialog, which ->
                selectedSensorType = sensorTypes[which]
                Toast.makeText(
                    requireContext(),
                    "Sensor selecionado: ${selectedSensorType.displayName}",
                    Toast.LENGTH_SHORT
                ).show()
                dashboardViewModel.loadDashboardData()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPontoColetaSelectionDialog() {
        if (allPontos.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Carregando pontos de coleta...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val pontoNames = allPontos.map { "${it.nome} (${it.tipo})" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Selecionar Ponto de Coleta")
            .setItems(pontoNames) { dialog, which ->
                selectedPontoColeta = allPontos[which]
                Toast.makeText(
                    requireContext(),
                    "Ponto selecionado: ${selectedPontoColeta?.nome}",
                    Toast.LENGTH_SHORT
                ).show()
                // Recarregar dados para o ponto selecionado
                reloadDataForSelectedPoint()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun reloadDataForSelectedPoint() {
        selectedPontoColeta?.let { ponto ->
            lifecycleScope.launch {
                try {
                    val leituras = repository.getLeiturasRecentes(ponto.pontoIdNuvem, limit = 30)

                    // Atualizar labels
                    binding.nomeSensorLocal.text = "${selectedSensorType.displayName} - ${ponto.nome}"
                    binding.itemAnaliseSensor.text = "${selectedSensorType.displayName} - ${ponto.nome}"
                    binding.labelStatusSensores.text = "Status dos Sensores - ${ponto.nome}"

                    // Atualizar card de destaque
                    val leituraSensor = leituras.firstOrNull {
                        it.sensorType == selectedSensorType
                    }
                    leituraSensor?.let {
                        binding.valorSensorDestaque.text = it.getFormattedValue()
                    } ?: run {
                        binding.valorSensorDestaque.text = "Sem dados"
                    }

                    // Atualizar gráfico
                    updateLineChart(leituras)

                    // Atualizar lista de status
                    updateSensorStatusList(leituras)

                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Erro ao carregar dados: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
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
     * GRÁFICO APRIMORADO - Agora suporta qualquer tipo de sensor
     */
    private fun updateLineChart(leituras: List<LeituraSensor>) {
        if (leituras.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            return
        }

        // Filtrar leituras do sensor selecionado
        val leiturasFiltradas = leituras
            .filter { it.sensorType == selectedSensorType }
            .sortedBy { it.getTimestampMillis() }

        if (leiturasFiltradas.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            return
        }

        val entries = ArrayList<Entry>()
        val timestamps = mutableListOf<Long>()

        leiturasFiltradas.forEachIndexed { index, leitura ->
            leitura.valor?.let { valor ->
                entries.add(Entry(index.toFloat(), valor.toFloat()))
                timestamps.add(leitura.getTimestampMillis())
            }
        }

        // Usar cor específica do tipo de sensor
        val sensorColor = selectedSensorType.chartColor

        val dataSet = LineDataSet(entries, "${selectedSensorType.displayName} (${selectedSensorType.unit})").apply {
            color = sensorColor
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.black)

            lineWidth = 2.5f
            valueTextSize = 9f
            setDrawCircles(true)
            setCircleColor(sensorColor)
            circleHoleColor = sensorColor
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

            // Usar formatador de timestamp personalizado
            xAxis.apply {
                valueFormatter = TimestampAxisFormatter.auto(timestamps)
                setDrawGridLines(false)
                granularity = 1f
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                textSize = 9f
                labelRotationAngle = -30f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridLineWidth = 0.3f
                textSize = 10f
            }

            legend.isEnabled = true
            legend.textSize = 11f

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

    /**
     * Exporta e compartilha dados do sensor em destaque
     */
    private fun exportAndShareData() {
        lifecycleScope.launch {
            try {
                // Buscar todas as leituras do sensor selecionado
                val pontoId = selectedPontoColeta?.pontoIdNuvem
                    ?: dashboardViewModel.dashboardState.value?.let { state ->
                        if (state is DashboardUIState.Success) state.pontoDestaque.pontoIdNuvem else null
                    }

                if (pontoId == null) {
                    Toast.makeText(
                        requireContext(),
                        "Nenhum ponto de coleta selecionado",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val leituras = repository.getLeiturasComFiltros(
                    pontoIdNuvem = pontoId,
                    sensorType = selectedSensorType,
                    limit = 1000
                )

                if (leituras.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Nenhum dado disponível para exportar",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Exportar para CSV
                val fileUri = CsvExporter.exportToCsv(
                    context = requireContext(),
                    leituras = leituras,
                    pontoColeta = selectedPontoColeta ?: dashboardViewModel.dashboardState.value?.let { state ->
                        if (state is DashboardUIState.Success) state.pontoDestaque else null
                    },
                    sensorType = selectedSensorType
                )

                fileUri?.let { uri ->
                    // Gerar resumo estatístico
                    val resumo = CsvExporter.generateSummaryText(leituras, selectedSensorType)

                    // Mostrar opções de compartilhamento
                    AlertDialog.Builder(requireContext())
                        .setTitle("Exportação Concluída!")
                        .setMessage("${leituras.size} leituras exportadas!\n\n$resumo\n\nComo deseja compartilhar?")
                        .setPositiveButton("📱 WhatsApp") { _, _ ->
                            CsvExporter.shareViaWhatsApp(
                                requireContext(),
                                uri,
                                selectedSensorType,
                                selectedPontoColeta
                            )
                        }
                        .setNeutralButton("Outros") { _, _ ->
                            CsvExporter.shareCsv(
                                requireContext(),
                                uri,
                                selectedSensorType,
                                selectedPontoColeta
                            )
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } ?: run {
                    Toast.makeText(
                        requireContext(),
                        "Erro ao criar arquivo CSV",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erro: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Mostra dialog com padrões de qualidade da água
     */
    private fun showWaterStandardsDialog() {
        val usageTypes = WaterUsageStandards.WaterUsageType.values()
        val usageNames = usageTypes.map { "${it.icon} ${it.displayName}" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Padrões de Qualidade da Água")
            .setItems(usageNames) { _, which ->
                val selectedType = usageTypes[which]
                showStandardDetails(selectedType)
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    /**
     * Mostra detalhes dos padrões para um tipo de uso específico
     */
    private fun showStandardDetails(usageType: WaterUsageStandards.WaterUsageType) {
        val message = buildString {
            append("${usageType.icon} ${usageType.displayName}\n\n")
            append("${usageType.description}\n\n")

            when (usageType) {
                WaterUsageStandards.WaterUsageType.CONSUMO_HUMANO -> {
                    append(WaterUsageStandards.ConsumoHumano.getDescription())
                }
                WaterUsageStandards.WaterUsageType.BALNEABILIDADE -> {
                    append(WaterUsageStandards.Balneabilidade.getDescription())
                }
                WaterUsageStandards.WaterUsageType.IRRIGACAO -> {
                    append(WaterUsageStandards.Irrigacao.getDescription())
                }
                WaterUsageStandards.WaterUsageType.AQUICULTURA -> {
                    append(WaterUsageStandards.Aquicultura.getDescription())
                }
                WaterUsageStandards.WaterUsageType.DESSEDENTACAO_ANIMAL -> {
                    append(WaterUsageStandards.DessedentacaoAnimal.getDescription())
                }
                WaterUsageStandards.WaterUsageType.PRESERVACAO_AMBIENTAL -> {
                    append(WaterUsageStandards.PreservacaoAmbiental.getDescription())
                }
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("${usageType.icon} ${usageType.displayName}")
            .setMessage(message)
            .setPositiveButton("Entendi", null)
            .setNeutralButton("Ver Outros", { _, _ ->
                showWaterStandardsDialog()
            })
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
