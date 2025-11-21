package br.iots.aqualab.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.observe
import br.iots.aqualab.databinding.ActivityDetalhesPontoColetaBinding
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.model.LeituraSensor
import br.iots.aqualab.ui.adapter.LeiturasAdapter
import br.iots.aqualab.ui.viewmodel.CriacaoPontosColetaViewModel
import br.iots.aqualab.ui.viewmodel.DetalhesPontoColetaViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.formatter.ValueFormatter
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
        observarViewModels()

        pontoInicial?.let {
            pontoColetaAtual = it
            preencherDados(it)
            detalhesViewModel.carregarLeituras(it.pontoIdNuvem)
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

        // Agrupa leituras por sensor
        val sensoresAgrupados = leituras.groupBy {
            it.sensorId?.lowercase() ?: "desconhecido"
        }

        // Vai armazenar todos os datasets plotados
        val datasets = mutableListOf<LineDataSet>()

        val cores = listOf(
            Color.parseColor("#1E88E5"),
            Color.parseColor("#E53935"),
            Color.parseColor("#8E24AA"),
            Color.parseColor("#43A047"),
            Color.parseColor("#FB8C00")
        )

        var idxGlobal = 0

        sensoresAgrupados.forEach { (sensorId, lista) ->

            // Ordena pelo timestamp real
            val ordenadas = lista.sortedBy { it.timestamp?.toDate()?.time ?: 0L }

            // Converte para Entries numerados (evita Float overflow)
            val entries = ordenadas.mapIndexed { index, leitura ->
                val v = leitura.valor?.toFloat() ?: 0f
                Entry(index.toFloat(), v)
            }

            if (entries.isEmpty()) return@forEach

            val ds = LineDataSet(entries, sensorId.replaceFirstChar { it.titlecase() }).apply {
                lineWidth = 2f
                setDrawCircles(true)
                circleRadius = 3f
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
                color = cores[idxGlobal % cores.size]
                setCircleColor(cores[idxGlobal % cores.size])
            }

            datasets.add(ds)
            idxGlobal++
        }

        chart.clear()

        val lineData = LineData()
        datasets.forEach { ds ->
            lineData.addDataSet(ds)
        }

        chart.data = lineData

        val primeiraListaOrdenada =
            sensoresAgrupados.values.first().sortedBy { it.timestamp?.toDate()?.time ?: 0L }

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                if (index in primeiraListaOrdenada.indices) {
                    val t = primeiraListaOrdenada[index].timestamp?.toDate() ?: Date()
                    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(t)
                }
                return ""
            }
        }

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
        }

        chart.axisLeft.apply {
            setDrawGridLines(true)
        }

        chart.axisRight.isEnabled = false

        chart.legend.apply {
            form = Legend.LegendForm.LINE
            textSize = 12f
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
}
