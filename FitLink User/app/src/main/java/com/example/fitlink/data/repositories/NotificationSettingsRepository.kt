package com.example.fitlink.data.repositories

import com.example.fitlink.data.models.NotificationSettings
import com.example.fitlink.data.service.FirebaseService
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSettingsRepository @Inject constructor(
    private val firebaseService: FirebaseService
) {

    private val collection = "notification_settings"

    suspend fun getSettings(userId: String): Result<NotificationSettings> {
        return try {

            val doc = firebaseService.firestore
                .collection(collection)
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                val settings =
                    doc.toObject(NotificationSettings::class.java)
                        ?: NotificationSettings()

                Result.success(settings)
            } else {
                // Create default settings if not exists
                val defaultSettings = NotificationSettings()
                saveSettings(userId, defaultSettings)
                Result.success(defaultSettings)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSettings(
        userId: String,
        settings: NotificationSettings
    ): Result<Boolean> {
        return try {

            firebaseService.firestore
                .collection(collection)
                .document(userId)
                .set(settings)
                .await()

            Result.success(true)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}