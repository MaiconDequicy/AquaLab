package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.model.UserProfile
import br.iots.aqualab.repository.AuthRepository
import kotlinx.coroutines.launch

class InicioViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _welcomeMessage = MutableLiveData<String>()
    val welcomeMessage: LiveData<String> = _welcomeMessage

    fun loadUserProfile() {
        viewModelScope.launch {
            val result = authRepository.getLoggedInUserProfile()
            result.fold(
                onSuccess = { profile ->
                    _userProfile.value = profile
                    if (profile != null) {
                        val nameToDisplay = profile.displayName?.takeIf { it.isNotBlank() } ?: profile.email
                        _welcomeMessage.value = "Olá, ${nameToDisplay ?: "Usuário"}!"
                    } else {
                        _welcomeMessage.value = "Olá!"
                    }
                },
                onFailure = {
                    _userProfile.value = null
                    _welcomeMessage.value = "Olá!"
                }
            )
        }
    }
}

