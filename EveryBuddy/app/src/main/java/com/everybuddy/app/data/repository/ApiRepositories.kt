package com.everybuddy.app.data.repository

// =============================================================================
// ApiRepositories.kt
// 스웨거 명세의 에러 코드를 모두 처리하는 Repository 구현체
//
// 에러 처리 전략:
//   ApiResult.Success  → 정상 응답
//   ApiResult.Error    → 서버 에러 (code/name/message)
//   ApiResult.NetworkError → 네트워크 예외
// =============================================================================

import com.everybuddy.app.data.dto.*
import com.everybuddy.app.data.network.ApiService
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// 공통 에러 파싱 헬퍼
// ─────────────────────────────────────────────────────────────────────────────
private fun <T> Response<T>.toApiResult(
    gson: Gson,
    onSuccess: (T?) -> ApiResult<T>,
): ApiResult<T> {
    return if (isSuccessful) {
        onSuccess(body())
    } else {
        try {
            val err = gson.fromJson(errorBody()?.string(), ApiErrorResponse::class.java)
            ApiResult.Error(err.code, err.name, err.message)
        } catch (e: Exception) {
            ApiResult.Error(code(), "PARSE_ERROR", "응답 파싱 오류")
        }
    }
}

private suspend fun <T> safeApiCall(
    gson    : Gson,
    block   : suspend () -> Response<T>,
    onSuccess: (T?) -> ApiResult<T> = { ApiResult.Success(it!!) },
): ApiResult<T> = try {
    block().toApiResult(gson, onSuccess)
} catch (e: Exception) {
    ApiResult.NetworkError(e)
}

// ─────────────────────────────────────────────────────────────────────────────
// FriendRepository
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class FriendRepository @Inject constructor(
    private val api  : ApiService,
    private val gson : Gson,
) {
    /**
     * 친구 추가 — POST /api/v1/friends/{toUserId}
     * 에러: 400(자기자신) | 404(유저없음) | 409(이미친구) | 410(탈퇴)
     */
    suspend fun addFriend(toUserId: Long): ApiResult<Unit> =
        safeApiCall(gson, { api.addFriend(toUserId) }) { ApiResult.Success(Unit) }

    /**
     * 친구 삭제 — DELETE /api/v1/friends/{toUserId}
     */
    suspend fun removeFriend(toUserId: Long): ApiResult<Unit> =
        safeApiCall(gson, { api.removeFriend(toUserId) }) { ApiResult.Success(Unit) }

    /**
     * 친구 목록 조회 — GET /api/v1/friends
     * 에러: 401(인증) | 404(유저없음)
     */
    suspend fun getFriends(): ApiResult<FriendListResponse> =
        safeApiCall(gson, { api.getFriends() })
}

// ─────────────────────────────────────────────────────────────────────────────
// BlockRepository
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class BlockRepository @Inject constructor(
    private val api  : ApiService,
    private val gson : Gson,
) {
    /**
     * 차단 — POST /api/v1/blocks/{userId}
     * 에러: 400(자기자신) | 404(유저없음) | 409(이미차단) | 410(탈퇴)
     */
    suspend fun blockUser(userId: Long): ApiResult<Unit> =
        safeApiCall(gson, { api.blockUser(userId) }) { ApiResult.Success(Unit) }

    /**
     * 차단 해제 — DELETE /api/v1/blocks/{userId}
     * 에러: 401(인증) | 404(차단관계없음)
     */
    suspend fun unblockUser(userId: Long): ApiResult<Unit> =
        safeApiCall(gson, { api.unblockUser(userId) }) { ApiResult.Success(Unit) }
}

// ─────────────────────────────────────────────────────────────────────────────
// DiscoverRepository
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class DiscoverRepository @Inject constructor(
    private val api  : ApiService,
    private val gson : Gson,
) {
    /**
     * 랜덤 탐색 — GET /api/v1/discover/random
     * 친구·차단 제외 랜덤 6명 반환
     */
    suspend fun discoverRandom(): ApiResult<DiscoverResponse> =
        safeApiCall(gson, { api.discoverRandom() })

    /**
     * 필터 탐색 — GET /api/v1/discover/filter (커서 기반)
     * @param gender         "MALE" | "FEMALE" | null
     * @param country        국가 코드 (null = 전체)
     * @param minAge         생년월일 기반 최소 나이 (epoch millis로 변환)
     * @param maxAge         생년월일 기반 최대 나이
     * @param languages      ["ENGLISH", "JAPANESE", ...] | null
     * @param tags           ["SPORTS", "TRAVEL", ...] | null
     * @param isOnline       현재 온라인만 | null
     * @param recentlyActive 24시간 이내 접속 | null
     * @param lastUserId     커서 (이전 응답의 nextCursor) | null
     * @param size           페이지 크기 (기본 20)
     */
    suspend fun discoverFilter(
        gender         : String?       = null,
        country        : String?       = null,
        minAge         : Long?         = null,
        maxAge         : Long?         = null,
        languages      : List<String>? = null,
        tags           : List<String>? = null,
        isOnline       : Boolean?      = null,
        recentlyActive : Boolean?      = null,
        lastUserId     : Long?         = null,
        size           : Int           = 20,
    ): ApiResult<DiscoverFilterResponse> = safeApiCall(gson, {
        api.discoverFilter(
            gender         = gender,
            country        = country,
            minAge         = minAge,
            maxAge         = maxAge,
            languages      = languages,
            tags           = tags,
            isOnline       = isOnline,
            recentlyActive = recentlyActive,
            lastUserId     = lastUserId,
            size           = size,
        )
    })
}

// ─────────────────────────────────────────────────────────────────────────────
// MessageRepository
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class MessageRepository @Inject constructor(
    private val api  : ApiService,
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
        val filePart = MultipartBody.Part.createFormData(name = "file", filename = file.name, body = file.asRequestBody(mimeType.toMediaType()))
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
    ): ApiResult<MessageDto> =
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

