package com.everybuddy.app.data.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * FCM 트레이 알림 채널 정의.
 * 채널 ID는 백엔드 푸시 페이로드의 `notification.android.notification.channel_id`와 일치해야 함.
 *
 * - FRIEND: 친구추가 알림 (manifest의 default_notification_channel_id로 지정)
 * - CHAT: 채팅 메시지 알림 (백엔드가 채팅 푸시에 channel_id 명시 필요)
 */
object NotificationChannels {

    const val FRIEND = "everybuddy_friend"
    const val CHAT   = "everybuddy_chat"

    fun register(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(FRIEND, "친구 알림", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHAT, "채팅 알림", NotificationManager.IMPORTANCE_HIGH)
        )
    }
}
