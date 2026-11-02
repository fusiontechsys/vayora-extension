package com.vayora.extension.cloud

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.vayora.extension.security.ExternalFlowGrace
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { value -> cont.resume(value) }
    addOnFailureListener { error -> cont.resumeWithException(error) }
}

object GoogleAuth {
    const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    /**
     * Builds the authorization request. Interactive callers get a short grace window so the
     * app does not lock itself while Google's approval screen is showing.
     */
    fun request(email: String? = null, beginGrace: Boolean = true): AuthorizationRequest {
        if (beginGrace) ExternalFlowGrace.begin()
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
        if (email != null) {
            builder.setAccount(Account(email, "com.google"))
        }
        return builder.build()
    }

    /**
     * Returns an access token without showing any screen, or null when the user
     * must approve access again (first connection, revoked access, expired consent).
     */
    suspend fun silentToken(context: Context, email: String?): String? {
        val result = Identity.getAuthorizationClient(context)
            .authorize(request(email, beginGrace = false))
            .awaitResult()
        return if (result.hasResolution()) null else result.accessToken
    }
}
