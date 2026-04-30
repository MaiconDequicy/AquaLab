package br.iots.aqualab.constants

import android.graphics.Color

/**
 * Classe estática que define constantes para análise de qualidade da água.
 * Contém valores de referência, limiares e cores para diferentes parâmetros de sensores.
 *
 * Referências:
 * - CONAMA (Conselho Nacional do Meio Ambiente)
 * - WHO (World Health Organization) - Diretrizes de Qualidade da Água
 * - Portaria MS nº 888/2021 (Padrões de Potabilidade do Brasil)
 */
object WaterQualityConstants {

    // ==================== CLASSIFICAÇÕES DE QUALIDADE ====================

    enum class QualityLevel(val label: String, val color: Int, val priority: Int) {
        OTIMA("Ótima", Color.parseColor("#2196F3"), 5),      // Azul
        BOA("Boa", Color.parseColor("#4CAF50"), 4),          // Verde
        REGULAR("Regular", Color.parseColor("#FFEB3B"), 3),  // Amarelo
        RUIM("Ruim", Color.parseColor("#FF9800"), 2),        // Laranja
        PESSIMA("Péssima", Color.parseColor("#F44336"), 1),  // Vermelho
        INDISPONIVEL("Indisponível", Color.parseColor("#9C27B0"), 0); // Roxo

        companion object {
            fun fromString(label: String): QualityLevel {
                return values().find { it.label.equals(label, ignoreCase = true) } ?: INDISPONIVEL
            }
        }
    }

    // ==================== TIPOS DE SENSORES ====================

    enum class SensorType(
        val id: String,
        val displayName: String,
        val unit: String,
        val chartColor: Int,
        val description: String
    ) {
        PH(
            id = "ph",
            displayName = "pH",
            unit = "",
            chartColor = Color.parseColor("#1E88E5"), // Azul
            description = "Potencial Hidrogeniônico - mede acidez/alcalinidade da água"
        ),
        TEMPERATURA(
            id = "temperatura",
            displayName = "Temperatura",
            unit = "°C",
            chartColor = Color.parseColor("#E53935"), // Vermelho
            description = "Temperatura da água em graus Celsius"
        ),
        TDS(
            id = "tds",
            displayName = "TDS",
            unit = "ppm",
            chartColor = Color.parseColor("#8E24AA"), // Roxo
            description = "Sólidos Dissolvidos Totais - mede impurezas dissolvidas"
        ),
        TURBIDEZ(
            id = "turbidez",
            displayName = "Turbidez",
            unit = "NTU",
            chartColor = Color.parseColor("#43A047"), // Verde
            description = "Medida de transparência/claridade da água"
        ),
        OXIGENIO_DISSOLVIDO(
            id = "od",
            displayName = "OD",
            unit = "mg/L",
            chartColor = Color.parseColor("#00ACC1"), // Ciano
            description = "Oxigênio Dissolvido - essencial para vida aquática"
        ),
        CONDUTIVIDADE(
            id = "condutividade",
            displayName = "Condutividade",
            unit = "μS/cm",
            chartColor = Color.parseColor("#FB8C00"), // Laranja
            description = "Capacidade da água de conduzir corrente elétrica"
        );

        companion object {
            fun fromId(id: String): SensorType? {
                return values().find { it.id.equals(id, ignoreCase = true) }
            }

            fun getAllSensorIds(): List<String> {
                return values().map { it.id }
            }

            fun getChartColors(): List<Int> {
                return values().map { it.chartColor }
            }
        }
    }

    // ==================== PARÂMETROS DE pH ====================

    object PhParameters {
        const val IDEAL_MIN = 6.5
        const val IDEAL_MAX = 8.5

        const val EXCELLENT_MIN = 7.0
        const val EXCELLENT_MAX = 7.8

        const val GOOD_MIN = 6.5
        const val GOOD_MAX = 8.5

        const val REGULAR_MIN = 6.0
        const val REGULAR_MAX = 9.0

        const val POOR_MIN = 5.5
        const val POOR_MAX = 9.5

        fun getQualityLevel(ph: Double): QualityLevel {
            return when {
                ph.isNaN() || ph <= 0 -> QualityLevel.INDISPONIVEL
                ph in EXCELLENT_MIN..EXCELLENT_MAX -> QualityLevel.OTIMA
                ph in GOOD_MIN..GOOD_MAX -> QualityLevel.BOA
                ph in REGULAR_MIN..REGULAR_MAX -> QualityLevel.REGULAR
                ph in POOR_MIN..POOR_MAX -> QualityLevel.RUIM
                else -> QualityLevel.PESSIMA
            }
        }

        fun getColorForValue(ph: Double): Int {
            return getQualityLevel(ph).color
        }
    }

    // ==================== PARÂMETROS DE TEMPERATURA ====================

    object TemperatureParameters {
        const val IDEAL_MIN = 20.0  // °C
        const val IDEAL_MAX = 30.0  // °C

        const val EXCELLENT_MIN = 22.0
        const val EXCELLENT_MAX = 28.0

        const val GOOD_MIN = 18.0
        const val GOOD_MAX = 32.0

        const val REGULAR_MIN = 15.0
        const val REGULAR_MAX = 35.0

        const val POOR_MIN = 10.0
        const val POOR_MAX = 38.0

        fun getQualityLevel(temperature: Double): QualityLevel {
            return when {
                temperature.isNaN() || temperature <= -50 || temperature >= 100 -> QualityLevel.INDISPONIVEL
                temperature in EXCELLENT_MIN..EXCELLENT_MAX -> QualityLevel.OTIMA
                temperature in GOOD_MIN..GOOD_MAX -> QualityLevel.BOA
                temperature in REGULAR_MIN..REGULAR_MAX -> QualityLevel.REGULAR
                temperature in POOR_MIN..POOR_MAX -> QualityLevel.RUIM
                else -> QualityLevel.PESSIMA
            }
        }

        fun getColorForValue(temperature: Double): Int {
            return getQualityLevel(temperature).color
        }
    }

    // ==================== PARÂMETROS DE TDS (Total Dissolved Solids) ====================

    object TdsParameters {
        const val IDEAL_MIN = 0.0    // ppm
        const val IDEAL_MAX = 300.0  // ppm

        const val EXCELLENT_MAX = 150.0  // Água excelente
        const val GOOD_MAX = 300.0       // Água boa para consumo
        const val REGULAR_MAX = 600.0    // Aceitável
        const val POOR_MAX = 900.0       // Ruim
        // Acima de 900 ppm: Péssima

        fun getQualityLevel(tds: Double): QualityLevel {
            return when {
                tds.isNaN() || tds < 0 -> QualityLevel.INDISPONIVEL
                tds <= EXCELLENT_MAX -> QualityLevel.OTIMA
                tds <= GOOD_MAX -> QualityLevel.BOA
                tds <= REGULAR_MAX -> QualityLevel.REGULAR
                tds <= POOR_MAX -> QualityLevel.RUIM
                else -> QualityLevel.PESSIMA
            }
        }

        fun getColorForValue(tds: Double): Int {
            return getQualityLevel(tds).color
        }

        fun getDescription(tds: Double): String {
            return when {
                tds < 0 -> "Valor inválido"
                tds <= 150 -> "Excelente - Água muito pura"
                tds <= 300 -> "Boa - Ideal para consumo"
                tds <= 600 -> "Regular - Aceitável"
                tds <= 900 -> "Ruim - Alta concentração de sólidos"
                else -> "Péssima - Água imprópria"
            }
        }
    }

    // ==================== PARÂMETROS DE TURBIDEZ ====================

    object TurbidityParameters {
        const val IDEAL_MIN = 0.0   // NTU
        const val IDEAL_MAX = 5.0   // NTU

        const val EXCELLENT_MAX = 1.0   // Água cristalina
        const val GOOD_MAX = 5.0        // Aceitável (padrão potabilidade)
        const val REGULAR_MAX = 10.0    // Visível mas aceitável
        const val POOR_MAX = 40.0       // Muito turva
        // Acima de 40 NTU: Péssima

        fun getQualityLevel(turbidity: Double): QualityLevel {
            return when {
                turbidity.isNaN() || turbidity < 0 -> QualityLevel.INDISPONIVEL
                turbidity <= EXCELLENT_MAX -> QualityLevel.OTIMA
                turbidity <= GOOD_MAX -> QualityLevel.BOA
                turbidity <= REGULAR_MAX -> QualityLevel.REGULAR
                turbidity <= POOR_MAX -> QualityLevel.RUIM
                else -> QualityLevel.PESSIMA
            }
        }

        fun getColorForValue(turbidity: Double): Int {
            return getQualityLevel(turbidity).color
        }

        fun getDescription(turbidity: Double): String {
            return when {
                turbidity < 0 -> "Valor inválido"
                turbidity <= 1 -> "Excelente - Água cristalina"
                turbidity <= 5 -> "Boa - Dentro do padrão"
                turbidity <= 10 -> "Regular - Levemente turva"
                turbidity <= 40 -> "Ruim - Muito turva"
                else -> "Péssima - Água muito turva e imprópria"
            }
        }
    }

    // ==================== PARÂMETROS DE OXIGÊNIO DISSOLVIDO ====================

    object DissolvedOxygenParameters {
        const val IDEAL_MIN = 5.0   // mg/L
        const val IDEAL_MAX = 14.0  // mg/L

        const val EXCELLENT_MIN = 7.0   // Excelente para vida aquática
        const val GOOD_MIN = 5.0        // Bom (mínimo CONAMA)
        const val REGULAR_MIN = 4.0     // Regular
        const val POOR_MIN = 2.0        // Ruim
        // Abaixo de 2 mg/L: Péssimo

        fun getQualityLevel(od: Double): QualityLevel {
            return when {
                od.isNaN() || od < 0 -> QualityLevel.INDISPONIVEL
                od >= EXCELLENT_MIN -> QualityLevel.OTIMA
                od >= GOOD_MIN -> QualityLevel.BOA
                od >= REGULAR_MIN -> QualityLevel.REGULAR
                od >= POOR_MIN -> QualityLevel.RUIM
                else -> QualityLevel.PESSIMA
            }
        }

        fun getColorForValue(od: Double): Int {
            return getQualityLevel(od).color
        }
    }

    // ==================== PARÂMETROS DE CONDUTIVIDADE ====================

    object ConductivityParameters {
        const val IDEAL_MIN = 50.0    // μS/cm
        const val IDEAL_MAX = 500.0   // μS/cm

        const val EXCELLENT_MAX = 250.0   // Água pura
        const val GOOD_MAX = 500.0        // Boa qualidade
        const val REGULAR_MAX = 1000.0    // Aceitável
        const val POOR_MAX = 2000.0       // Alta condutividade
        // Acima de 2000 μS/cm: Péssima

        fun getQualityLevel(conductivity: Double): QualityLevel {
            return when {
                conductivity.isNaN() || conductivity < 0 -> QualityLevel.INDISPONIVEL
                conductivity <= EXCELLENT_MAX -> QualityLevel.OTIMA
                conductivity <= GOOD_MAX -> QualityLevel.BOA
                conductivity <= REGULAR_MAX -> QualityLevel.REGULAR
                conductivity <= POOR_MAX -> QualityLevel.RUIM
                else -> QualityLevel.PESSIMA
            }
        }

        fun getColorForValue(conductivity: Double): Int {
            return getQualityLevel(conductivity).color
        }
    }

    // ==================== MÉTODOS UTILITÁRIOS ====================

    /**
     * Obtém o nível de qualidade para um sensor específico baseado em seu valor
     */
    fun getQualityForSensor(sensorType: SensorType, value: Double?): QualityLevel {
        if (value == null || value.isNaN()) return QualityLevel.INDISPONIVEL

        return when (sensorType) {
            SensorType.PH -> PhParameters.getQualityLevel(value)
            SensorType.TEMPERATURA -> TemperatureParameters.getQualityLevel(value)
            SensorType.TDS -> TdsParameters.getQualityLevel(value)
            SensorType.TURBIDEZ -> TurbidityParameters.getQualityLevel(value)
            SensorType.OXIGENIO_DISSOLVIDO -> DissolvedOxygenParameters.getQualityLevel(value)
            SensorType.CONDUTIVIDADE -> ConductivityParameters.getQualityLevel(value)
        }
    }

    /**
     * Obtém a cor para um valor de sensor específico
     */
    fun getColorForSensor(sensorType: SensorType, value: Double?): Int {
        if (value == null || value.isNaN()) return QualityLevel.INDISPONIVEL.color

        return when (sensorType) {
            SensorType.PH -> PhParameters.getColorForValue(value)
            SensorType.TEMPERATURA -> TemperatureParameters.getColorForValue(value)
            SensorType.TDS -> TdsParameters.getColorForValue(value)
            SensorType.TURBIDEZ -> TurbidityParameters.getColorForValue(value)
            SensorType.OXIGENIO_DISSOLVIDO -> DissolvedOxygenParameters.getColorForValue(value)
            SensorType.CONDUTIVIDADE -> ConductivityParameters.getColorForValue(value)
        }
    }

    /**
     * Calcula a qualidade geral baseada em múltiplos sensores
     * Usa o nível de qualidade mais baixo (pior) como referência
     */
    fun calculateOverallQuality(sensorReadings: Map<SensorType, Double?>): QualityLevel {
        val qualities = sensorReadings.mapNotNull { (type, value) ->
            value?.let { getQualityForSensor(type, it) }
        }

        if (qualities.isEmpty()) return QualityLevel.INDISPONIVEL

        // Retorna o pior nível de qualidade encontrado
        return qualities.minByOrNull { it.priority } ?: QualityLevel.INDISPONIVEL
    }

    /**
     * Formata o valor do sensor com a unidade apropriada
     */
    fun formatSensorValue(sensorType: SensorType, value: Double?): String {
        if (value == null || value.isNaN()) return "N/A"

        val formattedValue = when (sensorType) {
            SensorType.PH -> String.format("%.2f", value)
            SensorType.TEMPERATURA -> String.format("%.1f", value)
            SensorType.TDS -> String.format("%.0f", value)
            SensorType.TURBIDEZ -> String.format("%.2f", value)
            SensorType.OXIGENIO_DISSOLVIDO -> String.format("%.2f", value)
            SensorType.CONDUTIVIDADE -> String.format("%.0f", value)
        }

        return if (sensorType.unit.isNotEmpty()) {
            "$formattedValue ${sensorType.unit}"
        } else {
            formattedValue
        }
    }

    /**
     * Valida se um valor de sensor está dentro de um range aceitável
     */
    fun isValueValid(sensorType: SensorType, value: Double?): Boolean {
        if (value == null || value.isNaN()) return false

        return when (sensorType) {
            SensorType.PH -> value in 0.0..14.0
            SensorType.TEMPERATURA -> value in -10.0..50.0
            SensorType.TDS -> value >= 0.0 && value <= 5000.0
            SensorType.TURBIDEZ -> value >= 0.0 && value <= 1000.0
            SensorType.OXIGENIO_DISSOLVIDO -> value >= 0.0 && value <= 20.0
            SensorType.CONDUTIVIDADE -> value >= 0.0 && value <= 10000.0
        }
    }
}
