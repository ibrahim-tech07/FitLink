// File: com/example/fitlink/data/repositories/AuthRepository.kt
package com.example.fitlink.data.repositories

import com.example.fitlink.data.models.User
import com.example.fitlink.data.service.FirebaseService
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseService: FirebaseService
) {

    suspend fun login(email: String, password: String): Result<FirebaseUser> = try {
        val result = firebaseService.auth.signInWithEmailAndPassword(email, password).await()
        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun register(
        email: String,
        password: String,
        name: String,
        phone: String
    ): Result<FirebaseUser> = try {
        val result = firebaseService.auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user!!

        // Create user profile
        val user = User(
            id = firebaseUser.uid,
            email = email,
            name = name,
            phone = phone,
            createdAt = System.currentTimeMillis(),
            lastActive = System.currentTimeMillis()
        )

        firebaseService.createUser(user)

        Result.success(firebaseUser)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseService.auth.signInWithCredential(credential).await()
        val firebaseUser = result.user!!

        // Check if user exists, if not create profile
        val existingUser = firebaseService.getUser(firebaseUser.uid).getOrNull()
        if (existingUser == null) {
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: "User",
                profileImageUrl = firebaseUser.photoUrl?.toString() ?: "",
                createdAt = System.currentTimeMillis(),
                lastActive = System.currentTimeMillis()
            )
            firebaseService.createUser(user)
        }

        Result.success(firebaseUser)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun logout() {
        firebaseService.auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? = firebaseService.auth.currentUser

    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> = try {
        firebaseService.auth.sendPasswordResetEmail(email).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }
}