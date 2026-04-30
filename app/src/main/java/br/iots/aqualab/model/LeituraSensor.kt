package br.iots.aqualab.model

import br.iots.aqualab.constants.WaterQualityConstants
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modelo de dados para leitura de sensor.
 * Representa uma medição única de um sensor em um ponto de coleta específico.
 *
 * @property pontoId ID do ponto de coleta (corresponde ao pontoIdNuvem)
 * @property sensorId Tipo de sensor (ph, temperatura, tds, turbidez, od, condutividade)
 * @property valor Valor medido pelo sensor
 * @property timestamp Momento da medição
 */
data class LeituraSensor(
    val pontoId: String? = null,
    val sensorId: String? = null,
    val valor: Double? = null,
    val timestamp: Timestamp? = null
) {
    constructor() : this(null, null, null, null)

    /**
     * Obtém o tipo de sensor correspondente usando a classe de constantes
     */
    val sensorType: WaterQualityConstants.SensorType?
        get() = sensorId?.let { WaterQualityConstants.SensorType.fromId(it) }

    /**
     * Verifica se a leitura possui dados válidos
     */
    val isValid: Boolean
        get() = pontoId != null &&
                sensorId != null &&
                valor != null &&
                !valor.isNaN() &&
                timestamp != null

    /**
     * Verifica se o valor está dentro do range aceitável para o tipo de sensor
     */
    val isValueInValidRange: Boolean
        get() = sensorType?.let {
            WaterQualityConstants.isValueValid(it, valor)
        } ?: false

    /**
     * Obtém o nível de qualidade baseado no valor da leitura
     */
    val qualityLevel: WaterQualityConstants.QualityLevel
        get() = sensorType?.let {
            WaterQualityConstants.getQualityForSensor(it, valor)
        } ?: WaterQualityConstants.QualityLevel.INDISPONIVEL

    /**
     * Obtém a cor apropriada para o valor da leitura
     */
    val valueColor: Int
        get() = sensorType?.let {
            WaterQualityConstants.getColorForSensor(it, valor)
        } ?: WaterQualityConstants.QualityLevel.INDISPONIVEL.color

    /**
     * Formata o valor com a unidade apropriada
     */
    fun getFormattedValue(): String {
        return sensorType?.let {
            WaterQualityConstants.formatSensorValue(it, valor)
        } ?: "N/A"
    }

    /**
     * Formata o timestamp para exibição
     */
    fun getFormattedTimestamp(pattern: String = "dd/MM/yyyy HH:mm"): String {
        return timestamp?.toDate()?.let {
            SimpleDateFormat(pattern, Locale.getDefault()).format(it)
        } ?: "N/A"
    }

    /**
     * Obtém o timestamp em milissegundos (útil para gráficos)
     */
    fun getTimestampMillis(): Long {
        return timestamp?.toDate()?.time ?: 0L
    }

    /**
     * Retorna uma descrição legível da leitura
     */
    fun getDescription(): String {
        val sensorName = sensorType?.displayName ?: sensorId ?: "Desconhecido"
        val formattedValue = getFormattedValue()
        val formattedTime = getFormattedTimestamp()
        return "$sensorName: $formattedValue em $formattedTime"
    }

    companion object {
        /**
         * Agrupa leituras por tipo de sensor
         */
        fun groupBySensorType(leituras: List<LeituraSensor>): Map<WaterQualityConstants.SensorType, List<LeituraSensor>> {
            return leituras
                .filter { it.isValid }
                .mapNotNull { leitura ->
                    leitura.sensorType?.let { type -> type to leitura }
                }
                .groupBy({ it.first }, { it.second })
        }

        /**
         * Filtra leituras por tipo de sensor
         */
        fun filterBySensorType(
            leituras: List<LeituraSensor>,
            sensorType: WaterQualityConstants.SensorType
        ): List<LeituraSensor> {
            return leituras.filter { it.sensorType == sensorType && it.isValid }
        }

        /**
         * Obtém a última leitura de cada tipo de sensor
         */
        fun getLatestReadings(leituras: List<LeituraSensor>): Map<WaterQualityConstants.SensorType, LeituraSensor> {
            return groupBySensorType(leituras).mapValues { (_, readings) ->
                readings.maxByOrNull { it.timestamp?.toDate()?.time ?: 0L }!!
            }
        }

        /**
         * Calcula estatísticas básicas para um conjunto de leituras
         */
        fun calculateStats(leituras: List<LeituraSensor>): SensorStats? {
            val validValues = leituras.mapNotNull { it.valor }.filter { !it.isNaN() }
            if (validValues.isEmpty()) return null

            return SensorStats(
                min = validValues.minOrNull() ?: 0.0,
                max = validValues.maxOrNull() ?: 0.0,
                average = validValues.average(),
                count = validValues.size
            )
        }
    }
}

/**
 * Classe para estatísticas de leituras de sensor
 */
data class SensorStats(
    val min: Double,
    val max: Double,
    val average: Double,
    val count: Int
) {
    fun getFormattedAverage(): String = String.format("%.2f", average)
    fun getFormattedMin(): String = String.format("%.2f", min)
    fun getFormattedMax(): String = String.format("%.2f", max)
}