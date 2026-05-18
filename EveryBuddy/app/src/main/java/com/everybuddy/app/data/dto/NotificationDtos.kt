package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

data class Notification(
    @SerializedName("notificationId") val notificationId: Long,
    @SerializedName("body")           val body          : String,
    @SerializedName("fromUserId")     val fromUserId    : Long,
    @SerializedName("createdAt")      val createdAt     : String,
    // 서버 Kotlin Boolean 직렬화는 `is` 접두사 제거 → "read"로 옴. isRead는 alternate로 호환.
    @SerializedName(value = "read", alternate = ["isRead"])
    val isRead : Boolean,
)

data class NotificationListResponse(
    @SerializedName("notifications") val notifications: List<Notification>,
    @SerializedName("nextCursor")    val nextCursor   : Long?,
    @SerializedName("hasNext")       val hasNext      : Boolean,
)

data class HasUnreadResponse(
    @SerializedName("hasUnread") val hasUnread: Boolean,
)
