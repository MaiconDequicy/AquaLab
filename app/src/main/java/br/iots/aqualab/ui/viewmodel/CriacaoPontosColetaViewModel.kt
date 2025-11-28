package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.data.dao.MedicaoDao
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.repository.PontoColetaRepository
import kotlinx.coroutines.launch

class CriacaoPontosColetaViewModel(
    private val medicaoDao: MedicaoDao
) : ViewModel() {
    private val pontoColetaRepository = PontoColetaRepository()

    private val _pontosColeta = MutableLiveData<List<PontoColeta>>()
    val pontosColeta: LiveData<List<PontoColeta>> = _pontosColeta

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operacaoConcluida = MutableLiveData<Boolean>(false)
    val operacaoConcluida: LiveData<Boolean> = _operacaoConcluida

    private val _pontoDeletado = MutableLiveData<Boolean>(false)
    val pontoDeletado: LiveData<Boolean> = _pontoDeletado

    private val _idsDisponiveisNuvem = MutableLiveData<List<String>>()
    val idsDisponiveisNuvem: LiveData<List<String>> = _idsDisponiveisNuvem

    init {
        carregarPontosDeColetaDoUsuario()
    }

    fun carregarIdsDisponiveis() {
        viewModelScope.launch {
            val resultado = pontoColetaRepository.getIdsDisponiveisNuvem()

            resultado.onSuccess { ids ->
                _idsDisponiveisNuvem.value = ids
            }.onFailure { exception ->
                _errorMessage.value = "Erro ao buscar IDs da nuvem: ${exception.message}"
            }
        }
    }

    private fun carregarPontosDeColetaDoUsuario() {
        viewModelScope.launch {
            _isLoading.value = true
            val resultado = pontoColetaRepository.getPontosColetaDoUsuario()

            resultado.onSuccess { listaDePontos ->
                _pontosColeta.value = listaDePontos
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
                _operacaoConcluida.value = true
            }.onFailure { exception ->
                _errorMessage.value = "Erro ao criar ponto: ${exception.message}"
            }
        }
    }

    fun atualizarPonto(ponto: PontoColeta) {
        viewModelScope.launch {
            val resultado = pontoColetaRepository.atualizarPontoColeta(ponto)
            resultado.onSuccess {
                _operacaoConcluida.value = true
            }.onFailure { exception ->
                _errorMessage.value = "Erro ao atualizar ponto: ${exception.message}"
            }
        }
    }

    fun deletarPonto(ponto: PontoColeta) {
        viewModelScope.launch {
            ponto.id?.let { pontoId ->
                val resultado = pontoColetaRepository.deletarPontoColeta(pontoId)
                resultado.onSuccess {
                    val idParaDeletarLocal = if (!ponto.pontoIdNuvem.isNullOrEmpty()) {
                        ponto.pontoIdNuvem
                    } else {
                        ponto.id.toString()
                    }
                    medicaoDao.deletarMedicoesDoPonto(idParaDeletarLocal)

                    _pontoDeletado.value = true
                    carregarPontosDeColetaDoUsuario()
                }.onFailure { exception ->
                    _errorMessage.value = "Erro ao deletar ponto: ${exception.message}"
                }
            } ?: run {
                _errorMessage.value = "ID do ponto é nulo, não é possível deletar."
            }
        }
    }

    fun resetarStatusOperacao() {
        _operacaoConcluida.value = false
        _errorMessage.value = null
    }
}

class CriacaoViewModelFactory(private val dao: MedicaoDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CriacaoPontosColetaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CriacaoPontosColetaViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}