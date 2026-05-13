package com.everybuddy.app.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class FirebaseAuthManager @Inject constructor() {

    private val firebaseAuth = FirebaseAuth.getInstance()

    suspend fun signInWithCustomToken(customToken: String): Boolean =
        suspendCancellableCoroutine { cont ->
            firebaseAuth.signInWithCustomToken(customToken)
                .addOnSuccessListener {
                    if (cont.isActive) cont.resume(true)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "signInWithCustomToken failed", e)
                    if (cont.isActive) cont.resume(false)
                }
        }

    fun signOut() {
        firebaseAuth.signOut()
    }

    companion object {
        private const val TAG = "FirebaseAuthManager"
    }
}
