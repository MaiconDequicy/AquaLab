package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.model.UserProfile
import br.iots.aqualab.repository.AuthRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val authRepository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val result = authRepository.getLoggedInUserProfile()
            result.onSuccess { profile ->
                _userProfile.postValue(profile)
            }
            result.onFailure {
                _userProfile.postValue(null)
            }
        }
    }
}