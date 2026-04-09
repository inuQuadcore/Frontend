package com.everybuddy.app.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * ChatApiService
 *
 * 채팅 관련 Retrofit API 인터페이스.
 * AuthApiService와 동일한 Retrofit 인스턴스에 등록되어야 함.
 * (JWT Bearer 토큰 인터셉터가 OkHttpClient에 설정되어 있어야 함)
 */
interface ChatApiService {
    // 메시지 API
    /**
     * 메시지 전송 — POST /api/v1/메시지
     *
     * 멀티파트 요청:
     * - request: SendMessageRequest JSON 파트 (required)
     * - file   : 첨부 파일 파트 (선택, 최대 10MB)
     *
     * @param request  JSON 파트 — chatRoomId + content
     * @param file     첨부 파일 (null = 텍스트 메시지)
     * @return 204 No Content
     */
    @Multipart
    @POST("/api/v1/메시지")
    suspend fun sendMessage(
        @Part("request") request : RequestBody,
        @Part           file     : MultipartBody.Part? = null,
    ): Response<Unit>

    /**
     * 메시지 읽음 처리 — PUT /api/v1/messages/{messageId}/read
     *
     * messageId 이하의 모든 메시지를 읽음 처리.
     * @param messageId 읽어주세요 처리할 마지막 메시지 ID
     * @return 204 No Content
     */
    @PUT("/api/v1/messages/{messageId}/read")
    suspend fun markMessageRead(
        @Path("messageId") messageId: Long,
    ): Response<Unit>

    /**
     * 메시지 삭제 — DELETE /api/v1/messages/{messageId}
     *
     * 자신이 송신한 메시지만 삭제 가능.
     * @param messageId 삭제할 메시지 ID
     * @return 204 No Content
     */
    @DELETE("/api/v1/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("messageId") messageId: Long,
    ): Response<Unit>

    // 채팅방 API
    /**
     * 내 채팅방 목록 조회 — GET /api/v1/채팅방
     *
     * 내가 참여하는 모든 미팅방 목록 반환.
     * @return List<ChatRoomResponse>
     */
    @GET("/api/v1/채팅방")
    suspend fun getChatRooms(): Response<List<ChatRoomResponse>>

    /**
     * 채팅방 생성 — POST /api/v1/채팅방
     *
     * 새로운 미팅방을 생성하고 참여자를 초대.
     * @param request roomName + participantIds
     * @return ChatRoomResponse (생성된 채팅방 정보)
     */
    @POST("/api/v1/채팅방")
    suspend fun createChatRoom(
        @Body request: CreateChatRoomRequest,
    ): Response<ChatRoomResponse>

    // TODO: 추후 서버 구현 후 추가 예정
    /*
     * 1. 채팅방 메시지 목록 조회 (페이지네이션)
     *
     * 2. STT 변환
     *
     * 3. 텍스트 번역
     *
     * 4. 유저 프로필 조회
     *
     * 5. 채팅방 나가기
     */
}
