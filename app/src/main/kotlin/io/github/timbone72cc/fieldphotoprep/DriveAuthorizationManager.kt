package io.github.timbone72cc.fieldphotoprep

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

class DriveAuthorizationManager(
    activity: Activity,
) {
    private val client = Identity.getAuthorizationClient(activity)

    fun authorize(
        onResult: (AuthorizationResult) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_METADATA_READONLY_SCOPE)))
            .setOptOutIncludingGrantedScopes(true)
            .setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
            .build()

        client.authorize(request)
            .addOnSuccessListener(onResult)
            .addOnFailureListener(onFailure)
    }

    fun resultFromIntent(data: Intent): AuthorizationResult =
        client.getAuthorizationResultFromIntent(data)

    companion object {
        const val DRIVE_METADATA_READONLY_SCOPE =
            "https://www.googleapis.com/auth/drive.metadata.readonly"
    }
}
