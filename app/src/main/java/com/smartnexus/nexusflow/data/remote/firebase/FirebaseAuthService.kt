package com.smartnexus.nexusflow.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.smartnexus.nexusflow.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthService @Inject constructor() {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUserFlow: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            val mappedUser = firebaseUser?.let {
                User(
                    uid = it.uid,
                    email = it.email,
                    displayName = it.displayName,
                    photoUrl = it.photoUrl?.toString()
                )
            }
            trySend(mappedUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    fun getCurrentUser(): User? {
        return auth.currentUser?.let {
            User(
                uid = it.uid,
                email = it.email,
                displayName = it.displayName,
                photoUrl = it.photoUrl?.toString()
            )
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }
}
