package br.iots.aqualab.ui.viewmodel // Ou seu pacote de ViewModels

import androidx.activity.result.launch
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.model.UserProfile // Já definido em LoginViewModel, pode ser movido
import br.iots.aqualab.repository.AuthRepository
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _registrationState = MutableLiveData<AuthUIState>(AuthUIState.Idle)
    val registrationState: LiveData<AuthUIState> = _registrationState

    fun register(email: String, pass: String, displayName: String) {
        if (email.isBlank() || pass.isBlank() || displayName.isBlank()) {
            _registrationState.value = AuthUIState.Error("Todos os campos são obrigatórios")
            return
        }
        if (pass.length < 6) { // Exemplo de validação básica
            _registrationState.value = AuthUIState.Error("A senha deve ter pelo menos 6 caracteres")
            return
        }

        _registrationState.value = AuthUIState.Loading
        viewModelScope.launch {
            val result = authRepository.registerUser(email, pass, displayName)
            result.fold(
                onSuccess = { userProfile ->
                    _registrationState.value = AuthUIState.Success(userProfile)
                },
                onFailure = { exception ->
                    _registrationState.value = AuthUIState.Error(exception.message ?: "Erro desconhecido no cadastro")
                }
            )
        }
    }
}