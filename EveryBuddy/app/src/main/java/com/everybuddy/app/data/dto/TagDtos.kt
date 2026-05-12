package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

// 유저 태그 — 여러 도메인(Friend/Discover/User) 공유
data class TagDto(
    @SerializedName("tag")      val tag      : String,
    @SerializedName("category") val category : String = "",
)
