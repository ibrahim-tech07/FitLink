// File: com/example/fitlink/data/models/Notification.kt
package com.example.fitlinktrainer.data.model

data class Notification(
    val id: String = "",
    val userId: String = "",
    val trainerId: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val actionData: Map<String, String>? = null,
    val imageUrl: String? = null
)

enum class NotificationType {
    WORKOUT_REMINDER,
    ACHIEVEMENT_UNLOCKED,
    TRAINER_MESSAGE,
    STREAK_ALERT,
    GOAL_COMPLETED,
    SESSION_SCHEDULED,
    GENERAL
}