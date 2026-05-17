package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

/**
 * 채팅방 — GET / 응답, POST / 응답 양쪽 모두 사용.
 * - unreadCount: POST / 응답에서는 null, GET / 응답에서만 채워짐.
 * - roomName: 항상 값 있음 (서버 NotBlank validation).
 *   1:1방은 클라가 상대 이름을 박아 보냄. 단 표시 시점에는 클라가 무시하고 자체로 상대 이름 렌더.
 *   그룹방은 사용자 입력한 이름 그대로 표시.
 */
data class ChatRoom(
    @SerializedName("chatRoomId")     val chatRoomId     : Long,
    @SerializedName("roomName")       val roomName       : String,
    @SerializedName("isGroup")        val isGroup        : Boolean,
    @SerializedName("createdAt")      val createdAt      : String,
    @SerializedName("participantIds") val participantIds : List<Long>,
    @SerializedName("unreadCount")    val unreadCount    : Int? = null,
)

/**
 * 채팅방 생성 요청.
 * - roomName: 항상 필수. 1:1방은 클라가 상대 이름을, 그룹방은 사용자 입력값을 넣어 전송.
 * - isGroup: 클라가 UI 흐름("1:1 대화" vs "그룹 채팅 만들기")에 따라 결정. 1:1방은 false + participantIds.size==1, 그룹방은 true + participantIds.size>=1.
 */
data class CreateChatRoomRequest(
    @SerializedName("roomName")       val roomName       : String,
    @SerializedName("isGroup")        val isGroup        : Boolean,
    @SerializedName("participantIds") val participantIds : List<Long>,
)
