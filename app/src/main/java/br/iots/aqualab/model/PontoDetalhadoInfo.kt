package br.iots.aqualab.model

data class PontoDetalhadoInfo(
    val id: String,
    val nomeEstacao: String,
    val condicoesAtuais: String,
    val temperatura: String,
    val umidade: String,
    val analiseQualidade: String,
    val dicaEducativa: String
)
