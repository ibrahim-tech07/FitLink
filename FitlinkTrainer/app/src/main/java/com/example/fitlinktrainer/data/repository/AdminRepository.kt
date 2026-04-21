package com.example.fitlinktrainer.data.repository

import com.example.fitlinktrainer.data.model.*
import kotlinx.coroutines.flow.Flow

interface AdminRepository {

    fun getPendingTrainers(): Flow<List<Trainer>>

    fun getAllTrainers(): Flow<List<Trainer>>

    fun getAllUsers(): Flow<List<User>>

    suspend fun updateTrainerStatus(
        trainerId: String,
        status: String
    )

    suspend fun suspendTrainer(
        trainerId: String,
        suspended: Boolean
    )

    suspend fun blockUser(
        userId: String,
        blocked: Boolean
    )

    suspend fun deleteUser(userId: String)

    suspend fun sendNotification(notification: Notification)

    fun getDashboardStats(): Flow<DashboardStats>

}