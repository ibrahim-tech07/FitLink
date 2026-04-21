package com.example.fitlinktrainer.data.repository

import com.example.fitlinktrainer.data.service.FirebaseService
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseService: FirebaseService
) {

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return firebaseService.login(email, password)
    }

    suspend fun register(email: String, password: String): Result<FirebaseUser> {
        return firebaseService.register(email, password)
    }

    fun logout() {
        firebaseService.logout()
    }

    fun getCurrentUser(): FirebaseUser? {
        return firebaseService.auth.currentUser
    }
}