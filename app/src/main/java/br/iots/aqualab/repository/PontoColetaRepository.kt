package br.iots.aqualab.repository


import android.util.Log
import br.iots.aqualab.constants.WaterQualityConstants
import br.iots.aqualab.model.LeituraSensor
import br.iots.aqualab.model.PontoColeta
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

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


    suspend fun atualizarClassificacao(pontoId: String, novaClassificacao: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (pontoId.isEmpty() || novaClassificacao.isEmpty() || novaClassificacao == "Indisponível") {
                    return@withContext Result.success(Unit)
                }
                pontosColetaCollection.document(pontoId)
                    .update("classificacao", novaClassificacao)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("PontoColetaRepo", "Erro ao atualizar classificação", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Busca leituras de um sensor específico com suporte a filtros avançados
     *
     * @param pontoIdNuvem ID do ponto de coleta
     * @param sensorType Tipo de sensor (opcional - se null, busca todos)
     * @param startDate Data inicial (opcional)
     * @param endDate Data final (opcional)
     * @param limit Número máximo de leituras
     * @return Lista de leituras filtradas
     */
    suspend fun getLeiturasComFiltros(
        pontoIdNuvem: String?,
        sensorType: WaterQualityConstants.SensorType? = null,
        startDate: Date? = null,
        endDate: Date? = null,
        limit: Int = 50
    ): List<LeituraSensor> {
        return withContext(Dispatchers.IO) {
            try {
                if (pontoIdNuvem.isNullOrEmpty()) {
                    throw IllegalArgumentException("pontoIdNuvem não pode ser nulo ou vazio.")
                }

                Log.d("PontoColetaRepo", "Buscando leituras - pontoId: $pontoIdNuvem, sensorType: ${sensorType?.id}, limit: $limit")

                // ABORDAGEM ALTERNATIVA: Buscar todos e filtrar em memória
                // Isso evita problemas com índices compostos do Firestore

                var query: Query = leiturasSensoresCollection
                    .whereEqualTo("pontoId", pontoIdNuvem)

                // Se houver filtros de data, aplicar na query
                // (apenas se NÃO houver filtro de sensor, para evitar índice composto)
                if (sensorType == null) {
                    startDate?.let {
                        query = query.whereGreaterThanOrEqualTo("timestamp", Timestamp(it))
                    }
                    endDate?.let {
                        query = query.whereLessThanOrEqualTo("timestamp", Timestamp(it))
                    }
                }

                // Ordenar por timestamp
                val snapshot = query
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit((limit * 5).toLong()) // Buscar mais para compensar filtro em memória
                    .get()
                    .await()

                var leituras = snapshot.toObjects(LeituraSensor::class.java)

                Log.d("PontoColetaRepo", "Total de leituras encontradas: ${leituras.size}")

                // Log dos sensorIds únicos encontrados
                val uniqueSensorIds = leituras.mapNotNull { it.sensorId }.distinct()
                Log.d("PontoColetaRepo", "SensorIds encontrados no Firebase: $uniqueSensorIds")

                // Filtrar por tipo de sensor se especificado (em memória)
                sensorType?.let { tipo ->
                    Log.d("PontoColetaRepo", "Filtrando por sensorType: ${tipo.id}")
                    leituras = leituras.filter { leitura ->
                        // Comparação case-insensitive
                        leitura.sensorId?.lowercase() == tipo.id.lowercase()
                    }
                    Log.d("PontoColetaRepo", "Leituras após filtro de sensor: ${leituras.size}")
                }

                // Filtrar por data se especificado (em memória, se houver filtro de sensor)
                if (sensorType != null) {
                    startDate?.let { start ->
                        leituras = leituras.filter { leitura ->
                            leitura.timestamp?.toDate()?.time ?: 0L >= start.time
                        }
                    }
                    endDate?.let { end ->
                        leituras = leituras.filter { leitura ->
                            leitura.timestamp?.toDate()?.time ?: 0L <= end.time
                        }
                    }
                }

                // Aplicar limite final
                val resultado = leituras.take(limit)

                Log.d("PontoColetaRepo", "Leituras retornadas (após limite): ${resultado.size}")

                resultado
            } catch (e: Exception) {
                Log.e("PontoColetaRepo", "Erro ao buscar leituras com filtros: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Busca leituras de um sensor específico por ID
     */
    suspend fun getLeiturasDoSensor(
        pontoIdNuvem: String?,
        sensorId: String,
        limit: Int = 50
    ): List<LeituraSensor> {
        return withContext(Dispatchers.IO) {
            try {
                if (pontoIdNuvem.isNullOrEmpty()) {
                    return@withContext emptyList()
                }

                val snapshot = leiturasSensoresCollection
                    .whereEqualTo("pontoId", pontoIdNuvem)
                    .whereEqualTo("sensorId", sensorId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()

                snapshot.toObjects(LeituraSensor::class.java)
            } catch (e: Exception) {
                Log.e("PontoColetaRepo", "Erro ao buscar leituras do sensor $sensorId", e)
                emptyList()
            }
        }
    }

    /**
     * Busca as últimas leituras de cada tipo de sensor disponível
     */
    suspend fun getUltimasLeiturasDetalhadas(pontoIdNuvem: String?): Map<WaterQualityConstants.SensorType, LeituraSensor> {
        return withContext(Dispatchers.IO) {
            try {
                if (pontoIdNuvem.isNullOrEmpty()) {
                    return@withContext emptyMap()
                }

                val todasLeituras = getLeiturasRecentes(pontoIdNuvem, limit = 100)
                LeituraSensor.getLatestReadings(todasLeituras)
            } catch (e: Exception) {
                Log.e("PontoColetaRepo", "Erro ao buscar últimas leituras detalhadas", e)
                emptyMap()
            }
        }
    }

    /**
     * Verifica quais tipos de sensores estão disponíveis para um ponto de coleta
     */
    suspend fun getSensoresDisponiveis(pontoIdNuvem: String?): List<WaterQualityConstants.SensorType> {
        return withContext(Dispatchers.IO) {
            try {
                if (pontoIdNuvem.isNullOrEmpty()) {
                    return@withContext emptyList()
                }

                val snapshot = leiturasSensoresCollection
                    .whereEqualTo("pontoId", pontoIdNuvem)
                    .get()
                    .await()

                val sensoresEncontrados = mutableSetOf<String>()
                snapshot.documents.forEach { doc ->
                    doc.getString("sensorId")?.let { sensoresEncontrados.add(it) }
                }

                sensoresEncontrados.mapNotNull { sensorId ->
                    WaterQualityConstants.SensorType.fromId(sensorId)
                }.distinct()
            } catch (e: Exception) {
                Log.e("PontoColetaRepo", "Erro ao buscar sensores disponíveis", e)
                emptyList()
            }
        }
    }

    /**
     * Busca leituras de múltiplos pontos de coleta (útil para comparações)
     */
    suspend fun getLeiturasMultiplosPontos(
        pontosIdNuvem: List<String>,
        sensorType: WaterQualityConstants.SensorType? = null,
        limit: Int = 50
    ): Map<String, List<LeituraSensor>> {
        return withContext(Dispatchers.IO) {
            try {
                val resultados = mutableMapOf<String, List<LeituraSensor>>()

                pontosIdNuvem.forEach { pontoId ->
                    val leituras = if (sensorType != null) {
                        getLeiturasComFiltros(pontoId, sensorType = sensorType, limit = limit)
                    } else {
                        getLeiturasRecentes(pontoId, limit)
                    }
                    resultados[pontoId] = leituras
                }

                resultados
            } catch (e: Exception) {
                Log.e("PontoColetaRepo", "Erro ao buscar leituras de múltiplos pontos", e)
                emptyMap()
            }
        }
    }

    /**
     * Obtém todos os pontos de coleta (públicos e do usuário)
     */
    suspend fun getTodosPontosAcessiveis(): Result<List<PontoColeta>> {
        return withContext(Dispatchers.IO) {
            try {
                val pontosUsuario = getPontosColetaDoUsuario().getOrNull() ?: emptyList()
                val pontosPublicos = getPontosPublicos().getOrNull() ?: emptyList()

                // Combinar e remover duplicatas baseado no ID
                val todosPontos = (pontosUsuario + pontosPublicos).distinctBy { it.id }

                Result.success(todosPontos)
            } catch (e: Exception) {
                Log.e("PontoColetaRepo", "Erro ao buscar todos os pontos acessíveis", e)
                Result.failure(e)
            }
        }
    }
}
