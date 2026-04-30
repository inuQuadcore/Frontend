package com.everybuddy.app.data.network

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatRepository
 *
 * 채팅 관련 API 호출을 담당하는 Repository.
 * AuthRepository와 동일한 AuthResult 래퍼를 사용하여
 * ViewModel에서 일관된 방식으로 결과를 처리.
 *
 * Hilt @Singleton으로 주입.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val chatApi : ChatApiService,
    private val gson    : Gson,
) {

    // 채팅방 목록 조회
    // GET /api/v1/채팅방
    /**
     * 내가 참여하는 채팅방 목록을 서버에서 조회.
     *
     * 성공 | AuthResult.Success(List<ChatRoomResponse>)
     * 실패
     *  401 — JWT_ENTRY_POINT : 로그인 필요
     * 예외 AuthResult.Exception (네트워크 오류 등)
     */
    suspend fun getChatRooms(): AuthResult<List<ChatRoomResponse>> = safeCall {
        chatApi.getChatRooms()
    }

    // 채팅방 생성
    // POST /api/v1/채팅방
    /**
     * 새 채팅방 생성.
     *
     * @param roomName       채팅방 이름
     * @param participantIds 참여자 ID 목록 (나 자신 제외, 서버가 자동으로 추가)
     *
     * 성공 | AuthResult.Success(ChatRoomResponse)
     * 실패
     *  400 — INVALID_INPUT_VALUE : roomName 또는 participantIds 누락
     *  401 — JWT_ENTRY_POINT
     *  404 — USER_NOT_FOUND
     * 예외 AuthResult.Exception
     */
    suspend fun createChatRoom(
        roomName       : String,
        participantIds : List<Long>,
    ): AuthResult<ChatRoomResponse> = safeCall {
        chatApi.createChatRoom(CreateChatRoomRequest(roomName, participantIds))
    }

    // 메시지 전송
    // POST /api/v1/메시지
    /**
     * 텍스트 메시지 전송.
     *
     * @param chatRoomId 전송할 채팅방 ID
     * @param content    메시지 본문
     *
     * 성공 | AuthResult.Success(Unit) — 204 No Content
     * 실패
     *  400 — INVALID_INPUT_VALUE : chatRoomId 또는 content 누락
     *  401 — JWT_ENTRY_POINT
     *  403 — USER_NOT_IN_CHATROOM
     *  404 — USER_NOT_FOUND
     * 예외 AuthResult.Exception
     */
    suspend fun sendTextMessage(
        chatRoomId : Long,
        content    : String,
    ): AuthResult<Unit> = safeCall {
        val requestJson = gson.toJson(SendMessageRequest(chatRoomId, content))
        val requestBody = requestJson.toRequestBody("application/json".toMediaTypeOrNull())
        chatApi.sendMessage(request = requestBody, file = null)
    }

    /**
     * 파일(음성/이미지 등) 메시지 전송.
     *
     * @param chatRoomId 전송할 채팅방 ID
     * @param content    메시지 본문 (파일만 보낼 경우 빈 문자열 가능)
     * @param file       첨부 파일 (최대 10MB)
     *
     * 지원 파일 형식
     * 이미지: jpg/jpeg/png/gif/webp/heic
     * 비디오: mp4/mov/avi/webm
     * 오디오: mp3/wav/m4a/aac
     * 문서:   pdf/txt/doc/docx/xls/xlsx/ppt/pptx
     * 압축:   zip/rar
     *
     * 성공 | AuthResult.Success(Unit) — 204 No Content
     * 실패
     *  400 — INVALID_INPUT_VALUE
     *  401 — JWT_ENTRY_POINT
     *  403 — USER_NOT_IN_CHATROOM
     *  404 — USER_NOT_FOUND
     *  413 — FILE_SIZE_EXCEEDED (10MB 초과)
     * 예외 AuthResult.Exception
     */
    suspend fun sendFileMessage(
        chatRoomId : Long,
        content    : String,
        file       : File,
    ): AuthResult<Unit> = safeCall {
        val requestJson = gson.toJson(SendMessageRequest(chatRoomId, content))
        val requestBody = requestJson.toRequestBody("application/json".toMediaTypeOrNull())
        val mimeType    = resolveMimeType(file.extension)
        val filePart    = MultipartBody.Part.createFormData(
            name     = "file",
            filename = file.name,
            body     = file.asRequestBody(mimeType.toMediaTypeOrNull()),
        )
        chatApi.sendMessage(request = requestBody, file = filePart)
    }

    // 메시지 읽음 처리
    // PUT /api/v1/messages/{messageId}/read
    /**
     * 특정 메시지 ID 이하의 모든 메시지를 읽음 처리.
     *
     * @param messageId 읽음 처리할 마지막 메시지 ID
     *
     * 성공 | AuthResult.Success(Unit) — 204 No Content
     * 실패
     *  401 — JWT_ENTRY_POINT
     *  403 — USER_NOT_IN_CHATROOM
     *  404 — MESSAGE_NOT_FOUND
     * 예외 AuthResult.Exception
     */
    suspend fun markMessageRead(messageId: Long): AuthResult<Unit> = safeCall {
        chatApi.markMessageRead(messageId)
    }

    // 메시지 삭제
    // DELETE /api/v1/messages/{messageId}
    /**
     * 자신이 송신한 메시지를 삭제.
     *
     * @param messageId 삭제할 메시지 ID
     *
     * 성공 | AuthResult.Success(Unit) — 204 No Content
     * 실패
     *  401 — JWT_ENTRY_POINT
     *  403 — NOT_MESSAGE_OF_USER : 타인의 메시지 삭제 시도
     *  404 — MESSAGE_NOT_FOUND
     *  409 — MESSAGE_ALREADY_DELETED : 이미 삭제된 메시지
     * 예외 AuthResult.Exception
     */
    suspend fun deleteMessage(messageId: Long): AuthResult<Unit> = safeCall {
        chatApi.deleteMessage(messageId)
    }

    // TODO: 추후 서버 구현 후 추가 예정
    /*
     * 1. 채팅방 메시지 목록 조회 (페이지네이션)
     * 2. STT 변환 (음성 → 텍스트)
     * 3. 텍스트 번역
     * 4. 유저 프로필 조회
     * 5. 채팅방 나가기
     */


    // 내부 유틸
    /**
     * API 호출을 AuthResult로 래핑하는 공통 헬퍼.
     * AuthRepository의 safeCall과 동일한 패턴.
     */
    private suspend fun <T> safeCall(block: suspend () -> retrofit2.Response<T>): AuthResult<T> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                AuthResult.Success<T>(response.body())
            } else {
                val errorBody = response.errorBody()?.string()
                val apiError  = try {
                    gson.fromJson(errorBody, ApiErrorResponse::class.java)
                } catch (e: Exception) { null }
                AuthResult.Error(
                    code    = response.code(),
                    message = apiError?.message ?: response.message(),
                )
            }
        } catch (e: Exception) {
            AuthResult.Exception(e)
        }
    }

    /** 파일 확장자 → MIME 타입 */
    private fun resolveMimeType(ext: String): String = when (ext.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png"         -> "image/png"
        "gif"         -> "image/gif"
        "webp"        -> "image/webp"
        "heic"        -> "image/heic"
        "mp4"         -> "video/mp4"
        "mov"         -> "video/quicktime"
        "avi"         -> "video/x-msvideo"
        "webm"        -> "video/webm"
        "mp3"         -> "audio/mpeg"
        "wav"         -> "audio/wav"
        "m4a"         -> "audio/mp4"
        "aac"         -> "audio/aac"
        "pdf"         -> "application/pdf"
        "zip"         -> "application/zip"
        "rar"         -> "application/x-rar-compressed"
        else          -> "application/octet-stream"
    }
}
