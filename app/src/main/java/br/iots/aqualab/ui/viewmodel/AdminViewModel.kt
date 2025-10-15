package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.model.RequestStatus
import br.iots.aqualab.model.UserProfile
import br.iots.aqualab.model.UserRole
import br.iots.aqualab.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class AdminUIState {
    object Loading : AdminUIState()
    data class Success(val users: List<UserProfile>) : AdminUIState()
    data class Error(val message: String) : AdminUIState()
    object Idle : AdminUIState()
}

class AdminViewModel(private val authRepository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _adminState = MutableLiveData<AdminUIState>(AdminUIState.Idle)
    val adminState: LiveData<AdminUIState> = _adminState

    fun fetchPendingRequests() {
        viewModelScope.launch {
            _adminState.value = AdminUIState.Loading
            val result = authRepository.getPendingRoleRequests()
            result.fold(
                onSuccess = { users -> _adminState.value = AdminUIState.Success(users) },
                onFailure = { e -> _adminState.value = AdminUIState.Error(e.message ?: "Erro desconhecido") }
            )
        }
    }

    fun approveRequest(user: UserProfile) {
        viewModelScope.launch {
            val updatedProfile = user.copy(
                role = user.requestedRole ?: user.role,
                roleRequestStatus = RequestStatus.ACCEPTED,
                requestedRole = null
            )
            val result = authRepository.updateUserProfile(updatedProfile)
            result.onSuccess {
                fetchPendingRequests() // Refresh the list
            }
            result.onFailure {
                // Handle failure, maybe post an error to another LiveData
            }
        }
    }

    fun rejectRequest(user: UserProfile) {
        viewModelScope.launch {
            val updatedProfile = user.copy(
                roleRequestStatus = RequestStatus.REJECTED,
                requestedRole = null
            )
            val result = authRepository.updateUserProfile(updatedProfile)
            result.onSuccess {
                fetchPendingRequests() // Refresh the list
            }
            result.onFailure {
                // Handle failure
            }
        }
    }
}