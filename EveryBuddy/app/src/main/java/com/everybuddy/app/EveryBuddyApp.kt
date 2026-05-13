package com.everybuddy.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.everybuddy.app.data.firebase.PresenceManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EveryBuddyApp : Application() {

    @Inject lateinit var presenceManager: PresenceManager

    override fun onCreate() {
        super.onCreate()
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
