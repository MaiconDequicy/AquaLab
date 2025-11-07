package br.iots.aqualab.repository

import android.util.Log
import br.iots.aqualab.model.PontoColeta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PontoColetaRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val pontosColetaCollection = firestore.collection("pontosDeColeta")

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

                val pontos = snapshot.toObjects(PontoColeta::class.java)
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
            } catch (e: Exception) {
                Log.e("PontoColetaRepository", "Erro ao deletar ponto de coleta no Firestore", e)
                Result.failure(e)
            }
        }
    }
}
