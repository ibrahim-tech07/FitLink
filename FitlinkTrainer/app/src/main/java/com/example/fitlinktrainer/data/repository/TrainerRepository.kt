package com.example.fitlinktrainer.data.repository

import android.content.Context
import android.net.Uri
import com.example.fitlinktrainer.data.model.Trainer
import com.example.fitlinktrainer.data.model.User
import com.example.fitlinktrainer.data.service.CloudinaryService
import com.example.fitlinktrainer.data.service.FirebaseService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainerRepository @Inject constructor(
    private val firebaseService: FirebaseService,
    private val cloudinaryService: CloudinaryService
) {

    suspend fun createTrainer(trainer: Trainer): Result<Boolean> {

        return firebaseService.createTrainer(trainer)
    }

    suspend fun getTrainer(trainerId: String) =

        firebaseService.getTrainer(trainerId)

    fun listenToTrainer(trainerId: String): Flow<Trainer?> =

        firebaseService.listenToTrainer(trainerId)

    fun listenToClients(trainerId: String): Flow<List<User>> =

        firebaseService.listenToClients(trainerId)

    suspend fun updateTrainer(trainer: Trainer): Result<Boolean> {

        return firebaseService.updateTrainer(trainer)
    }

    suspend fun uploadProfileImage(
        context: Context,
        uri: Uri
    ): String {

        return cloudinaryService.uploadProfileImage(context, uri)
    }

    suspend fun updateProfileImage(
        context: Context,
        uri: Uri,
        oldUrl: String?
    ): String {

        return cloudinaryService.updateProfileImage(
            context,
            uri,
            oldUrl
        )
    }
}