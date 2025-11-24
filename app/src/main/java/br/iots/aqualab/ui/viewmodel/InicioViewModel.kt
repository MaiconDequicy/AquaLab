package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.model.Artigo
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.model.UserProfile
import br.iots.aqualab.repository.ArtigosRepository
import br.iots.aqualab.repository.AuthRepository
import br.iots.aqualab.repository.PontoColetaRepository
import kotlinx.coroutines.launch

class InicioViewModel(
    private val authRepository: AuthRepository = AuthRepository()

) : ViewModel() {

    private val artigosRepository = ArtigosRepository()
    private val pontosRepository = PontoColetaRepository()
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _welcomeMessage = MutableLiveData<String>()
    val welcomeMessage: LiveData<String> = _welcomeMessage

    private val _artigosRecentes = MutableLiveData<List<Artigo>>()
    val artigosRecentes: LiveData<List<Artigo>> = _artigosRecentes

    private val _pontosMapaHome = MutableLiveData<List<PontoColeta>>()
    val pontosMapaHome: LiveData<List<PontoColeta>> = _pontosMapaHome

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

    fun carregarArtigosRecentes() {
        viewModelScope.launch {
            val listaCompleta = artigosRepository.buscarNoticiasNaApi("água saneamento meio ambiente")
            _artigosRecentes.value = listaCompleta.take(2)
        }
    }

    fun carregarPontosDoMapa() {
        viewModelScope.launch {
            val result = pontosRepository.getPontosPublicos()

            result.onSuccess { listaDePontos ->
                _pontosMapaHome.value = listaDePontos
            }

            result.onFailure { exception ->
                exception.printStackTrace()
            }
        }
    }
}