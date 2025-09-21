package br.iots.aqualab.ui.viewmodel // Ou seu pacote de ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.model.UserProfile
import br.iots.aqualab.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class AuthUIState {
    object Idle : AuthUIState() // Estado inicial ou após logout
    object Loading : AuthUIState()
    data class Success(val userProfile: UserProfile) : AuthUIState()
    data class Error(val message: String) : AuthUIState()
}

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _loginState = MutableLiveData<AuthUIState>(AuthUIState.Idle)
    val loginState: LiveData<AuthUIState> = _loginState

    fun checkIfUserIsLoggedIn() {
        _loginState.value = AuthUIState.Loading
        viewModelScope.launch {
            val result = authRepository.getLoggedInUserProfile()
            result.fold(
                onSuccess = { userProfile ->
                    if (userProfile != null) {
                        _loginState.value = AuthUIState.Success(userProfile)
                    } else {
                        _loginState.value = AuthUIState.Idle //Nenhum usuário logado
                    }
                },
                onFailure = { exception ->
                    _loginState.value = AuthUIState.Error("Sessão inválida ou perfil não encontrado")
                }
            )
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _loginState.value = AuthUIState.Error("Email e senha são obrigatórios")
            return
        }
        _loginState.value = AuthUIState.Loading
        viewModelScope.launch {
            val result = authRepository.loginUser(email, pass)
            result.fold(
                onSuccess = { userProfile ->
                    _loginState.value = AuthUIState.Success(userProfile)
                },
                onFailure = { exception ->
                    _loginState.value = AuthUIState.Error(exception.message ?: "Erro desconhecido no login")
                }
            )
        }
    }
}
