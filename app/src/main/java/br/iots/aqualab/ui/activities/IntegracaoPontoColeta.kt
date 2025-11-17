package br.iots.aqualab.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import br.iots.aqualab.R
import br.iots.aqualab.databinding.ActivityIntegracaoPontoColetaBinding
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.ui.viewmodel.CriacaoPontosColetaViewModel
import com.google.android.material.appbar.MaterialToolbar

class IntegracaoPontoColeta : AppCompatActivity() {

    private lateinit var binding: ActivityIntegracaoPontoColetaBinding
    private val viewModel: CriacaoPontosColetaViewModel by viewModels()
    private var pontoParaEditar: PontoColeta? = null
    private enum class Acao { CRIANDO, EDITANDO, IDLE }
    private var acaoAtual = Acao.IDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntegracaoPontoColetaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarTelaIntegraPontos)

        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        toolbar.setNavigationOnClickListener {
            finish()
        }

        observarViewModel()
        viewModel.carregarIdsDisponiveis()


        pontoParaEditar = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("PONTO_PARA_EDITAR_EXTRA", PontoColeta::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<PontoColeta>("PONTO_PARA_EDITAR_EXTRA")
        }

        if (pontoParaEditar != null) {
            configurarModoEdicao()
        } else {
            configurarModoCriacao()
        }

        binding.botaoCriarPonto.setOnClickListener {
            salvarPontoColeta()
        }
    }

    private fun configurarModoCriacao() {
        binding.textViewArtigosNoticiasTitulo.text = "Crie um Novo Ponto de Coleta"
        binding.botaoCriarPonto.text = "Cadastrar"
    }

    private fun configurarModoEdicao() {
        binding.textViewArtigosNoticiasTitulo.text = "Editar Ponto de Coleta"
        binding.botaoCriarPonto.text = "Salvar Alterações"

        pontoParaEditar?.let { ponto ->
            binding.campoNomePonto.setText(ponto.nome)
            binding.campoTipoPonto.setText(ponto.tipo)
            binding.campoLocalPonto.setText(ponto.endereco)
            binding.campoLocalIDPontoNuvem.setText(ponto.pontoIdNuvem)
        }
    }

    private fun salvarPontoColeta() {
        val nome = binding.campoNomePonto.text.toString().trim()
        val tipo = binding.campoTipoPonto.text.toString().trim()
        val local = binding.campoLocalPonto.text.toString().trim()
        val idPontoNuvem = binding.campoLocalIDPontoNuvem.text.toString().trim()

        if (nome.isEmpty() || tipo.isEmpty() || local.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha nome, tipo e localização.", Toast.LENGTH_SHORT).show()
            return
        }

        if (pontoParaEditar != null) {
            val pontoAtualizado = pontoParaEditar!!.copy(
                nome = nome,
                tipo = tipo,
                endereco = local,
                pontoIdNuvem = idPontoNuvem.ifEmpty { null }
            )
            acaoAtual = Acao.EDITANDO
            viewModel.atualizarPonto(pontoAtualizado)

        } else {
            val novoPonto = PontoColeta(
                nome = nome,
                tipo = tipo,
                endereco = local,
                pontoIdNuvem = idPontoNuvem.ifEmpty { null },
                status = "Ativo",
                localizacao = local,
                latitude = 0.0,
                longitude = 0.0
            )
            acaoAtual = Acao.CRIANDO
            viewModel.criarNovoPonto(novoPonto)
        }
    }


    private fun observarViewModel() {
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "Erro: $it", Toast.LENGTH_LONG).show()
                acaoAtual = Acao.IDLE
            }
        }

        viewModel.operacaoConcluida.observe(this) { concluida ->
            if (concluida) {
                when (acaoAtual) {
                    Acao.CRIANDO -> {
                        Toast.makeText(this, "Ponto de coleta criado com sucesso!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    Acao.EDITANDO -> {
                        Toast.makeText(this, "Ponto atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        val resultadoIntent = Intent().putExtra("PONTO_ATUALIZADO_EXTRA", pontoParaEditar)
                        setResult(Activity.RESULT_OK, resultadoIntent)
                        finish()
                    }
                    Acao.IDLE -> {}
                }
                viewModel.resetarStatusOperacao()
                acaoAtual = Acao.IDLE
            }
        }

        viewModel.idsDisponiveisNuvem.observe(this) { ids ->
            if (ids != null && ids.isNotEmpty()) {
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ids)

                (binding.campoLocalIDPontoNuvem as? AutoCompleteTextView)?.setAdapter(adapter)
            }
        }
    }
}
