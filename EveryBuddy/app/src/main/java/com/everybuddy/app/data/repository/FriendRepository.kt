package com.everybuddy.app.data.repository

import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.FriendListResponse
import com.everybuddy.app.data.network.ApiService
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

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
