package com.example.fitlink.utlis

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.fitlink.R
import com.example.fitlink.receivers.WorkoutReminderReceiver
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Calendar

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val WORKOUT_REQUEST_CODE = 1001
    private const val CHANNEL_ID = "fitlink_notifications"

    // =========================
    // FCM TOPIC MANAGEMENT
    // =========================

    fun subscribeToTopics() {

        FirebaseMessaging.getInstance()
            .subscribeToTopic("all_users")

        FirebaseMessaging.getInstance()
            .subscribeToTopic("workout_reminders")

        Log.d(TAG, "Subscribed to FCM topics")
    }

    fun unsubscribeFromTopics() {

        FirebaseMessaging.getInstance()
            .unsubscribeFromTopic("all_users")

        FirebaseMessaging.getInstance()
            .unsubscribeFromTopic("workout_reminders")

        Log.d(TAG, "Unsubscribed from FCM topics")
    }

    // =========================
    // CREATE NOTIFICATION CHANNEL
    // =========================

    private fun createNotificationChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "FitLink Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Workout reminders and achievements"
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    // =========================
    // DAILY WORKOUT REMINDER
    // =========================
    fun showWaterReminder(context: Context) {

        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(
            context,
            "fitlink_notifications"
        )
            .setSmallIcon(R.drawable.ic_water)
            .setContentTitle("💧 Drink Water")
            .setContentText("Stay hydrated! Drink a glass of water now.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        manager.notify(3001, builder.build())
    }

    fun scheduleDailyWorkoutReminder(
        context: Context,
        hour: Int,
        minute: Int
    ) {

        val calendar = Calendar.getInstance().apply {

            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {

            putExtra("title", "Time for your workout! 💪")
            putExtra("message", "Don't break your streak! Your workout is waiting.")

        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WORKOUT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (!alarmManager.canScheduleExactAlarms()) {

                Log.e(TAG, "Exact alarm permission not granted")
                return
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        Log.d(TAG, "Daily workout reminder scheduled")
    }

    // =========================
    // STREAK NOTIFICATION
    // =========================

    fun showStreakNotification(
        context: Context,
        streakDays: Int
    ) {

        createNotificationChannel(context)

        val title = when (streakDays) {
            7 -> "🔥 7-Day Streak!"
            14 -> "🔥 14-Day Streak!"
            21 -> "🔥 21-Day Streak!"
            30 -> "🏆 30-Day Streak!"
            else -> "🔥 $streakDays Day Streak!"
        }

        val message = "You're on a $streakDays day workout streak! Keep going!"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_streak)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        manager.notify(2001, builder.build())
    }

    // =========================
    // CANCEL REMINDER
    // =========================

    fun cancelWorkoutReminder(context: Context) {

        val intent = Intent(context, WorkoutReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WORKOUT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(pendingIntent)

        Log.d(TAG, "Workout reminder cancelled")
    }
}