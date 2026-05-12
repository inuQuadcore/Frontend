package com.everybuddy.app.data.network

import com.everybuddy.app.data.dto.EditMessageRequest
import com.everybuddy.app.data.dto.Message
import com.everybuddy.app.data.dto.SyncMessagesResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MessageApi {

    /**
     * POST /api/v1/messages — 메시지 전송 (텍스트 또는 파일)
     * 파일 포함 시 파일 메시지, 없으면 텍스트 메시지
     * 지원 파일: 이미지(jpg/jpeg/png/gif/webp/heic), 비디오(mp4/mov/avi/webm),
     *            오디오(mp3/wav/m4a/aac), 문서(pdf/txt/doc/docx/xls/xlsx/ppt/pptx), 압축(zip/rar)
     * 최대 크기: 10MB
     * 204: 전송 성공 | 400: 잘못된 입력 / 지원 안 하는 파일 형식
     * 401: 인증 | 403: 채팅방 접근 권한 없음
     * 404: 유저 또는 채팅방 없음 | 413: 파일 크기 초과
     */
    @Multipart
    @POST("api/v1/messages")
    suspend fun sendMessage(
        @Part("request") request : RequestBody,   // JSON: { chatRoomId, content }
        @Part file       : MultipartBody.Part? = null,
    ): Response<Unit>

    /**
     * POST /api/v1/messages/{messageId}/read — 메시지 읽음 처리
     * 특정 메시지까지 읽음 처리
     * 204: 성공 | 401: 인증 | 403: 채팅방 접근 권한 | 404: 메시지 없음
     */
    @POST("api/v1/messages/{messageId}/read")
    suspend fun readMessage(
        @Path("messageId") messageId: Long,
    ): Response<Unit>

    /**
     * DELETE /api/v1/messages/{messageId} — 메시지 삭제
     * 자신이 전송한 메시지만, 전송 후 5분 이내만 삭제 가능
     * 204: 성공 | 401: 인증 | 403: 권한 없음 또는 5분 초과
     * 404: 메시지 없음 | 409: 이미 삭제된 메시지
     */
    @DELETE("api/v1/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("messageId") messageId: Long,
    ): Response<Unit>

    /**
     * PATCH /api/v1/messages/{messageId} — 텍스트 메시지 수정
     * 자신이 전송한 텍스트 메시지만, 전송 후 5분 이내만 수정 가능
     * 파일 메시지는 수정 불가 (400 CANNOT_EDIT_FILE_MESSAGE)
     * 200: { messageId, userId, userName, messageType, content, sendAt,
     *        fileUrl, fileName, fileSize, mediaType }
     * 400: 본문 누락 / 파일 메시지 수정 불가
     * 401: 인증 | 403: 타인 메시지 또는 5분 초과 | 404: 메시지 없음 | 409: 이미 삭제
     */
    @PATCH("api/v1/messages/{messageId}")
    suspend fun editMessage(
        @Path("messageId") messageId : Long,
        @Body request                : EditMessageRequest,
    ): Response<Message>

    /**
     * GET /api/v1/messages/chatrooms/{chatRoomId} — 채팅방 메시지 동기화
     * since 이후 새 메시지, 수정된 메시지, 삭제된 메시지 ID 반환
     * since 없으면 전체 메시지 반환 (채팅방 최초 진입 시)
     * 채팅방 접속할 때 한 번 호출 → 이후 실시간은 Firebase Realtime DB로 갱신
     * 200: { newMessages[], updatedMessages[], deletedIds[] }
     * 401: 인증 | 403: 채팅방 접근 권한 없음 | 404: 채팅방 없음
     */
    @GET("api/v1/messages/chatrooms/{chatRoomId}")
    suspend fun syncMessages(
        @Path("chatRoomId") chatRoomId : Long,
        @Query("since")     since      : String? = null,  // ISO 8601 (예: "2026-04-07T10:00:00")
    ): Response<SyncMessagesResponse>
}
