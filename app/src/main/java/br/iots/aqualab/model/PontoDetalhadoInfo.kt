package br.iots.aqualab.model

data class PontoDetalhadoInfo(
    val nomeEstacao: String,
    val condicoesAtuais: String,
    val temperatura: String,
    val umidade: String,
    val analiseQualidade: String,
    val dicaEducativa: String
)
