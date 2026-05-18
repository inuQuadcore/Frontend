package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

// 사용자 언어/수준 — 여러 도메인(Auth/Friend/Discover/User) 공유
// isPrimary: GET /users/{id}/languages 응답에서만 의미. 다른 도메인 응답엔 기본 false.
// 서버(Spring/Kotlin Boolean 직렬화)는 `is` 접두사 제거된 "primary"로 내려옴 → SerializedName primary, isPrimary는 alternate로 호환.
data class LanguageLevel(
    @SerializedName("language") val language  : String,
    @SerializedName("level")    val level     : Int,
    @SerializedName(value = "primary", alternate = ["isPrimary"])
    val isPrimary : Boolean = false,
)
