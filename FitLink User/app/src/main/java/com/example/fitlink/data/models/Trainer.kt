package com.example.fitlink.data.models

data class Trainer(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val bio: String = "",
    val phoneNumber: String = "",
    val specialties: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),

    val experience: Int = 0,
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val hourlyRate: Double = 0.0,

    val isVerified: Boolean = false,

    // ✅ Store client userIds here
    val clientIds: List<String> = emptyList(),

    val successRate: Int = 0,
    val availability: List<TimeSlot> = emptyList(),
    val status: String = "pending"
)

data class TimeSlot(
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val isAvailable: Boolean = true
)
