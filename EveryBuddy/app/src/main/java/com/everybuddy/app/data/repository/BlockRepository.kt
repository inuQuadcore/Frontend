package com.everybuddy.app.data.repository

import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.network.ApiService
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

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
