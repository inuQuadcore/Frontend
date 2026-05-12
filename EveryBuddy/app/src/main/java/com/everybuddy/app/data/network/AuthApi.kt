package com.everybuddy.app.data.network

import com.everybuddy.app.data.dto.FirebaseTokenResponse
import com.everybuddy.app.data.dto.GoogleAuthRequest
import com.everybuddy.app.data.dto.GoogleAuthResponse
import com.everybuddy.app.data.dto.GoogleRegisterRequest
import com.everybuddy.app.data.dto.LoginRequest
import com.everybuddy.app.data.dto.LoginResponse
import com.everybuddy.app.data.dto.LogoutRequest
import com.everybuddy.app.data.dto.RefreshTokenRequest
import com.everybuddy.app.data.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Retrofit API 인터페이스
interface AuthApi {
    @POST("/api/v1/auth/oauth/google")
    suspend fun googleAuth(
        @Body request: GoogleAuthRequest,
    ): Response<GoogleAuthResponse>

    // POST /api/v1/auth/oauth/google/register — 구글 신규 유저 가입 완료
    @POST("api/v1/auth/oauth/google/register")
    suspend fun googleRegister(
        @Body body: GoogleRegisterRequest,
    ): Response<LoginResponse>  // 200: 성공 / 401: TEMP_TOKEN_EXPIRED·INVALID_OAUTH_TOKEN / 409: DUPLICATED_USER

    // POST /api/v1/auth/register — 회원가입
    @POST("api/v1/auth/register")
    suspend fun register(
        @Body body: RegisterRequest,
    ): Response<Unit>           // 200: 성공 / 400: 잘못된 입력 / 409: 중복

    // POST /api/v1/auth/login — 로그인 + JWT 발급
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body body: LoginRequest,
    ): Response<LoginResponse>  // 200: 성공 / 401: 실패 / 404: 유저없음

    // POST /api/v1/auth/refresh — 토큰 재발급 (rotation)
    @POST("api/v1/auth/refresh")
    suspend fun refresh(
        @Body body: RefreshTokenRequest,
    ): Response<LoginResponse>  // 200: 성공 / 401: REFRESH_TOKEN_NOT_FOUND·EXPIRED

    // POST /api/v1/auth/logout — 리프레쉬 토큰 무효화
    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Body body: LogoutRequest,
    ): Response<Unit>           // 200: 성공 (본문 없음)

    // GET /api/v1/auth/firebaseToken — Firebase 실시간 채팅 토큰 발급 (JWT 필요)
    @GET("api/v1/auth/firebaseToken")
    suspend fun firebaseToken(): Response<FirebaseTokenResponse>
}
