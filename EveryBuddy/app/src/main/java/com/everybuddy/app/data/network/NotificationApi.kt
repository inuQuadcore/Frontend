package com.everybuddy.app.data.network

import com.everybuddy.app.data.dto.HasUnreadResponse
import com.everybuddy.app.data.dto.NotificationListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {

    /**
     * GET /api/v1/notifications?before&limit — 알림 리스트 (커서 페이지네이션, 최신순)
     * before: 이전 페이지 nextCursor. 첫 페이지는 생략. limit default 10.
     */
    @GET("api/v1/notifications")
    suspend fun list(
        @Query("before") before: Long? = null,
        @Query("limit")  limit : Int?  = null,
    ): Response<NotificationListResponse>

    /** GET /api/v1/notifications/has-unread — 미읽 존재 여부 */
    @GET("api/v1/notifications/has-unread")
    suspend fun hasUnread(): Response<HasUnreadResponse>

    /**
     * PATCH /api/v1/notifications/{notificationId}/read — 단건 읽음 처리 (멱등)
     * 204: 성공 | 403: NOT_NOTIFICATION_OF_USER | 404: NOTIFICATION_NOT_FOUND
     */
    @PATCH("api/v1/notifications/{notificationId}/read")
    suspend fun markRead(
        @Path("notificationId") notificationId: Long,
    ): Response<Unit>

    /** PATCH /api/v1/notifications/read-all — 전체 읽음 처리 (멱등) */
    @PATCH("api/v1/notifications/read-all")
    suspend fun markAllRead(): Response<Unit>
}
