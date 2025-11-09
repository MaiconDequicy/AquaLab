package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.model.LeituraSensor
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.repository.PontoColetaRepository
import kotlinx.coroutines.launch

sealed class DashboardUIState {
    object Loading : DashboardUIState()
    data class Success(
        val pontoDestaque: PontoColeta,
        val leiturasRecentes: List<LeituraSensor>,
        val ultimaLeitura: LeituraSensor?
    ) : DashboardUIState()
    data class Error(val message: String) : DashboardUIState()
    object Empty : DashboardUIState()
}

class DashboardViewModel : ViewModel() {

    private val repository = PontoColetaRepository()

    private val _dashboardState = MutableLiveData<DashboardUIState>()
    val dashboardState: LiveData<DashboardUIState> = _dashboardState

    fun loadDashboardData() {
        _dashboardState.value = DashboardUIState.Loading
        viewModelScope.launch {
            try {
                val pontosColeta = repository.getPontosColeta() // Implementar essa função no seu repositório

                if (pontosColeta.isEmpty()) {
                    _dashboardState.postValue(DashboardUIState.Empty)
                    return@launch
                }

                val pontoDestaque = pontosColeta.first()

                val leituras = repository.getLeiturasRecentes(pontoDestaque.pontoIdNuvem, limit = 10)
                val ultimaLeitura = leituras.firstOrNull()

                _dashboardState.postValue(DashboardUIState.Success(pontoDestaque, leituras, ultimaLeitura))

            } catch (e: Exception) {
                _dashboardState.postValue(DashboardUIState.Error(e.message ?: "Erro desconhecido"))
            }
        }
    }
}