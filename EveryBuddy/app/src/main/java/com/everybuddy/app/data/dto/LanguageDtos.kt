package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

// 사용자 언어/수준 — 여러 도메인(Auth/Friend/Discover/User) 공유
// isPrimary: GET /users/{id}/languages 응답에서만 의미. 다른 도메인 응답엔 기본 false.
data class LanguageLevel(
    @SerializedName("language")  val language  : String,
    @SerializedName("level")     val level     : Int,
    @SerializedName("isPrimary") val isPrimary : Boolean = false,
)
