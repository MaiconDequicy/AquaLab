package br.iots.aqualab.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicoes_manuais")
data class MedicaoManual(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pontoIdNuvem: String,
    val parametro: String,
    val valor: Double,
    val timestamp: Long,
    val observacoes: String? = null,
    val sincronizado: Boolean = false
)