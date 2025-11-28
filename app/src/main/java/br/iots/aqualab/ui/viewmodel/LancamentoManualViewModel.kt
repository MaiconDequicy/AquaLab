package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.data.dao.MedicaoDao
import br.iots.aqualab.model.MedicaoManual
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LancamentoManualViewModel(
    private val medicaoDao: MedicaoDao
) : ViewModel() {

    private val _salvamentoSucesso = MutableSharedFlow<Boolean>()
    val salvamentoSucesso = _salvamentoSucesso.asSharedFlow()

    private val _mensagemErro = MutableSharedFlow<String>()
    val mensagemErro = _mensagemErro.asSharedFlow()

    fun salvarMedicao(
        pontoId: String,
        parametro: String,
        valorTexto: String,
        dataSelecionada: Long,
        local: String,
        obs: String
    ) {
        viewModelScope.launch {
            if (valorTexto.isBlank()) {
                _mensagemErro.emit("Por favor, informe o valor da medição.")
                return@launch
            }

            try {
                val valorDouble = valorTexto.replace(",", ".").toDouble()

                val novaMedicao = MedicaoManual(
                    pontoIdNuvem = pontoId,
                    parametro = parametro,
                    valor = valorDouble,
                    timestamp = dataSelecionada,
                    observacoes = obs
                )

                medicaoDao.inserir(novaMedicao)
                _salvamentoSucesso.emit(true)

            } catch (e: NumberFormatException) {
                _mensagemErro.emit("Valor inválido. Use apenas números.")
            } catch (e: Exception) {
                _mensagemErro.emit("Erro ao salvar: ${e.message}")
            }
        }
    }
}

class LancamentoViewModelFactory(private val dao: MedicaoDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LancamentoManualViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LancamentoManualViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}