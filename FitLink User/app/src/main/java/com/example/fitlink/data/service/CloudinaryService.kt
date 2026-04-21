package com.example.fitlink.data.service

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudinaryService @Inject constructor() {

    fun uploadImage(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        MediaManager.get()
            .upload(imageUri)
            .unsigned("fitlink_upload")
            .option("folder", "fitlink/images")
            .callback(object : UploadCallback {

                override fun onStart(requestId: String) {}

                override fun onProgress(
                    requestId: String,
                    bytes: Long,
                    totalBytes: Long
                ) {}

                override fun onSuccess(
                    requestId: String,
                    resultData: Map<*, *>
                ) {

                    val url = resultData["secure_url"].toString()
                    onSuccess(url)
                }

                override fun onError(
                    requestId: String,
                    error: com.cloudinary.android.callback.ErrorInfo
                ) {
                    onError(error.description)
                }

                override fun onReschedule(
                    requestId: String,
                    error: com.cloudinary.android.callback.ErrorInfo
                ) {}
            })
            .dispatch()
    }
}