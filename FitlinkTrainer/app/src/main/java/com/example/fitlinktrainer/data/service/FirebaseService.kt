package com.example.fitlinktrainer.data.service

import android.net.Uri
import com.example.fitlinktrainer.data.model.Chat
import com.example.fitlinktrainer.data.model.Notification
import com.example.fitlinktrainer.data.model.User
import com.example.fitlinktrainer.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseService @Inject constructor() {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    val storage: FirebaseStorage = FirebaseStorage.getInstance()

    companion object {
        const val USERS = "users"
        const val TRAINERS = "trainers"
        const val WORKOUTS = "workouts"
        const val DAILY_STATS = "daily_stats"
        const val CHATS = "chats"
        const val NOTIFICATIONS = "notifications"
    }

    // ==================== AUTH ====================
    suspend fun login(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun register(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun logout() { auth.signOut() }

    // ==================== TRAINER ====================
    suspend fun createTrainer(trainer: Trainer): Result<Boolean> = try {
        firestore.collection(TRAINERS).document(trainer.id).set(trainer).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTrainer(trainerId: String): Result<Trainer?> = try {
        val doc = firestore.collection(TRAINERS).document(trainerId).get().await()
        Result.success(doc.toObject(Trainer::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun listenToTrainer(trainerId: String): Flow<Trainer?> = callbackFlow {
        val listener = firestore.collection(TRAINERS).document(trainerId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                else trySend(snap?.toObject(Trainer::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateTrainerStatus(trainerId: String, status: String): Result<Boolean> = try {
        firestore.collection(TRAINERS).document(trainerId).update("status", status).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }
// ==================== USERS ====================

    suspend fun getUser(userId: String): Result<User?> = try {
        val doc = firestore.collection(USERS)
            .document(userId)
            .get()
            .await()

        Result.success(doc.toObject(User::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }


    fun listenToUser(userId: String): Flow<User?> = callbackFlow {

        val listener = firestore.collection(USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.toObject(User::class.java))

            }

        awaitClose { listener.remove() }

    }
    suspend fun getPendingTrainers(): Result<List<Trainer>> = try {
        val snapshot = firestore.collection(TRAINERS)
            .whereEqualTo("status", "pending")
            .get().await()
        Result.success(snapshot.documents.mapNotNull { it.toObject(Trainer::class.java) })
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== CLIENTS ====================
    fun listenToClients(trainerId: String): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection(USERS)
            .whereEqualTo("connectedTrainerId", trainerId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                else trySend(snap?.documents?.mapNotNull { it.toObject(User::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ==================== WORKOUTS ====================
    suspend fun createWorkout(workout: Workout): Result<String> = try {

        firestore.collection(WORKOUTS)
            .document(workout.id)
            .set(workout)
            .await()

        Result.success(workout.id)

    } catch (e: Exception) {
        Result.failure(e)
    }

    fun listenToAssignedWorkouts(trainerId: String): Flow<List<Workout>> = callbackFlow {

        val listener = firestore.collection(WORKOUTS)
            .whereEqualTo("trainerId", trainerId)
            .orderBy("scheduledDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->

                if (error != null) {
                    error.printStackTrace()
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val workouts = snap?.documents?.mapNotNull {
                    it.toObject(Workout::class.java)
                } ?: emptyList()

                trySend(workouts)
            }

        awaitClose { listener.remove() }
    }
    suspend fun uploadExerciseImage(
        trainerId: String,
        exerciseId: String,
        imageUri: Uri
    ): Result<String> = try {

        val ref = storage.reference
            .child("exercise_images")
            .child(trainerId)
            .child("$exerciseId.jpg")

        ref.putFile(imageUri).await()

        val url = ref.downloadUrl.await().toString()

        Result.success(url)

    } catch (e: Exception) {
        Result.failure(e)
    }
    suspend fun uploadExerciseGif(
        trainerId: String,
        exerciseId: String,
        gifUri: Uri
    ): Result<String> = try {

        val ref = storage.reference
            .child("exercise_gifs")
            .child(trainerId)
            .child("$exerciseId.gif")

        ref.putFile(gifUri).await()

        val url = ref.downloadUrl.await().toString()

        Result.success(url)

    } catch (e: Exception) {
        Result.failure(e)
    }
    fun listenToUserWorkouts(userId: String): Flow<List<Workout>> = callbackFlow {

        val listener = firestore.collection("workouts")
            .whereEqualTo("userId", userId)
            .orderBy("scheduledDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->

                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                trySend(
                    snap?.documents?.mapNotNull {
                        it.toObject(Workout::class.java)
                    } ?: emptyList()
                )

            }

        awaitClose { listener.remove() }
    }
    fun listenTrainerChats(trainerId: String): Flow<List<Chat>> = callbackFlow {

        val listener = firestore.collection(CHATS)
            .whereArrayContains("participants", trainerId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    error.printStackTrace()
                    trySend(emptyList()) // prevent crash
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull {
                    it.toObject(Chat::class.java)
                } ?: emptyList()

                trySend(chats)
            }

        awaitClose { listener.remove() }
    }
    // ==================== CHAT ====================
    fun listenToChats(userId: String): Flow<List<Chat>> = callbackFlow {

        val listener = firestore.collection(CHATS)
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->

                if (error != null) {
                    error.printStackTrace()
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val chats = snap?.documents?.mapNotNull {
                    it.toObject(Chat::class.java)?.copy(id = it.id)
                } ?: emptyList()

                trySend(chats)
            }

        awaitClose { listener.remove() }
    }

    fun listenToMessages(chatId: String): Flow<List<Message>> = callbackFlow {

        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject(Message::class.java)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    suspend fun updateTrainer(trainer: Trainer): Result<Boolean> {
        return try {

            firestore.collection("trainers")
                .document(trainer.id)
                .set(trainer)
                .await()

            Result.success(true)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        content: String
    ): Result<Boolean> = try {

        val messageRef = firestore
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .document()

        val timestamp = System.currentTimeMillis()

        val message = Message(
            id = messageRef.id,
            chatId = chatId,
            senderId = senderId,
            content = content,
            timestamp = timestamp,
            status = MessageStatus.SENT
        )

        val chatRef = firestore.collection("chats").document(chatId)

        val batch = firestore.batch()

        batch.set(messageRef, message)

        batch.update(
            chatRef,
            mapOf(
                "lastMessage" to content,
                "lastMessageTime" to timestamp
            )
        )

        batch.commit().await()

        Result.success(true)

    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== NOTIFICATIONS ====================
    fun listenToNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = firestore.collection(NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) close(error)
                else trySend(snap?.documents?.mapNotNull { it.toObject(Notification::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun createNotification(notification: Notification): Result<Boolean> = try {
        firestore.collection(NOTIFICATIONS)
            .document(notification.id)
            .set(
                mapOf(
                    "id" to notification.id,
                    "userId" to notification.userId,
                    "trainerId" to notification.trainerId,
                    "type" to notification.type.name,
                    "title" to notification.title,
                    "message" to notification.message,
                    "timestamp" to notification.timestamp,
                    "isRead" to notification.isRead, // 🔥 FORCE THIS
                    "actionData" to notification.actionData,
                    "imageUrl" to notification.imageUrl
                )
            )
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Boolean> = try {
        firestore.collection(NOTIFICATIONS).document(notificationId).update("isRead", true).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== STORAGE ====================
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String> = try {
        val ref = storage.reference.child("profile_images").child(userId).child("${System.currentTimeMillis()}.jpg")
        ref.putFile(imageUri).await()
        val url = ref.downloadUrl.await().toString()
        Result.success(url)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun uploadWorkoutImage(trainerId: String, workoutId: String, imageUri: Uri): Result<String> = try {
        val ref = storage.reference.child("trainer_workouts").child(trainerId).child(workoutId).child("${System.currentTimeMillis()}.jpg")
        ref.putFile(imageUri).await()
        val url = ref.downloadUrl.await().toString()
        Result.success(url)
    } catch (e: Exception) {
        Result.failure(e)
    }
    suspend fun createChat(
        trainerId: String,
        userId: String,
        trainerName: String,
        trainerImage: String,
        userName: String
    ): Result<String> = try {

        val currentTrainerId = auth.currentUser!!.uid
        val chatId = "${currentTrainerId}_${userId}"

        val chatRef = firestore.collection(CHATS).document(chatId)

        val chat = Chat(
            id = chatId,
            trainerId = currentTrainerId,
            userId = userId,
            participants = listOf(currentTrainerId, userId),
            trainerName = trainerName,
            trainerImage = trainerImage,
            userName = userName,
            lastMessage = "",
            lastMessageTime = System.currentTimeMillis(),
            unreadCountTrainer = 0,
            unreadCountUser = 0,
            createdAt = System.currentTimeMillis()
        )

        chatRef.set(chat, SetOptions.merge()).await()

        Result.success(chatId)

    } catch (e: Exception) {
        Result.failure(e)
    }
    fun listenToChat(chatId: String): Flow<Chat> = callbackFlow {
        val listener = firestore.collection(CHATS)
            .document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chat = snapshot?.toObject(Chat::class.java)?.copy(id = snapshot.id)
                if (chat != null) {
                    trySend(chat)
                }
            }
        awaitClose { listener.remove() }
    }
}