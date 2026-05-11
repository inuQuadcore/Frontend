package com.everybuddy.app.data.repository

import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.DiscoverFilterResponse
import com.everybuddy.app.data.dto.DiscoverResponse
import com.everybuddy.app.data.network.ApiService
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

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
