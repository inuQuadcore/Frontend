package com.everybuddy.app.data.firebase

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RTDB `viewing/{userId}` 노드 관리. 본인이 현재 보고 있는 채팅방 ID 기록.
 * 서버는 FCM 푸시 발송 시 이 값이 해당 chatRoomId와 같으면 푸시 제외 (불필요 알림 방지).
 *
 *  - 채팅방 진입 → [enter]: set(chatRoomId) + onDisconnect.remove 등록
 *  - 채팅방 이탈 → [leave]: currentRoomId가 같을 때만 remove (방 이동 race 방지)
 *  - 로그아웃 → [stop]: 강제 remove + 상태 초기화
 *  - 백그라운드/종료 → onDisconnect가 자동 처리, 명시 호출 불필요
 *
 * 본인 노드 write 책임만. 다른 사용자의 viewing은 서버만 본다.
 */
@Singleton
class ViewingManager @Inject constructor() {

    private val rtdb = FirebaseDatabase.getInstance()
    private var userId        : Long? = null
    private var currentRoomId : Long? = null

    fun enter(userId: Long, chatRoomId: Long) {
        this.userId        = userId
        this.currentRoomId = chatRoomId
        val ref = rtdb.getReference("viewing/$userId")
        ref.onDisconnect().removeValue()
        ref.setValue(chatRoomId)
            .addOnFailureListener { e -> Log.w(TAG, "viewing set failed", e) }
    }

    /**
     * 채팅방 이탈. 현재 보관된 방과 일치할 때만 remove.
     * 빠른 방 이동(A→B) 시 B의 enter가 먼저 도착 + A의 leave가 늦게 도착해도
     * currentRoomId가 B로 갱신돼 있어 무시 — viewing이 잘못 비워지지 않음.
     */
    fun leave(chatRoomId: Long) {
        if (currentRoomId != chatRoomId) return
        currentRoomId = null
        val uid = userId ?: return
        rtdb.getReference("viewing/$uid").removeValue()
    }

    fun stop() {
        currentRoomId = null
        val uid = userId
        userId = null
        if (uid != null) {
            rtdb.getReference("viewing/$uid").removeValue()
        }
    }

    companion object {
        private const val TAG = "ViewingManager"
    }
}
