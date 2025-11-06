package br.iots.aqualab.ui.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import br.iots.aqualab.databinding.ActivityDetalhesPontoColetaBinding
import br.iots.aqualab.model.PontoColeta
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DetalhesPontoColeta : AppCompatActivity() {

    private lateinit var binding: ActivityDetalhesPontoColetaBinding
    private var isSpeedDialOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalhesPontoColetaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarFabSpeedDial()

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

    private fun configurarFabSpeedDial() {
        binding.fabAcaoSecundaria1.visibility = View.GONE
        binding.fabAcaoSecundaria2.visibility = View.GONE

        binding.fabConfigPontoColeta.setOnClickListener {
            isSpeedDialOpen = !isSpeedDialOpen

            if (isSpeedDialOpen) {
                abrirSpeedDial()
            } else {
                fecharSpeedDial()
            }
        }

        binding.fabAcaoSecundaria1.setOnClickListener {
            // Exemplo de ação
            Toast.makeText(this, "Ação de Editar Clicada", Toast.LENGTH_SHORT).show()
            fecharSpeedDial()
        }

        binding.fabAcaoSecundaria2.setOnClickListener {

            Toast.makeText(this, "Ação de Deletar Clicada", Toast.LENGTH_SHORT).show()
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
        fab.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()
    }

    private fun esconderBotaoSpeedDial(fab: FloatingActionButton) {
        fab.animate()
            .alpha(0f)
            .translationY(fab.height.toFloat())
            .setDuration(300)
            .withEndAction {
                fab.visibility = View.GONE
            }
            .start()
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
