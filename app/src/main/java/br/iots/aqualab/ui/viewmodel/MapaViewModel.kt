package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.domain.usecase.GetDetalhesCompletosDoPontoUseCase
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.model.PontoDetalhadoInfo
import br.iots.aqualab.repository.AnaliseQualidadeRepository
import br.iots.aqualab.repository.PontoColetaRepository
import br.iots.aqualab.repository.WeatherRepository
import kotlinx.coroutines.launch

class MapaViewModel : ViewModel() {

    private val pontoColetaRepository = PontoColetaRepository()
    private val weatherRepository = WeatherRepository()
    private val analiseQualidadeRepository = AnaliseQualidadeRepository()

    private val getDetalhesCompletosUseCase = GetDetalhesCompletosDoPontoUseCase(
        pontoColetaRepository,
        weatherRepository,
        analiseQualidadeRepository
    )

    private val _pontosPublicos = MutableLiveData<List<PontoColeta>>()
    val pontosPublicos: LiveData<List<PontoColeta>> = _pontosPublicos

    private val _detalhesDoPonto = MutableLiveData<PontoDetalhadoInfo?>()
    val detalhesDoPonto: LiveData<PontoDetalhadoInfo?> = _detalhesDoPonto

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isLoadingDetalhes = MutableLiveData<Boolean>()
    val isLoadingDetalhes: LiveData<Boolean> = _isLoadingDetalhes

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        carregarPontosPublicos()
    }

    private fun carregarPontosPublicos() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = pontoColetaRepository.getPontosPublicos()

            result.onSuccess { pontos ->
                _pontosPublicos.value = pontos
            }.onFailure { exception ->
                _errorMessage.value = "Erro ao carregar pontos: ${exception.message}"
            }
            _isLoading.value = false
        }
    }

    fun onDetalhesRequested(ponto: PontoColeta) {
        _isLoadingDetalhes.value = true
        _detalhesDoPonto.value = null

        viewModelScope.launch {
            val result = getDetalhesCompletosUseCase(ponto)

            result.onSuccess { detalhesResult ->
                _detalhesDoPonto.value = detalhesResult.info

                atualizarClassificacaoLocalmente(ponto.id, detalhesResult.classificacao)

            }.onFailure { exception ->
                _errorMessage.value = "Erro ao buscar detalhes: ${exception.message}"
                onDialogDetalhesDismissed()
            }

            _isLoadingDetalhes.value = false
        }
    }

    private fun atualizarClassificacaoLocalmente(pontoId: String, novaClassificacao: String) {
        val listaAtual = _pontosPublicos.value ?: emptyList()
        val listaAtualizada = listaAtual.map { p ->
            if (p.id == pontoId) {
                p.copy(classificacao = novaClassificacao)
            } else {
                p
            }
        }
        _pontosPublicos.value = listaAtualizada
    }

    fun onDialogDetalhesDismissed() {
        _detalhesDoPonto.value = null
        _isLoadingDetalhes.value = false
    }
}