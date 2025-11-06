package br.iots.aqualab.ui.activities

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.iots.aqualab.databinding.ActivityDetalhesPontoColetaBinding // Importar ViewBinding
import br.iots.aqualab.model.PontoColeta // Importar o modelo PontoColeta

class DetalhesPontoColeta : AppCompatActivity() {

    private lateinit var binding: ActivityDetalhesPontoColetaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalhesPontoColetaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarDetalhesPonto.setNavigationOnClickListener {
            finish()
        }
        val ponto = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("PONTO_COLETA_EXTRA", PontoColeta::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<PontoColeta>("PONTO_COLETA_EXTRA")
        }

        ponto?.let {
            preencherDados(it)
        }
    }

    private fun preencherDados(ponto: PontoColeta) {

        binding.textViewNomePonto.text = "Ponto de Coleta: ${ponto.nome}"

        binding.tvClassificacao.text = "Boa"
        binding.tvStatusOperacional.text = "Status Operacional: ${ponto.status}"
        binding.tvUltimaLeitura.text = "Última Leitura: (não disponível)"

        binding.tvNomeCadastral.text = "Nome: ${ponto.nome}"
        binding.tvTipoPontoCadastral.text = "Tipo de Ponto: ${ponto.tipo}"
        binding.tvLocalizacaoCadastral.text = "Endereço: ${ponto.endereco}"
    }
}
