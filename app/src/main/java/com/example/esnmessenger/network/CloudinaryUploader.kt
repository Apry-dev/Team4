package com.example.esnmessenger.network

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

object CloudinaryUploader {
    private const val CLOUD_NAME = "dmftit3lf"
    private const val UPLOAD_PRESET = "ESNmessenger"

    private val client = OkHttpClient()

    suspend fun upload(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Cannot open image")

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", "image.jpg",
                bytes.toRequestBody("image/*".toMediaType())
            )
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response")

        if (!response.isSuccessful) throw IOException("Upload failed (${response.code}): $responseBody")

        JSONObject(responseBody).getString("secure_url")
    }

    suspend fun uploadAudio(file: File): String = withContext(Dispatchers.IO) {
        val bytes = file.readBytes()

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", file.name,
                bytes.toRequestBody("audio/mp4".toMediaType())
            )
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/auto/upload")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response")

        if (!response.isSuccessful) throw IOException("Upload failed (${response.code}): $responseBody")

        JSONObject(responseBody).getString("secure_url")
    }
}
