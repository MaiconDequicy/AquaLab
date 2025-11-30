package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.model.LeituraSensor
import br.iots.aqualab.repository.HistoricoRepository
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.launch

class HistoricoViewModel : ViewModel() {

    private val repository = HistoricoRepository()

    private var NOME_SENSOR_PH = "ph"
    private var NOME_SENSOR_TEMP = "temperatura"

    private val _ultimoPH = MutableLiveData<Double>()
    val ultimoPH: LiveData<Double> = _ultimoPH

    private val _ultimaTemp = MutableLiveData<Double>()
    val ultimaTemp: LiveData<Double> = _ultimaTemp

    private val _qualidadeAtual = MutableLiveData<String>()
    val qualidadeAtual: LiveData<String> = _qualidadeAtual

    private val _dadosGrafico = MutableLiveData<List<Entry>>()
    val dadosGrafico: LiveData<List<Entry>> = _dadosGrafico

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun carregarDados(pontoId: String, periodoStr: String) {
        _isLoading.value = true
        val dias = 0

        viewModelScope.launch {
            val lista = repository.buscarLeiturasPorPeriodo(pontoId, dias)
            processarDados(lista)
            _isLoading.value = false
        }
    }

    private fun processarDados(lista: List<LeituraSensor>) {
        if (lista.isEmpty()) return

        val listaPH = lista.filter { it.sensorId?.equals(NOME_SENSOR_PH, ignoreCase = true) == true }
        val listaTemp = lista.filter { it.sensorId?.equals(NOME_SENSOR_TEMP, ignoreCase = true) == true }

        if (listaPH.isNotEmpty()) {
            val valor = listaPH.last().valor ?: 0.0
            _ultimoPH.value = valor
            calcularQualidade(valor)
        }

        if (listaTemp.isNotEmpty()) {
            _ultimaTemp.value = listaTemp.last().valor ?: 0.0
        }

        val entries = ArrayList<Entry>()
        listaPH.forEachIndexed { index, leitura ->
            entries.add(Entry(index.toFloat(), (leitura.valor ?: 0.0).toFloat()))
        }

        _dadosGrafico.value = entries
    }

    private fun calcularQualidade(ph: Double) {
        _qualidadeAtual.value = if (ph in 6.0..9.0) "Boa" else "Ruim"
    }
}
