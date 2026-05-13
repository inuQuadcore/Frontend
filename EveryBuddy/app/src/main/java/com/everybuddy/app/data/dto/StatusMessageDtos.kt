package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

data class StatusMessageRequest(
    @SerializedName("content") val content : String,
)

data class MyStatusMessageResponse(
    @SerializedName("statusMessageId") val statusMessageId : Long,
    @SerializedName("profileImageUrl") val profileImageUrl : String? = null,
    @SerializedName("nickname")        val nickname        : String,
    @SerializedName("content")         val content         : String,
    @SerializedName("updatedAt")       val updatedAt       : String,   // ISO 8601 LocalDateTime
)

data class FriendStatusMessagesResponse(
    @SerializedName("statusMessages") val statusMessages : List<FriendStatusMessage>,
    @SerializedName("nextCursor")     val nextCursor     : Long?   = null,
    @SerializedName("hasNext")        val hasNext        : Boolean,
)

data class FriendStatusMessage(
    @SerializedName("statusMessageId") val statusMessageId : Long,
    @SerializedName("profileImageUrl") val profileImageUrl : String? = null,
    @SerializedName("nickname")        val nickname        : String,
    @SerializedName("content")         val content         : String,
    @SerializedName("updatedAt")       val updatedAt       : String,
)
