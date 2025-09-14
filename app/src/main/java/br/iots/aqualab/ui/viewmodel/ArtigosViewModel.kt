package br.iots.aqualab.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.iots.aqualab.model.Artigo

class ArtigosViewModel : ViewModel() {

    private val _listaDeArtigos = MutableLiveData<List<Artigo>>()
    val listaDeArtigos: LiveData<List<Artigo>> = _listaDeArtigos

    private val _eventoNavegarParaDetalhes = MutableLiveData<String?>()
    val eventoNavegarParaDetalhes: LiveData<String?> = _eventoNavegarParaDetalhes

    init {
        carregarArtigosMocados()
    }

    private fun carregarArtigosMocados() {
        val artigosMocados = listOf(
            Artigo(
                id = "1",
                titulo = "A Importância da Qualidade da Água",
                resumo = "Descubra por que monitorar a qualidade da água é crucial para a saúde e o meio ambiente.",
                urlImagem = "https://exemplo.com/imagem_agua.jpg" // Use uma URL real ou deixe null
            ),
            Artigo(
                id = "2",
                titulo = "Novas Tecnologias de Filtragem",
                resumo = "Conheça as últimas inovações em sistemas de filtragem para garantir água pura em sua casa.",
                urlImagem = null
            ),
            Artigo(
                id = "3",
                titulo = "O Impacto do Plástico nos Oceanos",
                resumo = "Um olhar aprofundado sobre como o descarte inadequado de plástico afeta a vida marinha.",
                urlImagem = "https://exemplo.com/imagem_oceano.jpg"
            ),
            Artigo(
                id = "4",
                titulo = "Dicas para Economizar Água no Dia a Dia",
                resumo = "Pequenas mudanças de hábitos que podem fazer uma grande diferença no consumo de água.",
                urlImagem = null
            )
        )
        _listaDeArtigos.value = artigosMocados
    }

    fun buscarArtigos(query: String) {

        if (query.isBlank()) {
            carregarArtigosMocados()
            return
        }
        val artigosFiltrados = _listaDeArtigos.value?.filter {
            it.titulo.contains(query, ignoreCase = true) ||
                    it.resumo.contains(query, ignoreCase = true)
        }
        _listaDeArtigos.value = artigosFiltrados ?: emptyList()
    }

    fun onArtigoClicado(artigo: Artigo) {
        _eventoNavegarParaDetalhes.value = artigo.id
    }

    fun onNavegacaoParaDetalhesCompleta() {
        _eventoNavegarParaDetalhes.value = null
    }
}
