// File: com/example/fitlink/data/models/AchievementModel.kt
package com.example.fitlink.data.models

data class AchievementModel(
    val achievementId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val icon: String = "",
    val dateEarned: Long = System.currentTimeMillis(),
    val progress: Int = 0,
    val target: Int = 0,
    val completed: Boolean = false,
    val category: String = ""
)