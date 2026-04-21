package com.example.fitlinktrainer.data.model



data class Activity(
    val id: String = "",
    val type: String = "",
    val description: String = "",
    val timestamp: Long = 0L,
    val userId: String? = null,
    val trainerId: String? = null
)