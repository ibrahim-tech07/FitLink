package com.example.fitlinktrainer.data.service

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object FCMTokenManager {

    fun registerToken(userId: String) {

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("fcmToken", token)

            }
    }
}