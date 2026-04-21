package com.example.fitlinktrainer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.fitlinktrainer.navigation.TrainerNavGraph
import com.example.fitlinktrainer.ui.theme.TrainerTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Notification permission launcher (Android 13+)
     */
    private val requestNotificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                Log.d("Notification", "Permission granted")
            } else {
                Log.d("Notification", "Permission denied")
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        saveFcmToken()

        setContent {
            TrainerTheme {
                TrainerNavGraph()
            }
        }
    }

    /**
     * Request notification permission
     */
    private fun requestNotificationPermissionIfNeeded() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestNotificationPermission.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )

            }

        }
    }

    /**
     * Get and store FCM token in Firestore
     */
    private fun saveFcmToken() {

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->

                Log.d("FCM_TOKEN", token)

                val userId = FirebaseAuth.getInstance().currentUser?.uid

                if (userId != null) {

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .update("fcmToken", token)
                }

            }
            .addOnFailureListener {

                Log.e("FCM", "Token fetch failed", it)

            }

    }
}