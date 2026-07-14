package com.smartnexus.nexusflow.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.smartnexus.nexusflow.data.remote.firebase.FirebaseAuthService
import com.smartnexus.nexusflow.domain.model.User
import com.smartnexus.nexusflow.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authService: FirebaseAuthService
) : AuthRepository {

    override fun getCurrentUser(): Flow<User?> = authService.currentUserFlow

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    displayName = firebaseUser.displayName,
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
                
                // Write user details to Realtime Database users/{userId}/profile node
                try {
                    val ref = FirebaseDatabase.getInstance().getReference("users/${user.uid}/profile")
                    ref.setValue(user).await()
                } catch (dbEx: Exception) {
                    // Ignore DB write errors so authentication still succeeds if offline/permission issue
                }

                Result.success(user)
            } else {
                Result.failure(Exception("Firebase user is null after sign in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            authService.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
