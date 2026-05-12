package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

data class StatusMessageRequest(
    @SerializedName("content") val content : String,
)

data class MyStatusMessageResponse(
    @SerializedName("statusMessageId") val statusMessageId : Long,
    @SerializedName("content")         val content         : String,
    @SerializedName("timeAgo")         val timeAgo         : String,
)

data class FriendStatusMessagesResponse(
    @SerializedName("statusMessages") val statusMessages : List<FriendStatusMessageDto>,
    @SerializedName("nextCursor")     val nextCursor     : Long?   = null,
    @SerializedName("hasNext")        val hasNext        : Boolean,
)

data class FriendStatusMessageDto(
    @SerializedName("statusMessageId") val statusMessageId : Long,
    @SerializedName("userId")          val userId          : Long,
    @SerializedName("userName")        val userName        : String,
    @SerializedName("profileImageUrl") val profileImageUrl : String? = null,
    @SerializedName("content")         val content         : String,
    @SerializedName("timeAgo")         val timeAgo         : String,
)
