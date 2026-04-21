package com.example.fitlinktrainer.data.model



data class DashboardStats(

    val totalUsers: Int = 0,

    val totalTrainers: Int = 0,

    val activeWorkouts: Int = 0,

    val mrr: Int = 0,

    val userChange: Float = 0f,

    val trainerChange: Float = 0f,

    val workoutChange: Float = 0f,

    val mrrChange: Float = 0f,

    val registrationsOverTime: List<Float> = emptyList(),

    val workoutsPerDay: List<Float> = emptyList()

)