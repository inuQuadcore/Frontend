package com.everybuddy.app.data.firebase

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RTDB `presence/` 노드 전체 구독해 온라인 사용자 ID 집합 노출.
 *
 * - Custom Token 인증 후 start() 호출 → ChildEventListener attach
 * - logout 시 stop() → detach + set 초기화
 * - 본인 노드 write는 [PresenceManager] 담당 (책임 분리)
 *
 * 활용처: Friend / Discover / UserProfile / Chat 사이드바 ViewModel에서
 * `onlineIds`를 combine해 각 사용자 isOnline 표시.
 */
@Singleton
class PresenceRepository @Inject constructor() {

    private val rtdb = FirebaseDatabase.getInstance()
    private var listener: ChildEventListener? = null
    private var ref: DatabaseReference? = null

    private val _onlineIds = MutableStateFlow<Set<Long>>(emptySet())
    val onlineIds: StateFlow<Set<Long>> = _onlineIds.asStateFlow()

    fun start() {
        if (listener != null) return
        val node = rtdb.getReference("presence")
        val l = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.key?.toLongOrNull()?.let { uid ->
                    _onlineIds.value = _onlineIds.value + uid
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                snapshot.key?.toLongOrNull()?.let { uid ->
                    _onlineIds.value = _onlineIds.value - uid
                }
            }

            // presence 노드는 boolean 값만 — 변경/이동은 무시.
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "presence listener cancelled", error.toException())
            }
        }
        node.addChildEventListener(l)
        listener = l
        ref = node
    }

    fun stop() {
        val l = listener ?: return
        ref?.removeEventListener(l)
        listener = null
        ref = null
        _onlineIds.value = emptySet()
    }

    companion object {
        private const val TAG = "PresenceRepository"
    }
}
