// File: com/example/fitlink/data/models/DailyStats.kt
package com.example.fitlinktrainer.data.model

data class DailyStats(
    val id: String = "",
    val userId: String = "",
    val date: Long = System.currentTimeMillis(),
    val caloriesBurned: Int = 0,
    val caloriesGoal: Int = 2000,
    val workoutsCompleted: Int = 0,
    val workoutDuration: Int = 0,
    val waterIntake: Int = 0,
    val waterGoal: Int = 8,
    val activeMinutes: Int = 0,
    val streakDay: Boolean = false,
    val streakDays:Int=0,
    val achievements: List<String> = emptyList()
)