package com.everybuddy.app.data.repository

import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.HasUnreadResponse
import com.everybuddy.app.data.dto.NotificationListResponse
import com.everybuddy.app.data.network.NotificationApi
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val api  : NotificationApi,
    private val gson : Gson,
) {
    suspend fun list(before: Long? = null, limit: Int? = null): ApiResult<NotificationListResponse> =
        safeApiCall(gson, { api.list(before, limit) })

    suspend fun hasUnread(): ApiResult<HasUnreadResponse> =
        safeApiCall(gson, { api.hasUnread() })

    suspend fun markRead(notificationId: Long): ApiResult<Unit> =
        safeApiCall(gson, { api.markRead(notificationId) }) { ApiResult.Success(Unit) }

    suspend fun markAllRead(): ApiResult<Unit> =
        safeApiCall(gson, { api.markAllRead() }) { ApiResult.Success(Unit) }
}
