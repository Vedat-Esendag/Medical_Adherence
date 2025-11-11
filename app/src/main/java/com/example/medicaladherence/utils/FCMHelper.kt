package com.example.medicaladherence.utils

import android.util.Log
import com.example.medicaladherence.utils.AppConstants.FCM_SEND_URL
import com.example.medicaladherence.utils.AppConstants.FCM_SERVER_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Helper for sending FCM notifications directly via HTTP API
 * Used when Cloud Functions are unavailable (Spark plan)
 */
object FCMHelper {

    private const val TAG = "FCMHelper"

    /**
     * Send push notification directly to a device via FCM HTTP API
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
            val url = URL(FCM_SEND_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "key=$FCM_SERVER_KEY")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            // Build FCM payload
            val notification = JSONObject().apply {
                put("title", title)
                put("body", body)
            }

            val payload = JSONObject().apply {
                put("to", fcmToken)
                put("notification", notification)
                put("priority", "high")
            }

            // Send request
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            // Check response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "FCM notification sent successfully")
                true
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "FCM send failed. Code: $responseCode, Error: $errorStream")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending FCM notification", e)
            false
        }
    }
}