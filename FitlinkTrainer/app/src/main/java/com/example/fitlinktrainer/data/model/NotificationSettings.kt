package com.example.fitlinktrainer.data.model



data class NotificationSettings(
    val workoutReminders: Boolean = true,
    val achievementAlerts: Boolean = true,
    val messageAlerts: Boolean = true,
    val streakAlerts: Boolean = true,
    val goalAlerts: Boolean = true,
    val marketingAlerts: Boolean = false,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0
)
