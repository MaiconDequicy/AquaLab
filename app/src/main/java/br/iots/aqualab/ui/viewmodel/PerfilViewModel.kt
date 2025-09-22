package br.iots.aqualab.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.iots.aqualab.repository.AuthRepository

sealed class PerfilUIState {
    object Idle : PerfilUIState()
    object LogoutSuccess : PerfilUIState()
    data class Error(val message: String) : PerfilUIState()
}

class PerfilViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _perfilState = MutableLiveData<PerfilUIState>(PerfilUIState.Idle)
    val perfilState: LiveData<PerfilUIState> = _perfilState

    companion object {
        private const val TAG = "PerfilViewModel"
    }

    fun logout() {
        try {
            authRepository.logoutUser()
            _perfilState.value = PerfilUIState.LogoutSuccess
        } catch (e: Exception) {
            _perfilState.value = PerfilUIState.Error("Falha ao tentar sair: ${e.message}")
        }
    }
}

