package com.everybuddy.app.data.repository

import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.EditMessageRequest
import com.everybuddy.app.data.dto.Message
import com.everybuddy.app.data.dto.SendMessageRequest
import com.everybuddy.app.data.dto.SyncMessagesResponse
import com.everybuddy.app.data.network.MessageApi
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val api  : MessageApi,
    private val gson : Gson,
) {
    /**
     * 텍스트 메시지 전송 — POST /api/v1/messages
     * 에러: 400(입력오류) | 401(인증) | 403(채팅방 접근 불가) | 404(방없음)
     */
    suspend fun sendTextMessage(
        chatRoomId : Long,
        content    : String,
    ): ApiResult<Unit> {
        val requestBody = gson.toJson(SendMessageRequest(chatRoomId, content))
            .toRequestBody("application/json".toMediaType())
        return safeApiCall(gson, { api.sendMessage(request = requestBody) }) {
            ApiResult.Success(Unit)
        }
    }

    /**
     * 파일 메시지 전송 — POST /api/v1/messages (multipart)
     * 지원: 이미지/비디오/오디오/문서/압축 (최대 10MB)
     * 에러: 400(지원안하는파일형식) | 413(파일크기초과) | 403(접근불가) | 404(방없음)
     */
    suspend fun sendFileMessage(
        chatRoomId : Long,
        content    : String,
        file       : File,
        mimeType   : String,
    ): ApiResult<Unit> {
        val requestBody = gson.toJson(SendMessageRequest(chatRoomId, content))
            .toRequestBody("application/json".toMediaType())
        val filePart = MultipartBody.Part.createFormData(
            name     = "file",
            filename = file.name,
            body     = file.asRequestBody(mimeType.toMediaType()),
        )
        return safeApiCall(gson, { api.sendMessage(request = requestBody, file = filePart) }) {
            ApiResult.Success(Unit)
        }
    }

    /**
     * 메시지 읽음 처리 — POST /api/v1/messages/{messageId}/read
     * 에러: 401(인증) | 403(채팅방 접근 불가) | 404(메시지 없음)
     */
    suspend fun readMessage(messageId: Long): ApiResult<Unit> =
        safeApiCall(gson, { api.readMessage(messageId) }) { ApiResult.Success(Unit) }

    /**
     * 텍스트 메시지 수정 — PATCH /api/v1/messages/{messageId}
     * 자신 메시지, 전송 후 5분 이내, 텍스트만 수정 가능
     * 에러: 400(본문누락 / CANNOT_EDIT_FILE_MESSAGE)
     *       403(타인메시지 NOT_MESSAGE_OF_USER / 5분초과 MESSAGE_EDIT_TIME_EXCEEDED)
     *       404(없음) | 409(이미삭제)
     */
    suspend fun editMessage(
        messageId : Long,
        content   : String,
    ): ApiResult<Message> =
        safeApiCall(gson, { api.editMessage(messageId, EditMessageRequest(content)) })

    /**
     * 메시지 삭제 — DELETE /api/v1/messages/{messageId}
     * 전송 후 5분 이내 자기 메시지만 삭제 가능
     * 에러: 403(권한없음 또는 5분 초과) | 404(없음) | 409(이미삭제)
     */
    suspend fun deleteMessage(messageId: Long): ApiResult<Unit> =
        safeApiCall(gson, { api.deleteMessage(messageId) }) { ApiResult.Success(Unit) }

    /**
     * 채팅방 메시지 동기화 — GET /api/v1/messages/chatrooms/{chatRoomId}
     * 채팅방 진입 시 1회 호출 → 이후는 Firebase Realtime DB로 실시간 갱신
     * @param since ISO 8601 시각 (null = 전체 조회)
     * 에러: 401(인증) | 403(채팅방 접근 불가) | 404(채팅방 없음)
     */
    suspend fun syncMessages(
        chatRoomId : Long,
        since      : String? = null,
    ): ApiResult<SyncMessagesResponse> =
        safeApiCall(gson, { api.syncMessages(chatRoomId, since) })
}
