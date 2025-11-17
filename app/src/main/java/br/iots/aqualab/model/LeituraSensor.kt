package br.iots.aqualab.model

import com.google.firebase.Timestamp

data class LeituraSensor(
    val pontoId: String? = null,
    val sensorId: String? = null,
    val valor: Double? = null,
    val timestamp: Timestamp? = null
) {
    constructor() : this(null, null, null, null)
}