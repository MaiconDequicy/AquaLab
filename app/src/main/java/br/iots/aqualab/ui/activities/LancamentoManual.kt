package br.iots.aqualab.ui.activities

import android.R
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.iots.aqualab.data.database.AppDatabase
import br.iots.aqualab.databinding.ActivityLancamentoManualBinding
import br.iots.aqualab.ui.viewmodel.LancamentoManualViewModel
import br.iots.aqualab.ui.viewmodel.LancamentoViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LancamentoManual : AppCompatActivity() {

    private lateinit var binding: ActivityLancamentoManualBinding

    private val viewModel: LancamentoManualViewModel by viewModels {
        LancamentoViewModelFactory(AppDatabase.getDatabase(this).medicaoDao())
    }

    private val calendario = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLancamentoManualBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        setupObservers()
        setupDropdownParametros()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbarLancamentoManual)
        supportActionBar?.title = ""

        val nomePonto = intent.getStringExtra("NOME_PONTO") ?: "Ponto Desconhecido"

        binding.textViewNomePontoColeta.text = "Ponto de coleta: $nomePonto"

        atualizarCamposDataHora()
    }

    private fun setupDropdownParametros() {
        val opcoes = listOf("pH", "Temperatura", "Oxigênio Dissolvido", "Condutividade", "Turbidez", "Amônia")
        val adapter = ArrayAdapter(this, R.layout.simple_dropdown_item_1line, opcoes)
        (binding.campoParametro as? android.widget.AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.toolbarLancamentoManual.setNavigationOnClickListener {
            finish()
        }

        binding.buttonCancelar.setOnClickListener {
            finish()
        }

        binding.campoDataColeta.setOnClickListener {
            abrirDatePicker()
        }

        binding.campoHorarioColeta.setOnClickListener {
            abrirTimePicker()
        }

        binding.buttonSalvar.setOnClickListener {
            val pontoId = intent.getStringExtra("PONTO_ID") ?: ""

            val parametroSelecionado = binding.campoParametro.text.toString()

            if (pontoId.isEmpty()) {
                Toast.makeText(this, "Erro: ID do ponto não encontrado.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.salvarMedicao(
                pontoId = pontoId,
                parametro = parametroSelecionado,
                valorTexto = binding.campoValorMedicao.text.toString(),
                dataSelecionada = calendario.timeInMillis,
                local = binding.campoLocalizaoColeta.text.toString(),
                obs = binding.campoObservacoes.text.toString()
            )
        }
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.salvamentoSucesso.collect { sucesso ->
                if (sucesso) {
                    Toast.makeText(this@LancamentoManual, "Medição salva com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.mensagemErro.collect { erro ->
                Toast.makeText(this@LancamentoManual, erro, Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun atualizarCamposDataHora() {
        val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        val formatoHora = SimpleDateFormat("HH:mm", Locale("pt", "BR"))

        binding.campoDataColeta.setText(formatoData.format(calendario.time))
        binding.campoHorarioColeta.setText(formatoHora.format(calendario.time))
    }

    private fun abrirDatePicker() {
        DatePickerDialog(
            this,
            { _, ano, mes, dia ->
                calendario.set(Calendar.YEAR, ano)
                calendario.set(Calendar.MONTH, mes)
                calendario.set(Calendar.DAY_OF_MONTH, dia)
                atualizarCamposDataHora()
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun abrirTimePicker() {
        TimePickerDialog(
            this,
            { _, hora, minuto ->
                calendario.set(Calendar.HOUR_OF_DAY, hora)
                calendario.set(Calendar.MINUTE, minuto)
                atualizarCamposDataHora()
            },
            calendario.get(Calendar.HOUR_OF_DAY),
            calendario.get(Calendar.MINUTE),
            true // Formato 24h
        ).show()
    }
}