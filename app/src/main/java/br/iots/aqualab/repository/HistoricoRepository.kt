package br.iots.aqualab.repository

import br.iots.aqualab.model.LeituraSensor
import com.google.firebase.Timestamp
import java.util.*

class HistoricoRepository {

    suspend fun buscarLeiturasPorPeriodo(pontoId: String, diasParaTras: Int): List<LeituraSensor> {
        return gerarMock()
    }

    private fun gerarMock(): List<LeituraSensor> {
        val lista = mutableListOf<LeituraSensor>()
        val agora = System.currentTimeMillis()

        for (i in 0 until 30) {
            val ts = Timestamp(Date(agora - (30 - i) * 3600_000L)) // 1h entre pontos

            lista.add(
                LeituraSensor(
                    sensorId = "ph",
                    valor = 6.5 + Math.random() * 2.5,
                    timestamp = ts
                )
            )

            lista.add(
                LeituraSensor(
                    sensorId = "temperatura",
                    valor = 22 + Math.random() * 5,
                    timestamp = ts
                )
            )
        }

        return lista
    }
}
