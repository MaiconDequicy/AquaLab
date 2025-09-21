package br.iots.aqualab.ui.viewmodel

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

    fun logout() {
        authRepository.logoutUser()
        _perfilState.value = PerfilUIState.LogoutSuccess
    }
}
