// File: com/example/fitlink/data/repositories/DailyStatsRepository.kt
package com.example.fitlink.data.repositories

import com.example.fitlink.data.models.DailyStats
import com.example.fitlink.data.service.FirebaseService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyStatsRepository @Inject constructor(
    private val firebaseService: FirebaseService
) {

    fun getTodayStatsStream(userId: String): Flow<DailyStats?> =
        firebaseService.listenToDailyStats(userId)

    suspend fun getTodayStats(userId: String): Result<DailyStats?> =
        firebaseService.getTodayStats(userId)

    suspend fun updateStats(stats: DailyStats): Result<Boolean> =
        firebaseService.updateDailyStats(stats)

    suspend fun updateWaterIntake(statsId: String, glasses: Int): Result<Boolean> = try {
        firebaseService.firestore
            .collection(FirebaseService.DAILY_STATS_COLLECTION)
            .document(statsId)
            .update("waterIntake", glasses)
            .await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateSteps(statsId: String, steps: Int): Result<Boolean> = try {
        firebaseService.firestore
            .collection(FirebaseService.DAILY_STATS_COLLECTION)
            .document(statsId)
            .update("steps", steps)
            .await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getWeeklyStats(userId: String): Result<List<DailyStats>> = try {
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)

        val snapshot = firebaseService.firestore
            .collection(FirebaseService.DAILY_STATS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", weekAgo)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()

        val stats = snapshot.documents.mapNotNull { it.toObject(DailyStats::class.java) }
        Result.success(stats)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getMonthlyStats(userId: String, year: Int, month: Int): Result<List<DailyStats>> = try {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        val startOfMonth = calendar.timeInMillis
        calendar.set(year, month - 1, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH), 23, 59, 59)
        val endOfMonth = calendar.timeInMillis

        val snapshot = firebaseService.firestore
            .collection(FirebaseService.DAILY_STATS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThanOrEqualTo("date", endOfMonth)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get()
            .await()

        val stats = snapshot.documents.mapNotNull { it.toObject(DailyStats::class.java) }
        Result.success(stats)
    } catch (e: Exception) {
        Result.failure(e)
    }
}