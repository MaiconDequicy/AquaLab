package br.iots.aqualab.repository

import android.util.Log
import br.iots.aqualab.model.Artigo
import br.iots.aqualab.network.RetrofitInstance

class ArtigosRepository {

    suspend fun buscarNoticiasNaApi(termo: String): List<Artigo> {
        return try {
            val response = RetrofitInstance.api.getNoticias(query = termo)

            if (response.isSuccessful && response.body() != null) {
                val listaDtos = response.body()!!.articles

                listaDtos.map { dto ->
                    Artigo(
                        id = dto.url ?: System.currentTimeMillis().toString(),
                        titulo = dto.title ?: "Sem título disponível",
                        resumo = dto.description ?: "Toque em 'Leia Mais' para ver os detalhes.",
                        urlImagem = dto.urlToImage,
                        urlOrigem = dto.url ?: ""
                    )
                }.filter {
                    it.urlOrigem.isNotEmpty() && it.titulo != "[Removed]"
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ArtigosRepo", "Erro: ${e.message}")
            emptyList()
        }
    }
}