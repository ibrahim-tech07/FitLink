package com.example.fitlink.data.models

data class Chat(

    val id: String = "",

    val trainerId: String = "",
    val userId: String = "",

    val participants: List<String> = emptyList(),

    val trainerName: String = "",
    val trainerImage: String = "",

    val userName: String = "",
    val userImage: String = "",

    val lastMessage: String = "",
    val lastMessageTime: Long = 0,

    val unreadCountTrainer: Int = 0,
    val unreadCountUser: Int = 0,
    val isVideoCalling: Boolean = false,
    val videoChannelId: String = "",
    val callStartedBy: String = "",
    val callStartedAt: Long = 0L,
    val callStatus: String = "",
    val createdAt: Long = 0
) {

    val isOnline: Boolean
        get() = false
}