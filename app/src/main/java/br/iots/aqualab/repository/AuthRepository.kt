package br.iots.aqualab.repository

import br.iots.aqualab.model.UserProfile
import br.iots.aqualab.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class AuthRepository {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun registerUser(email: String, pass: String, displayName: String): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try
            {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user
                if (firebaseUser == null) {
                    Result.failure(Exception("Falha ao criar usuário no Firebase Auth"))
                } else {
                    val newUserProfile = UserProfile(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = displayName,
                        role = UserRole.COMMON
                    )

                    usersCollection.document(firebaseUser.uid).set(
                        mapOf(
                            "uid" to newUserProfile.uid,
                            "email" to newUserProfile.email,
                            "displayName" to newUserProfile.displayName,
                            "role" to newUserProfile.role.name,
                            "photoUrl" to null
                        )
                    ).await()
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
                        val displayName = profileSnapshot.getString("displayName")
                        val photoUrl = profileSnapshot.getString("photoUrl")
                        val roleString = profileSnapshot.getString("role") ?: UserRole.COMMON.name
                        val role = UserRole.entries.firstOrNull { it.name == roleString } ?: UserRole.COMMON

                        val userProfile = UserProfile(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email,
                            displayName = displayName,
                            photoUrl = photoUrl,
                            role = role
                        )
                        Result.success(userProfile)
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
            } catch (e: Exception)
            {
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
                        val displayName = profileSnapshot.getString("displayName")
                        val photoUrl = profileSnapshot.getString("photoUrl")
                        val roleString = profileSnapshot.getString("role") ?: UserRole.COMMON.name
                        val role = UserRole.entries.firstOrNull { it.name == roleString } ?: UserRole.COMMON

                        Result.success(UserProfile(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email,
                            displayName = displayName,
                            photoUrl = photoUrl,
                            role = role
                        ))
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

}