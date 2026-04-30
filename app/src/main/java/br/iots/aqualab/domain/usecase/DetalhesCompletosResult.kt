package br.iots.aqualab.domain.usecase

import android.util.Log
import br.iots.aqualab.constants.WaterQualityConstants
import br.iots.aqualab.model.LeituraSensor
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.model.PontoDetalhadoInfo
import br.iots.aqualab.repository.AnaliseQualidadeRepository
import br.iots.aqualab.repository.PontoColetaRepository
import br.iots.aqualab.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Locale

data class DetalhesCompletosResult(
    val info: PontoDetalhadoInfo,
    val classificacao: String
)

class GetDetalhesCompletosDoPontoUseCase(
    private val pontoColetaRepository: PontoColetaRepository,
    private val weatherRepository: WeatherRepository,
    private val analiseQualidadeRepository: AnaliseQualidadeRepository
) {

    /**
     * Infere o tipo de corpo hídrico a partir do nome e tipo do ponto de coleta
     */
    private fun inferirTipoCorpoHidrico(ponto: PontoColeta): String {
        val nomeMinusculo = ponto.nome.lowercase()
        val enderecoMinusculo = ponto.endereco.lowercase()
        val tipoMinusculo = ponto.tipo.lowercase()
        val textoCompleto = "$nomeMinusculo $enderecoMinusculo $tipoMinusculo"

        return when {
            // Rios e córregos
            textoCompleto.contains("rio ") || textoCompleto.contains("riacho") ||
            textoCompleto.contains("córrego") || textoCompleto.contains("corrego") ||
            textoCompleto.contains("igarapé") || textoCompleto.contains("igarape") ||
            textoCompleto.contains("ribeirão") || textoCompleto.contains("ribeirao") ||
            textoCompleto.contains("creek") || textoCompleto.contains("stream")
            -> "Rio/Córrego (água corrente)"

            // Lagos e lagoas
            textoCompleto.contains("lago") || textoCompleto.contains("lagoa") ||
            textoCompleto.contains("lake")
            -> "Lago/Lagoa (água lêntica)"

            // Represas e reservatórios
            textoCompleto.contains("represa") || textoCompleto.contains("reservatório") ||
            textoCompleto.contains("reservatorio") || textoCompleto.contains("açude") ||
            textoCompleto.contains("acude") || textoCompleto.contains("barragem") ||
            textoCompleto.contains("reservoir") || textoCompleto.contains("dam")
            -> "Reservatório/Represa (artificial)"

            // Nascentes e fontes
            textoCompleto.contains("nascente") || textoCompleto.contains("fonte") ||
            textoCompleto.contains("olho d'água") || textoCompleto.contains("olho dagua") ||
            textoCompleto.contains("spring")
            -> "Nascente/Fonte (água subterrânea aflorante)"

            // Poços
            textoCompleto.contains("poço") || textoCompleto.contains("poco") ||
            textoCompleto.contains("well")
            -> "Poço (água subterrânea)"

            // Estuário
            textoCompleto.contains("estuário") || textoCompleto.contains("estuario") ||
            textoCompleto.contains("foz") || textoCompleto.contains("desembocadura") ||
            textoCompleto.contains("estuary")
            -> "Estuário (transição água doce/salgada)"

            // Mangue
            textoCompleto.contains("mangue") || textoCompleto.contains("manguezal") ||
            textoCompleto.contains("mangrove")
            -> "Manguezal (ecossistema costeiro)"

            // Tanque/piscina
            textoCompleto.contains("tanque") || textoCompleto.contains("piscina") ||
            textoCompleto.contains("aquicultura") || textoCompleto.contains("piscicultura") ||
            textoCompleto.contains("pond") || textoCompleto.contains("tank")
            -> "Tanque/Sistema de aquicultura (artificial)"

            // Tratamento
            textoCompleto.contains("eta") || textoCompleto.contains("tratamento") ||
            textoCompleto.contains("estação") || textoCompleto.contains("estacao") ||
            textoCompleto.contains("treatment plant")
            -> "Estação de Tratamento de Água"

            // Padrão
            else -> "Corpo hídrico (tipo não especificado)"
        }
    }

    suspend operator fun invoke(ponto: PontoColeta): Result<DetalhesCompletosResult> =
        withContext(Dispatchers.IO) {
            try {

                val (weatherResponse, leituras) = coroutineScope {
                    val weatherDeferred = async {
                        weatherRepository.getCurrentWeather(
                            ponto.latitude,
                            ponto.longitude
                        )
                    }

                    val leiturasDeferred = async {
                        if (!ponto.pontoIdNuvem.isNullOrEmpty()) {
                            pontoColetaRepository.getLeiturasRecentes(
                                ponto.pontoIdNuvem,
                                limit = 10
                            )
                        } else emptyList()
                    }

                    Pair(
                        weatherDeferred.await().getOrThrow(),
                        leiturasDeferred.await()
                    )
                }

                val condicoes = weatherResponse.weather.firstOrNull()
                    ?.description
                    ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
                    ?: "N/A"

                val temperatura = "%.1f°C".format(weatherResponse.main.temp)
                val umidade = "${weatherResponse.main.humidity}%"

                // Obter últimas leituras de cada tipo de sensor disponível
                val ultimasLeituras = LeituraSensor.getLatestReadings(leituras)

                // Construir lista de leituras de sensores de forma dinâmica e opcional
                val leiturasTexto = buildString {
                    // pH
                    ultimasLeituras[WaterQualityConstants.SensorType.PH]?.let { leitura ->
                        append("- pH: ${leitura.getFormattedValue()}\n")
                    }

                    // Temperatura da água
                    ultimasLeituras[WaterQualityConstants.SensorType.TEMPERATURA]?.let { leitura ->
                        append("- Temperatura da Água: ${leitura.getFormattedValue()}\n")
                    }

                    // TDS (Total Dissolved Solids)
                    ultimasLeituras[WaterQualityConstants.SensorType.TDS]?.let { leitura ->
                        append("- TDS (Sólidos Dissolvidos Totais): ${leitura.getFormattedValue()}\n")
                    }

                    // Turbidez
                    ultimasLeituras[WaterQualityConstants.SensorType.TURBIDEZ]?.let { leitura ->
                        append("- Turbidez: ${leitura.getFormattedValue()}\n")
                    }

                    // Oxigênio Dissolvido
                    ultimasLeituras[WaterQualityConstants.SensorType.OXIGENIO_DISSOLVIDO]?.let { leitura ->
                        append("- Oxigênio Dissolvido: ${leitura.getFormattedValue()}\n")
                    }

                    // Condutividade
                    ultimasLeituras[WaterQualityConstants.SensorType.CONDUTIVIDADE]?.let { leitura ->
                        append("- Condutividade: ${leitura.getFormattedValue()}\n")
                    }

                    // Se não houver leituras, informar
                    if (this.isEmpty()) {
                        append("- Nenhuma leitura de sensor disponível no momento\n")
                    }
                }.trimEnd()

                // Tentar inferir o tipo de corpo hídrico do nome ou tipo do ponto
                val tipoCorpoHidrico = inferirTipoCorpoHidrico(ponto)

                val promptDoUsuario = """
                    ## Contexto do Monitoramento
                    Local: ${ponto.nome}
                    Endereço: ${ponto.endereco}
                    Tipo de corpo hídrico: $tipoCorpoHidrico
                    Coordenadas: ${ponto.latitude}, ${ponto.longitude}

                    ## Condições Ambientais
                    Condições climáticas: $condicoes
                    Temperatura ambiente: $temperatura
                    Umidade do ar: $umidade

                    ## Dados dos Sensores
                    $leiturasTexto

                    ## Requisitos da Análise

                    IMPORTANTE: Analise APENAS os parâmetros medidos acima. Não especule sobre parâmetros não disponíveis.

                    Considere na sua análise:
                    1. **Conformidade com CONAMA 357/2005**: Avalie se os parâmetros atendem aos padrões para águas Classe 1, 2 ou 3
                    2. **Portaria MS 888/2021**: Verifique se atende aos padrões de potabilidade (pH 6.0-9.5, Turbidez ≤5 NTU, TDS ≤1000 mg/L)
                    3. **Características do corpo hídrico**: Considere se os valores são típicos para o tipo de ambiente aquático identificado
                    4. **Condições climáticas**: Avalie como temperatura e clima podem influenciar os parâmetros
                    5. **Saúde pública e ecológica**: Identifique possíveis riscos ou benefícios

                    Padrões de referência detalhados:

                    **pH**:
                    - CONAMA 357/2005 (Classe 1): 6.0 a 9.0
                    - Portaria MS 888/2021: 6.0 a 9.5
                    - Ideal para consumo: 6.5 a 8.5
                    - Ideal para vida aquática: 6.5 a 8.0

                    **Temperatura da Água**:
                    - Ideal para consumo: 20°C a 30°C
                    - Rios tropicais: 22°C a 28°C é normal
                    - Acima de 30°C: reduz oxigênio dissolvido

                    **TDS (Sólidos Dissolvidos Totais)**:
                    - Portaria MS 888/2021: máximo 1000 mg/L
                    - WHO: máximo 600 mg/L (aceitabilidade)
                    - < 300 ppm: Excelente
                    - 300-600 ppm: Bom
                    - 600-900 ppm: Aceitável
                    - > 1000 ppm: Impróprio para consumo

                    **Turbidez**:
                    - CONAMA 357/2005 (Classe 1): até 40 NTU
                    - Portaria MS 888/2021: máximo 5 NTU (água tratada)
                    - < 5 NTU: Excelente transparência
                    - 5-25 NTU: Aceitável para corpos naturais
                    - > 50 NTU: Inadequada

                    **Oxigênio Dissolvido (OD)**:
                    - CONAMA 357/2005 (Classe 1): > 6 mg/L
                    - CONAMA 357/2005 (Classe 2): > 5 mg/L
                    - > 8 mg/L: Excelente para vida aquática
                    - 4-6 mg/L: Adequado
                    - < 4 mg/L: Estresse para organismos aquáticos
                    - < 2 mg/L: Condição crítica

                    **Condutividade Elétrica**:
                    - < 100 μS/cm: Água muito pura
                    - 100-500 μS/cm: Boa qualidade
                    - 500-1000 μS/cm: Aceitável
                    - > 1000 μS/cm: Possível poluição

                    ## Formato da Resposta

                    Sua resposta DEVE ter EXATAMENTE TRÊS seções:

                    [CLASSIFICACAO]
                    Uma única palavra: Ótima, Boa, Regular, Ruim ou Péssima
                    (baseie-se no parâmetro mais restritivo encontrado)

                    [ANALISE]
                    Análise técnica objetiva (máximo 400 caracteres):
                    - Mencione conformidade/não-conformidade com legislação
                    - Cite valores específicos medidos
                    - Identifique o principal fator limitante, se houver
                    - Considere o contexto do tipo de corpo hídrico

                    [DICA]
                    Explicação educativa em linguagem acessível (máximo 300 caracteres):
                    - Use analogias simples quando apropriado
                    - Explique o significado prático dos valores
                    - Forneça orientação sobre uso seguro da água, se aplicável
                """.trimIndent()

                val respostaCompleta =
                    analiseQualidadeRepository.obterAnalise(promptDoUsuario).getOrThrow()

                // CLASSIFICAÇÃO
                val classificacao = Regex("\\[CLASSIFICACAO\\]\\s*([A-Za-zÀ-ú]+)")
                    .find(respostaCompleta)
                    ?.groupValues?.get(1)
                    ?: "Indisponível"

                // ANALISE
                val analise = Regex("\\[ANALISE\\]([\\s\\S]*?)(?=\\[DICA\\]|$)")
                    .find(respostaCompleta)
                    ?.groupValues?.get(1)
                    ?.trim()
                    ?: "Análise não disponível."

                // DICA
                val dica = Regex("\\[DICA\\]([\\s\\S]*)$")
                    .find(respostaCompleta)
                    ?.groupValues?.get(1)
                    ?.trim()
                    ?: "Dica não disponível."


                Log.d("GetDetalhesUseCase", "-----------------------------------------")
                Log.d("GetDetalhesUseCase", "RESPOSTA COMPLETA DA IA: $respostaCompleta")
                Log.d("GetDetalhesUseCase", "CLASSIFICAÇÃO EXTRAÍDA: $classificacao")
                Log.d("GetDetalhesUseCase", "ANÁLISE EXTRAÍDA: $analise")
                Log.d("GetDetalhesUseCase", "DICA EXTRAÍDA: $dica")
                Log.d("GetDetalhesUseCase", "-----------------------------------------")
                // --- FIM DA MODIFICAÇÃO ---


                pontoColetaRepository.atualizarClassificacao(
                    ponto.id,
                    classificacao
                )
                val detalhes = PontoDetalhadoInfo(
                    nomeEstacao = ponto.nome,
                    condicoesAtuais = condicoes,
                    temperatura = temperatura,
                    umidade = umidade,
                    analiseQualidade = analise,
                    dicaEducativa = dica
                )

                Result.success(
                    DetalhesCompletosResult(
                        info = detalhes,
                        classificacao = classificacao
                    )
                )

            } catch (e: Exception) {
                Log.e(
                    "GetDetalhesUseCase",
                    "Erro ao obter detalhes: ${e.message}",
                    e
                )
                Result.failure(e)
            }
        }
}