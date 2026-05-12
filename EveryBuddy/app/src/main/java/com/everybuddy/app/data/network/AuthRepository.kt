package com.everybuddy.app.data.network

import android.content.Context
import com.everybuddy.app.data.auth.GoogleAuthManager
import com.everybuddy.app.data.auth.GoogleSignInResult
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api               : AuthApi,
    private val googleAuthManager : GoogleAuthManager,
    private val tokenManager      : TokenManager,
    private val authDataHolder    : AuthDataHolder,
) {
    private val gson = Gson()

    val accessToken: Flow<String?> = tokenManager.accessToken

    suspend fun login(loginId: String, password: String): AuthResult<LoginResponse> {
        return try {
            val res = api.login(LoginRequest(loginId, password))
            if (res.isSuccessful) {
                val body = res.body()!!
                tokenManager.saveToken(
                    accessToken           = body.accessToken,
                    refreshToken          = body.refreshToken,
                    accessTokenExpiresAt  = body.accessTokenExpiresAt,
                    refreshTokenExpiresAt = body.refreshTokenExpiresAt,
                    userId                = body.userId,
                )
                AuthResult.Success(body)
            } else {
                AuthResult.Error(res.code(), parseError(res.errorBody()?.string()))
            }
        } catch (e: Exception) {
            AuthResult.Exception(e)
        }
    }

    suspend fun refresh(refreshToken: String): AuthResult<LoginResponse> {
        return try {
            val res = api.refresh(RefreshTokenRequest(refreshToken))
            if (res.isSuccessful) {
                val body = res.body()!!
                tokenManager.saveToken(
                    accessToken           = body.accessToken,
                    refreshToken          = body.refreshToken,
                    accessTokenExpiresAt  = body.accessTokenExpiresAt,
                    refreshTokenExpiresAt = body.refreshTokenExpiresAt,
                    userId                = body.userId,
                )
                AuthResult.Success(body)
            } else {
                AuthResult.Error(res.code(), parseError(res.errorBody()?.string()))
            }
        } catch (e: Exception) {
            AuthResult.Exception(e)
        }
    }

    suspend fun register(req: RegisterRequest): AuthResult<Unit> {
        return try {
            val res = api.register(req)
            if (res.isSuccessful) {
                AuthResult.Success(Unit)
            } else {
                AuthResult.Error(res.code(), parseError(res.errorBody()?.string()))
            }
        } catch (e: Exception) {
            AuthResult.Exception(e)
        }
    }

    suspend fun firebaseToken(): AuthResult<String> {
        return try {
            val res = api.firebaseToken()
            if (res.isSuccessful) {
                AuthResult.Success(res.body()!!.firebaseToken)
            } else {
                AuthResult.Error(res.code(), parseError(res.errorBody()?.string()))
            }
        } catch (e: Exception) {
            AuthResult.Exception(e)
        }
    }

    suspend fun googleLogin(
        activityContext : Context,
        webClientId     : String,
    ): AuthResult<Boolean> {
        val signInResult = googleAuthManager.signIn(activityContext, webClientId)
        if (signInResult is GoogleSignInResult.Error) {
            return AuthResult.Error(-1, signInResult.message)
        }
        val idToken = (signInResult as GoogleSignInResult.Success).idToken

        return try {
            val res = api.googleAuth(GoogleAuthRequest(idToken))
            if (!res.isSuccessful) {
                return AuthResult.Error(res.code(), parseError(res.errorBody()?.string()))
            }
            val body = res.body() ?: return AuthResult.Error(-1, "서버 응답이 없습니다.")

            if (!body.isNewUser) {
                val loginData = body.loginData
                    ?: return AuthResult.Error(-1, "토큰 정보가 없습니다.")
                tokenManager.saveToken(
                    accessToken           = loginData.accessToken,
                    refreshToken          = loginData.refreshToken,
                    accessTokenExpiresAt  = loginData.accessTokenExpiresAt,
                    refreshTokenExpiresAt = loginData.refreshTokenExpiresAt,
                    userId                = loginData.userId,
                )
                AuthResult.Success(false)
            } else {
                val tempToken = body.tempToken
                    ?: return AuthResult.Error(-1, "임시 토큰이 없습니다.")
                authDataHolder.setGoogleSignup(tempToken)   // Onboarding 완료 시 googleRegister 호출에 사용
                AuthResult.Success(true)
            }
        } catch (e: Exception) {
            AuthResult.Exception(e)
        }
    }

    suspend fun googleRegister(req: GoogleRegisterRequest): AuthResult<LoginResponse> {
        return try {
            val res = api.googleRegister(req)
            if (res.isSuccessful) {
                val body = res.body()!!
                tokenManager.saveToken(
                    accessToken           = body.accessToken,
                    refreshToken          = body.refreshToken,
                    accessTokenExpiresAt  = body.accessTokenExpiresAt,
                    refreshTokenExpiresAt = body.refreshTokenExpiresAt,
                    userId                = body.userId,
                )
                AuthResult.Success(body)
            } else {
                AuthResult.Error(res.code(), parseError(res.errorBody()?.string()))
            }
        } catch (e: Exception) {
            AuthResult.Exception(e)
        }
    }

    suspend fun logout(): AuthResult<Unit> {
        val refreshToken = tokenManager.refreshToken.firstOrNull()
        return try {
            val result = if (refreshToken != null) {
                val res = api.logout(LogoutRequest(refreshToken))
                if (res.isSuccessful) AuthResult.Success(Unit)
                else AuthResult.Error(res.code(), parseError(res.errorBody()?.string()))
            } else {
                AuthResult.Success(Unit)
            }
            tokenManager.clearToken()
            result
        } catch (e: Exception) {
            tokenManager.clearToken()   // 옵션 A: 네트워크 실패해도 로컬 토큰은 삭제 (UX 우선)
            AuthResult.Exception(e)
        }
    }

    private fun parseError(body: String?): String {
        return try {
            gson.fromJson(body, ErrorResponse::class.java).message
        } catch (_: Exception) {
            "알 수 없는 오류가 발생했습니다."
        }
    }
}
