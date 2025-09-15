package br.iots.aqualab.model

data class Artigo(
    val id: String,
    val titulo: String,
    val resumo: String,
    val urlImagem: String? = null
)