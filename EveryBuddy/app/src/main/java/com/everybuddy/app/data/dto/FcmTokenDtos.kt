package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

data class FcmTokenRegisterRequest(
    @SerializedName("token") val token: String,
)
