package com.everybuddy.app.data.firebase

import android.util.Log
import com.everybuddy.app.data.dto.FcmTokenRegisterRequest
import com.everybuddy.app.data.network.FcmTokenApi
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * FCM 디바이스 토큰을 백엔드(`/api/v1/fcm-tokens`)에 등록/삭제하는 매니저.
 *
 *  - 로그인 성공 후 register(): FirebaseMessaging.getToken() → POST /fcm-tokens
 *  - 로그아웃 시 delete(): DELETE /fcm-tokens
 *
 * 실패해도 앱 흐름은 막지 않음 (푸시만 못 받게 됨).
 */
@Singleton
class FcmTokenManager @Inject constructor(
    private val api: FcmTokenApi,
) {
    suspend fun register() {
        val token = fetchFcmToken() ?: return
        try {
            val res = api.register(FcmTokenRegisterRequest(token))
            if (!res.isSuccessful) {
                Log.w(TAG, "register API failed: ${res.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "register failed", e)
        }
    }

    suspend fun delete() {
        try {
            val res = api.delete()
            if (!res.isSuccessful) {
                Log.w(TAG, "delete API failed: ${res.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "delete failed", e)
        }
    }

    private suspend fun fetchFcmToken(): String? =
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (cont.isActive) cont.resume(token)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "FirebaseMessaging.getToken failed", e)
                    if (cont.isActive) cont.resume(null)
                }
        }

    companion object {
        private const val TAG = "FcmTokenManager"
    }
}
