package com.everybuddy.app.data.network

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
// 공통 에러 응답
// {
//   "code"    : 401,
//   "name"    : "JWT_ENTRY_POINT",
//   "message" : "로그인이 필요합니다.",
//   "errors"  : null   // 또는 필드별 에러 Map
// }
// ─────────────────────────────────────────────────────────────────────────────
data class ApiErrorResponse(
    @SerializedName("code")    val code    : Int,
    @SerializedName("name")    val name    : String,
    @SerializedName("message") val message : String,
    @SerializedName("errors")  val errors  : Map<String, String>? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// GET /api/v1/chatrooms — 내 채팅방 목록 조회
//
// 응답 200
// [
//   {
//     "chatRoomId"     : 1,
//     "roomName"       : "스터디 그룹",
//     "createdAt"      : "2026-01-12T10:30:00",
//     "participantIds" : [1, 2, 3],
//     "unreadCount"    : 5
//   }
// ]
// 응답 401 — JWT_ENTRY_POINT
// ─────────────────────────────────────────────────────────────────────────────
data class ChatRoomResponse(
    @SerializedName("chatRoomId")     val chatRoomId     : Long,
    @SerializedName("roomName")       val roomName       : String,
    @SerializedName("createdAt")      val createdAt      : String,
    @SerializedName("participantIds") val participantIds : List<Long>,
    @SerializedName("unreadCount")    val unreadCount    : Int,
)

// ─────────────────────────────────────────────────────────────────────────────
// POST /api/v1/chatrooms — 채팅방 생성
//
// 요청 (application/json)
// {
//   "roomName"       : "스터디 그룹",
//   "participantIds" : [2, 3, 4]
// }
//
// 응답 200  — ChatRoomResponse
// 응답 400  — INVALID_INPUT_VALUE
//   "errors": {
//     "roomName"       : "채팅방 이름을 입력해주세요.",
//     "participantIds" : "채팅방 참여자를 선택해주세요."
//   }
// 응답 401  — JWT_ENTRY_POINT
// 응답 404  — USER_NOT_FOUND : 해당 유저를 찾을 수 없습니다.
// ─────────────────────────────────────────────────────────────────────────────
data class CreateChatRoomRequest(
    @SerializedName("roomName")       val roomName       : String,
    @SerializedName("participantIds") val participantIds : List<Long>,
)

// ─────────────────────────────────────────────────────────────────────────────
// POST /api/v1/메시지 — 메시지 전송 (멀티파트)
//
// 응답 204 — 전송 성공
// 응답 400 — INVALID_INPUT_VALUE
// 응답 401 — JWT_ENTRY_POINT
// 응답 403 — USER_NOT_IN_CHATROOM
// 응답 404 — USER_NOT_FOUND
// 응답 413 — FILE_SIZE_EXCEEDED (10MB 초과)
// ─────────────────────────────────────────────────────────────────────────────
data class SendMessageRequest(
    @SerializedName("chatRoomId") val chatRoomId : Long,
    @SerializedName("content")    val content    : String,
)

// ─────────────────────────────────────────────────────────────────────────────
// TODO: 추후 서버 구현 후 추가
//   1. GET  /api/v1/messages?chatRoomId={id}&page={n}  — 메시지 목록 페이지네이션
//   2. WebSocket / SSE                                  — 실시간 메시지 수신
//   3. POST /api/v1/voice/stt                           — STT 변환
//   4. POST /api/v1/translate                           — 번역 (QWEN3-mt)
//   5. GET  /api/v1/users/{userId}                      — 유저 프로필 조회
//   6. DELETE /api/v1/chatrooms/{chatRoomId}            — 채팅방 나가기
// ─────────────────────────────────────────────────────────────────────────────
