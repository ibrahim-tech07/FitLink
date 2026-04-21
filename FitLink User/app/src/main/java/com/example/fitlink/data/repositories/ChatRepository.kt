package com.example.fitlink.data.repositories

import com.example.fitlink.data.models.Chat
import com.example.fitlink.data.models.ChatMessageModel
import com.example.fitlink.data.models.Message
import com.example.fitlink.data.models.MessageStatus
import com.example.fitlink.data.models.MessageType
import com.example.fitlink.data.service.FirebaseService
import com.example.fitlink.data.service.FirebaseService.Companion.CHATS_COLLECTION
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.emptyList

@Singleton
class ChatRepository @Inject constructor(
    private val firebaseService: FirebaseService,
) {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getChats(userId: String): Result<List<Chat>> = try {
        val snapshot = firestore
            .collection(CHATS_COLLECTION)
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .get()
            .await()

        val chats = snapshot.documents.mapNotNull {
            it.toObject(Chat::class.java)?.copy(id = it.id)
        }

        Result.success(chats)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getMessagesStream(chatId: String): Flow<List<Message>> =
        firebaseService.listenToChatMessages(chatId)

    // New method: listen to a single chat document in real time
    fun listenChat(chatId: String): Flow<Chat> {
        return firebaseService.listenToChat(chatId)
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        content: String
    ): Result<Boolean> {
        return try {
            if (content.isBlank()) {
                return Result.failure(Exception("Empty message"))
            }

            val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)
            val messageRef = chatRef.collection("messages").document()

            val message = Message(
                id = messageRef.id,
                chatId = chatId,
                senderId = senderId,
                content = content,
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT,
                status = MessageStatus.SENT
            )

            firestore.runTransaction { transaction ->
                transaction.set(messageRef, message)
                transaction.update(chatRef, mapOf(
                    "lastMessage" to content,
                    "lastMessageTime" to message.timestamp,
                    "unreadCount" to FieldValue.increment(1)
                ))
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getRecentActivityStream(userId: String): Flow<List<ChatMessageModel>> {
        return firebaseService.listenToUserActivity(userId)
    }

    suspend fun markMessagesAsRead(
        chatId: String,
        userId: String
    ): Result<Boolean> = try {
        val chatDocRef = firestore
            .collection(CHATS_COLLECTION)
            .document(chatId)

        val snapshot = chatDocRef
            .collection("messages")
            .whereNotEqualTo("senderId", userId)
            .whereEqualTo("status", MessageStatus.SENT.name)
            .get()
            .await()

        val batch = firestore.batch()

        snapshot.documents.forEach {
            batch.update(it.reference, "status", MessageStatus.READ.name)
        }

        batch.update(
            chatDocRef,
            "unreadCounts.$userId",
            0
        )

        batch.commit().await()

        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getChatsStream(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection(CHATS_COLLECTION)
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull {
                    it.toObject(Chat::class.java)?.copy(id = it.id)
                } ?: emptyList()

                trySend(chats)
            }

        awaitClose { listener.remove() }
    }

    suspend fun createChat(
        userId: String,
        trainerId: String,
        trainerName: String,
        trainerImage: String
    ): Result<String> {
        val chat = Chat(
            userId = userId,
            trainerId = trainerId,
            trainerName = trainerName,
            trainerImage = trainerImage,
            participants = listOf(userId, trainerId),
            lastMessage = "",
            lastMessageTime = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        return firebaseService.createChat(chat)
    }

    suspend fun getUnreadCount(userId: String): Result<Int> = try {
        val snapshot = firestore
            .collection(CHATS_COLLECTION)
            .whereArrayContains("participants", userId)
            .get()
            .await()

        val count = snapshot.documents.sumOf {
            it.getLong("unreadCount")?.toInt() ?: 0
        }

        Result.success(count)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun startVideoCall(chatId: String, startedBy: String): Result<String> {
        return try {
            val channelId = "fitlink_call_$chatId"

            firestore.collection(CHATS_COLLECTION)
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
            firestore.collection(CHATS_COLLECTION)
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
            firestore.collection(CHATS_COLLECTION)
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
            firestore.collection(CHATS_COLLECTION)
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