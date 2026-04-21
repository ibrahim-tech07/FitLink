package com.example.fitlink

import android.app.Application
import com.cloudinary.android.MediaManager
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class FitLinkApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Move ProviderInstaller to background thread to avoid NetworkOnMainThreadException
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ProviderInstaller.installIfNeeded(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()

        FirebaseFirestore.getInstance().firestoreSettings = settings

        // Cloudinary initialization
        val config = HashMap<String, String>()
        config["cloud_name"] = "dwcqn9ilb"

        MediaManager.init(this, config)

    }

}
