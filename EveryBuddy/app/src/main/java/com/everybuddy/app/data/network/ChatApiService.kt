package com.everybuddy.app.data.network

import com.everybuddy.app.data.dto.ChatRoom
import com.everybuddy.app.data.dto.CreateChatRoomRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * ChatApiService
 *
 * 채팅 관련 Retrofit API 인터페이스.
 * NetworkModule의 단일 Retrofit 인스턴스에 등록.
 * JWT Bearer 토큰은 OkHttpClient 인터셉터에서 자동 첨부.
 *
 * [변경] 스웨거 문서 확정 기준으로 엔드포인트 한국어 → 영어 수정:
 *   "/api/v1/채팅방" → "/api/v1/chatrooms"
 */
interface ChatApiService {

    // GET /api/v1/chatrooms — 내 채팅방 목록 조회
    @GET("/api/v1/chatrooms")
    suspend fun getChatRooms(): Response<List<ChatRoom>>

    // POST /api/v1/chatrooms — 채팅방 생성
    @POST("/api/v1/chatrooms")
    suspend fun createChatRoom(
        @Body request: CreateChatRoomRequest,
    ): Response<ChatRoom>

    // POST /api/v1/messages — 메시지 전송 (멀티파트)
    @Multipart
    @POST("/api/v1/messages")
    suspend fun sendMessage(
        @Part("request") request : RequestBody,
        @Part           file     : MultipartBody.Part? = null,
    ): Response<Unit>

    // PUT /api/v1/messages/{messageId}/read — 메시지 읽음 처리
    @PUT("/api/v1/messages/{messageId}/read")
    suspend fun markMessageRead(
        @Path("messageId") messageId: Long,
    ): Response<Unit>

    // DELETE /api/v1/messages/{messageId} — 메시지 삭제
    @DELETE("/api/v1/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("messageId") messageId: Long,
    ): Response<Unit>

    // TODO: 추후 서버 구현 후 추가
    //   1. 메시지 목록 조회 (페이지네이션)
    //   2. STT 변환
    //   3. 텍스트 번역
    //   4. 유저 프로필 조회
    //   5. 채팅방 나가기
}
