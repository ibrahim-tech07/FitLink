package com.example.fitlink.data.models

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENT,
    val attachments: List<String> = emptyList()
)

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    FILE,
    WORKOUT,
    ACHIEVEMENT,
    GENERAL,
    WORKOUT_COMPLETE,
    STREAK_ALERT,
    WELCOME
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}