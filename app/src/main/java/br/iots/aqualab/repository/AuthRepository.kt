package br.iots.aqualab.repository

import android.net.Uri
import br.iots.aqualab.model.RequestStatus
import br.iots.aqualab.model.UserProfile
import br.iots.aqualab.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class AuthRepository {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()

    suspend fun registerUser(email: String, pass: String, displayName: String): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user
                if (firebaseUser == null) {
                    Result.failure(Exception("Falha ao criar usuário no Firebase Auth"))
                } else {
                    val newUserProfile = UserProfile(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = displayName,
                        role = UserRole.COMMON,
                        requestedRole = null,
                        roleRequestStatus = null
                    )

                    usersCollection.document(firebaseUser.uid).set(newUserProfile).await()
                    Result.success(newUserProfile)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun loginUser(email: String, pass: String): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user
                if (firebaseUser == null) {
                    Result.failure(Exception("Usuário do Firebase não encontrado após login"))
                } else {
                    val profileSnapshot = usersCollection.document(firebaseUser.uid).get().await()
                    if (profileSnapshot.exists()) {
                        val userProfile = profileSnapshot.toObject(UserProfile::class.java)
                        if (userProfile != null) {
                            Result.success(userProfile)
                        } else {
                            Result.failure(Exception("Falha ao converter snapshot para UserProfile"))
                        }
                    } else {
                        Result.success(
                            UserProfile(
                                uid = firebaseUser.uid,
                                email = firebaseUser.email,
                                role = UserRole.COMMON
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getCurrentFirebaseUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    suspend fun getLoggedInUserProfile(): Result<UserProfile?> {
        val firebaseUser = getCurrentFirebaseUser()
        return if (firebaseUser != null) {
            withContext(Dispatchers.IO) {
                try {
                    val profileSnapshot = usersCollection.document(firebaseUser.uid).get().await()
                    if (profileSnapshot.exists()) {
                        val userProfile = profileSnapshot.toObject(UserProfile::class.java)
                        Result.success(userProfile)
                    } else {
                        Result.success(UserProfile(uid = firebaseUser.uid, email = firebaseUser.email, role = UserRole.COMMON))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        } else {
            Result.success(null)
        }
    }

    fun logoutUser() {
        firebaseAuth.signOut()
    }

    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val user = firebaseAuth.currentUser
                if (user == null) {
                    Result.failure(Exception("Usuário não autenticado para fazer upload da imagem."))
                } else {
                    val fileName = "${user.uid}.jpg"
                    val storageRef = firebaseStorage.reference.child("profile_images/$fileName")
                    val downloadUrl = storageRef.putFile(imageUri).await().storage.downloadUrl.await().toString()
                    Result.success(downloadUrl)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                usersCollection.document(userProfile.uid).set(userProfile).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getPendingRoleRequests(): Result<List<UserProfile>> {
        return withContext(Dispatchers.IO) {
            try {
                val querySnapshot = usersCollection
                    .whereEqualTo("roleRequestStatus", RequestStatus.PENDING.name)
                    .get()
                    .await()

                val users = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(UserProfile::class.java)
                }
                Result.success(users)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}