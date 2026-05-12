package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

/**
 * 채팅방 — GET / 응답, POST / 응답 양쪽 모두 사용.
 * 단 POST / (생성) 응답의 unreadCount는 null. 값이 채워지는 건 GET / (목록 조회)에서만.
 * → unreadCount는 nullable로 둠.
 */
data class ChatRoomDto(
    @SerializedName("chatRoomId")     val chatRoomId     : Long,
    @SerializedName("roomName")       val roomName       : String,
    @SerializedName("createdAt")      val createdAt      : String,
    @SerializedName("participantIds") val participantIds : List<Long>,
    @SerializedName("unreadCount")    val unreadCount    : Int? = null,
)

data class CreateChatRoomRequest(
    @SerializedName("roomName")       val roomName       : String,
    @SerializedName("participantIds") val participantIds : List<Long>,
)
