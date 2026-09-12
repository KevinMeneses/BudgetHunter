package com.meneses.budgethunter.commons.platform

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.meneses.budgethunter.commons.data.sync.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Supplies the [Activity] that Credential Manager needs to present its account picker.
 *
 * Koin only holds the application [Context], and Credential Manager refuses to show UI from one,
 * so `MainActivity` hands itself over the same way it does for the camera and file picker.
 */
interface GoogleSignInActivityDelegate {
    fun currentActivity(): Activity?
}

class AndroidGoogleSignInManager(
    private val context: Context,
    private val serverClientId: String,
    private val scope: CoroutineScope,
    private val logger: Logger
) : GoogleSignInManager {

    private var activityDelegate: GoogleSignInActivityDelegate? = null

    /**
     * Pass null from `onDestroy`. This manager is a singleton, so a delegate left pointing at a
     * finished Activity would keep it alive across every configuration change.
     */
    fun setActivityDelegate(delegate: GoogleSignInActivityDelegate?) {
        activityDelegate = delegate
    }

    override val isAvailable get() = serverClientId.isNotBlank()

    override fun signIn(onResult: (GoogleSignInResult) -> Unit) {
        val activity = activityDelegate?.currentActivity()
        if (activity == null) {
            logger.warn(TAG, "No activity available to present the Google account picker")
            onResult(GoogleSignInResult.Failure("No activity available"))
            return
        }

        scope.launch {
            // GetSignInWithGoogleOption, not GetGoogleIdOption: the latter is the one-tap path for
            // accounts that already authorised this app, and it fails outright for a first-time
            // user - exactly the person tapping a "sign up with Google" button.
            val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

            val result = try {
                val credential = CredentialManager.create(activity)
                    .getCredential(activity, request)
                    .credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    GoogleSignInResult.Success(
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    )
                } else {
                    logger.warn(TAG, "Unexpected credential type: ${credential.type}")
                    GoogleSignInResult.Failure("Unexpected credential type")
                }
            } catch (e: GetCredentialCancellationException) {
                // Backing out of the picker is ordinary, not a failure worth logging loudly.
                GoogleSignInResult.Cancelled
            } catch (e: NoCredentialException) {
                logger.warn(TAG, "No Google credential available on this device", e)
                GoogleSignInResult.NoCredentialAvailable
            } catch (e: GetCredentialException) {
                // Covers GetCredentialProviderConfigurationException too, which is what a device
                // without Play services throws.
                logger.warn(TAG, "Credential Manager could not complete the sign in", e)
                GoogleSignInResult.Failure(e.message ?: "Credential Manager failed")
            }

            onResult(result)
        }
    }

    override fun signOut() {
        scope.launch {
            try {
                CredentialManager.create(context)
                    .clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                // Worth knowing about, but never worth failing a sign out over: the tokens are
                // already gone by this point.
                logger.warn(TAG, "Could not clear the cached Google credential", e)
            }
        }
    }

    private companion object {
        const val TAG = "GoogleSignIn"
    }
}
