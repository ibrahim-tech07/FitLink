package com.example.fitlink.data.repositories

import com.example.fitlink.data.models.AchievementModel
import com.example.fitlink.data.models.User
import com.example.fitlink.data.service.FirebaseService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firebaseService: FirebaseService
) {

    fun getUserStream(userId: String): Flow<User?> =
        firebaseService.listenToUser(userId)

    suspend fun getUser(userId: String): Result<User?> =
        firebaseService.getUser(userId)

    suspend fun updateUser(
        userId: String,
        data: Map<String, Any>
    ): Result<Boolean> =
        firebaseService.updateUser(userId, data)

    suspend fun updateProfile(
        userId: String,
        name: String,
        phone: String,
        age: Int,
        gender: String,
        height: Double,
        weight: Double
    ): Result<Boolean> {

        return firebaseService.updateUser(
            userId,
            mapOf(
                "name" to name,
                "phone" to phone,
                "age" to age,
                "gender" to gender,
                "height" to height,
                "weight" to weight
            )
        )
    }

    suspend fun updateFitnessLevel(
        userId: String,
        level: String
    ): Result<Boolean> {

        return firebaseService.updateUser(
            userId,
            mapOf("fitnessLevel" to level)
        )
    }

    suspend fun updateGoals(
        userId: String,
        calorieGoal: Int,
        workoutGoal: Int
    ): Result<Boolean> {

        return firebaseService.updateUser(
            userId,
            mapOf(
                "dailyCalorieGoal" to calorieGoal,
                "weeklyWorkoutGoal" to workoutGoal
            )
        )
    }

    suspend fun updateUserProfileImage(
        userId: String,
        imageUrl: String
    ): Result<Boolean> {

        return firebaseService.updateUser(
            userId,
            mapOf("profileImageUrl" to imageUrl)
        )
    }

    fun getUserAchievementsStream(
        userId: String
    ): Flow<List<AchievementModel>> {

        return firebaseService.listenToUserAchievements(userId)
    }

    suspend fun saveAchievement(
        userId: String,
        achievement: AchievementModel
    ) {

        firebaseService.saveAchievement(userId, achievement)
    }  suspend fun createUser(user: User): Result<Boolean> {
        return firebaseService.createUser(user)
    }

    suspend fun getCurrentUser(): Result<User?> {

        val currentUid =
            FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

        return try {

            val snapshot =
                firebaseService.firestore
                    .collection("users")
                    .document(currentUid)
                    .get()
                    .await()

            val user =
                snapshot.toObject(User::class.java)?.copy(id = snapshot.id)

            Result.success(user)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}