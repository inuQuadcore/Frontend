package com.everybuddy.app.data.network

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 401 응답 시 자동으로 refresh 토큰으로 access token 재발급 후 원래 요청 재시도.
 * refresh 실패 시 토큰 클리어 + null 반환 → 호출자가 401 받음 (LoginScreen 라우팅 트리거).
 *
 * AuthApi는 Auth 전용 OkHttpClient(Authenticator 미포함)를 사용하므로 이 클래스가 직접 의존해도 순환 없음.
 * refresh API 자체가 401 받아도 또 Authenticator 호출되는 무한 루프도 방지됨.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager : TokenManager,
    private val authApi      : AuthApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 이미 한 번 재시도된 요청이면 또 시도 안 함 (무한 루프 방지)
        if (response.priorResponse != null) return null

        val refreshToken = runBlocking { tokenManager.refreshToken.firstOrNull() }
            ?: return null  // refresh 토큰 없음 → 포기

        // refresh 시도 (synchronous — Authenticator 계약)
        val refreshRes = runBlocking {
            try {
                authApi.refresh(RefreshTokenRequest(refreshToken))
            } catch (_: Exception) {
                null
            }
        }

        if (refreshRes == null || !refreshRes.isSuccessful) {
            // refresh 실패 (네트워크 에러 / REFRESH_TOKEN_EXPIRED 등)
            runBlocking { tokenManager.clearToken() }
            return null   // 원래 요청 401 그대로 → UI가 LoginScreen으로 라우팅
        }

        val body = refreshRes.body() ?: return null
        runBlocking {
            tokenManager.saveToken(
                accessToken           = body.accessToken,
                refreshToken          = body.refreshToken,
                accessTokenExpiresAt  = body.accessTokenExpiresAt,
                refreshTokenExpiresAt = body.refreshTokenExpiresAt,
                userId                = body.userId,
            )
        }

        // 새 토큰으로 원래 요청 재시도
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${body.accessToken}")
            .build()
    }
}
