package com.example.fitlink.receivers



import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.fitlink.R

class WorkoutReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val title = intent.getStringExtra("title") ?: "Workout Reminder"
        val message = intent.getStringExtra("message")
            ?: "Time for your workout!"

        val builder = NotificationCompat.Builder(
            context,
            "fitlink_notifications"
        )
            .setSmallIcon(R.drawable.ic_notification_workout)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        manager.notify(1001, builder.build())
    }
}