// File: com/example/fitlink/data/models/User.kt
package com.example.fitlink.data.models

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val profileImageUrl: String = "",
    val age: Int = 0,
    val gender: String = "",
    val height: Double = 0.0,
    val weight: Double = 0.0,
    val fitnessGoal: String = "Beginner",
    val dailyCalorieGoal: Int = 2000,
    val weeklyWorkoutGoal: Int = 5,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val lastStreakDate: Long = 0L,
    val totalWorkoutsCompleted: Int = 0,
    val totalCaloriesBurned: Int = 0,
    val achievements: List<String> = emptyList(),
    val connectedTrainerId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
    val preferences: UserPreferences = UserPreferences(),
    val onboardingCompleted: Boolean = false
)
data class UserPreferences(
    val workoutReminderEnabled: Boolean = true,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0
)