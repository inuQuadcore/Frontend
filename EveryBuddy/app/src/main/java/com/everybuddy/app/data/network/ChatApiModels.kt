package com.everybuddy.app.data.network

import com.google.gson.annotations.SerializedName

// 공통 에러 응답
/**
 * 서버 공통 에러 응답 구조
 * {
 *   "code": 400,
 *   "name": "INVALID_INPUT_VALUE",
 *   "message": "잘못된 입력입니다.",
 *   "errors": { ... }   // 필드별 에러 (선택)
 * }
 */
data class ApiErrorResponse(
    @SerializedName("code")    val code    : Int,
    @SerializedName("name")    val name    : String,
    @SerializedName("message") val message : String,
    @SerializedName("errors")  val errors  : Map<String, String>? = null,
)


// 메시지 API — POST /api/v1/메시지
/**
 * 메시지 전송 요청 (멀티파트/폼 데이터)
 *
 * 필드
 * - request: 메시지 전송 요청 JSON (required)
 *     - chatRoomId : Long  — 전송할 채팅방 ID
 *     - content    : String — 메시지 본문
 * - file: 원본 파일 (선택, 최대 10MB)
 *     지원 형식: jpg/jpeg/png/gif/webp/heic,
 *                mp4/mov/avi/webm,
 *                mp3/wav/m4a/aac,
 *                pdf/txt/doc/docx/xls/xlsx/ppt/pptx,
 *                zip/rar
 *
 * 응답
 * 204 — 메시지 전송 성공
 * 400 — INVALID_INPUT_VALUE (chatRoomId, content 누락)
 * 401 — JWT_ENTRY_POINT (로그인 필요)
 * 403 — USER_NOT_IN_CHATROOM (채팅방 접근 권한 없음)
 * 404 — USER_NOT_FOUND (사용자를 찾을 수 없음)
 * 413 — FILE_SIZE_EXCEEDED (파일 크기 10MB 초과)
 */
data class SendMessageRequest(
    @SerializedName("chatRoomId") val chatRoomId : Long,
    @SerializedName("content")    val content    : String,
)


// 메시지 API — PUT /api/v1/messages/{messageId}/read
/**
 * 메시지 읽음 처리
 *
 * Path Parameter
 * - messageId: Long (required) — 읽어주세요 처리할 마지막 메시지 ID
 *   "특정 내용까지 처리합니다" → messageId 이하 모든 메시지를 읽음 처리
 *
 * 응답
 * 204 — 읽음 처리 성공
 * 401 — JWT_ENTRY_POINT
 * 403 — USER_NOT_IN_CHATROOM
 * 404 — MESSAGE_NOT_FOUND
 */
// 별도 Request Body 없음 — Path Parameter만 사용


// 메시지 API — DELETE /api/v1/messages/{messageId}
/**
 * 메시지 삭제 (자신이 송신한 메시지만 삭제 가능)
 *
 * Path Parameter
 * - messageId: Long (required)
 *
 * 응답
 * 204 — 메시지 삭제 성공
 * 401 — JWT_ENTRY_POINT
 * 403 — NOT_MESSAGE_OF_USER (자신의 메시지만 삭제 가능)
 * 404 — MESSAGE_NOT_FOUND
 * 409 — MESSAGE_ALREADY_DELETED (이미 삭제된 메시지)
 */
// 별도 Request Body 없음 — Path Parameter만 사용

// 채팅방 API — GET /api/v1/채팅방
/**
 * 내 채팅방 목록 조회 (내가 참여하는 모든 미팅방)
 *
 * 응답 200
 * [
 *   {
 *     "chatRoomId"     : 1,
 *     "roomName"       : "스터디 그룹",
 *     "createdAt"      : "2026-01-12T10:30:00",
 *     "participantIds" : [1, 2, 3],
 *     "unreadCount"    : 5
 *   },
 *   ...
 * ]
 * 401 — JWT_ENTRY_POINT
 */
data class ChatRoomResponse(
    @SerializedName("chatRoomId")     val chatRoomId     : Long,
    @SerializedName("roomName")       val roomName       : String,
    @SerializedName("createdAt")      val createdAt      : String,
    @SerializedName("participantIds") val participantIds : List<Long>,
    @SerializedName("unreadCount")    val unreadCount    : Int,
)


// 채팅방 API — POST /api/v1/채팅방
/**
 * 채팅방(소개방) 생성
 *
 * Request Body — application/json
 * {
 *   "roomName"       : "스터디 그룹",
 *   "participantIds" : [2, 3, 4]
 * }
 *
 * 응답 200
 * {
 *   "chatRoomId"     : 1,
 *   "roomName"       : "스터디 그룹",
 *   "createdAt"      : "2026-01-12T10:30:00",
 *   "participantIds" : [1, 2, 3],
 *   "unreadCount"    : 0
 * }
 * 400 — INVALID_INPUT_VALUE (roomName, participantIds 누락)
 * 401 — JWT_ENTRY_POINT
 * 404 — USER_NOT_FOUND
 */
data class CreateChatRoomRequest(
    @SerializedName("roomName")       val roomName       : String,
    @SerializedName("participantIds") val participantIds : List<Long>,
)


// TODO: 아직 없는 API — 더 있을듯, 페이지들도 덜 구현 상태라.
/*
 * 1. GET /api/v1/messages?chatRoomId={id}&page={n}
 *    채팅방 메시지 목록 페이지네이션 조회
 *    현재 서버 미구현 — ChatViewModel의 loadMessages() 함수에 TODO로 표시됨
 *
 * 2. WebSocket / SSE — 실시간 메시지 수신
 *    현재 서버 미구현 — 채팅방 내 새 메시지 실시간 수신에 필요
 *    연동 전까지는 더미 데이터로 동작
 *
 * 3. POST /api/v1/voice/stt
 *    음성 파일 → 텍스트(STT) 변환 (Whisper 연동? QWEN3-mt 모델 사용)
 *    현재 서버 미구현 — ChatViewModel.onVoicePlayClick() 내 TODO로 표시됨
 *
 * 4. POST /api/v1/translate
 *    텍스트 번역 (QWEN3-mt 연동)
 *    현재 서버 미구현 — ChatViewModel.onTranslateClick() 내 TODO로 표시됨
 *
 * 5. GET /api/v1/users/{userId}
 *    유저 프로필 조회 (이름, 국적, 프로필 이미지 URL 등)
 *    현재 서버 미구현 — ChatRoom.partnerImageUrl 로딩에 필요
 *
 * 6. DELETE /api/v1/채팅방/{chatRoomId}
 *    채팅방 나가기
 *    현재 서버 미구현 — ChatRoomMenuSheet의 "채팅방 나가기" 버튼에 연결 필요
 */
