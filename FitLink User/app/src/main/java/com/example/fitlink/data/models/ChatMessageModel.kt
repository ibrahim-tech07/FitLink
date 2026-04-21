package com.example.fitlink.data.models


data class ChatMessageModel(
    val messageId: String = "",
    val userId: String = "",
    val userName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.WORKOUT_COMPLETE,
    val read: Boolean = false,
    val metadata: MessageMetadata? = null
)



data class MessageMetadata(
    val value: Int = 0,
    val unit: String = "",
    val icon: String = "",
    val color: String = ""
)