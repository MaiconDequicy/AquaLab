package br.iots.aqualab.ui.components

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Formatador de valores do eixo X para exibir timestamps formatados
 */
class TimestampAxisFormatter(
    private val pattern: String = "HH:mm",
    private val timestamps: List<Long> = emptyList()
) : ValueFormatter() {

    private val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())

    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return try {
            // Se temos lista de timestamps, usar o índice
            if (timestamps.isNotEmpty()) {
                val index = value.toInt()
                if (index >= 0 && index < timestamps.size) {
                    dateFormat.format(Date(timestamps[index]))
                } else {
                    ""
                }
            } else {
                // Caso contrário, assumir que value é o timestamp em milissegundos
                dateFormat.format(Date(value.toLong()))
            }
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        /**
         * Cria um formatador para timestamps curtos (apenas hora:minuto)
         */
        fun short(timestamps: List<Long> = emptyList()): TimestampAxisFormatter {
            return TimestampAxisFormatter("HH:mm", timestamps)
        }

        /**
         * Cria um formatador para timestamps médios (dia/mês hora:minuto)
         */
        fun medium(timestamps: List<Long> = emptyList()): TimestampAxisFormatter {
            return TimestampAxisFormatter("dd/MM HH:mm", timestamps)
        }

        /**
         * Cria um formatador para timestamps longos (dia/mês/ano hora:minuto)
         */
        fun long(timestamps: List<Long> = emptyList()): TimestampAxisFormatter {
            return TimestampAxisFormatter("dd/MM/yyyy HH:mm", timestamps)
        }

        /**
         * Cria um formatador apenas com a data
         */
        fun dateOnly(timestamps: List<Long> = emptyList()): TimestampAxisFormatter {
            return TimestampAxisFormatter("dd/MM/yyyy", timestamps)
        }

        /**
         * Escolhe automaticamente o melhor formato baseado no intervalo de tempo
         */
        fun auto(timestamps: List<Long>): TimestampAxisFormatter {
            if (timestamps.isEmpty()) return short()

            val minTime = timestamps.minOrNull() ?: 0
            val maxTime = timestamps.maxOrNull() ?: 0
            val diffMillis = maxTime - minTime

            // Menos de 1 dia: mostrar apenas hora
            val oneDayMillis = 24 * 60 * 60 * 1000
            // Menos de 30 dias: mostrar dia/mês e hora
            val oneMonthMillis = 30L * oneDayMillis
            // Mais de 30 dias: mostrar data completa

            return when {
                diffMillis < oneDayMillis -> short(timestamps)
                diffMillis < oneMonthMillis -> medium(timestamps)
                else -> long(timestamps)
            }
        }
    }
}

/**
 * Formatador de valores do eixo Y para exibir valores de sensores formatados
 */
class SensorValueFormatter(
    private val unit: String = "",
    private val decimalPlaces: Int = 2
) : ValueFormatter() {

    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        val formatted = String.format("%.${decimalPlaces}f", value)
        return if (unit.isNotEmpty()) {
            "$formatted $unit"
        } else {
            formatted
        }
    }

    companion object {
        fun forPh(): SensorValueFormatter = SensorValueFormatter("", 1)
        fun forTemperature(): SensorValueFormatter = SensorValueFormatter("°C", 1)
        fun forTds(): SensorValueFormatter = SensorValueFormatter("ppm", 0)
        fun forTurbidity(): SensorValueFormatter = SensorValueFormatter("NTU", 2)
        fun forDissolvedOxygen(): SensorValueFormatter = SensorValueFormatter("mg/L", 2)
        fun forConductivity(): SensorValueFormatter = SensorValueFormatter("μS/cm", 0)
    }
}

/**
 * Classe de configuração para gráficos de sensores
 */
data class ChartConfig(
    val maxDataPoints: Int = 50,
    val showTimestampsOnXAxis: Boolean = true,
    val enableZoom: Boolean = true,
    val enablePinch: Boolean = true,
    val enableTouch: Boolean = true,
    val showLegend: Boolean = true,
    val showGridLines: Boolean = true,
    val animationDuration: Int = 700,
    val timestampFormat: String = "HH:mm"
) {
    companion object {
        /**
         * Configuração padrão para gráficos de detalhes de ponto de coleta
         */
        fun detailsDefault(): ChartConfig {
            return ChartConfig(
                maxDataPoints = 50,
                showTimestampsOnXAxis = true,
                enableZoom = true,
                enablePinch = true,
                enableTouch = true,
                showLegend = true,
                showGridLines = true,
                animationDuration = 700,
                timestampFormat = "dd/MM HH:mm"
            )
        }

        /**
         * Configuração para dashboard de pesquisador
         */
        fun dashboardDefault(): ChartConfig {
            return ChartConfig(
                maxDataPoints = 30,
                showTimestampsOnXAxis = true,
                enableZoom = false,
                enablePinch = false,
                enableTouch = true,
                showLegend = true,
                showGridLines = true,
                animationDuration = 700,
                timestampFormat = "HH:mm"
            )
        }

        /**
         * Configuração para visualização compacta
         */
        fun compact(): ChartConfig {
            return ChartConfig(
                maxDataPoints = 20,
                showTimestampsOnXAxis = false,
                enableZoom = false,
                enablePinch = false,
                enableTouch = false,
                showLegend = false,
                showGridLines = false,
                animationDuration = 500,
                timestampFormat = "HH:mm"
            )
        }
    }
}

/**
 * Classe para armazenar filtros de dados de gráfico
 */
data class ChartDataFilter(
    val startDate: Date? = null,
    val endDate: Date? = null,
    val maxSamples: Int = 50,
    val sensorTypes: List<String> = emptyList() // IDs dos sensores a mostrar
) {
    /**
     * Verifica se há algum filtro de data aplicado
     */
    fun hasDateFilter(): Boolean = startDate != null || endDate != null

    /**
     * Verifica se há filtro de tipos de sensores
     */
    fun hasSensorFilter(): Boolean = sensorTypes.isNotEmpty()

    /**
     * Verifica se um timestamp está dentro do range de datas
     */
    fun isInDateRange(timestamp: Long): Boolean {
        val date = Date(timestamp)

        val afterStart = startDate?.let { date.time >= it.time } ?: true
        val beforeEnd = endDate?.let { date.time <= it.time } ?: true

        return afterStart && beforeEnd
    }

    companion object {
        /**
         * Filtro padrão sem restrições
         */
        fun default(): ChartDataFilter {
            return ChartDataFilter(
                startDate = null,
                endDate = null,
                maxSamples = 50,
                sensorTypes = emptyList()
            )
        }

        /**
         * Filtro para últimas 24 horas
         */
        fun last24Hours(maxSamples: Int = 50): ChartDataFilter {
            val now = Date()
            val yesterday = Date(now.time - 24 * 60 * 60 * 1000)
            return ChartDataFilter(
                startDate = yesterday,
                endDate = now,
                maxSamples = maxSamples,
                sensorTypes = emptyList()
            )
        }

        /**
         * Filtro para última semana
         */
        fun lastWeek(maxSamples: Int = 100): ChartDataFilter {
            val now = Date()
            val lastWeek = Date(now.time - 7 * 24 * 60 * 60 * 1000)
            return ChartDataFilter(
                startDate = lastWeek,
                endDate = now,
                maxSamples = maxSamples,
                sensorTypes = emptyList()
            )
        }

        /**
         * Filtro para último mês
         */
        fun lastMonth(maxSamples: Int = 200): ChartDataFilter {
            val now = Date()
            val lastMonth = Date(now.time - 30L * 24 * 60 * 60 * 1000)
            return ChartDataFilter(
                startDate = lastMonth,
                endDate = now,
                maxSamples = maxSamples,
                sensorTypes = emptyList()
            )
        }
    }
}
