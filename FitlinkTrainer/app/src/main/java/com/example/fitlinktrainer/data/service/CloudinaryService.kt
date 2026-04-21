package com.example.fitlinktrainer.data.service

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CloudinaryService @Inject constructor() {

    companion object {
        const val PROFILE_FOLDER = "fitlink/trainers/profile_images"
        const val WORKOUT_IMAGE_FOLDER = "fitlink/trainers/workout_images"
        const val WORKOUT_VIDEO_FOLDER = "fitlink/trainers/workout_videos"
    }

    private suspend fun uploadFile(
        context: Context,
        uri: Uri,
        folder: String,
        resourceType: String
    ): String = withContext(Dispatchers.IO) {

        suspendCancellableCoroutine { continuation ->

            try {

                val file = uriToFile(context, uri)

                MediaManager.get()
                    .upload(file.absolutePath)
                    .unsigned("fitlink_upload")
                    .option("folder", folder)
                    .option("resource_type", resourceType)
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

                            val url = resultData["secure_url"]?.toString()

                            if (!url.isNullOrBlank()) {

                                if (continuation.isActive) {
                                    continuation.resume(url)
                                }

                            } else {

                                continuation.resumeWithException(
                                    Exception("Upload succeeded but URL missing")
                                )
                            }
                        }

                        override fun onError(
                            requestId: String,
                            error: ErrorInfo
                        ) {

                            continuation.resumeWithException(
                                Exception(error.description ?: "Upload failed")
                            )
                        }

                        override fun onReschedule(
                            requestId: String,
                            error: ErrorInfo
                        ) {}
                    })
                    .dispatch()

            } catch (e: Exception) {

                continuation.resumeWithException(e)
            }
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File {

        val input = context.contentResolver.openInputStream(uri)

        val file = File.createTempFile("upload", ".jpg", context.cacheDir)

        input?.use { inputStream ->

            file.outputStream().use { output ->

                inputStream.copyTo(output)
            }
        }

        return file
    }

    suspend fun uploadWorkoutImage(context: Context, uri: Uri): String {

        return uploadFile(context, uri, WORKOUT_IMAGE_FOLDER, "image")
    }

    suspend fun uploadWorkoutVideo(context: Context, uri: Uri): String {

        return uploadFile(context, uri, WORKOUT_VIDEO_FOLDER, "video")
    }

    suspend fun uploadProfileImage(context: Context, uri: Uri): String {

        return uploadFile(context, uri, PROFILE_FOLDER, "image")
    }

    suspend fun updateProfileImage(
        context: Context,
        uri: Uri,
        oldImageUrl: String?
    ): String {

        oldImageUrl?.let {

            val publicId = extractPublicId(it)

            if (publicId != null) {
                deleteImage(publicId)
            }
        }

        return uploadProfileImage(context, uri)
    }

    private fun extractPublicId(url: String): String? {

        return try {

            val start = url.indexOf("upload/") + 7
            val end = url.lastIndexOf(".")

            if (start >= 0 && end > start) {

                url.substring(start, end)

            } else null

        } catch (e: Exception) {

            null
        }
    }

    private fun deleteImage(publicId: String) {

        try {

            MediaManager.get()
                .cloudinary
                .uploader()
                .destroy(publicId, mapOf<String, Any>())

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }
}