package br.iots.aqualab.domain.usecase

import android.util.Log
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

                val ultimaLeituraPH =
                    leituras.firstOrNull { it.sensorId == "ph" }?.valor

                val ultimaLeituraTempAgua =
                    leituras.firstOrNull { it.sensorId == "temperatura" }?.valor

                val promptDoUsuario = """
                    Faça uma análise de qualidade da água para este local: '${ponto.nome}'.
                    Condições climáticas atuais: $condicoes, temperatura ambiente de $temperatura, umidade de $umidade.
                    Últimas leituras dos sensores na água:
                    - pH: ${ultimaLeituraPH?.toString() ?: "não medido"}
                    - Temperatura da Água: ${ultimaLeituraTempAgua?.let { "%.1f°C".format(it) } ?: "não medida"}

                    Sua resposta deve ter TRÊS partes, marcadas exatamente assim:
                    1. Comece com [CLASSIFICACAO] e forneça UMA ÚNICA palavra: Ótima, Boa, Regular, Ruim ou Péssima.
                    2. Comece com [ANALISE] e forneça uma análise técnica concisa (máximo 400 caracteres) sobre a qualidade da água com base nos dados.
                    3. Comece com [DICA] e forneça uma explicação didática e curta (máximo 300 caracteres) para um público jovem.
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
                    id = ponto.id,
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