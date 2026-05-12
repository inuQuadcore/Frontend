package com.everybuddy.app.data.network

import com.everybuddy.app.data.dto.FriendListResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FriendApi {

    /**
     * POST /api/v1/friends/{toUserId} — 친구 추가
     * 200: 성공 | 400: 자기 자신 추가 | 401: 인증 필요
     * 404: 유저 없음 | 409: 이미 친구 | 410: 탈퇴한 유저
     */
    @POST("api/v1/friends/{toUserId}")
    suspend fun addFriend(
        @Path("toUserId") toUserId: Long,
    ): Response<Unit>

    /**
     * DELETE /api/v1/friends/{toUserId} — 친구 삭제
     * TODO: 스웨거 확인 후 추가
     */
    @DELETE("api/v1/friends/{toUserId}")
    suspend fun removeFriend(
        @Path("toUserId") toUserId: Long,
    ): Response<Unit>

    /**
     * GET /api/v1/friends — 내 친구 전체 목록 조회
     * 200: { friends: [ {userId, name, profileImageUrl, country, bio, languages[], tags[]} ] }
     * 401: 인증 필요 | 404: 유저 없음
     */
    @GET("api/v1/friends")
    suspend fun getFriends(): Response<FriendListResponse>
}
