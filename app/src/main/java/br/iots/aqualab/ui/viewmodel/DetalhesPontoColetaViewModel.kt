package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.model.LeituraSensor
import br.iots.aqualab.repository.PontoColetaRepository
import kotlinx.coroutines.launch

class DetalhesPontoColetaViewModel : ViewModel() {

    private val repository = PontoColetaRepository()

    private val _leituras = MutableLiveData<List<LeituraSensor>>()
    val leituras: LiveData<List<LeituraSensor>> = _leituras

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _ultimaLeitura = MutableLiveData<LeituraSensor?>()
    val ultimaLeitura: LiveData<LeituraSensor?> = _ultimaLeitura

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun carregarLeituras(pontoIdNuvem: String?) {
        if (pontoIdNuvem.isNullOrEmpty()) {
            _errorMessage.value = "Não há um ID da nuvem associado a este ponto."
            _leituras.value = emptyList()
            _ultimaLeitura.value = null
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val listaDeLeituras = repository.getLeiturasRecentes(pontoIdNuvem, limit = 50)

                _leituras.value = listaDeLeituras
                _ultimaLeitura.value = listaDeLeituras.firstOrNull()

            } catch (exception: Exception) {
                _errorMessage.value = "Erro ao carregar leituras: ${exception.message}"
                _leituras.value = emptyList()
                _ultimaLeitura.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}