package com.everybuddy.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.everybuddy.app.data.firebase.FcmTokenManager
import com.everybuddy.app.navigation.AppNavGraph
import com.everybuddy.app.navigation.Route
import com.everybuddy.app.ui.main.PendingDeepLink
import com.everybuddy.app.ui.theme.EveryBuddyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var fcmTokenManager: FcmTokenManager

    // 권한 허용 직후 FcmTokenManager.register() 재호출 — 명세 권장.
    // 거부 시 무시 (별도 안내 없음).
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            lifecycleScope.launch { fcmTokenManager.register() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        handlePushIntent(intent)
        setContent {
            EveryBuddyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(
                        startDest = Route.SPLASH,
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 웜 스타트 (앱 이미 떠있을 때 푸시 클릭) — 새 intent로 라우팅 갱신
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    // FCM 트레이 알림 클릭으로 들어온 Intent의 data payload를 PendingDeepLink로 전달.
    // MainScreen이 LaunchedEffect로 consume.
    private fun handlePushIntent(intent: Intent?) {
        intent ?: return
        intent.getStringExtra("chatRoomId")?.let { PendingDeepLink.chatRoomId = it }
        intent.getStringExtra("fromUserId")?.toLongOrNull()?.let { PendingDeepLink.friendUserId = it }
    }
}