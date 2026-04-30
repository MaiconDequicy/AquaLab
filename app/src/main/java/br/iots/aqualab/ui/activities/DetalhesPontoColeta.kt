package br.iots.aqualab.ui.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.observe
import androidx.lifecycle.lifecycleScope
import br.iots.aqualab.R
import br.iots.aqualab.constants.WaterQualityConstants
import br.iots.aqualab.databinding.ActivityDetalhesPontoColetaBinding
import br.iots.aqualab.databinding.DialogFotoComentarioBinding
import br.iots.aqualab.model.FotoLocal
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.model.LeituraSensor
import br.iots.aqualab.repository.FotoLocalRepository
import br.iots.aqualab.repository.PontoColetaRepository
import br.iots.aqualab.ui.adapter.LeiturasAdapter
import br.iots.aqualab.ui.adapters.FotoLocalAdapter
import br.iots.aqualab.ui.components.ChartConfigDialog
import br.iots.aqualab.ui.components.ChartDataFilter
import br.iots.aqualab.ui.components.TimestampAxisFormatter
import br.iots.aqualab.ui.viewmodel.CriacaoPontosColetaViewModel
import br.iots.aqualab.ui.viewmodel.DetalhesPontoColetaViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date
import android.graphics.Color

class DetalhesPontoColeta : AppCompatActivity() {

    private lateinit var binding: ActivityDetalhesPontoColetaBinding
    private var isSpeedDialOpen = false
    private var pontoColetaAtual: PontoColeta? = null

    private val operacoesViewModel: CriacaoPontosColetaViewModel by viewModels()
    private val detalhesViewModel: DetalhesPontoColetaViewModel by viewModels()

    private lateinit var leiturasAdapter: LeiturasAdapter

    // Filtros e configurações de gráfico
    private var currentFilter: ChartDataFilter = ChartDataFilter.default()
    private var selectedSensorType: WaterQualityConstants.SensorType? = null
    private var availableSensors: List<WaterQualityConstants.SensorType> = emptyList()
    private val repository = PontoColetaRepository()

    // Fotos locais
    private lateinit var fotoAdapter: FotoLocalAdapter
    private lateinit var fotoRepository: FotoLocalRepository
    private var currentPhotoFile: File? = null
    private var currentPhotoUri: Uri? = null

    private val edicaoPontoResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pontoAtualizado =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra("PONTO_ATUALIZADO_EXTRA", PontoColeta::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra<PontoColeta>("PONTO_ATUALIZADO_EXTRA")
                }

            pontoAtualizado?.let {
                preencherDados(it)
                Toast.makeText(this, "Ponto atualizado!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher para permissão de câmera
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Permissão de câmera necessária para tirar fotos", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher para captura de foto
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoFile != null) {
            showCommentDialog()
        } else {
            Toast.makeText(this, "Falha ao capturar foto", Toast.LENGTH_SHORT).show()
            currentPhotoFile?.delete()
            currentPhotoFile = null
            currentPhotoUri = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalhesPontoColetaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarDetalhesPonto.setNavigationOnClickListener { finish() }

        configurarFabSpeedDial()

        val pontoInicial =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("PONTO_COLETA_EXTRA", PontoColeta::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<PontoColeta>("PONTO_COLETA_EXTRA")
            }

        configurarRecyclerView()
        configurarGrafico()
        configurarFotos()
        observarViewModels()

        pontoInicial?.let {
            pontoColetaAtual = it
            preencherDados(it)
            detalhesViewModel.carregarLeituras(it.pontoIdNuvem)
            carregarSensoresDisponiveis(it.pontoIdNuvem)
            carregarFotos(it.id)
        }

        // Botão para configurar filtros de gráfico
        binding.toolbarDetalhesPonto.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                android.R.id.home -> {
                    finish()
                    true
                }
                else -> false
            }
        }

        // Adicionar ação de clique longo no gráfico para abrir configurações
        binding.chartHistorico.setOnLongClickListener {
            showChartConfigDialog()
            true
        }
    }

    private fun carregarSensoresDisponiveis(pontoIdNuvem: String?) {
        lifecycleScope.launch {
            availableSensors = repository.getSensoresDisponiveis(pontoIdNuvem)
        }
    }

    private fun showChartConfigDialog() {
        if (availableSensors.isEmpty()) {
            Toast.makeText(this, "Nenhum sensor disponível", Toast.LENGTH_SHORT).show()
            return
        }

        ChartConfigDialog.show(
            context = this,
            availableSensors = availableSensors,
            currentFilter = currentFilter
        ) { filter, sensorType ->
            currentFilter = filter
            selectedSensorType = sensorType
            recarregarDadosComFiltros()
        }
    }

    private fun recarregarDadosComFiltros() {
        pontoColetaAtual?.pontoIdNuvem?.let { pontoId ->
            lifecycleScope.launch {
                try {
                    val leituras = repository.getLeiturasComFiltros(
                        pontoIdNuvem = pontoId,
                        sensorType = selectedSensorType,
                        startDate = currentFilter.startDate,
                        endDate = currentFilter.endDate,
                        limit = currentFilter.maxSamples
                    )

                    leiturasAdapter.updateData(leituras)
                    atualizarGrafico(leituras)

                    val sensorName = selectedSensorType?.displayName ?: "Todos"
                    Toast.makeText(
                        this@DetalhesPontoColeta,
                        "Filtrado: $sensorName (${leituras.size} leituras)",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@DetalhesPontoColeta,
                        "Erro ao carregar dados: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun configurarRecyclerView() {
        leiturasAdapter = LeiturasAdapter(emptyList())
        binding.recyclerViewLeituras.adapter = leiturasAdapter
        binding.recyclerViewLeituras.isNestedScrollingEnabled = false
    }

    private fun observarViewModels() {

        operacoesViewModel.pontoDeletado.observe(this) { deletado ->
            if (deletado) {
                Toast.makeText(this, "Ponto removido com sucesso", Toast.LENGTH_LONG).show()
                val resultIntent = Intent().apply {
                    putExtra("PONTO_ID_EXTRA", pontoColetaAtual?.id)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                operacoesViewModel.resetarStatusOperacao()
                finish()
            }
        }

        operacoesViewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "Falha na operação: $it", Toast.LENGTH_LONG).show()
                operacoesViewModel.resetarStatusOperacao()
            }
        }

        detalhesViewModel.leituras.observe(this) { leituras ->
            leiturasAdapter.updateData(leituras)
            atualizarGrafico(leituras)
        }

        detalhesViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarLeituras.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }

        detalhesViewModel.ultimaLeitura.observe(this) { ultimo ->
            if (ultimo != null) {
                val valor = ultimo.valor?.let {
                    String.format(Locale.getDefault(), "%.2f", it)
                } ?: "N/A"

                val nome = ultimo.sensorId?.replaceFirstChar { it.titlecase() } ?: ""

                val dataFormatada =
                    ultimo.timestamp?.toDate()?.let {
                        SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(it)
                    } ?: "(não disponível)"

                binding.tvUltimaLeitura.text =
                    "Última Leitura: $valor $nome ($dataFormatada)"
            } else {
                binding.tvUltimaLeitura.text = "Última Leitura: (nenhuma leitura)"
            }
        }
    }

    private fun configurarGrafico() {
        val chart = binding.chartHistorico

        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.setDrawGridBackground(false)

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                return sdf.format(Date(value.toLong()))
            }
        }

        chart.axisLeft.setDrawGridLines(true)
        chart.axisRight.isEnabled = false

        chart.legend.apply {
            form = Legend.LegendForm.LINE
            textSize = 12f
        }

        chart.data = LineData()
        chart.invalidate()
    }

    private fun atualizarGrafico(leituras: List<LeituraSensor>) {
        val chart = binding.chartHistorico

        if (leituras.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        // Agrupa leituras por tipo de sensor usando as constantes
        val sensoresAgrupados = LeituraSensor.groupBySensorType(leituras)

        // Vai armazenar todos os datasets plotados
        val datasets = mutableListOf<LineDataSet>()

        // Coletar todos os timestamps para formatação do eixo X
        val todosTimestamps = mutableListOf<Long>()

        sensoresAgrupados.forEach { (sensorType, lista) ->
            // Ordena pelo timestamp real
            val ordenadas = lista.sortedBy { it.getTimestampMillis() }

            // Converte para Entries numerados (evita Float overflow)
            val entries = ordenadas.mapIndexed { index, leitura ->
                todosTimestamps.add(leitura.getTimestampMillis())
                val v = leitura.valor?.toFloat() ?: 0f
                Entry(index.toFloat(), v)
            }

            if (entries.isEmpty()) return@forEach

            // Usar a cor específica do sensor das constantes
            val sensorColor = sensorType.chartColor

            val ds = LineDataSet(entries, sensorType.displayName).apply {
                lineWidth = 2.5f
                setDrawCircles(true)
                circleRadius = 4f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity = 0.2f
                color = sensorColor
                setCircleColor(sensorColor)
                fillColor = sensorColor
                setDrawFilled(false)
            }

            datasets.add(ds)
        }

        chart.clear()

        val lineData = LineData()
        datasets.forEach { ds ->
            lineData.addDataSet(ds)
        }

        chart.data = lineData

        // Usar o TimestampAxisFormatter personalizado
        val primeiraListaOrdenada = sensoresAgrupados.values.first()
            .sortedBy { it.getTimestampMillis() }

        val timestamps = primeiraListaOrdenada.map { it.getTimestampMillis() }

        // Escolher formato automático baseado no range de datas
        chart.xAxis.valueFormatter = TimestampAxisFormatter.auto(timestamps)

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(true)
            gridColor = Color.LTGRAY
            labelRotationAngle = -45f // Rotacionar labels para melhor visualização
        }

        chart.axisLeft.apply {
            setDrawGridLines(true)
            gridColor = Color.LTGRAY
        }

        chart.axisRight.isEnabled = false

        chart.legend.apply {
            form = Legend.LegendForm.LINE
            textSize = 11f
            formSize = 12f
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        }

        chart.notifyDataSetChanged()
        chart.invalidate()
    }


    private fun configurarFabSpeedDial() {
        binding.fabAcaoSecundaria1.visibility = View.GONE
        binding.fabAcaoSecundaria2.visibility = View.GONE

        binding.fabConfigPontoColeta.setOnClickListener {
            isSpeedDialOpen = !isSpeedDialOpen
            if (isSpeedDialOpen) abrirSpeedDial() else fecharSpeedDial()
        }

        binding.fabAcaoSecundaria1.setOnClickListener {
            pontoColetaAtual?.let { ponto ->
                val intent = Intent(this, IntegracaoPontoColeta::class.java).apply {
                    putExtra("PONTO_PARA_EDITAR_EXTRA", ponto)
                }
                edicaoPontoResultLauncher.launch(intent)
            }
            fecharSpeedDial()
        }

        binding.fabAcaoSecundaria2.setOnClickListener {
            removerPontoDeColeta()
            fecharSpeedDial()
        }
    }

    private fun abrirSpeedDial() {
        binding.fabConfigPontoColeta.animate().rotation(45f).setDuration(200).start()
        mostrarBotao(binding.fabAcaoSecundaria1)
        mostrarBotao(binding.fabAcaoSecundaria2)
    }

    private fun fecharSpeedDial() {
        binding.fabConfigPontoColeta.animate().rotation(0f).setDuration(200).start()
        esconderBotao(binding.fabAcaoSecundaria1)
        esconderBotao(binding.fabAcaoSecundaria2)
    }

    private fun mostrarBotao(fab: FloatingActionButton) {
        fab.visibility = View.VISIBLE
        fab.alpha = 0f
        fab.translationY = fab.height.toFloat()
        fab.animate().alpha(1f).translationY(0f).setDuration(200).start()
    }

    private fun esconderBotao(fab: FloatingActionButton) {
        fab.animate().alpha(0f).translationY(fab.height.toFloat()).setDuration(200)
            .withEndAction { fab.visibility = View.GONE }.start()
    }

    private fun preencherDados(ponto: PontoColeta) {
        binding.textViewNomePonto.text = "Ponto de Coleta: ${ponto.nome}"
        binding.tvClassificacao.text = "Boa"
        binding.tvStatusOperacional.text = "Status Operacional: ${ponto.status}"
        binding.tvUltimaLeitura.text = "Última Leitura: (carregando...)"
        binding.tvNomeCadastral.text = "Nome: ${ponto.nome}"
        binding.tvTipoPontoCadastral.text = "Tipo de Ponto: ${ponto.tipo}"
        binding.tvLocalizacaoCadastral.text = "Endereço: ${ponto.endereco}"
    }

    private fun removerPontoDeColeta() {
        pontoColetaAtual?.let {
            operacoesViewModel.deletarPonto(it)
        } ?: Toast.makeText(
            this,
            "Não foi possível identificar o ponto para remoção.",
            Toast.LENGTH_LONG
        ).show()
    }

    // ==================== Funcionalidades de Fotos ====================

    private fun configurarFotos() {
        fotoRepository = FotoLocalRepository(this)

        fotoAdapter = FotoLocalAdapter(
            onEditComment = { foto, newComment ->
                lifecycleScope.launch {
                    val result = fotoRepository.atualizarComentario(foto.id, newComment)
                    if (result.isSuccess) {
                        Toast.makeText(this@DetalhesPontoColeta, "Comentário atualizado", Toast.LENGTH_SHORT).show()
                        pontoColetaAtual?.id?.let { carregarFotos(it) }
                    } else {
                        Toast.makeText(this@DetalhesPontoColeta, "Erro ao atualizar comentário", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDelete = { foto ->
                lifecycleScope.launch {
                    val result = fotoRepository.deletarFoto(foto.id)
                    if (result.isSuccess) {
                        Toast.makeText(this@DetalhesPontoColeta, "Foto excluída", Toast.LENGTH_SHORT).show()
                        pontoColetaAtual?.id?.let { carregarFotos(it) }
                    } else {
                        Toast.makeText(this@DetalhesPontoColeta, "Erro ao excluir foto", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onClick = { foto ->
                // Visualizar foto em tela cheia (opcional - implementação futura)
                Toast.makeText(this, "Visualização de foto: ${foto.id}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerPhotos.adapter = fotoAdapter
        binding.recyclerPhotos.isNestedScrollingEnabled = false

        // Botão adicionar foto
        binding.btnAddPhoto.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }
    }

    private fun carregarFotos(pontoColetaId: String) {
        lifecycleScope.launch {
            val fotos = fotoRepository.getFotosPorPonto(pontoColetaId)

            if (fotos.isEmpty()) {
                binding.tvEmptyPhotos.visibility = View.VISIBLE
                binding.recyclerPhotos.visibility = View.GONE
            } else {
                binding.tvEmptyPhotos.visibility = View.GONE
                binding.recyclerPhotos.visibility = View.VISIBLE
                fotoAdapter.submitList(fotos)
            }
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        lifecycleScope.launch {
            try {
                val photoFile = fotoRepository.createPhotoFile()
                val photoUri = fotoRepository.getUriForFile(photoFile)

                currentPhotoFile = photoFile
                currentPhotoUri = photoUri

                cameraLauncher.launch(photoUri)
            } catch (e: Exception) {
                Toast.makeText(this@DetalhesPontoColeta, "Erro ao iniciar câmera: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun showCommentDialog() {
        val dialogBinding = DialogFotoComentarioBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            // Cancelar - deletar foto
            currentPhotoFile?.delete()
            currentPhotoFile = null
            currentPhotoUri = null
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val comentario = dialogBinding.etComentario.text.toString().trim()
            salvarFoto(comentario)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun salvarFoto(comentario: String) {
        val pontoId = pontoColetaAtual?.id
        val photoPath = currentPhotoFile?.absolutePath

        if (pontoId == null || photoPath == null) {
            Toast.makeText(this, "Erro: dados inválidos", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val result = fotoRepository.salvarFoto(
                pontoColetaId = pontoId,
                caminhoArquivo = photoPath,
                comentario = comentario
            )

            if (result.isSuccess) {
                Toast.makeText(this@DetalhesPontoColeta, "Foto salva com sucesso!", Toast.LENGTH_SHORT).show()
                carregarFotos(pontoId)
            } else {
                Toast.makeText(
                    this@DetalhesPontoColeta,
                    "Erro ao salvar foto: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Limpar referências
            currentPhotoFile = null
            currentPhotoUri = null
        }
    }
}
