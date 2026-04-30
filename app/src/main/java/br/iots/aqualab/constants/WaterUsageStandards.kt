package br.iots.aqualab.constants

/**
 * Padrões de qualidade da água baseados em diferentes usos.
 * Referências:
 * - CONAMA 357/2005 e 274/2000
 * - Portaria MS 888/2021
 * - WHO Guidelines for Drinking-water Quality
 */
object WaterUsageStandards {

    /**
     * Tipos de uso da água com seus respectivos padrões de qualidade
     */
    enum class WaterUsageType(
        val displayName: String,
        val description: String,
        val icon: String
    ) {
        CONSUMO_HUMANO(
            displayName = "Consumo Humano",
            description = "Água própria para beber e cozinhar",
            icon = "🚰"
        ),
        BALNEABILIDADE(
            displayName = "Balneabilidade",
            description = "Água própria para banho e recreação",
            icon = "🏊"
        ),
        IRRIGACAO(
            displayName = "Irrigação",
            description = "Água para agricultura e irrigação",
            icon = "🌾"
        ),
        AQUICULTURA(
            displayName = "Aquicultura",
            description = "Água para criação de organismos aquáticos",
            icon = "🐟"
        ),
        DESSEDENTACAO_ANIMAL(
            displayName = "Dessedentação Animal",
            description = "Água para consumo animal",
            icon = "🐄"
        ),
        PRESERVACAO_AMBIENTAL(
            displayName = "Preservação Ambiental",
            description = "Água para proteção de ecossistemas",
            icon = "🌿"
        );

        companion object {
            fun fromString(name: String): WaterUsageType? {
                return values().find { it.displayName.equals(name, ignoreCase = true) }
            }
        }
    }

    /**
     * Padrões de qualidade para consumo humano
     */
    object ConsumoHumano {
        data class Standard(
            val parameter: String,
            val excelente: String,
            val bom: String,
            val regular: String,
            val ruim: String,
            val pessimo: String
        )

        val standards = listOf(
            Standard(
                parameter = "pH",
                excelente = "7.0 - 7.8",
                bom = "6.5 - 8.5",
                regular = "6.0 - 9.0",
                ruim = "5.5 - 9.5",
                pessimo = "< 5.5 ou > 9.5"
            ),
            Standard(
                parameter = "Turbidez",
                excelente = "< 1 NTU",
                bom = "< 5 NTU",
                regular = "5 - 10 NTU",
                ruim = "10 - 40 NTU",
                pessimo = "> 40 NTU"
            ),
            Standard(
                parameter = "TDS",
                excelente = "< 150 ppm",
                bom = "< 300 ppm",
                regular = "300 - 600 ppm",
                ruim = "600 - 900 ppm",
                pessimo = "> 900 ppm"
            ),
            Standard(
                parameter = "Temperatura",
                excelente = "15 - 25°C",
                bom = "10 - 30°C",
                regular = "5 - 35°C",
                ruim = "0 - 40°C",
                pessimo = "< 0 ou > 40°C"
            )
        )

        fun getDescription(): String {
            return """
                Água própria para consumo humano deve atender aos padrões da
                Portaria MS 888/2021. A água deve ser segura, sem riscos à saúde.

                Critérios principais:
                • pH entre 6.5 e 8.5
                • Turbidez máxima de 5 NTU
                • Livre de contaminantes químicos e biológicos
                • TDS abaixo de 500 ppm (recomendado < 300 ppm)
            """.trimIndent()
        }
    }

    /**
     * Padrões de qualidade para balneabilidade (CONAMA 274/2000)
     */
    object Balneabilidade {
        data class Standard(
            val parameter: String,
            val excelente: String,
            val muito_boa: String,
            val satisfatoria: String,
            val impropria: String
        )

        val standards = listOf(
            Standard(
                parameter = "pH",
                excelente = "7.0 - 8.0",
                muito_boa = "6.5 - 8.5",
                satisfatoria = "6.0 - 9.0",
                impropria = "< 6.0 ou > 9.0"
            ),
            Standard(
                parameter = "Turbidez",
                excelente = "< 5 NTU",
                muito_boa = "< 10 NTU",
                satisfatoria = "< 40 NTU",
                impropria = "> 40 NTU"
            ),
            Standard(
                parameter = "OD (Oxigênio Dissolvido)",
                excelente = "> 6 mg/L",
                muito_boa = "> 5 mg/L",
                satisfatoria = "> 4 mg/L",
                impropria = "< 4 mg/L"
            )
        )

        fun getDescription(): String {
            return """
                Água própria para banho e recreação segundo CONAMA 274/2000.

                Classificação:
                • Excelente: Água de ótima qualidade, baixíssimo risco
                • Muito Boa: Adequada para recreação
                • Satisfatória: Aceitável, mas requer atenção
                • Imprópria: Não recomendada para contato

                Aspectos avaliados: pH, turbidez, oxigênio dissolvido
            """.trimIndent()
        }
    }

    /**
     * Padrões para irrigação (CONAMA 357/2005 - Classe 2)
     */
    object Irrigacao {
        data class Standard(
            val parameter: String,
            val excelente: String,
            val adequada: String,
            val limitrofe: String,
            val inadequada: String
        )

        val standards = listOf(
            Standard(
                parameter = "pH",
                excelente = "6.5 - 7.5",
                adequada = "6.0 - 8.0",
                limitrofe = "5.5 - 8.5",
                inadequada = "< 5.5 ou > 8.5"
            ),
            Standard(
                parameter = "Condutividade",
                excelente = "< 250 µS/cm",
                adequada = "< 750 µS/cm",
                limitrofe = "750 - 2000 µS/cm",
                inadequada = "> 2000 µS/cm"
            ),
            Standard(
                parameter = "TDS",
                excelente = "< 175 ppm",
                adequada = "< 500 ppm",
                limitrofe = "500 - 1400 ppm",
                inadequada = "> 1400 ppm"
            )
        )

        fun getDescription(): String {
            return """
                Água para irrigação de culturas agrícolas.

                Aspectos importantes:
                • Salinidade (TDS e condutividade) - afeta o solo
                • pH adequado para absorção de nutrientes
                • Ausência de contaminantes tóxicos

                Alta salinidade pode prejudicar plantas sensíveis.
            """.trimIndent()
        }
    }

    /**
     * Padrões para aquicultura (CONAMA 357/2005 - Classe 2)
     */
    object Aquicultura {
        data class Standard(
            val parameter: String,
            val otima: String,
            val boa: String,
            val regular: String,
            val inadequada: String
        )

        val standards = listOf(
            Standard(
                parameter = "OD (Oxigênio Dissolvido)",
                otima = "> 7 mg/L",
                boa = "5 - 7 mg/L",
                regular = "4 - 5 mg/L",
                inadequada = "< 4 mg/L"
            ),
            Standard(
                parameter = "pH",
                otima = "7.0 - 8.0",
                boa = "6.5 - 8.5",
                regular = "6.0 - 9.0",
                inadequada = "< 6.0 ou > 9.0"
            ),
            Standard(
                parameter = "Temperatura",
                otima = "24 - 28°C",
                boa = "20 - 30°C",
                regular = "18 - 32°C",
                inadequada = "< 18 ou > 32°C"
            ),
            Standard(
                parameter = "Turbidez",
                otima = "< 10 NTU",
                boa = "< 25 NTU",
                regular = "25 - 50 NTU",
                inadequada = "> 50 NTU"
            )
        )

        fun getDescription(): String {
            return """
                Água para criação de peixes e organismos aquáticos.

                Parâmetros críticos:
                • Oxigênio Dissolvido (OD): Essencial para respiração
                • pH: Afeta metabolismo e crescimento
                • Temperatura: Influencia desenvolvimento
                • Turbidez: Afeta fotossíntese e alimentação
            """.trimIndent()
        }
    }

    /**
     * Padrões para dessedentação animal (CONAMA 357/2005 - Classe 2)
     */
    object DessedentacaoAnimal {
        data class Standard(
            val parameter: String,
            val excelente: String,
            val adequada: String,
            val aceitavel: String,
            val inadequada: String
        )

        val standards = listOf(
            Standard(
                parameter = "pH",
                excelente = "6.5 - 7.5",
                adequada = "6.0 - 8.5",
                aceitavel = "5.5 - 9.0",
                inadequada = "< 5.5 ou > 9.0"
            ),
            Standard(
                parameter = "TDS",
                excelente = "< 500 ppm",
                adequada = "< 1000 ppm",
                aceitavel = "1000 - 3000 ppm",
                inadequada = "> 3000 ppm"
            ),
            Standard(
                parameter = "Turbidez",
                excelente = "< 5 NTU",
                adequada = "< 20 NTU",
                aceitavel = "20 - 50 NTU",
                inadequada = "> 50 NTU"
            )
        )

        fun getDescription(): String {
            return """
                Água para consumo de animais de criação.

                Aspectos relevantes:
                • Salinidade moderada (TDS) é tolerada
                • pH neutro a levemente alcalino
                • Baixa turbidez para melhor palatabilidade
                • Ausência de contaminantes químicos

                Diferentes animais têm tolerâncias variadas.
            """.trimIndent()
        }
    }

    /**
     * Padrões para preservação ambiental (CONAMA 357/2005 - Classe Especial e 1)
     */
    object PreservacaoAmbiental {
        data class Standard(
            val parameter: String,
            val pristina: String,
            val preservada: String,
            val conservada: String,
            val degradada: String
        )

        val standards = listOf(
            Standard(
                parameter = "OD (Oxigênio Dissolvido)",
                pristina = "> 8 mg/L",
                preservada = "> 6 mg/L",
                conservada = "> 5 mg/L",
                degradada = "< 5 mg/L"
            ),
            Standard(
                parameter = "pH",
                pristina = "6.8 - 7.4",
                preservada = "6.5 - 8.0",
                conservada = "6.0 - 9.0",
                degradada = "< 6.0 ou > 9.0"
            ),
            Standard(
                parameter = "Turbidez",
                pristina = "< 1 NTU",
                preservada = "< 5 NTU",
                conservada = "< 10 NTU",
                degradada = "> 10 NTU"
            ),
            Standard(
                parameter = "Temperatura",
                pristina = "Variação natural",
                preservada = "< 3°C variação",
                conservada = "< 5°C variação",
                degradada = "> 5°C variação"
            )
        )

        fun getDescription(): String {
            return """
                Água para proteção de ecossistemas aquáticos e biodiversidade.

                Níveis de preservação:
                • Pristina: Condições naturais intocadas
                • Preservada: Mínima interferência humana
                • Conservada: Ecossistema funcional
                • Degradada: Necessita recuperação

                Oxigênio dissolvido é indicador-chave da saúde do ecossistema.
            """.trimIndent()
        }
    }
}
