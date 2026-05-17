package com.everybuddy.app.data.firebase

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FCM 친구추가 푸시 수신 이벤트 버스.
 * MessagingService가 emit, HasUnreadViewModel이 collect → has-unread 재요청.
 * (채팅 푸시는 알림함과 무관해서 emit 대상 아님.)
 */
@Singleton
class NotificationEvents @Inject constructor() {
    private val _friendNotificationReceived = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val friendNotificationReceived: SharedFlow<Unit> = _friendNotificationReceived.asSharedFlow()

    fun emitFriendNotification() {
        _friendNotificationReceived.tryEmit(Unit)
    }
}
