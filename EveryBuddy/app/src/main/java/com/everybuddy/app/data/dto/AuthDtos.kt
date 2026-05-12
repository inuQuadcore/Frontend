package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

// OAuth
data class GoogleAuthRequest(
    @SerializedName("idToken") val idToken: String,
)

data class GoogleAuthResponse(
    @SerializedName("isNewUser") val isNewUser : Boolean,
    @SerializedName("tempToken") val tempToken : String?,
    @SerializedName("loginData") val loginData : LoginData?,
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
    val loginId           : String,
    val password          : String,
    val name              : String,
    val checked           : Boolean,                // 약관 동의
    val country           : String,                 // "KOREA", "USA" 등
    val birthday          : String,                 // "2000-01-01"
    val gender            : String,                 // "MALE" | "FEMALE"
    val bio               : String,
    val tags              : List<String>,           // ["SPORTS", "INTJ"]
    val primaryLanguage   : String,                 // "KOREAN" — 자동 level=5, 자동 번역 기본 대상
    val interestLanguages : List<LanguageLevel>,    // [{ "language": "ENGLISH", "level": 3 }]
)

data class LoginRequest(
    val loginId  : String,
    val password : String,
)

data class RefreshTokenRequest(
    val refreshToken: String,
)

data class LogoutRequest(
    val refreshToken: String,
)

data class GoogleRegisterRequest(
    val tempToken         : String,
    val country           : String,
    val birthday          : String,
    val gender            : String,
    val bio               : String,
    val tags              : List<String>,
    val primaryLanguage   : String,
    val interestLanguages : List<LanguageLevel>,
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
