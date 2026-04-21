package com.example.fitlinktrainer

import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TrainerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Firestore offline cache
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()

        // Cloudinary
        val config: HashMap<String, String> = HashMap()
        config["cloud_name"] = "dwcqn9ilb"
        config["secure"] = "true"

        MediaManager.init(this, config)

        // Get FCM Token
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    Log.e("FCM", "Token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d("FCM_TOKEN", token)
            }
    }
}