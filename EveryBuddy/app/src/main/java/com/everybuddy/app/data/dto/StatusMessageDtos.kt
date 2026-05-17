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
    @SerializedName("userId")          val userId          : Long   = 0L,
    @SerializedName("profileImageUrl") val profileImageUrl : String? = null,
    @SerializedName("nickname")        val nickname        : String,
    @SerializedName("content")         val content         : String,
    @SerializedName("updatedAt")       val updatedAt       : String,
)

data class FriendStatusMessageDto(
    val statusMessageId : Long,
    val userId          : Long    = 0L,
    val userName        : String,
    val profileImageUrl : String? = null,
    val content         : String,
    val timeAgo         : String  = "",
)

fun FriendStatusMessage.toDto(): FriendStatusMessageDto = FriendStatusMessageDto(
    statusMessageId = statusMessageId,
    userId          = userId,
    userName        = nickname,
    profileImageUrl = profileImageUrl,
    content         = content,
    timeAgo         = updatedAt,
)
