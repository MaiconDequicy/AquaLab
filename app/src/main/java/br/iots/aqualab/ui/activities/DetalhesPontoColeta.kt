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
import br.iots.aqualab.ui.adapter.LeiturasAdapter
import br.iots.aqualab.ui.viewmodel.CriacaoPontosColetaViewModel
import br.iots.aqualab.ui.viewmodel.DetalhesPontoColetaViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

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
            val pontoAtualizado = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

        configurarFabSpeedDial()
        binding.toolbarDetalhesPonto.setNavigationOnClickListener { finish() }

        val pontoInicial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("PONTO_COLETA_EXTRA", PontoColeta::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<PontoColeta>("PONTO_COLETA_EXTRA")
        }

        pontoInicial?.let { ponto ->
            pontoColetaAtual = ponto
            preencherDados(ponto)
            configurarRecyclerView()
            observarViewModels()
            detalhesViewModel.carregarLeituras(ponto.pontoIdNuvem)
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
                val resultadoIntent = Intent().apply {
                    putExtra("PONTO_ID_EXTRA", pontoColetaAtual?.id)
                }
                setResult(Activity.RESULT_OK, resultadoIntent)
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
        }

        detalhesViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarLeituras.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        detalhesViewModel.ultimaLeitura.observe(this) { ultimaLeitura ->
            if (ultimaLeitura != null) {
                val sensorNome = ultimaLeitura.sensorId?.replaceFirstChar { it.titlecase() } ?: ""
                val valor = ultimaLeitura.valor ?: "N/A"
                val dataFormatada = ultimaLeitura.timestamp?.toDate()?.let { date ->
                    SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(date)
                } ?: "(não disponível)"

                binding.tvUltimaLeitura.text = "Última Leitura: $valor $sensorNome ($dataFormatada)"
            } else {
                binding.tvUltimaLeitura.text = "Última Leitura: (nenhuma leitura encontrada)"
            }
        }
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
        binding.fabConfigPontoColeta.animate().rotation(45f).setDuration(300).start()
        mostrarBotaoSpeedDial(binding.fabAcaoSecundaria1)
        mostrarBotaoSpeedDial(binding.fabAcaoSecundaria2)
    }

    private fun fecharSpeedDial() {
        binding.fabConfigPontoColeta.animate().rotation(0f).setDuration(300).start()
        esconderBotaoSpeedDial(binding.fabAcaoSecundaria1)
        esconderBotaoSpeedDial(binding.fabAcaoSecundaria2)
    }

    private fun mostrarBotaoSpeedDial(fab: FloatingActionButton) {
        fab.visibility = View.VISIBLE
        fab.alpha = 0f
        fab.translationY = fab.height.toFloat()
        fab.animate().alpha(1f).translationY(0f).setDuration(300).start()
    }

    private fun esconderBotaoSpeedDial(fab: FloatingActionButton) {
        fab.animate().alpha(0f).translationY(fab.height.toFloat()).setDuration(300)
            .withEndAction { fab.visibility = View.GONE }.start()
    }

    private fun preencherDados(ponto: PontoColeta) {
        pontoColetaAtual = ponto
        binding.textViewNomePonto.text = "Ponto de Coleta: ${ponto.nome}"
        binding.tvClassificacao.text = "Boa"
        binding.tvStatusOperacional.text = "Status Operacional: ${ponto.status}"
        binding.tvUltimaLeitura.text = "Última Leitura: (carregando...)"
        binding.tvNomeCadastral.text = "Nome: ${ponto.nome}"
        binding.tvTipoPontoCadastral.text = "Tipo de Ponto: ${ponto.tipo}"
        binding.tvLocalizacaoCadastral.text = "Endereço: ${ponto.endereco}"
    }

    private fun removerPontoDeColeta() {
        pontoColetaAtual?.let { pontoParaRemover ->
            operacoesViewModel.deletarPonto(pontoParaRemover)
        } ?: run {
            Toast.makeText(this, "Não foi possível identificar o ponto para remoção.", Toast.LENGTH_LONG).show()
        }
    }
}