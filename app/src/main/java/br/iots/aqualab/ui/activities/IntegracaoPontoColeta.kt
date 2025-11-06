package br.iots.aqualab.ui.activities

import android.os.Bundle
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

    private var isSaving = false

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

        configurarListenerBotaoSalvar()
        observarViewModel()
    }

    private fun configurarListenerBotaoSalvar() {
        binding.botaoCriarPonto.setOnClickListener {
            val nome = binding.campoNomePonto.text.toString().trim()
            val tipo = binding.campoTipoPonto.text.toString().trim()
            val local = binding.campoLocalPonto.text.toString().trim()
            val idPontoNuvem = binding.campoLocalIDPontoNuvem.text.toString().trim()

            if (nome.isNotEmpty() && tipo.isNotEmpty() && local.isNotEmpty()) {

                val novoPonto = PontoColeta(
                    nome = nome,
                    tipo = tipo,
                    localizacao = local,
                    pontoIdNuvem = idPontoNuvem.ifEmpty { null },
                    endereco = local,
                    status = "Ativo",
                    latitude = 0.0,
                    longitude = 0.0
                )
                isSaving = true
                viewModel.criarNovoPonto(novoPonto)

            } else {
                Toast.makeText(this, "Por favor, preencha nome, tipo e localização.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun observarViewModel() {
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "Erro: $it", Toast.LENGTH_LONG).show()
                isSaving = false
            }
        }

        viewModel.pontosColeta.observe(this) {
            if (isSaving) {
                Toast.makeText(this, "Ponto de coleta criado com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
