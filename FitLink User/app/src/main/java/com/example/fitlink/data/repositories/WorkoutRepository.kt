// File: com/example/fitlink/data/repositories/WorkoutRepository.kt
package com.example.fitlink.data.repositories

import com.example.fitlink.data.models.Workout
import com.example.fitlink.data.service.FirebaseService
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.example.fitlink.data.models.WorkoutStatus

@Singleton
class WorkoutRepository @Inject constructor(
    private val firebaseService: FirebaseService

) {

    companion object {
        private const val MILLIS_IN_DAY = 24L * 60L * 60L * 1000L
    }
    private val _refreshEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshEvents: SharedFlow<Unit> = _refreshEvents
    /**
     * Update an arbitrary workout document fields.
     */
    suspend fun updateWorkout(workoutId: String, data: Map<String, Any>): Result<Boolean> = try {
        firebaseService.firestore
            .collection("workouts")
            .document(workoutId)
            .update(data)
            .await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Get a single workout by id.
     */
    suspend fun getWorkoutById(workoutId: String): Result<Workout?> = try {
        val doc = firebaseService.firestore.collection("workouts").document(workoutId).get().await()
        Result.success(doc.toObject(Workout::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Complete workflow: runs the atomic transaction implemented in FirebaseService.
     * calories: integer calories to add
     * durationSeconds: duration in seconds (be consistent across client)
     */
    // File: com/example/fitlink/data/repositories/WorkoutRepository.kt

    suspend fun completeWorkoutAtomic(
        userId: String,
        workoutId: String,
        workoutTitle: String,          // new
        calories: Int,
        durationSeconds: Int
    ): Result<Boolean> {
        val result = firebase_service_completeAtomic(
            userId,
            workoutId,
            workoutTitle,
            calories,
            durationSeconds
        )
        result.onSuccess { _refreshEvents.tryEmit(Unit) }
        return result
    }

    private suspend fun firebase_service_completeAtomic(
        userId: String,
        workoutId: String,
        workoutTitle: String,
        calories: Int,
        durationSeconds: Int
    ): Result<Boolean> = try {
        firebaseService.completeWorkoutAtomic(userId, workoutId, workoutTitle, calories, durationSeconds)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Fetch workouts that either were scheduled in the given month OR completed in the given month.
     *
     * We run two queries (scheduledDate range, completedDate range) and merge results to simulate an 'OR'.
     *
     * @param userId owner
     * @param year e.g. 2026
     * @param month 1..12 (same as YearMonth.monthValue)
     */
    suspend fun getWorkoutsByMonth(userId: String, year: Int, month: Int): Result<List<Workout>> = try {
        // compute month start and next month start (device timezone)
        val cal = java.util.Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val nextMonthStart = cal.timeInMillis

        // Query scheduledDate in range
        val scheduledQuery = firebaseService.firestore
            .collection("workouts")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("scheduledDate", monthStart)
            .whereLessThan("scheduledDate", nextMonthStart)
            .get()
            .await()

        // Query completedDate in range (may be 0 or absent for many docs)
        val completedQuery = firebaseService.firestore
            .collection("workouts")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("completedDate", monthStart)
            .whereLessThan("completedDate", nextMonthStart)
            .get()
            .await()

        val fromScheduled = scheduledQuery.documents.mapNotNull { it.toObject(Workout::class.java) }
        val fromCompleted = completedQuery.documents.mapNotNull { it.toObject(Workout::class.java) }

        // Merge + dedupe by id
        val merged = (fromScheduled + fromCompleted)
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.scheduledDate ?: Long.MAX_VALUE }, { it.completedDate ?: Long.MAX_VALUE }))

        Result.success(merged)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Fetch workouts for a specific day.
     *
     * The `dayStartMillis` should be start-of-day (00:00 local). This returns workouts
     * which were scheduled on that day OR completed on that day (merged/deduped).
     */
    suspend fun getWorkoutsByDate(userId: String, dayStartMillis: Long): Result<List<Workout>> = try {
        val dayEnd = dayStartMillis + MILLIS_IN_DAY

        val scheduledQuery = firebaseService.firestore
            .collection("workouts")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("scheduledDate", dayStartMillis)
            .whereLessThan("scheduledDate", dayEnd)
            .get()
            .await()

        val completedQuery = firebaseService.firestore
            .collection("workouts")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("completedDate", dayStartMillis)
            .whereLessThan("completedDate", dayEnd)
            .get()
            .await()

        val fromScheduled = scheduledQuery.documents.mapNotNull { it.toObject(Workout::class.java) }
        val fromCompleted = completedQuery.documents.mapNotNull { it.toObject(Workout::class.java) }

        val merged = (fromScheduled + fromCompleted)
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.scheduledDate }, { it.completedDate ?: Long.MAX_VALUE }))

        Result.success(merged)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Generic helper: get all workouts for a user (optionally limit)
     */
    suspend fun getAllWorkoutsForUser(userId: String, limit: Long = 100): Result<List<Workout>> = try {
        val query = firebaseService.firestore
            .collection("workouts")
            .whereEqualTo("userId", userId)
            .orderBy("scheduledDate")
            .limit(limit)
            .get()
            .await()

        val list = query.documents.mapNotNull { it.toObject(Workout::class.java) }
        Result.success(list)
    } catch (e: Exception) {
        Result.failure(e)
    }
}