package br.iots.aqualab.ui.viewmodel.utils

import br.iots.aqualab.model.MedicaoManual
import br.iots.aqualab.model.LeituraSensor
import com.google.firebase.Timestamp
import java.util.Date

fun MedicaoManual.toLeituraSensor(): LeituraSensor {
    return LeituraSensor(
        pontoId = this.pontoIdNuvem,
        sensorId = this.parametro,
        valor = this.valor,
        timestamp = Timestamp(Date(this.timestamp))
    )
}