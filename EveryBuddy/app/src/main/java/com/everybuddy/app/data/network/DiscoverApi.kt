package com.everybuddy.app.data.network

import com.everybuddy.app.data.dto.DiscoverFilterResponse
import com.everybuddy.app.data.dto.DiscoverResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DiscoverApi {

    /**
     * GET /api/v1/discover/random — 랜덤 유저 탐색
     * 친구·차단 관계를 제외한 랜덤 유저 6명 반환
     * 200: { users: [{userId, name, profileImageUrl, country, bio, languages[], tags[], lastSeenAt}] }
     * 401: 인증 필요
     */
    @GET("api/v1/discover/random")
    suspend fun discoverRandom(): Response<DiscoverResponse>

    /**
     * GET /api/v1/discover/filter — 필터 유저 탐색 (커서 기반 페이징)
     * 조건: isOnline(현재 온라인), recentlyActive(24시간 이내), 두 조건 동시 적용 가능
     * 200: { users[], hasNext, nextCursor }
     * 400: 잘못된 파라미터 | 401: 인증 필요
     */
    @GET("api/v1/discover/filter")
    suspend fun discoverFilter(
        @Query("gender")         gender         : String?  = null,
        @Query("country")        country        : String?  = null,
        @Query("minAge")         minAge         : Long?    = null,
        @Query("maxAge")         maxAge         : Long?    = null,
        @Query("languages")      languages      : List<String>? = null,
        @Query("tags")           tags           : List<String>? = null,
        @Query("isOnline")       isOnline       : Boolean? = null,
        @Query("recentlyActive") recentlyActive : Boolean? = null,
        @Query("lastUserId")     lastUserId     : Long?    = null,   // 커서
        @Query("size")           size           : Int      = 20,
    ): Response<DiscoverFilterResponse>
}
