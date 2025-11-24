package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.iots.aqualab.model.Artigo
import br.iots.aqualab.repository.ArtigosRepository
import kotlinx.coroutines.launch

class ArtigosViewModel : ViewModel() {

    private val repository = ArtigosRepository()

    private val _listaDeArtigos = MutableLiveData<List<Artigo>>()
    val listaDeArtigos: LiveData<List<Artigo>> = _listaDeArtigos

    private val _eventoAbrirLink = MutableLiveData<String?>()
    val eventoAbrirLink: LiveData<String?> = _eventoAbrirLink

    private val queryPadrao = "(água OR saneamento OR \"recursos hídricos\" OR \"meio ambiente\" OR sustentabilidade) AND NOT (horóscopo OR signo)"

    init {
        buscarArtigos("")
    }

    fun buscarArtigos(query: String) {
        viewModelScope.launch {

            val termoFinal = if (query.isBlank()) {
                queryPadrao
            } else {
                "$query AND (água OR rio OR mar OR ambiente OR sustentabilidade)"
            }

            val resultado = repository.buscarNoticiasNaApi(termoFinal)
            _listaDeArtigos.value = resultado
        }
    }

    fun onArtigoClicado(artigo: Artigo) {
        _eventoAbrirLink.value = artigo.urlOrigem
    }

    fun onNavegacaoCompleta() {
        _eventoAbrirLink.value = null
    }
}