package br.iots.aqualab.ui.viewmodel

import android.net.Uri
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
    object Loading : PerfilUIState()
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


    fun uploadProfileImage(imageUri: Uri) {
        viewModelScope.launch {
            _perfilState.value = PerfilUIState.Loading
            Log.d(TAG, "uploadProfileImage: Iniciando upload da imagem: $imageUri")
            try {
                val downloadUrlResult = authRepository.uploadProfileImage(imageUri)

                downloadUrlResult.fold(
                    onSuccess = { downloadUrl ->
                        Log.d(TAG, "uploadProfileImage: Imagem enviada com sucesso. URL: $downloadUrl")
                        val currentUserProfile = _userProfile.value
                        if (currentUserProfile != null) {
                            val updatedProfile = currentUserProfile.copy(photoUrl = downloadUrl)
                            val updateResult = authRepository.updateUserProfile(updatedProfile) // Ou um método específico como updateUserProfilePhotoUrl

                            updateResult.fold(
                                onSuccess = {
                                    Log.d(TAG, "uploadProfileImage: URL da foto do perfil atualizada com sucesso no banco de dados.")
                                    _userProfile.value = updatedProfile
                                    _perfilState.value = PerfilUIState.UserProfileLoaded(updatedProfile)
                                },
                                onFailure = { exception ->
                                    Log.e(TAG, "uploadProfileImage: Falha ao atualizar URL da foto no perfil.", exception)
                                    _perfilState.value = PerfilUIState.Error("Falha ao atualizar a foto do perfil: ${exception.message}")
                                }
                            )
                        } else {
                            Log.e(TAG, "uploadProfileImage: Perfil do usuário é nulo, não é possível atualizar a URL da foto.")
                            _perfilState.value = PerfilUIState.Error("Usuário não encontrado para atualizar a foto.")
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "uploadProfileImage: Falha no upload da imagem.", exception)
                        _perfilState.value = PerfilUIState.Error("Falha no upload da imagem: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "uploadProfileImage: Exceção durante o processo de upload.", e)
                _perfilState.value = PerfilUIState.Error("Erro ao enviar imagem: ${e.message}")
            }
        }
    }
}