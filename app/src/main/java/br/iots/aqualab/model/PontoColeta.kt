package br.iots.aqualab.model

import com.google.firebase.firestore.DocumentId

data class PontoColeta(
    @DocumentId val id: String = "",
    val userId: String = "",
    val nome: String = "",
    val tipo: String = "",
    val endereco: String = "",
    val status: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val localizacao: String = "", // <-- Valor padrão adicionado
    val pontoIdNuvem: String? = null // <-- Valor padrão adicionado
)
