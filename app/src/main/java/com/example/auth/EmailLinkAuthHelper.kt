package com.example.auth

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.actionCodeSettings
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

object EmailLinkAuthHelper {

    private const val TAG = "EmailLinkAuth"
    private const val DEFAULT_PROJECT_ID = "gen-lang-client-0342166282"

    fun buildActionCodeSettings(context: Context): ActionCodeSettings {
        val projectId = try {
            FirebaseApp.getInstance().options.projectId?.takeIf { it.isNotBlank() } ?: DEFAULT_PROJECT_ID
        } catch (_: Exception) {
            DEFAULT_PROJECT_ID
        }
        return actionCodeSettings {
            url = "https://$projectId.firebaseapp.com/finishSignUp"
            handleCodeInApp = true
            setAndroidPackageName(
                context.packageName,
                /* installIfNotAvailable = */ true,
                /* minimumVersion = */ null
            )
        }
    }

    fun isIncomingEmailLink(link: String?): Boolean {
        if (link.isNullOrBlank()) return false
        return try {
            Firebase.auth.isSignInWithEmailLink(link)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun sendVerificationLink(
        context: Context,
        email: String
    ): Result<Unit> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        return try {
            val settings = buildActionCodeSettings(context)
            Firebase.auth.sendSignInLinkToEmail(cleanEmail, settings).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "sendSignInLinkToEmail failed: ${e.errorCode}", e)
            val friendly = when (e.errorCode) {
                "ERROR_OPERATION_NOT_ALLOWED", "ERROR_ADMIN_RESTRICTED_OPERATION" ->
                    "Email-Link Sign-In is not enabled yet in Firebase Console (Authentication → Sign-in method → Email/Password → Email link). You can enable it there, or verify now with Google Sign-In / Local DEMO Mode."
                "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
                "ERROR_MISSING_CONTINUE_URI", "ERROR_INVALID_CONTINUE_URI", "ERROR_UNAUTHORIZED_DOMAIN" ->
                    "Authorized domain not configured in Firebase Console for ${context.packageName}."
                else -> e.localizedMessage ?: "Failed to send verification email link."
            }
            Result.failure(IllegalStateException(friendly, e))
        } catch (e: Exception) {
            Log.e(TAG, "sendSignInLinkToEmail error", e)
            Result.failure(e)
        }
    }

    suspend fun completeSignInWithEmailLink(
        email: String,
        emailLink: String
    ): Result<FirebaseUser?> {
        val cleanEmail = email.trim()
        val cleanLink = emailLink.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter the email address that requested this verification link."))
        }
        if (!isIncomingEmailLink(cleanLink)) {
            return Result.failure(IllegalArgumentException("The provided link is not a valid Firebase email verification link."))
        }
        return try {
            val result = Firebase.auth.signInWithEmailLink(cleanEmail, cleanLink).await()
            Result.success(result.user)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "signInWithEmailLink failed: ${e.errorCode}", e)
            val friendly = when (e.errorCode) {
                "ERROR_EXPIRED_ACTION_CODE" -> "This verification link has expired. Please request a new link."
                "ERROR_INVALID_ACTION_CODE" -> "This verification link has already been used or is invalid. Please request a new link."
                "ERROR_INVALID_EMAIL" -> "Email address does not match the verification link."
                else -> e.localizedMessage ?: "Email-link verification failed."
            }
            Result.failure(IllegalStateException(friendly, e))
        } catch (e: Exception) {
            Log.e(TAG, "signInWithEmailLink error", e)
            Result.failure(e)
        }
    }
}
