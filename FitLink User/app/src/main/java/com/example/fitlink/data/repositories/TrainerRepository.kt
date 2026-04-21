package com.example.fitlink.data.repositories

import com.example.fitlink.data.models.Trainer
import com.example.fitlink.data.service.FirebaseService
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainerRepository @Inject constructor(
    private val firebaseService: FirebaseService
) {

    private val firestore get() = firebaseService.firestore
    private val trainersRef get() = firestore.collection(FirebaseService.TRAINERS_COLLECTION)

    fun listenToTrainers(): Flow<List<Trainer>> = callbackFlow {

        val listener = trainersRef.addSnapshotListener { snapshot, error ->

            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val trainers = snapshot?.documents?.mapNotNull { doc ->

                val trainer = doc.toObject(Trainer::class.java)?.copy(id = doc.id)

                // 🔥 FILTER INVALID + ADMIN
                if (
                    trainer == null ||
                    trainer.email == "admin@fitlink.com" ||
                    trainer.name.isBlank() ||
                    trainer.specialties.isEmpty() ||
                    trainer.specialties.all { it.isBlank() }
                ) {
                    null
                } else trainer

            } ?: emptyList()

            trySend(trainers)
        }

        awaitClose { listener.remove() }
    }

    suspend fun connectWithTrainer(
        userId: String,
        trainerId: String,
        code: String
    ): Result<Boolean> = try {

        // 🔥 Remove user from all trainers (ONLY ONE CONNECTION)
        val snapshot = trainersRef.get().await()

        snapshot.documents.forEach {
            it.reference.update(
                "clientIds",
                FieldValue.arrayRemove(userId)
            )
        }

        // 🔥 Add to selected trainer
        trainersRef.document(trainerId)
            .update("clientIds", FieldValue.arrayUnion(userId))
            .await()

        Result.success(true)

    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun disconnectTrainer(
        userId: String,
        trainerId: String
    ): Result<Boolean> = try {

        trainersRef.document(trainerId)
            .update("clientIds", FieldValue.arrayRemove(userId))
            .await()

        Result.success(true)

    } catch (e: Exception) {
        Result.failure(e)
    }
    // In TrainerRepository.kt (add this method)
    suspend fun getTrainerById(trainerId: String): Trainer? {
        return try {
            val doc = trainersRef.document(trainerId).get().await()
            doc.toObject(Trainer::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }
}