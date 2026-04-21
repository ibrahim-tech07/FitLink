// File: com/example/fitlink/data/services/FirebaseService.kt
package com.example.fitlink.data.service

import android.net.Uri
import android.util.Log
import com.example.fitlink.data.models.AchievementModel
import com.example.fitlink.data.models.Chat
import com.example.fitlink.data.models.ChatMessageModel
import com.example.fitlink.data.models.DailyStats
import com.example.fitlink.data.models.Message
import com.example.fitlink.data.models.MessageStatus
import com.example.fitlink.data.models.Notification
import com.example.fitlink.data.models.NotificationType
import com.example.fitlink.data.models.Trainer
import com.example.fitlink.data.models.User
import com.example.fitlink.data.models.Workout
import com.example.fitlink.data.models.WorkoutStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.DelicateCoroutinesApi


import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseService @Inject constructor(val firestore: FirebaseFirestore,
                                          val auth: FirebaseAuth) {




    companion object {
        const val USERS_COLLECTION = "users"
        const val WORKOUTS_COLLECTION = "workouts"
        const val DAILY_STATS_COLLECTION = "daily_stats"
        const val TRAINERS_COLLECTION = "trainers"
        const val CHATS_COLLECTION = "chats"
        const val MESSAGES_COLLECTION = "messages"
        const val NOTIFICATIONS_COLLECTION = "notifications"
    }

    // ==================== REAL-TIME LISTENERS ====================

    fun listenToUser(userId: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                trySend(user).isSuccess
            }

        awaitClose { listener.remove() }
    }

    fun listenToDailyStats(userId: String): Flow<DailyStats?> = callbackFlow {

        // Ensure today's stats exists before starting listener
        launch {
            try {
                ensureTodayStats(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val listener = firestore.collection(DAILY_STATS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    error.printStackTrace()
                    trySend(null)
                    return@addSnapshotListener
                }

                val doc = snapshot?.documents?.firstOrNull()
                val stats = doc?.toObject(DailyStats::class.java)

                trySend(stats)
            }

        awaitClose { listener.remove() }
    }
    fun listenToTodayWorkouts(userId: String): Flow<List<Workout>> = callbackFlow {
        val today = getStartOfDayTimestamp()
        val tomorrow = today + 86400000

        val listener = firestore.collection(WORKOUTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("scheduledDate", today)
            .whereLessThan("scheduledDate", tomorrow)
            .orderBy("scheduledDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val workouts = snapshot?.documents?.mapNotNull {
                    it.toObject(Workout::class.java)
                } ?: emptyList()
                trySend(workouts).isSuccess
            }

        awaitClose { listener.remove() }
    }
    fun listenToChat(chatId: String): Flow<Chat> = callbackFlow {
        val listener = firestore.collection(CHATS_COLLECTION)
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
    fun listenToTrainerConnection(userId: String): Flow<Map<String, Any>?> = callbackFlow {

        val listener = firestore.collection(TRAINERS_COLLECTION)
            .whereArrayContains("clientIds", userId)
            .limit(1)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val trainerDoc = snapshot?.documents?.firstOrNull()

                if (trainerDoc != null) {
                    trySend(trainerDoc.data).isSuccess
                } else {
                    trySend(null).isSuccess
                }
            }

        awaitClose { listener.remove() }
    }



    fun listenToChatMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection(CHATS_COLLECTION)
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

    fun listenToNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = firestore.collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notifications = snapshot?.documents?.mapNotNull {
                    it.toObject(Notification::class.java)
                } ?: emptyList()
                trySend(notifications).isSuccess
            }

        awaitClose { listener.remove() }
    }


    fun listenToWeeklyStats(userId: String): Flow<List<DailyStats>> = callbackFlow {

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val today = calendar.timeInMillis
        val sevenDaysAgo = today - (6 * 86400000)

        val listener = firestore.collection(DAILY_STATS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.ASCENDING) // 🔥 MUST COME FIRST
            .whereGreaterThanOrEqualTo("date", sevenDaysAgo)
            .whereLessThanOrEqualTo("date", today)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    error.printStackTrace()
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val stats = snapshot?.documents?.mapNotNull {
                    it.toObject(DailyStats::class.java)
                } ?: emptyList()

                trySend(stats)
            }

        awaitClose { listener.remove() }
    }
    // ==================== USER OPERATIONS ====================

    suspend fun createUser(user: User): Result<Boolean> = try {
        firestore.collection(USERS_COLLECTION)
            .document(user.id)
            .set(user)
            .await()

        // Create initial daily stats
        ensureTodayStats(user.id)

        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUser(userId: String): Result<User?> = try {
        val doc = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()
        Result.success(doc.toObject(User::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateUser(userId: String, data: Map<String, Any>): Result<Boolean> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .update(data)
            .await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== DAILY STATS OPERATIONS ====================

    private suspend fun createDailyStats(userId: String): DailyStats {
        val stats = DailyStats(
            id = firestore.collection(DAILY_STATS_COLLECTION).document().id,
            userId = userId,
            date = getStartOfDayTimestamp()
        )

        firestore.collection(DAILY_STATS_COLLECTION)
            .document(stats.id)
            .set(stats)
            .await()

        return stats
    }

    suspend fun getTodayStats(userId: String): Result<DailyStats> {
        return try {
            val stats = ensureTodayStats(userId)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDailyStats(stats: DailyStats): Result<Boolean> = try {
        firestore.collection(DAILY_STATS_COLLECTION)
            .document(stats.id)
            .set(stats)
            .await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== WORKOUT OPERATIONS ====================

    suspend fun getTodayWorkouts(userId: String): Result<List<Workout>> = try {
        val today = getStartOfDayTimestamp()
        val tomorrow = today + 86400000

        val query = firestore.collection(WORKOUTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("scheduledDate", today)
            .whereLessThan("scheduledDate", tomorrow)
            .orderBy("scheduledDate", Query.Direction.ASCENDING)
            .get()
            .await()

        val workouts = query.documents.mapNotNull { it.toObject(Workout::class.java) }
        Result.success(workouts)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAllWorkouts(userId: String): Result<List<Workout>> = try {
        val query = firestore.collection(WORKOUTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("scheduledDate", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()

        val workouts = query.documents.mapNotNull { it.toObject(Workout::class.java) }
        Result.success(workouts)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun createWorkout(workout: Workout): Result<String> = try {
        val docRef = firestore.collection(WORKOUTS_COLLECTION).document()
        val newWorkout = workout.copy(id = docRef.id)
        docRef.set(newWorkout).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateWorkout(workoutId: String, data: Map<String, Any>): Result<Boolean> = try {
        firestore.collection(WORKOUTS_COLLECTION)
            .document(workoutId)
            .update(data)
            .await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }



    // ==================== TRAINER OPERATIONS ====================

    suspend fun getTrainers(): Result<List<Trainer>> = try {

        val snapshot = firestore.collection(TRAINERS_COLLECTION)
            .whereEqualTo("isVerified", true)
            .get()
            .await()

        val trainers = snapshot.documents.mapNotNull {
            it.toObject(Trainer::class.java)?.copy(
                id = it.id,
                clientIds = it.get("clientIds") as? List<String> ?: emptyList()
            )
        }

        Result.success(trainers)

    } catch (e: Exception) {
        Result.failure(e)
    }
    suspend fun createChat(chat: Chat): Result<String> = try {
        val docRef = firestore.collection(CHATS_COLLECTION).document()
        val newChat = chat.copy(id = docRef.id)
        docRef.set(newChat).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }
    suspend fun getTrainerById(trainerId: String): Result<Trainer?> = try {
        val doc = firestore.collection(TRAINERS_COLLECTION)
            .document(trainerId)
            .get()
            .await()
        Result.success(doc.toObject(Trainer::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }
    suspend fun connectWithTrainer(
        clientId: String,
        trainerId: String,
        code: String
    ): Result<Boolean> = try {

        val trainerRef = firestore
            .collection(TRAINERS_COLLECTION)
            .document(trainerId)

        // Add clientId to trainer's clientIds array
        trainerRef.update(
            "clientIds",
            FieldValue.arrayUnion(clientId)
        ).await()

        Result.success(true)

    } catch (e: Exception) {
        Result.failure(e)
    }


    // ==================== CHAT OPERATIONS ====================

    suspend fun getChats(userId: String): Result<List<Chat>> = try {
        val query = firestore.collection(CHATS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .get()
            .await()

        val chats = query.documents.mapNotNull { it.toObject(Chat::class.java) }
        Result.success(chats)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendMessage(message: Message): Result<Boolean> = try {
        val messageId = firestore.collection(MESSAGES_COLLECTION).document().id
        val newMessage = message.copy(id = messageId, status = MessageStatus.SENT)

        firestore.collection(MESSAGES_COLLECTION)
            .document(messageId)
            .set(newMessage)
            .await()

        // Update chat's last message
        firestore.collection(CHATS_COLLECTION)
            .document(message.chatId)
            .update(mapOf(
                "lastMessage" to message.content,
                "lastMessageTime" to message.timestamp,
                "unreadCount" to FieldValue.increment(1)
            ))
            .await()

        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Boolean> = try {
        val query = firestore.collection(MESSAGES_COLLECTION)
            .whereEqualTo("chatId", chatId)
            .whereNotEqualTo("senderId", userId)
            .whereEqualTo("status", MessageStatus.SENT.name)
            .get()
            .await()

        query.documents.forEach { doc ->
            doc.reference.update("status", MessageStatus.READ.name).await()
        }

        // Reset unread count
        firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .update("unreadCount", 0)
            .await()

        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== NOTIFICATION OPERATIONS ====================

    suspend fun getNotifications(userId: String): Result<List<Notification>> = try {
        val query = firestore.collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()

        val notifications = query.documents.mapNotNull { it.toObject(Notification::class.java) }
        Result.success(notifications)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Boolean> = try {
        firestore.collection(NOTIFICATIONS_COLLECTION)
            .document(notificationId)
            .update("isRead", true)
            .await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }
    suspend fun completeWorkoutAtomic(
        userId: String,
        workoutId: String,
        workoutTitle: String,
        calories: Int,
        duration: Int
    ): Result<Boolean> {
        return try {

            val todayStart = getStartOfDayTimestamp()

            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            val workoutRef = firestore.collection(WORKOUTS_COLLECTION).document(workoutId)

            // 🔥 deterministic daily stats id
            val statsId = "${userId}_$todayStart"
            val statsRef = firestore.collection(DAILY_STATS_COLLECTION).document(statsId)

            firestore.runTransaction { tx ->

                val userSnap = tx.get(userRef)
                val workoutSnap = tx.get(workoutRef)
                val statsSnap = tx.get(statsRef)

                if (!workoutSnap.exists()) {
                    throw Exception("Workout not found")
                }

                val status = workoutSnap.getString("status")

                // prevent duplicate completion
                if (status == WorkoutStatus.COMPLETED.name) {
                    return@runTransaction true
                }

                // ---------- USER DATA ----------
                val streak = userSnap.getLong("streak")?.toInt() ?: 0
                val longest = userSnap.getLong("longestStreak")?.toInt() ?: 0
                val lastDate = userSnap.getLong("lastStreakDate") ?: 0L

                val totalCompleted = userSnap.getLong("totalWorkoutsCompleted") ?: 0
                val totalCalories = userSnap.getLong("totalCaloriesBurned") ?: 0

                val newCompleted = totalCompleted + 1
                val newCalories = totalCalories + calories

                // ---------- STREAK ----------
                // ---------- STREAK ----------
                var newStreak = streak

                val oneDay = 86400000L

                when {

                    // First workout ever
                    lastDate == 0L -> {
                        newStreak = 1
                    }

                    // Already counted streak today (multiple workouts same day)
                    lastDate == todayStart -> {
                        newStreak = streak
                    }

                    // Continued streak (yesterday workout exists)
                    todayStart - lastDate == oneDay -> {
                        newStreak = streak + 1
                    }

                    // Missed one or more days → reset streak
                    todayStart - lastDate > oneDay -> {
                        newStreak = 1
                    }
                }



                val newLongest = maxOf(longest, newStreak)

                // ---------- WORKOUT UPDATE ----------
                tx.update(
                    workoutRef,
                    mapOf(
                        "status" to WorkoutStatus.COMPLETED.name,
                        "actualCaloriesBurned" to calories,
                        "actualDuration" to duration,
                        "completedDate" to System.currentTimeMillis()
                    )
                )

                // ---------- DAILY STATS ----------
                if (statsSnap.exists()) {

                    tx.update(
                        statsRef,
                        mapOf(
                            "caloriesBurned" to FieldValue.increment(calories.toLong()),
                            "workoutsCompleted" to FieldValue.increment(1),
                            "activeMinutes" to FieldValue.increment(duration.toLong())
                        )
                    )

                } else {

                    val stats = DailyStats(
                        id = statsId,
                        userId = userId,
                        date = todayStart,
                        caloriesBurned = calories,
                        caloriesGoal = 2000,
                        workoutsCompleted = 1,
                        workoutDuration = duration,
                        waterIntake = 0,
                        waterGoal = 8,
                        activeMinutes = duration
                    )

                    tx.set(statsRef, stats)
                }

                // ---------- USER UPDATE ----------
                val updates = mutableMapOf<String, Any>(
                    "totalWorkoutsCompleted" to newCompleted,
                    "totalCaloriesBurned" to newCalories,
                    "lastActive" to System.currentTimeMillis()
                )

                if (lastDate != todayStart) {
                    updates["streak"] = newStreak
                    updates["longestStreak"] = newLongest
                    updates["lastStreakDate"] = todayStart
                }

                tx.update(userRef, updates)

                true
            }.await()
// In completeWorkoutAtomic, after transaction
            addUserActivity(userId, "Completed $workoutTitle")
            Log.d("WorkoutCompletion", "Workout $workoutId marked COMPLETED")
            Result.success(true)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    suspend fun markAllNotificationsAsRead(userId: String): Result<Boolean> = try {
        val query = firestore.collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        query.documents.forEach { doc ->
            doc.reference.update("isRead", true).await()
        }

        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun createNotification(notification: Notification): Result<Boolean> = try {
        firestore.collection(NOTIFICATIONS_COLLECTION)
            .document(notification.id)
            .set(notification)
            .await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== STREAK OPERATIONS ====================

    suspend fun updateStreak(userId: String): Result<Int> = try {
        val today = getStartOfDayTimestamp()
        val yesterday = today - 86400000

        val workoutsToday = firestore.collection(WORKOUTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", WorkoutStatus.COMPLETED.name)
            .whereGreaterThanOrEqualTo("completedDate", today)
            .get()
            .await()
            .size()

        val workoutsYesterday = firestore.collection(WORKOUTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", WorkoutStatus.COMPLETED.name)
            .whereGreaterThanOrEqualTo("completedDate", yesterday)
            .whereLessThan("completedDate", today)
            .get()
            .await()
            .size()

        val userDoc = firestore.collection(USERS_COLLECTION).document(userId)

        val result = firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDoc)
            val currentStreak = snapshot.getLong("streak")?.toInt() ?: 0
            val longestStreak = snapshot.getLong("longestStreak")?.toInt() ?: 0

            val newStreak = when {
                workoutsToday > 0 && workoutsYesterday > 0 -> currentStreak + 1
                workoutsToday > 0 && workoutsYesterday == 0 -> 1
                else -> currentStreak
            }

            val newLongestStreak = maxOf(newStreak, longestStreak)

            transaction.update(userDoc, mapOf(
                "streak" to newStreak,
                "longestStreak" to newLongestStreak,
                "lastActive" to System.currentTimeMillis()
            ))

            newStreak
        }.await()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== STORAGE OPERATIONS ====================

    fun listenToUserAchievements(userId: String): Flow<List<AchievementModel>> = callbackFlow {
        val listener = firestore.collection("users")
            .document(userId)
            .collection("achievements")
            .orderBy("dateEarned", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->

                val achievements = snapshot?.documents?.mapNotNull {
                    it.toObject(AchievementModel::class.java)
                } ?: emptyList()

                trySend(achievements)
            }

        awaitClose { listener.remove() }
    }
    suspend fun saveAchievement(userId: String, achievement: AchievementModel) {
        firestore.collection("users")
            .document(userId)
            .collection("achievements")
            .document(achievement.achievementId)
            .set(achievement)
            .await()
    }
    suspend fun addUserActivity(userId: String, message: String) {
        try {
            Log.d("Activity", "Adding activity for user $userId: $message")
            val activity = hashMapOf(
                "messageId" to System.currentTimeMillis().toString(),
                "message" to message,
                "timestamp" to System.currentTimeMillis(),
                "type" to "WORKOUT_COMPLETE"
            )
            firestore.collection("users")
                .document(userId)
                .collection("activity")
                .document(activity["messageId"].toString())
                .set(activity)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // --- add near other user operations (FirebaseService) ---

    /**
     * Save the user's FCM token on their user document so your server/cloud functions
     * can target the user.
     */
    suspend fun saveFcmToken(userId: String, token: String): Result<Boolean> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .update(mapOf("fcmToken" to token))
            .await()
        Result.success(true)
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }

    /**
     * Optional: read token (not required but handy for debugging) */
    suspend fun getFcmTokenForUser(userId: String): Result<String?> = try {
        val doc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
        Result.success(doc.getString("fcmToken"))
    } catch (e: Exception) {
        Result.failure(e)
    }
    fun logout() {
        auth.signOut()
    }

    fun listenToUserActivity(userId: String): Flow<List<ChatMessageModel>> = callbackFlow {
        val listener = firestore.collection("users")
            .document(userId)
            .collection("activity")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseService", "Activity listener error", error)
                    // Emit empty list to avoid breaking the flow
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val activities = snapshot?.documents?.mapNotNull {
                    it.toObject(ChatMessageModel::class.java)
                } ?: emptyList()
                trySend(activities)
            }

        awaitClose { listener.remove() }
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid
    // inside FirebaseService (companion or top of class scope)
    private fun deterministicDailyStatsId(userId: String, dayStartMillis: Long) =
        "${userId}_$dayStartMillis"

    // Add this suspend helper inside FirebaseService
    suspend fun sendReminderNotification(
        userId: String,
        title: String,
        message: String
    ) {

        val notification = Notification(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = NotificationType.GENERAL,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        firestore.collection(NOTIFICATIONS_COLLECTION)
            .document(notification.id)
            .set(notification)
            .await()

    }
    private suspend fun ensureTodayStats(userId: String): DailyStats {
        val todayStart = getStartOfDayTimestamp()
        // 1) try to find any existing stats for this user/date (legacy/random id support)
        val existingQuery = firestore.collection(DAILY_STATS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", todayStart)
            .limit(1)
            .get()
            .await()

        if (!existingQuery.isEmpty) {
            // found an existing document (legacy/random id or previously created)
            return existingQuery.documents.first().toObject(DailyStats::class.java)!!
        }

        // 2) Not found — create / reserve deterministic doc id but only if not present
        val deterministicId = deterministicDailyStatsId(userId, todayStart)
        val deterministicRef = firestore.collection(DAILY_STATS_COLLECTION).document(deterministicId)

        // double-check the deterministic doc doesn't already exist (avoid overwrite)
        val deterministicSnap = deterministicRef.get().await()
        if (deterministicSnap.exists()) {
            // another writer created it concurrently — return it
            return deterministicSnap.toObject(DailyStats::class.java)!!
        }

        // 3) create new stats at deterministic id
        val newStats = DailyStats(
            id = deterministicId,
            userId = userId,
            date = todayStart,
            caloriesBurned = 0,
            caloriesGoal = 2000,
            workoutsCompleted = 0,
            workoutDuration = 0,
            waterIntake = 0,
            waterGoal = 8,
            activeMinutes = 0
        )

        deterministicRef.set(newStats).await()
        return newStats
    }
}

