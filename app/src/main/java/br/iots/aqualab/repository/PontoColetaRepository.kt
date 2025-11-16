package br.iots.aqualab.repository


import android.util.Log
import br.iots.aqualab.BuildConfig

import br.iots.aqualab.model.ChatGptRequest
import br.iots.aqualab.model.ChatMessage
import br.iots.aqualab.model.LeituraSensor
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.model.PontoDetalhadoInfo
import br.iots.aqualab.network.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class PontoColetaRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val pontosColetaCollection = firestore.collection("pontosDeColeta")
    private val leiturasSensoresCollection = firestore.collection("leiturasSensores")

    suspend fun getPontosColeta(): List<PontoColeta> {
        val result = getPontosColetaDoUsuario()
        return result.getOrThrow()
    }

    /**
     * Busca as leituras mais recentes de um ponto de coleta específico, com um limite dinâmico
     */
    suspend fun getLeiturasRecentes(pontoIdNuvem: String?, limit: Int): List<LeituraSensor> {
        return withContext(Dispatchers.IO) {
            try {
                if (pontoIdNuvem.isNullOrEmpty()) {
                    throw IllegalArgumentException("pontoIdNuvem não pode ser nulo ou vazio.")
                }
                val snapshot = firestore.collection("leiturasSensores")
                    .whereEqualTo("pontoId", pontoIdNuvem)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()
                snapshot.toObjects(LeituraSensor::class.java)
            } catch (e: Exception) {
                Log.e("PontoColetaRepo", "Erro ao buscar leituras recentes", e)
                throw e
            }
        }
    }

    suspend fun criarPontoColeta(novoPonto: PontoColeta): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = firebaseAuth.currentUser?.uid ?: return@withContext Result.failure(
                    Exception("Usuário não autenticado")
                )
                val pontoComUserId = novoPonto.copy(userId = userId)
                pontosColetaCollection.add(pontoComUserId).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    suspend fun getPontosColetaDoUsuario(): Result<List<PontoColeta>> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = firebaseAuth.currentUser?.uid ?: return@withContext Result.failure(
                    Exception("Usuário não autenticado")
                )
                val snapshot = pontosColetaCollection
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                val pontos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PontoColeta::class.java)?.copy(id = doc.id)
                }
                Result.success(pontos)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun atualizarPontoColeta(ponto: PontoColeta): Result<Unit> {
        return try {
            val pontoId = ponto.id
                ?: return Result.failure(Exception("ID do ponto é nulo, não é possível atualizar."))
            if (pontoId.isEmpty()) {
                return Result.failure(Exception("ID do ponto está vazio, não é possível atualizar."))
            }
            val db = FirebaseFirestore.getInstance()
            val documentoRef = db.collection("pontosDeColeta").document(pontoId)
            documentoRef.set(ponto, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PontoColetaRepository", "Erro ao atualizar ponto de coleta no Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun deletarPontoColeta(pontoId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (pontoId.isEmpty()) {
                    return@withContext Result.failure(Exception("ID do ponto está vazio, não é possível deletar."))
                }
                pontosColetaCollection.document(pontoId).delete().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("PontoColetaRepository", "Erro ao deletar ponto de coleta no Firestore", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getIdsDisponiveisNuvem(): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = leiturasSensoresCollection.get().await()
                val idsUnicos = hashSetOf<String>()
                for (document in snapshot.documents) {
                    document.getString("pontoId")?.let { id ->
                        if (id.isNotEmpty()) {
                            idsUnicos.add(id)
                        }
                    }
                }
                Result.success(idsUnicos.sorted())
            } catch (e: Exception) {
                Log.e("PontoColetaRepository", "Erro ao buscar IDs da nuvem do Firestore", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getPontosPublicos(): Result<List<PontoColeta>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = pontosColetaCollection
                    .whereEqualTo("tipo", "Público")
                    .get()
                    .await()

                val pontosPublicos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PontoColeta::class.java)?.copy(id = doc.id)
                }
                Result.success(pontosPublicos)
            } catch (e: Exception) {
                Log.e("PontoColetaRepo", "Erro ao buscar pontos públicos", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getDetalhesCompletosDoPonto(ponto: PontoColeta): Result<PontoDetalhadoInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val weatherResponse = RetrofitInstance.weatherApiService.getCurrentWeather(
                    lat = ponto.latitude,
                    lon = ponto.longitude,
                    apiKey = BuildConfig.OPENWEATHER_API_KEY
                )
                val condicoes =
                    weatherResponse.weather.firstOrNull()?.description?.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    } ?: "N/A"
                val temperatura = "%.1f°C".format(weatherResponse.main.temp)
                val umidade = "${weatherResponse.main.humidity}%"

                var ultimaLeituraPH: Double? = null
                var ultimaLeituraTempAgua: Double? = null
                if (!ponto.pontoIdNuvem.isNullOrEmpty()) {
                    val leituras = getLeiturasRecentes(ponto.pontoIdNuvem, limit = 10)
                    ultimaLeituraPH = leituras.firstOrNull { it.sensorId == "PH" }?.valor
                    ultimaLeituraTempAgua =
                        leituras.firstOrNull { it.sensorId == "TEMPERATURA_AGUA" }?.valor
                }

                val promptDoUsuario = """
                Faça uma análise de qualidade da água para este local: '${ponto.nome}'.
                Condições climáticas atuais: ${condicoes}, temperatura ambiente de ${temperatura}, umidade de ${umidade}.
                Últimas leituras dos sensores na água:
                - pH: ${ultimaLeituraPH ?: "não medido"}
                - Temperatura da Água: ${ultimaLeituraTempAgua ?: "não medida"} °C

                Sua resposta deve ter duas partes, marcadas exatamente assim:
                1. Comece com [ANALISE] e forneça uma análise técnica concisa (máximo 400 caracteres) sobre a qualidade da água, considerando os padrões de potabilidade e balneabilidade (CONAMA 357/2005).
                2. Comece com [DICA] e forneça uma explicação didática e curta (máximo 300 caracteres) para um público jovem sobre o que esses dados significam para o meio ambiente local.
            """.trimIndent()

                val chatRequest = ChatGptRequest(
                    model = "gpt-3.5-turbo",
                    messages = listOf(
                        ChatMessage(
                            "system",
                            "Você é um assistente especializado em monitoramento ambiental e educação, focado em dados de qualidade da água na Amazônia. Suas respostas devem ser claras e diretas."
                        ),
                        ChatMessage("user", promptDoUsuario)
                    )
                )

                val chatResponse = RetrofitInstance.chatGptApiService.getChatCompletion(
                    apiKey = "Bearer ${BuildConfig.OPENAI_API_KEY}",
                    request = chatRequest
                )

                val respostaCompleta = chatResponse.choices.firstOrNull()?.message?.content
                    ?: "Não foi possível obter a análise."
                val analise =
                    respostaCompleta.substringAfter("[ANALISE]", "Análise não disponível.")
                        .substringBefore("[DICA]").trim()
                val dica = respostaCompleta.substringAfter("[DICA]", "Dica não disponível.").trim()

                val detalhes = PontoDetalhadoInfo(
                    nomeEstacao = ponto.nome,
                    condicoesAtuais = condicoes,
                    temperatura = temperatura,
                    umidade = umidade,
                    linkMaisInfo = null,
                    analiseQualidade = analise,
                    dicaEducativa = dica
                )
                Result.success(detalhes)

            } catch (e: Exception) {
                Log.e(
                    "PontoColetaRepo",
                    "Erro ao buscar detalhes completos do ponto: ${e.message}",
                    e
                )
                Result.failure(e)
            }
        }
    }
}
