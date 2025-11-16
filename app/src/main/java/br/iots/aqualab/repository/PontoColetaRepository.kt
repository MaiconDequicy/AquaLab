package br.iots.aqualab.repository

import android.os.Build
import android.util.Log
import br.iots.aqualab.BuildConfig
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
                val userId = firebaseAuth.currentUser?.uid ?: return@withContext Result.failure(Exception("Usuário não autenticado"))
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
                val userId = firebaseAuth.currentUser?.uid ?: return@withContext Result.failure(Exception("Usuário não autenticado"))
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
            val pontoId = ponto.id ?: return Result.failure(Exception("ID do ponto é nulo, não é possível atualizar."))
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
            } catch (e: Exception)
            {
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

                val condicoes = weatherResponse.weather.firstOrNull()?.description?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                } ?: "N/A"

                val temperatura = "%.1f°C".format(weatherResponse.main.temp)

                val umidade = "${weatherResponse.main.humidity}%"

                delay(1000)

                val analiseQualidadeSimulada = "A análise indica parâmetros dentro dos padrões CONAMA para a classe 2, considerando o clima atual."
                val dicaEducativaSimulada = "💡 Sabia que o clima de '${condicoes.lowercase()}' pode influenciar o pH da água?"

                val detalhes = PontoDetalhadoInfo(
                    nomeEstacao = ponto.nome,
                    condicoesAtuais = condicoes,
                    temperatura = temperatura,
                    umidade = umidade,
                    linkMaisInfo = null,
                    analiseQualidade = analiseQualidadeSimulada,
                    dicaEducativa = dicaEducativaSimulada
                )
                Result.success(detalhes)

            } catch (e: Exception) {
                Log.e("PontoColetaRepo", "Erro ao buscar detalhes completos do ponto: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}
