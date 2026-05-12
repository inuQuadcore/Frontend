package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

// 사용자 언어/수준 — 여러 도메인(Auth/Friend/Discover/User) 공유
data class LanguageLevel(
    @SerializedName("language") val language : String,
    @SerializedName("level")    val level    : Int,
)
