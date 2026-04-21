package com.example.fitlink.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.fitlink.MainActivity
import com.example.fitlink.R
import com.example.fitlink.data.models.Notification
import com.example.fitlink.data.models.NotificationType
import com.example.fitlink.data.repositories.NotificationRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class NotificationService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    companion object {
        private const val TAG = "NotificationService"
        private const val CHANNEL_ID = "fitlink_notifications"
        private const val CHANNEL_NAME = "FitLink Notifications"
        private const val CHANNEL_DESCRIPTION =
            "Notifications for workouts, achievements, and messages"
    }

    // Safer Coroutine Scope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        saveTokenToPrefs(token)

        serviceScope.launch {

            try {

                val userId = notificationRepository.getCurrentUserId()

                userId?.let {

                    notificationRepository.saveUserFcmToken(
                        it,
                        token
                    )

                }

            } catch (e: Exception) {

                Log.e(TAG, "Token save error", e)

            }

        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM_DEBUG", "🔥 MESSAGE RECEIVED")
        Log.d("FCM_DEBUG", "FROM: ${remoteMessage.from}")
        Log.d("FCM_DEBUG", "DATA: ${remoteMessage.data}")
        Log.d("FCM_DEBUG", "NOTIFICATION: ${remoteMessage.notification}")

        if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data)
        }

        remoteMessage.notification?.let {
            showNotificationUI(
                it.title ?: "FitLink",
                it.body ?: "New Notification",
                "dashboard",
                emptyMap()
            )
        }
    }

    // ================= DATA MESSAGE =================

    private fun handleDataMessage(data: Map<String, String>) {

        val typeString = data["type"] ?: "GENERAL"
        val title = data["title"] ?: "FitLink"
        val message = data["message"] ?: "You have a new notification"
        val userId = data["userId"]

        val safeType = try {
            NotificationType.valueOf(typeString)
        } catch (e: Exception) {
            NotificationType.GENERAL
        }

        when (safeType) {
            NotificationType.WORKOUT_REMINDER ->
                showNotificationUI(title, message, "workouts", data)

            NotificationType.ACHIEVEMENT_UNLOCKED ->
                showNotificationUI(title, message, "profile", data)

            NotificationType.TRAINER_MESSAGE ->
                showNotificationUI(title, message, "chat", data)

            NotificationType.STREAK_ALERT ->
                showNotificationUI(title, message, "dashboard", data)

            NotificationType.GOAL_COMPLETED ->
                showNotificationUI(title, message, "dashboard", data)

            else ->
                showNotificationUI(title, message, "dashboard", data)
        }
        userId?.let {

            val trainerId = data["trainerId"] ?: ""

            saveNotificationToFirestore(
                userId = it,
                trainerId = trainerId,
                type = safeType,
                title = title,
                message = message,
                data = data
            )
        }
    }

    // ================= SHOW NOTIFICATION =================

    private fun showNotificationUI(
        title: String,
        message: String,
        screen: String,
        data: Map<String, String>
    ) {

        createChannelIfNeeded()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("screen", screen)
            data.forEach { putExtra(it.key, it.value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            Random().nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_general)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(Random().nextInt(), builder.build())
    }

    // ================= FIRESTORE SAVE =================

    private fun saveNotificationToFirestore(
        userId: String,
        trainerId: String,
        type: NotificationType,
        title: String,
        message: String,
        data: Map<String, String>
    ) {

        serviceScope.launch {
            try {

                val notificationId = UUID.randomUUID().toString()

                val notification = Notification(
                    id = notificationId,
                    userId = userId,
                    trainerId = trainerId,
                    type = type,
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    actionData = data
                )

                notificationRepository.createNotification(notification)

            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification", e)
            }
        }
    }

    // ================= CHANNEL CREATION =================

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val manager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (manager.getNotificationChannel(CHANNEL_ID) == null) {

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                }

                manager.createNotificationChannel(channel)
            }
        }
    }

    // ================= TOKEN STORAGE =================

    private fun saveTokenToPrefs(token: String) {
        val prefs = getSharedPreferences("fitlink_prefs", MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    fun getStoredToken(): String? {
        val prefs = getSharedPreferences("fitlink_prefs", MODE_PRIVATE)
        return prefs.getString("fcm_token", null)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}