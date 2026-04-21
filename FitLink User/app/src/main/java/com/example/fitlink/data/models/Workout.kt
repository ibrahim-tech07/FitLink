// File: com/example/fitlink/data/models/Workout.kt
package com.example.fitlink.data.models

data class Workout(
    val id: String = "",
    val userId: String = "",
    val trainerId: String = "",

    val title: String = "",
    val description: String = "",
    val difficulty: String = "Beginner",

    val exercises: List<Exercise> = emptyList(),

    val durationMinutes: Int = 0,
    val caloriesBurnEstimate: Int = 0,

    val scheduledDate: Long = System.currentTimeMillis(),

    val mediaUrl: String? = null,   // workout thumbnail
    val videoUrl: String? = null,   // workout demo video

    val status: WorkoutStatus = WorkoutStatus.SCHEDULED,
    val completedDate: Long? = null,
    val progress: Int = 0
)
enum class WorkoutStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    MISSED,
    CANCELLED
}



data class WorkoutFeedback(
    val rating: Int = 0,
    val comment: String = "",
    val difficulty: String = "",
    val submittedAt: Long = 0
)