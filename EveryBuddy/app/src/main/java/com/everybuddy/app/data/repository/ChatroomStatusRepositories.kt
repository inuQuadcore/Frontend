package com.everybuddy.app.data.repository

// =============================================================================
// ChatroomStatusRepositories.kt
// 채팅방 API + 상태메시지 API Repository
// =============================================================================

import com.everybuddy.app.data.dto.*
import com.everybuddy.app.data.network.ApiService
import com.everybuddy.app.data.network.CreateChatRoomRequest
import com.google.gson.Gson
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

private fun <T> Response<T>.toApiResult(gson: Gson, onSuccess: (T?) -> ApiResult<T>): ApiResult<T> =
    if (isSuccessful) onSuccess(body())
    else try {
        val err = gson.fromJson(errorBody()?.string(), ApiErrorResponse::class.java)
        ApiResult.Error(err.code, err.name, err.message)
    } catch (e: Exception) {
        ApiResult.Error(code(), "PARSE_ERROR", "응답 파싱 오류")
    }

private suspend fun <T> safeApiCall(
    gson      : Gson,
    block     : suspend () -> Response<T>,
    onSuccess : (T?) -> ApiResult<T> = { ApiResult.Success(it!!) },
): ApiResult<T> = try {
    block().toApiResult(gson, onSuccess)
} catch (e: Exception) {
    ApiResult.NetworkError(e)
}

// ─────────────────────────────────────────────────────────────────────────────
// ChatRoomRepository
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class ChatRoomRepository @Inject constructor(
    private val api  : ApiService,
    private val gson : Gson,
) {
    /**
     * 내 채팅방 목록 조회 — GET /api/v1/chatrooms
     * 에러: 401(인증)
     */
    suspend fun getChatRooms(): ApiResult<List<ChatRoomDto>> =
        safeApiCall(gson, { api.getChatRooms() })

    /**
     * 채팅방 생성 — POST /api/v1/chatrooms
     * 에러: 400(errors.roomName / errors.participantIds) | 401 | 404(유저없음)
     */
    suspend fun createChatRoom(
        roomName       : String,
        participantIds : List<Long>,
    ): ApiResult<ChatRoomDto> = safeApiCall(gson, {
        api.createChatRoom(CreateChatRoomRequest(roomName, participantIds))
    })
}

// ─────────────────────────────────────────────────────────────────────────────
// StatusMessageRepository
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class StatusMessageRepository @Inject constructor(
    private val api  : ApiService,
    private val gson : Gson,
) {
    /**
     * 상태메시지 작성 — POST /api/v1/status-messages
     * 1인 1개 제한 → 이미 있으면 409 STATUS_MESSAGE_ALREADY_EXISTS
     * 에러: 401(인증) | 409(이미존재)
     */
    suspend fun postStatusMessage(content: String): ApiResult<Unit> =
        safeApiCall(gson, { api.postStatusMessage(StatusMessageRequest(content)) }) {
            ApiResult.Success(Unit)
        }

    /**
     * 상태메시지 수정 — PATCH /api/v1/status-messages
     * 24시간 이내만 수정 가능 → 만료 시 400 STATUS_MESSAGE_EXPIRED
     * 에러: 400(STATUS_MESSAGE_EXPIRED) | 401(인증) | 404(없음)
     */
    suspend fun updateStatusMessage(content: String): ApiResult<Unit> =
        safeApiCall(gson, { api.updateStatusMessage(StatusMessageRequest(content)) }) {
            ApiResult.Success(Unit)
        }

    /**
     * 상태메시지 삭제 — DELETE /api/v1/status-messages
     * 에러: 401(인증) | 404(없음)
     */
    suspend fun deleteStatusMessage(): ApiResult<Unit> =
        safeApiCall(gson, { api.deleteStatusMessage() }) { ApiResult.Success(Unit) }

    /**
     * 내 상태메시지 조회 — GET /api/v1/status-messages/me
     * 없으면 404 → null 처리
     * 에러: 401(인증) | 404(없음)
     */
    suspend fun getMyStatusMessage(): ApiResult<MyStatusMessageResponse> =
        safeApiCall(gson, { api.getMyStatusMessage() })

    /**
     * 친구 상태메시지 목록 — GET /api/v1/status-messages/friends
     * 커서 기반 무한스크롤. 본인 미포함 → getMyStatusMessage() 별도 호출 후 병합
     * 에러: 401(인증) | 404(커서에 해당하는 메시지 없음)
     */
    suspend fun getFriendStatusMessages(
        cursor : Long? = null,
        size   : Int   = 20,
    ): ApiResult<FriendStatusMessagesResponse> =
        safeApiCall(gson, { api.getFriendStatusMessages(cursor, size) })
}
