package com.example.fitlink.data.repositories

import android.util.Log
import com.example.fitlink.data.models.Notification
import com.example.fitlink.data.service.FirebaseService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firebaseService: FirebaseService,
    private val auth: FirebaseAuth
) {

    companion object {
        private const val TAG = "NotificationRepository"
    }

    private val firestore: FirebaseFirestore
        get() = firebaseService.firestore

    private val notificationsRef
        get() = firestore.collection(FirebaseService.NOTIFICATIONS_COLLECTION)

    // ================= REALTIME STREAM =================

    fun getNotificationsStream(userId: String): Flow<List<Notification>> {
        return firebaseService.listenToNotifications(userId)
    }

    // ================= FETCH (ONE TIME) =================

    suspend fun getNotifications(userId: String): Result<List<Notification>> {
        return try {

            val snapshot = notificationsRef
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val notifications = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Notification::class.java)?.copy(id = doc.id)
            }

            Result.success(notifications)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notifications", e)
            Result.failure(e)
        }
    }

    // ================= MARK AS READ =================

    suspend fun markAsRead(notificationId: String): Result<Boolean> {
        return try {

            notificationsRef
                .document(notificationId)
                .update("isRead", true)
                .await()

            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
            Result.failure(e)
        }
    }

    // ================= MARK ALL AS READ =================

    suspend fun markAllAsRead(userId: String): Result<Boolean> {
        return try {

            val snapshot = notificationsRef
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            if (snapshot.documents.isEmpty()) {
                return Result.success(true)
            }

            val batch = firestore.batch()

            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }

            batch.commit().await()

            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error marking all notifications as read", e)
            Result.failure(e)
        }
    }

    // ================= DELETE =================

    suspend fun deleteNotification(notificationId: String): Result<Boolean> {
        return try {

            notificationsRef
                .document(notificationId)
                .delete()
                .await()

            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification", e)
            Result.failure(e)
        }
    }

    // ================= CREATE =================

    suspend fun createNotification(notification: Notification): Result<Boolean> {
        return try {

            if (notification.id.isBlank()) {
                return Result.failure(
                    IllegalArgumentException("Notification ID cannot be empty")
                )
            }

            notificationsRef
                .document(notification.id)
                .set(notification)
                .await()

            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification", e)
            Result.failure(e)
        }
    }

    // ================= CURRENT USER =================

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // ================= UNREAD COUNT =================

    suspend fun getUnreadCount(userId: String): Result<Int> {
        return try {

            val snapshot = notificationsRef
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            Result.success(snapshot.size())

        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count", e)
            Result.failure(e)
        }
    }

    // ================= SAVE FCM TOKEN =================

    suspend fun saveUserFcmToken(
        userId: String,
        token: String
    ) {

        firestore.collection("users")
            .document(userId)
            .update("fcmToken", token)
            .await()
    }

    // ================= PAGINATION =================

    suspend fun getMoreNotifications(
        userId: String,
        lastTimestamp: Long
    ): Result<List<Notification>> {

        return try {

            val snapshot = notificationsRef
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastTimestamp)
                .limit(20)
                .get()
                .await()

            val notifications = snapshot.documents.mapNotNull {
                it.toObject(Notification::class.java)?.copy(id = it.id)
            }

            Result.success(notifications)

        } catch (e: Exception) {
            Log.e(TAG, "Error loading more notifications", e)
            Result.failure(e)
        }
    }
}