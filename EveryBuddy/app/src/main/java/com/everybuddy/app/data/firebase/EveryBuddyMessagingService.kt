package com.everybuddy.app.data.firebase

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.everybuddy.app.MainActivity
import com.everybuddy.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FCM 디바이스 토큰 갱신과 foreground 푸시 수신을 처리.
 *
 * - onNewToken: 토큰이 갱신될 때마다 백엔드에 재등록
 * - onMessageReceived: 앱이 foreground인 경우에만 호출 (background/killed는 OS가 트레이 알림 자동 표시)
 *   백엔드는 notification + data 페이로드를 같이 보내므로 여기선 NotificationCompat으로 트레이에 직접 띄움
 */
@AndroidEntryPoint
class EveryBuddyMessagingService : FirebaseMessagingService() {

    @Inject lateinit var fcmTokenManager: FcmTokenManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { fcmTokenManager.register() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: return
        val body  = message.notification?.body.orEmpty()
        showNotification(title, body, message.data)
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data.forEach { (k, v) -> putExtra(k, v) }
        }
        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // data 페이로드로 채널 분기: chatRoomId 있으면 채팅, 없으면 친구추가 (default)
        val channelId =
            if (data.containsKey("chatRoomId")) NotificationChannels.CHAT
            else                                NotificationChannels.FRIEND
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_alarm)
            .setColor(getColor(R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(this).notify(notifId, notification)
    }
}
