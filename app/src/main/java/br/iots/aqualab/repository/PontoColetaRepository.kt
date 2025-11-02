package br.iots.aqualab.repository

import br.iots.aqualab.model.PontoColeta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
}
