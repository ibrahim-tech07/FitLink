package com.example.fitlinktrainer.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.fitlinktrainer.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class FitLinkMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.data["title"] ?: "FitLink"
        val body = message.data["message"] ?: "You have a new notification"

        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("trainers") // 🔥 FIX HERE
            .document(userId)
            .update("fcmToken", token)
    }

    private fun showNotification(title: String, body: String) {

        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "fitlink_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "FitLink Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )

            manager.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

        manager.notify(Random.nextInt(), notification)
    }
}