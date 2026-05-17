package com.everybuddy.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.everybuddy.app.data.firebase.PresenceManager
import com.everybuddy.app.data.repository.AuthRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EveryBuddyApp : Application() {

    @Inject lateinit var presenceManager: PresenceManager
    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate() {
        super.onCreate()
        // 콜드스타트 시 Firebase 세션 복구 + presence/userChatRooms listener 재부착.
        // 로그인 이벤트(login/google) 외에는 signInToFirebase가 안 불려서 listener도 끊겨 있는 상태.
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            authRepository.ensureFirebaseSession()
        }
        // 앱이 포그라운드로 진입할 때마다 presence 재등록.
        // Firebase 재연결 후 onDisconnect 등록이 초기화되는 케이스를 위한 safety net.
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    presenceManager.onForeground()
                }
            }
        )
    }
}
