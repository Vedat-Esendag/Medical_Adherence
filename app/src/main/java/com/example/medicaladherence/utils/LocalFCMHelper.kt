package com.example.medicaladherence.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Helper for sending FCM notifications via local Express server
 * Used for testing without Firebase Cloud Functions (no Blaze plan needed)
 */
object LocalFCMHelper {

    private const val TAG = "LocalFCMHelper"
    private val client = OkHttpClient()
    
    // Use 10.0.2.2 for Android Emulator (maps to host's localhost)
    // For physical device on same WiFi, replace with your computer's IP (e.g., "192.168.1.100:3000")
    private const val SERVER_URL = "http://10.0.2.2:3000/sendNotification"

    /**
     * Send push notification via local server
     *
     * @param fcmToken FCM token of target device
     * @param title Notification title
     * @param body Notification body
     * @return true if sent successfully
     */
    suspend fun sendNotification(
        fcmToken: String,
        title: String,
        body: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("token", fcmToken)
                put("title", title)
                put("body", body)
            }

            val requestBody = json.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build()

            var result = false
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "{}")
                    
                    if (jsonResponse.optBoolean("success", false)) {
                        val messageId = jsonResponse.optString("messageId", "unknown")
                        Log.d(TAG, "✅ Notification sent successfully: $messageId")
                        result = true
                    } else {
                        val error = jsonResponse.optString("error", "Unknown error")
                        Log.e(TAG, "❌ Server returned error: $error")
                        result = false
                    }
                } else {
                    Log.e(TAG, "❌ HTTP ${response.code}: ${response.message}")
                    result = false
                }
            }
            
            result

        } catch (e: IOException) {
            Log.e(TAG, "❌ Network error sending notification", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending notification", e)
            false
        }
    }
}

