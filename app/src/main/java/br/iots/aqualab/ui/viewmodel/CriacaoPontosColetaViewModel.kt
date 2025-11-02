package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.repository.PontoColetaRepository
import kotlinx.coroutines.launch

class CriacaoPontosColetaViewModel : ViewModel() {

    private val pontoColetaRepository = PontoColetaRepository()

    private val _pontosColeta = MutableLiveData<List<PontoColeta>>()
    val pontosColeta: LiveData<List<PontoColeta>> = _pontosColeta

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        carregarPontosDeColetaDoUsuario()
    }

    private fun carregarPontosDeColetaDoUsuario()
    {
        viewModelScope.launch {
            _isLoading.value = true
            val resultado = pontoColetaRepository.getPontosColetaDoUsuario()

            resultado.onSuccess { listaDePontos ->
                _pontosColeta.value = listaDePontos
                if (listaDePontos.isEmpty()) {
                }
            }.onFailure { exception ->
                _errorMessage.value = "Erro ao carregar pontos: ${exception.message}"
            }
            _isLoading.value = false
        }
    }

    fun criarNovoPonto(novoPonto: PontoColeta) {
        viewModelScope.launch {
            val resultado = pontoColetaRepository.criarPontoColeta(novoPonto)
            resultado.onSuccess {
                carregarPontosDeColetaDoUsuario()
            }.onFailure { exception ->
                _errorMessage.value = "Erro ao criar ponto: ${exception.message}"
            }
        }
    }
}
