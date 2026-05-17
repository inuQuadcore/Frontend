package com.everybuddy.app.data.network

import com.everybuddy.app.data.dto.FcmTokenRegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface FcmTokenApi {

    /**
     * POST /api/v1/fcm-tokens — FCM 디바이스 토큰 등록 (upsert)
     * 200: 성공 | 400: MISSING_PARAMETER | 404: USER_NOT_FOUND
     */
    @POST("api/v1/fcm-tokens")
    suspend fun register(
        @Body body: FcmTokenRegisterRequest,
    ): Response<Unit>

    /**
     * DELETE /api/v1/fcm-tokens — 본인 FCM 토큰 삭제 (멱등)
     * 204: 성공 | 404: USER_NOT_FOUND
     */
    @DELETE("api/v1/fcm-tokens")
    suspend fun delete(): Response<Unit>
}
