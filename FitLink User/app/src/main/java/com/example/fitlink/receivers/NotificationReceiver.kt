package com.example.fitlink.receivers



import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.fitlink.data.service.NotificationService
import com.example.fitlink.utlis.NotificationHelper

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationReceiver", "Received broadcast: ${intent.action}")

        when (intent.action) {
            "SCHEDULE_NOTIFICATION" -> {
                val title = intent.getStringExtra("title") ?: "FitLink Reminder"
                val message = intent.getStringExtra("message") ?: "Time for your workout!"
                val notificationId = intent.getIntExtra("notificationId", 1000)

                // Forward to NotificationService
                val serviceIntent = Intent(context, NotificationService::class.java).apply {
                    action = "SHOW_NOTIFICATION"
                    putExtra("type", "GENERAL")
                    putExtra("title", title)
                    putExtra("message", message)
                    putExtra("notificationId", notificationId)
                }
                context.startService(serviceIntent)
            }

            "WORKOUT_REMINDER" -> {
                val workoutId = intent.getStringExtra("workoutId")
                val workoutName = intent.getStringExtra("workoutName")

                val serviceIntent = Intent(context, NotificationService::class.java).apply {
                    action = "SHOW_NOTIFICATION"
                    putExtra("type", "WORKOUT_REMINDER")
                    putExtra("title", "Time for your workout! 💪")
                    putExtra("message", "Your workout '$workoutName' is scheduled now.")
                    putExtra("workoutId", workoutId)
                }
                context.startService(serviceIntent)
            }

            "STREAK_CHECK" -> {
                // Check streak and send notification if needed
                val streakDays = intent.getIntExtra("streakDays", 0)
                if (streakDays > 0 && streakDays % 7 == 0) {
                    NotificationHelper.showStreakNotification(context, streakDays)
                }
            }
            "DRINK_WATER" -> {

                NotificationHelper.showWaterReminder(context)

            }
        }
    }
}