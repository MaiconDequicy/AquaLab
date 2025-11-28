package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.data.dao.MedicaoDao
import br.iots.aqualab.model.LeituraSensor
import br.iots.aqualab.model.MedicaoManual
import br.iots.aqualab.repository.PontoColetaRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date

class DetalhesPontoColetaViewModel(
    private val medicaoDao: MedicaoDao
) : ViewModel() {

    private val repository = PontoColetaRepository()

    private val _leiturasNuvemFlow = MutableStateFlow<List<LeituraSensor>>(emptyList())

    private val _leituras = MutableLiveData<List<LeituraSensor>>()
    val leituras: LiveData<List<LeituraSensor>> = _leituras

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _ultimaLeitura = MutableLiveData<LeituraSensor?>()
    val ultimaLeitura: LiveData<LeituraSensor?> = _ultimaLeitura

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun carregarLeituras(pontoId: String?) {
        if (pontoId.isNullOrEmpty()) {
            _errorMessage.value = "ID inválido."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            launch {
                try {
                    val listaNuvem = repository.getLeiturasRecentes(pontoId, limit = 50)
                    _leiturasNuvemFlow.value = listaNuvem
                } catch (e: Exception) {
                } finally {
                    _isLoading.value = false
                }
            }

            combine(
                _leiturasNuvemFlow,
                medicaoDao.getMedicoesPorPonto(pontoId)
            ) { nuvem, local ->

                val localConvertido = local.map { it.toLeituraSensor() }

                val listaCompleta = nuvem + localConvertido

                listaCompleta.sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }

            }.collectLatest { listaFinal ->
                _leituras.value = listaFinal
                _ultimaLeitura.value = listaFinal.firstOrNull()
            }
        }
    }

    private fun MedicaoManual.toLeituraSensor(): LeituraSensor {
        return LeituraSensor(
            pontoId = this.pontoIdNuvem,
            sensorId = this.parametro,
            valor = this.valor,
            timestamp = Timestamp(Date(this.timestamp))
        )
    }
}

class DetalhesViewModelFactory(
    private val medicaoDao: MedicaoDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetalhesPontoColetaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetalhesPontoColetaViewModel(medicaoDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}