package com.example.fitlinktrainer.data.repository

import android.util.Log
import com.example.fitlinktrainer.data.model.*
import com.example.fitlinktrainer.data.service.FirebaseService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAdminRepository @Inject constructor(
    private val firebase: FirebaseService
) : AdminRepository {

    private val tag = "FirebaseAdminRepo"

    // ================= TRAINERS =================

    override fun getPendingTrainers(): Flow<List<Trainer>> = callbackFlow {

        val listener = firebase.firestore
            .collection(FirebaseService.TRAINERS)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, error ->

                if (error != null) {
                    Log.e(tag, "getPendingTrainers error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val trainers = snap?.documents
                    ?.mapNotNull { it.toObject(Trainer::class.java) }
                    ?: emptyList()

                trySend(trainers)
            }

        awaitClose { listener.remove() }
    }

    override fun getAllTrainers(): Flow<List<Trainer>> = callbackFlow {

        val listener = firebase.firestore
            .collection(FirebaseService.TRAINERS)
            .addSnapshotListener { snap, error ->

                if (error != null) {
                    Log.e(tag, "getAllTrainers error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val trainers = snap?.documents
                    ?.mapNotNull { it.toObject(Trainer::class.java) }
                    ?: emptyList()

                trySend(trainers)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun updateTrainerStatus(
        trainerId: String,
        status: String
    ) {
        try {
            firebase.firestore
                .collection(FirebaseService.TRAINERS)
                .document(trainerId)
                .update("status", status)
                .await()
        } catch (e: Exception) {
            Log.e(tag, "updateTrainerStatus error", e)
        }
    }

    override suspend fun suspendTrainer(
        trainerId: String,
        suspended: Boolean
    ) {
        try {
            firebase.firestore
                .collection(FirebaseService.TRAINERS)
                .document(trainerId)
                .update("suspended", suspended)
                .await()
        } catch (e: Exception) {
            Log.e(tag, "suspendTrainer error", e)
        }
    }

    // ================= USERS =================

    override fun getAllUsers(): Flow<List<User>> = callbackFlow {

        val listener = firebase.firestore
            .collection(FirebaseService.USERS)
            .addSnapshotListener { snap, error ->

                if (error != null) {
                    Log.e(tag, "getAllUsers error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val users = snap?.documents
                    ?.mapNotNull { it.toObject(User::class.java) }
                    ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun blockUser(
        userId: String,
        blocked: Boolean
    ) {
        try {
            firebase.firestore
                .collection(FirebaseService.USERS)
                .document(userId)
                .update("blocked", blocked)
                .await()
        } catch (e: Exception) {
            Log.e(tag, "blockUser error", e)
        }
    }

    override suspend fun deleteUser(userId: String) {
        try {
            firebase.firestore
                .collection(FirebaseService.USERS)
                .document(userId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(tag, "deleteUser error", e)
        }
    }

    // ================= NOTIFICATIONS =================

    override suspend fun sendNotification(notification: Notification) {
        try {
            firebase.firestore
                .collection(FirebaseService.NOTIFICATIONS)
                .document(notification.id)
                .set(notification)
                .await()
        } catch (e: Exception) {
            Log.e(tag, "sendNotification error", e)
        }
    }

    // ================= DASHBOARD =================

    override fun getDashboardStats(): Flow<DashboardStats> = callbackFlow {

        var usersCount = 0
        var trainersCount = 0
        var workoutsCount = 0

        val usersListener =
            firebase.firestore.collection(FirebaseService.USERS)
                .addSnapshotListener { snap, error ->

                    if (error != null) {
                        Log.e(tag, "Users stats error", error)
                        return@addSnapshotListener
                    }

                    usersCount = snap?.size() ?: 0

                    trySend(
                        DashboardStats(
                            totalUsers = usersCount,
                            totalTrainers = trainersCount,
                            activeWorkouts = workoutsCount
                        )
                    )
                }

        val trainersListener =
            firebase.firestore.collection(FirebaseService.TRAINERS)
                .addSnapshotListener { snap, error ->

                    if (error != null) {
                        Log.e(tag, "Trainers stats error", error)
                        return@addSnapshotListener
                    }

                    trainersCount = snap?.size() ?: 0

                    trySend(
                        DashboardStats(
                            totalUsers = usersCount,
                            totalTrainers = trainersCount,
                            activeWorkouts = workoutsCount
                        )
                    )
                }

        val workoutsListener =
            firebase.firestore.collection(FirebaseService.WORKOUTS)
                .addSnapshotListener { snap, error ->

                    if (error != null) {
                        Log.e(tag, "Workouts stats error", error)
                        return@addSnapshotListener
                    }

                    workoutsCount = snap?.size() ?: 0

                    trySend(
                        DashboardStats(
                            totalUsers = usersCount,
                            totalTrainers = trainersCount,
                            activeWorkouts = workoutsCount
                        )
                    )
                }

        awaitClose {
            usersListener.remove()
            trainersListener.remove()
            workoutsListener.remove()
        }
    }
}