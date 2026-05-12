package com.everybuddy.app.data.repository

import android.content.Context
import com.everybuddy.app.data.auth.AuthDataHolder
import com.everybuddy.app.data.auth.GoogleAuthManager
import com.everybuddy.app.data.auth.GoogleSignInResult
import com.everybuddy.app.data.dto.ApiErrorResponse
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.local.TokenManager
import com.everybuddy.app.data.network.AuthApi
import com.everybuddy.app.data.network.GoogleAuthRequest
import com.everybuddy.app.data.network.GoogleRegisterRequest
import com.everybuddy.app.data.network.LoginRequest
import com.everybuddy.app.data.network.LoginResponse
import com.everybuddy.app.data.network.LogoutRequest
import com.everybuddy.app.data.network.RefreshTokenRequest
import com.everybuddy.app.data.network.RegisterRequest
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.Response
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

    suspend fun login(loginId: String, password: String): ApiResult<LoginResponse> {
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
                ApiResult.Success(body)
            } else {
                parseError(res)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun refresh(refreshToken: String): ApiResult<LoginResponse> {
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
                ApiResult.Success(body)
            } else {
                parseError(res)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun register(req: RegisterRequest): ApiResult<Unit> {
        return try {
            val res = api.register(req)
            if (res.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                parseError(res)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun firebaseToken(): ApiResult<String> {
        return try {
            val res = api.firebaseToken()
            if (res.isSuccessful) {
                ApiResult.Success(res.body()!!.firebaseToken)
            } else {
                parseError(res)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun googleLogin(
        activityContext : Context,
        webClientId     : String,
    ): ApiResult<Boolean> {
        val signInResult = googleAuthManager.signIn(activityContext, webClientId)
        if (signInResult is GoogleSignInResult.Error) {
            return ApiResult.Error(-1, "GOOGLE_SIGN_IN_FAILED", signInResult.message)
        }
        val idToken = (signInResult as GoogleSignInResult.Success).idToken

        return try {
            val res = api.googleAuth(GoogleAuthRequest(idToken))
            if (!res.isSuccessful) {
                return parseError(res)
            }
            val body = res.body() ?: return ApiResult.Error(-1, "EMPTY_RESPONSE", "서버 응답이 없습니다.")

            if (!body.isNewUser) {
                val loginData = body.loginData
                    ?: return ApiResult.Error(-1, "EMPTY_LOGIN_DATA", "토큰 정보가 없습니다.")
                tokenManager.saveToken(
                    accessToken           = loginData.accessToken,
                    refreshToken          = loginData.refreshToken,
                    accessTokenExpiresAt  = loginData.accessTokenExpiresAt,
                    refreshTokenExpiresAt = loginData.refreshTokenExpiresAt,
                    userId                = loginData.userId,
                )
                ApiResult.Success(false)
            } else {
                val tempToken = body.tempToken
                    ?: return ApiResult.Error(-1, "EMPTY_TEMP_TOKEN", "임시 토큰이 없습니다.")
                authDataHolder.setGoogleSignup(tempToken)   // Onboarding 완료 시 googleRegister 호출에 사용
                ApiResult.Success(true)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun googleRegister(req: GoogleRegisterRequest): ApiResult<LoginResponse> {
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
                ApiResult.Success(body)
            } else {
                parseError(res)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun logout(): ApiResult<Unit> {
        val refreshToken = tokenManager.refreshToken.firstOrNull()
        return try {
            val result = if (refreshToken != null) {
                val res = api.logout(LogoutRequest(refreshToken))
                if (res.isSuccessful) ApiResult.Success(Unit)
                else parseError(res)
            } else {
                ApiResult.Success(Unit)
            }
            tokenManager.clearToken()
            result
        } catch (e: Exception) {
            tokenManager.clearToken()   // 옵션 A: 네트워크 실패해도 로컬 토큰은 삭제 (UX 우선)
            ApiResult.NetworkError(e)
        }
    }

    private fun parseError(res: Response<*>): ApiResult.Error {
        return try {
            val err = gson.fromJson(res.errorBody()?.string(), ApiErrorResponse::class.java)
            ApiResult.Error(err.code, err.name, err.message)
        } catch (_: Exception) {
            ApiResult.Error(res.code(), "PARSE_ERROR", "알 수 없는 오류가 발생했습니다.")
        }
    }
}
