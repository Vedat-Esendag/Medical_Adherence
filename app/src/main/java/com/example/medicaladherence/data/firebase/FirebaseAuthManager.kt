package com.example.medicaladherence.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseAuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    // Cached offline user ID for consistency
    private var cachedOfflineUserId: String? = null

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String?
        get() = auth.currentUser?.uid ?: cachedOfflineUserId

    val isSignedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Sign in anonymously (each device gets unique user ID)
     */
    suspend fun signInAnonymously(): Result<String> {
        return try {
            val result = auth.signInAnonymously().await()
            val userId = result.user?.uid ?: return Result.failure(Exception("No user ID"))
            Result.success(userId)
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            // Network error - return failure but don't crash
            Result.failure(Exception("Network unavailable - operating in offline mode", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ensure user is authenticated (sign in if needed)
     * Returns existing user ID or creates a persistent fallback local ID if offline
     */
    suspend fun ensureAuthenticated(): String {
        // Return existing user if already authenticated
        currentUserId?.let { return it }
        
        // Try to sign in anonymously
        val result = signInAnonymously()
        return result.getOrNull() ?: run {
            // If sign-in fails (e.g., offline), use a persistent local fallback ID
            // This allows the app to work offline with Firestore's cache
            if (cachedOfflineUserId == null) {
                cachedOfflineUserId = "offline_user_local"
                android.util.Log.w("FirebaseAuth", "Using offline mode with local ID: $cachedOfflineUserId")
            }
            cachedOfflineUserId!!
        }
    }

    /**
     * Auth state as Flow
     */
    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)

        // Send initial value
        trySend(auth.currentUser)

        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    /**
     * Sign out (clear local data)
     */
    fun signOut() {
        auth.signOut()
        cachedOfflineUserId = null  // Clear cached offline ID
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseAuthManager? = null

        fun getInstance(): FirebaseAuthManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseAuthManager()
                INSTANCE = instance
                instance
            }
        }
    }
}
