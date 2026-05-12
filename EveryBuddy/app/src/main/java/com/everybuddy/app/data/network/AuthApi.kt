package com.everybuddy.app.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.google.gson.annotations.SerializedName

//OAuth
data class GoogleAuthRequest(
    @SerializedName("idToken") val idToken: String,
)
data class GoogleAuthResponse(
    @SerializedName("isNewUser")  val isNewUser  : Boolean,
    @SerializedName("tempToken")  val tempToken  : String?,
    @SerializedName("loginData")  val loginData  : LoginData?,
)
data class LoginData(
    @SerializedName("userId")                val userId                : Long,
    @SerializedName("accessToken")           val accessToken           : String,
    @SerializedName("refreshToken")          val refreshToken          : String,
    @SerializedName("tokenType")             val tokenType             : String,
    @SerializedName("accessTokenExpiresAt")  val accessTokenExpiresAt  : String,
    @SerializedName("refreshTokenExpiresAt") val refreshTokenExpiresAt : String,
)

// Request DTOs
data class RegisterRequest(
    val loginId   : String,
    val password  : String,
    val name      : String,
    val country   : String,          // "KOREA", "USA" 등
    val birthday  : String,          // "2000-01-01"
    val gender    : String,          // "MALE" | "FEMALE"
    val bio       : String,
    val tags      : List<String>,    // ["SPORTS", "INTJ"]
    val languages : List<LanguageLevel>,
    val checked   : Boolean,         // 약관 동의
)

data class LanguageLevel(
    val language : String,           // "ENGLISH", "KOREAN" 등
    val level    : Int,              // 1~5
)

data class LoginRequest(
    val loginId  : String,
    val password : String,
)

// Response DTOs
data class LoginResponse(
    val userId                : Long,
    val accessToken           : String,
    val refreshToken          : String,
    val tokenType             : String,   // "Bearer"
    val accessTokenExpiresAt  : String,   // ISO 8601
    val refreshTokenExpiresAt : String,   // ISO 8601
)

data class FirebaseTokenResponse(
    val firebaseToken: String,
)

// 에러 공통 응답
data class ErrorResponse(
    val code    : Int,
    val name    : String,
    val message : String,
)

// Retrofit API 인터페이스
interface AuthApi {
    @POST("/api/v1/auth/oauth/google")
    suspend fun googleAuth(
        @Body request: GoogleAuthRequest,
    ): Response<GoogleAuthResponse>

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

    // GET /api/v1/auth/firebaseToken — Firebase 실시간 채팅 토큰 발급 (JWT 필요)
    @GET("api/v1/auth/firebaseToken")
    suspend fun firebaseToken(): Response<FirebaseTokenResponse>
}
