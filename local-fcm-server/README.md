# Local FCM Notification Server

A local Express.js server for sending FCM (Firebase Cloud Messaging) push notifications using the Firebase Admin SDK. This allows you to test FCM notifications without deploying to Firebase Cloud Functions or requiring a Blaze plan.

## 🚀 Quick Start

### 1. Install Dependencies

```bash
cd local-fcm-server
npm install
```

### 2. Start the Server

```bash
npm start
```

The server will start on `http://localhost:3000` by default.

To use a different port:

```bash
PORT=8080 npm start
```

### 3. Verify Server is Running

Open your browser or use curl:

```bash
curl http://localhost:3000/health
```

Expected response:
```json
{
  "status": "ok",
  "service": "Local FCM Server",
  "timestamp": "2024-11-11T12:00:00.000Z"
}
```

---

## 📤 Sending Notifications

### Using cURL

```bash
curl -X POST http://localhost:3000/sendNotification \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_FCM_DEVICE_TOKEN_HERE",
    "title": "Medication Reminder",
    "body": "Time to take your medication!"
  }'
```

**Success Response:**
```json
{
  "success": true,
  "messageId": "projects/medical-adherence-22fd2/messages/0:1234567890"
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "Missing required fields: token, title, and body are required."
}
```

---

## 📱 Android Integration (Kotlin)

### Option 1: Using OkHttp (Recommended)

Add this to your Android app:

```kotlin
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object LocalFCMClient {
    private val client = OkHttpClient()
    
    // Update this to your computer's local IP address when testing on a physical device
    // Use "10.0.2.2:3000" for Android Emulator (maps to host machine's localhost)
    private const val SERVER_URL = "http://10.0.2.2:3000/sendNotification"
    
    fun sendNotification(
        token: String,
        title: String,
        body: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("token", token)
            put("title", title)
            put("body", body)
        }

        val requestBody = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onFailure("HTTP ${response.code}: ${response.message}")
                        return
                    }

                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "{}")
                    
                    if (jsonResponse.optBoolean("success", false)) {
                        val messageId = jsonResponse.optString("messageId", "unknown")
                        onSuccess(messageId)
                    } else {
                        val error = jsonResponse.optString("error", "Unknown error")
                        onFailure(error)
                    }
                }
            }
        })
    }
}
```

### Usage Example

```kotlin
// In your CaretakerViewModel or Activity
LocalFCMClient.sendNotification(
    token = patientFcmToken,
    title = "Medication Reminder",
    body = "Don't forget to take your evening dose!",
    onSuccess = { messageId ->
        Log.d("FCM", "Notification sent successfully: $messageId")
        // Update UI or show success message
    },
    onFailure = { error ->
        Log.e("FCM", "Failed to send notification: $error")
        // Show error message to user
    }
)
```

### Option 2: Using Retrofit

First, define the API interface:

```kotlin
interface FCMService {
    @POST("sendNotification")
    suspend fun sendNotification(
        @Body request: NotificationRequest
    ): NotificationResponse
}

data class NotificationRequest(
    val token: String,
    val title: String,
    val body: String
)

data class NotificationResponse(
    val success: Boolean,
    val messageId: String? = null,
    val error: String? = null
)
```

Then create the Retrofit instance:

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("http://10.0.2.2:3000/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val fcmService = retrofit.create(FCMService::class.java)
```

And use it:

```kotlin
viewModelScope.launch {
    try {
        val response = fcmService.sendNotification(
            NotificationRequest(
                token = patientFcmToken,
                title = "Medication Reminder",
                body = "Time to take your medication!"
            )
        )
        
        if (response.success) {
            Log.d("FCM", "Sent: ${response.messageId}")
        } else {
            Log.e("FCM", "Error: ${response.error}")
        }
    } catch (e: Exception) {
        Log.e("FCM", "Network error", e)
    }
}
```

---

## 🔧 Configuration

### Connecting from Physical Device

If testing on a **physical Android device** (not emulator):

1. Make sure your phone and computer are on the **same Wi-Fi network**
2. Find your computer's local IP address:
   - **Mac/Linux**: `ifconfig | grep inet`
   - **Windows**: `ipconfig`
3. Update the `SERVER_URL` in your Kotlin code:
   ```kotlin
   private const val SERVER_URL = "http://192.168.1.XXX:3000/sendNotification"
   ```

### Connecting from Android Emulator

Use the special IP `10.0.2.2` which maps to your host machine's `localhost`:

```kotlin
private const val SERVER_URL = "http://10.0.2.2:3000/sendNotification"
```

---

## 📋 API Endpoints

### `POST /sendNotification`

Send a push notification to a device.

**Request Body:**
```json
{
  "token": "FCM_DEVICE_TOKEN",
  "title": "Notification Title",
  "body": "Notification Body"
}
```

**Response (Success):**
```json
{
  "success": true,
  "messageId": "projects/medical-adherence-22fd2/messages/..."
}
```

**Response (Error):**
```json
{
  "success": false,
  "error": "Error message"
}
```

### `GET /health`

Health check endpoint.

**Response:**
```json
{
  "status": "ok",
  "service": "Local FCM Server",
  "timestamp": "2024-11-11T12:00:00.000Z"
}
```

### `GET /`

API documentation endpoint.

---

## 🔒 Security Notes

- ⚠️ This server is intended for **local testing only**
- The service account JSON file contains sensitive credentials
- **Never expose this server to the public internet**
- For production, deploy to Firebase Cloud Functions with proper security rules

---

## 🐛 Troubleshooting

### "Connection refused" error from Android app

- ✅ Verify server is running: `curl http://localhost:3000/health`
- ✅ Check firewall settings (allow port 3000)
- ✅ For physical device: use computer's local IP, not `localhost`
- ✅ For emulator: use `10.0.2.2:3000` instead of `localhost:3000`

### "Error sending notification" from server

- ✅ Verify the FCM token is valid and not expired
- ✅ Check that the service account JSON file is in the correct location
- ✅ Ensure Firebase project ID matches: `medical-adherence-22fd2`
- ✅ Check server logs for detailed error messages

### Server crashes on startup

- ✅ Verify Node.js is installed: `node --version` (requires v14+)
- ✅ Check that `firebase-admin` initialized correctly
- ✅ Ensure service account JSON path is correct in `index.js`

---

## 🚀 Migration to Cloud Functions

When ready to deploy to production, you can easily migrate this code to Firebase Cloud Functions:

1. Copy the notification sending logic from `index.js`
2. Wrap it in `functions.https.onRequest()`
3. Deploy with `firebase deploy --only functions`
4. Update Android app URL to Cloud Functions endpoint

The core logic remains identical! 🎉

---

## 📦 Dependencies

- **express**: Web framework for Node.js
- **firebase-admin**: Firebase Admin SDK for server-side operations
- **cors**: Enable CORS for cross-origin requests
- **nodemon** (dev): Auto-restart server on file changes

---

## 📝 License

ISC

---

## 🤝 Contributing

This is a local development tool for the Medical Adherence project.

