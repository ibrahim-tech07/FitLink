package com.example.fitlinktrainer.data.model
data class Workout(
    val id: String = "",
    val userId: String = "",
    val trainerId: String = "",

    val title: String = "",
    val description: String = "",

    val exercises: List<Exercise> = emptyList(),
    val difficulty: String = "Beginner",
    val completedDate: Long? = null,
    val durationMinutes: Int = 0,
    val caloriesBurnEstimate: Int = 0,

    val scheduledDate: Long = System.currentTimeMillis(),

    val mediaUrl: String? = null,
    val imageUrl: String? = null,// workout thumbnail
    val videoUrl: String? = null,   // workout demo video

    val status: WorkoutStatus = WorkoutStatus.SCHEDULED,

    val progress: Int = 0
)

enum class WorkoutStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    MISSED
}