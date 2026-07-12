package com.fabian.todolist.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.fabian.todolist.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import android.util.Log

class AuthManager(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    fun getContext(): Context = context

    fun isUserLoggedIn(): Boolean = auth.currentUser != null || isGuestUser()

    fun isGuestUser(): Boolean = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).getBoolean("is_guest", false)

    fun setGuestUser(isGuest: Boolean) {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_guest", isGuest)
            .apply()
    }

    fun getCurrentUser() = auth.currentUser

    suspend fun signInWithGoogle(context: Context): Result<Unit> {
        return try {
            // Reject the placeholder values used by .env.example and the CI workflow
            // when GOOGLE_WEB_CLIENT_ID is not configured. Calling GetGoogleIdOption
            // with a placeholder serverClientId would produce a confusing runtime
            // error from Google Play services.
            val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            if (clientId.isBlank() ||
                clientId == "PLACEHOLDER_NOT_CONFIGURED" ||
                clientId == "YOUR_GOOGLE_WEB_CLIENT_ID") {
                return Result.failure(Exception("Google Web Client ID is not configured. Set the GOOGLE_WEB_CLIENT_ID secret in GitHub Actions or in your local .env file."))
            }

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
                context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_guest", false)
                    .apply()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Tipo de credencial no soportado"))
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Error en login con Google", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        // After signing out the user is no longer logged in and no longer a guest.
        // The UI is responsible for redirecting to the login screen.
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_guest", false)
            .apply()
    }
}
