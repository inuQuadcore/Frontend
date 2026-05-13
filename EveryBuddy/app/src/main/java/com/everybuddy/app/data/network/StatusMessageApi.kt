package com.everybuddy.app.data.network

import com.everybuddy.app.data.dto.FriendStatusMessagesResponse
import com.everybuddy.app.data.dto.MyStatusMessageResponse
import com.everybuddy.app.data.dto.StatusMessageRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface StatusMessageApi {

    /**
     * POST /api/v1/status-messages — 상태메시지 작성
     * 1인당 하나만 작성 가능 (이미 있으면 409 STATUS_MESSAGE_ALREADY_EXISTS)
     * Request: { "content": "오늘 날씨 너무 좋다!" }
     * 200: 성공 | 401: 인증 | 409: 이미 상태메시지 존재
     */
    @POST("api/v1/status-messages")
    suspend fun postStatusMessage(
        @Body request: StatusMessageRequest,
    ): Response<Unit>

    /**
     * PATCH /api/v1/status-messages — 상태메시지 수정
     * 24시간 이내에만 수정 가능 (만료 시 400 STATUS_MESSAGE_EXPIRED)
     * Request: { "content": "오늘 날씨 너무 좋다!" }
     * 200: 성공
     * 400: STATUS_MESSAGE_EXPIRED (24시간 만료)
     * 401: 인증 | 404: STATUS_MESSAGE_NOT_FOUND
     */
    @PATCH("api/v1/status-messages")
    suspend fun updateStatusMessage(
        @Body request: StatusMessageRequest,
    ): Response<Unit>

    /**
     * DELETE /api/v1/status-messages — 상태메시지 삭제
     * 204: 성공 | 401: 인증 | 404: STATUS_MESSAGE_NOT_FOUND
     */
    @DELETE("api/v1/status-messages")
    suspend fun deleteStatusMessage(): Response<Unit>

    /**
     * GET /api/v1/status-messages/me — 내 상태메시지 조회
     * 200: { statusMessageId, content, timeAgo }
     * 401: 인증 | 404: STATUS_MESSAGE_NOT_FOUND
     */
    @GET("api/v1/status-messages/me")
    suspend fun getMyStatusMessage(): Response<MyStatusMessageResponse>

    /**
     * GET /api/v1/status-messages/friends — 친구 상태메시지 목록 조회
     * 최신순, 커서 기반 무한스크롤
     * 본인 상태메시지 미포함 (별도로 GET /me 호출 후 목록 첫 번째에 추가)
     * 200: { statusMessages[], nextCursor, hasNext }
     * 401: 인증 | 404: STATUS_MESSAGE_NOT_FOUND (커서에 해당하는 메시지 없음)
     */
    @GET("api/v1/status-messages/friends")
    suspend fun getFriendStatusMessages(
        @Query("cursor") cursor : Long? = null,  // 이전 페이지 마지막 statusMessageId
        @Query("size")   size   : Int   = 20,
    ): Response<FriendStatusMessagesResponse>
}
