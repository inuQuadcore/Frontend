package com.everybuddy.app.data.network

import com.google.gson.annotations.SerializedName

data class ApiErrorResponse(
    @SerializedName("code")    val code    : Int,
    @SerializedName("name")    val name    : String,
    @SerializedName("message") val message : String,
    @SerializedName("errors")  val errors  : Map<String, String>? = null,
)

// GET /api/v1/chatrooms — 내 채팅방 목록 조회
data class ChatRoomResponse(
    @SerializedName("chatRoomId")     val chatRoomId     : Long,
    @SerializedName("roomName")       val roomName       : String,
    @SerializedName("createdAt")      val createdAt      : String,
    @SerializedName("participantIds") val participantIds : List<Long>,
    @SerializedName("unreadCount")    val unreadCount    : Int,
)

// POST /api/v1/chatrooms — 채팅방 생성
data class CreateChatRoomRequest(
    @SerializedName("roomName")       val roomName       : String,
    @SerializedName("participantIds") val participantIds : List<Long>,
)

// POST /api/v1/메시지 — 메시지 전송 (멀티파트)
data class SendMessageRequest(
    @SerializedName("chatRoomId") val chatRoomId : Long,
    @SerializedName("content")    val content    : String,
)

// TODO: 추후 서버 구현 후 추가
//   1. GET  /api/v1/messages?chatRoomId={id}&page={n}  — 메시지 목록 페이지네이션
//   2. WebSocket / SSE                                  — 실시간 메시지 수신
//   3. POST /api/v1/voice/stt                           — STT 변환
//   4. POST /api/v1/translate                           — 번역 (QWEN3-mt)
//   5. GET  /api/v1/users/{userId}                      — 유저 프로필 조회
//   6. DELETE /api/v1/chatrooms/{chatRoomId}            — 채팅방 나가기
