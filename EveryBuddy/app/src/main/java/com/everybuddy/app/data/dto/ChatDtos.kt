package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

/**
 * 채팅방 — GET / 응답, POST / 응답 양쪽 모두 사용.
 * - unreadCount/lastMessage/lastMessageTime: POST 응답에서는 null, GET 응답에서만 채워짐.
 * - participants: 본인 포함 참여자 표시용 정보. 1:1방 상대 이름/그룹방 모자이크 등 클라가 별도 GET /users/{id} 호출 없이 표시 가능.
 * - roomName: 항상 값 있음 (서버 NotBlank validation). 1:1방은 표시 시점에 무시하고 participants에서 상대 이름 직접 사용.
 */
data class ChatRoom(
    @SerializedName("chatRoomId")      val chatRoomId      : Long,
    @SerializedName("roomName")        val roomName        : String,
    // 서버는 "group"으로 직렬화 (Kotlin Boolean `is` 접두사 제거). isGroup은 alternate로 호환.
    @SerializedName(value = "group", alternate = ["isGroup"])
    val isGroup : Boolean,
    @SerializedName("createdAt")       val createdAt       : String,
    @SerializedName("participants")    val participants    : List<ParticipantSummary> = emptyList(),
    @SerializedName("unreadCount")     val unreadCount     : Int?    = null,
    @SerializedName("lastMessage")     val lastMessage     : String? = null,
    @SerializedName("lastMessageTime") val lastMessageTime : String? = null,   // ISO 8601 LocalDateTime
)

/** 채팅방 참여자 표시용 정보 (GET /chatrooms 응답에 동봉). */
data class ParticipantSummary(
    @SerializedName("userId")          val userId          : Long,
    @SerializedName("name")            val name            : String,
    @SerializedName("profileImageUrl") val profileImageUrl : String? = null,
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

/** POST /chatrooms/{id}/participants — 그룹방 멤버 초대 요청. */
data class InviteParticipantsRequest(
    @SerializedName("participantIds") val participantIds : List<Long>,
)
