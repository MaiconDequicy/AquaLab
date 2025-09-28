package br.iots.aqualab.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.model.UserProfile
import br.iots.aqualab.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class PerfilUIState
{
    object Idle : PerfilUIState()
    object LogoutSuccess : PerfilUIState()
    data class Error(val message: String) : PerfilUIState()
    data class UserProfileLoaded(val userProfile: UserProfile) : PerfilUIState()
}

class PerfilViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _perfilState = MutableLiveData<PerfilUIState>(PerfilUIState.Idle)
    val perfilState: LiveData<PerfilUIState> = _perfilState

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    companion object {
        private const val TAG = "PerfilViewModel"
    }

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            Log.d(TAG, "loadUserProfile: Carregando perfil do usuário...")
            val result = authRepository.getLoggedInUserProfile()
            result.fold(
                onSuccess = { profile ->
                    if (profile != null) {
                        Log.d(TAG, "loadUserProfile: Perfil carregado com sucesso: $profile")
                        _userProfile.value = profile
                    } else {
                        Log.d(TAG, "loadUserProfile: Nenhum perfil de usuário logado encontrado.")
                        _perfilState.value = PerfilUIState.Error("Usuário não encontrado.")
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "loadUserProfile: Falha ao carregar perfil.", exception)
                    _perfilState.value = PerfilUIState.Error("Falha ao carregar perfil: ${exception.message}")
                }
            )
        }
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

