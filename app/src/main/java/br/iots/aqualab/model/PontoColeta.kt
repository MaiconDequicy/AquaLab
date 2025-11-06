package br.iots.aqualab.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class PontoColeta(
    @DocumentId val id: String = "",
    val userId: String = "",
    val nome: String = "",
    val tipo: String = "",
    val endereco: String = "",
    val status: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val localizacao: String = "",
    val pontoIdNuvem: String? = null
) : Parcelable
