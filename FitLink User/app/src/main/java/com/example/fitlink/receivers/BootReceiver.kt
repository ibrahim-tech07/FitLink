package com.example.fitlink.receivers



import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fitlink.utlis.NotificationHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule daily workout reminder after reboot
            NotificationHelper.scheduleDailyWorkoutReminder(context, 18, 0) // 6 PM

            // You can also reschedule other notifications here
        }
    }
}