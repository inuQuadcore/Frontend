package com.everybuddy.app.data.firebase

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RTDB `presence/{userId}` 노드를 통한 접속 여부 관리. docs/api/rtdb.md 명세 기반.
 *
 *  - login 성공 → start(userId): write + onDisconnect 등록 + .info/connected 구독
 *  - 포그라운드 진입 → onForeground(): write + onDisconnect 재등록
 *  - Firebase 재연결 → .info/connected 리스너가 자동으로 write + onDisconnect 재등록
 *  - logout → stop(): remove + 구독 해제
 *  - 백그라운드/종료 → 명시 호출 X. onDisconnect가 TCP 끊김 시 자동 처리.
 *
 * 주의: stop()은 Firebase signOut 호출 전에 불러야 RTDB write 권한이 살아있음.
 */
@Singleton
class PresenceManager @Inject constructor() {

    private val rtdb = FirebaseDatabase.getInstance()
    private var userId: Long? = null
    private var connectedListener: ValueEventListener? = null

    fun start(userId: Long) {
        this.userId = userId
        attachConnectedListener()
        writeOnline()
    }

    fun stop() {
        detachConnectedListener()
        writeOffline()
        userId = null
    }

    fun onForeground() {
        if (userId != null) writeOnline()
    }

    private fun attachConnectedListener() {
        if (connectedListener != null) return
        val ref = rtdb.getReference(".info/connected")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) == true
                if (connected) writeOnline()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, ".info/connected listener cancelled", error.toException())
            }
        }
        ref.addValueEventListener(listener)
        connectedListener = listener
    }

    private fun detachConnectedListener() {
        val listener = connectedListener ?: return
        rtdb.getReference(".info/connected").removeEventListener(listener)
        connectedListener = null
    }

    private fun writeOnline() {
        val uid = userId ?: return
        val ref = rtdb.getReference("presence/$uid")
        ref.onDisconnect().removeValue()
        ref.setValue(true)
    }

    private fun writeOffline() {
        val uid = userId ?: return
        rtdb.getReference("presence/$uid").removeValue()
    }

    companion object {
        private const val TAG = "PresenceManager"
    }
}
