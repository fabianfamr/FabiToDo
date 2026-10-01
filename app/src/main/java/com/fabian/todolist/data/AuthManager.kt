package com.fabian.todolist.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.fabian.todolist.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(context: Context) {
    // Avoid leaking Activity instances by retaining only applicationContext
    private val appContext: Context = context.applicationContext
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(appContext)

    fun getContext(): Context = appContext

    fun isUserLoggedIn(): Boolean = auth.currentUser != null || isGuestUser()

    fun isGuestUser(): Boolean = appContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).getBoolean("is_guest", false)

    fun setGuestUser(isGuest: Boolean) {
        appContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_guest", isGuest)
            .apply()
    }

    fun getCurrentUser() = auth.currentUser

    /**
     * Signs in with Google.
     * Attempts native Credential Manager first if available and SHA-1 is registered.
     * If native fails (e.g. DEVELOPER_ERROR / code 10 due to missing SHA registration,
     * or missing Google credentials on device), it automatically and transparently falls
     * back to Firebase Web OAuthProvider (via Chrome Custom Tabs), which does NOT require
     * any Android SHA certificate fingerprint!
     */
    suspend fun signInWithGoogle(context: Context): Result<Unit> {
        val activity = findActivity(context)

        val defaultWebClientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val resolvedWebClientId = if (defaultWebClientIdRes != 0) context.getString(defaultWebClientIdRes) else BuildConfig.GOOGLE_WEB_CLIENT_ID
        val clientId = if (resolvedWebClientId.isNotBlank() &&
            resolvedWebClientId != "PLACEHOLDER_NOT_CONFIGURED" &&
            resolvedWebClientId != "YOUR_GOOGLE_WEB_CLIENT_ID"
        ) {
            resolvedWebClientId
        } else {
            "14026788849-6o166494a1rsfqj0ed5lm849upa9l2ds.apps.googleusercontent.com"
        }
        val isClientIdConfigured = clientId.isNotBlank()

        // 1. Try native Credential Manager if Client ID is present
        if (isClientIdConfigured) {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is GoogleIdTokenCredential) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                    auth.signInWithCredential(firebaseCredential).await()
                    setGuestUser(false)
                    return Result.success(Unit)
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.w("AuthManager", "Native Credential Manager flow failed or lacks SHA-1. Falling back to Web OAuth: ${e.message}")
                }
            }
        }

        // 2. Seamless fallback to Firebase Web OAuthProvider (Custom Tabs) — NO SHA NEEDED!
        if (activity != null) {
            return signInWithGoogleWeb(activity)
        }

        return Result.failure(Exception("No se encontró una actividad activa para completar el inicio de sesión."))
    }

    /**
     * Direct Web OAuth provider flow via Custom Tabs.
     * Does NOT require any Android SHA-1 or SHA-256 fingerprint registered in Firebase/Google Cloud!
     */
    suspend fun signInWithGoogleWeb(activity: Activity): Result<Unit> {
        return try {
            val provider = OAuthProvider.newBuilder("google.com").build()
            auth.startActivityForSignInWithProvider(activity, provider).await()
            setGuestUser(false)
            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("AuthManager", "Error in Web OAuth Provider sign-in", e)
            }
            Result.failure(e)
        }
    }

    private fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }

    fun signOut() {
        auth.signOut()
        setGuestUser(false)
    }
}
