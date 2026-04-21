package com.example.fitlinktrainer.data.repository

import com.example.fitlinktrainer.data.model.Workout
import com.example.fitlinktrainer.data.service.FirebaseService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val firebaseService: FirebaseService
) {

    fun listenToTrainerWorkouts(trainerId: String): Flow<List<Workout>> =
        firebaseService.listenToAssignedWorkouts(trainerId)

    fun listenToUserWorkouts(userId: String): Flow<List<Workout>> =
        firebaseService.listenToUserWorkouts(userId)
    suspend fun createWorkout(workout: Workout): Result<String> =
        firebaseService.createWorkout(workout)

}