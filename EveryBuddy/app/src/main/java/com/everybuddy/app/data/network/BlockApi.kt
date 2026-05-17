package com.everybuddy.app.data.network

import com.everybuddy.app.data.dto.BlockedUsersResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BlockApi {

    /**
     * GET /api/v1/blocks — 내가 차단한 사용자 목록
     * 200: { blockedUsers: [...] } | 401: 인증 필요
     */
    @GET("api/v1/blocks")
    suspend fun getBlockedUsers(): Response<BlockedUsersResponse>

    /**
     * POST /api/v1/blocks/{userId} — 사용자 차단
     * 200: 성공 | 400: 자기 자신 차단 | 401: 인증 필요
     * 404: 유저 없음 | 409: 이미 차단 | 410: 탈퇴한 유저
     */
    @POST("api/v1/blocks/{userId}")
    suspend fun blockUser(
        @Path("userId") userId: Long,
    ): Response<Unit>

    /**
     * DELETE /api/v1/blocks/{userId} — 차단 해제
     * 204: 성공 | 401: 인증 필요 | 404: 차단 관계 없음
     */
    @DELETE("api/v1/blocks/{userId}")
    suspend fun unblockUser(
        @Path("userId") userId: Long,
    ): Response<Unit>
}
