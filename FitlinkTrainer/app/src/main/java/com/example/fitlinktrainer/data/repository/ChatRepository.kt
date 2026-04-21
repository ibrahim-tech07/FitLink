package com.example.fitlinktrainer.data.repository

import com.example.fitlinktrainer.data.model.Chat
import com.example.fitlinktrainer.data.model.Message
import com.example.fitlinktrainer.data.service.FirebaseService
import com.example.fitlinktrainer.data.service.FirebaseService.Companion.CHATS
import com.example.fitlinktrainer.data.service.FirebaseService.Companion.USERS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firebaseService: FirebaseService
) {

    private val firestore = firebaseService.firestore

    fun listenTrainerChats(trainerId: String): Flow<List<Chat>> {
        return firebaseService.listenTrainerChats(trainerId)
    }

    fun listenMessages(chatId: String): Flow<List<Message>> {
        return firebaseService.listenToMessages(chatId)
    }

    // New method: listen to a single chat document in real time
    fun listenChat(chatId: String): Flow<Chat> {
        return firebaseService.listenToChat(chatId)
    }

    suspend fun markChatAsRead(
        chatId: String,
        isTrainer: Boolean
    ): Result<Boolean> = try {

        val field = if (isTrainer) {
            "unreadCountTrainer"
        } else {
            "unreadCountUser"
        }

        firestore.collection(CHATS)
            .document(chatId)
            .update(field, 0)
            .await()

        Result.success(true)

    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        content: String
    ): Result<Boolean> {
        return firebaseService.sendMessage(chatId, senderId, content)
    }

    suspend fun createChat(
        trainerId: String,
        userId: String,
        trainerName: String,
        trainerImage: String,
        userName: String
    ): Result<String> {
        return firebaseService.createChat(trainerId, userId, trainerName, trainerImage, userName)
    }

    suspend fun startVideoCall(chatId: String, startedBy: String): Result<String> {
        return try {
            val channelId = "fitlink_call_$chatId"

            firestore.collection(CHATS)
                .document(chatId)
                .update(
                    mapOf(
                        "isVideoCalling" to true,
                        "videoChannelId" to channelId,
                        "callStartedBy" to startedBy,
                        "callStartedAt" to System.currentTimeMillis(),
                        "callStatus" to "ringing"
                    )
                )
                .await()

            Result.success(channelId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptVideoCall(chatId: String): Result<Unit> {
        return try {
            firestore.collection(CHATS)
                .document(chatId)
                .update("callStatus", "accepted")
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineVideoCall(chatId: String): Result<Unit> {
        return try {
            firestore.collection(CHATS)
                .document(chatId)
                .update(
                    mapOf(
                        "isVideoCalling" to false,
                        "videoChannelId" to "",
                        "callStartedBy" to "",
                        "callStartedAt" to 0L,
                        "callStatus" to "ended"
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endVideoCall(chatId: String): Result<Unit> {
        return try {
            firestore.collection(CHATS)
                .document(chatId)
                .update(
                    mapOf(
                        "isVideoCalling" to false,
                        "videoChannelId" to "",
                        "callStartedBy" to "",
                        "callStartedAt" to 0L,
                        "callStatus" to "ended"
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}