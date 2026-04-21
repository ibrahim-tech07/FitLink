package com.example.fitlinktrainer.data.repository



import com.example.fitlinktrainer.data.model.User
import com.example.fitlinktrainer.data.service.FirebaseService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firebaseService: FirebaseService
) {
    fun listenToUser(userId: String): Flow<User?> =
        firebaseService.listenToUser(userId)  // need to add listenToUser in FirebaseService

    suspend fun getUser(userId: String): Result<User?> =
        firebaseService.getUser(userId)
    fun listenToClients(trainerId: String): Flow<List<User>> =
        firebaseService.listenToClients(trainerId)
}