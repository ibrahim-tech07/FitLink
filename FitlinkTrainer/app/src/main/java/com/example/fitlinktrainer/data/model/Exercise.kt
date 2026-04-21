package com.example.fitlinktrainer.data.model



data class Exercise(
    val id: String = "",
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val duration: Boolean = true,
    val durationSeconds: Int = 60,
    // NEW
    val caloriesBurn: Int = 0,
    val restSeconds: Int = 30,

    // MEDIA
    val imageUrl: String? = null,
    val gifUrl: String? = null,
    val videoUrl: String? = null
)