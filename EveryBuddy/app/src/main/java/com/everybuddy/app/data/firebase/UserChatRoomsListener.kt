package com.everybuddy.app.data.firebase

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

/**
 * 본인 채팅방 목록 메타데이터 실시간 구독.
 * RTDB `users/{userId}/chatrooms/` 노드의 각 채팅방 메타(lastMessage/lastMessageTime/unreadCount 등) 변경 사항을
 * 콜백으로 전달.
 *
 * 초기값(unreadCount/lastMessage/lastMessageTime)은 GET /chatrooms 응답이 source of truth.
 * RTDB는 그 이후 변경분만 반영하기 위해 사용. attach 시점에 발사되는 onChildAdded(기존 방)는
 * REST가 이미 채운 ID이므로 무시. 진짜 신규 방(초대/그룹방 추가 등)만 onNewRoom으로 알림.
 *
 * - 메시지 송수신/read 처리 시 서버 multi-path update → onChildChanged → onMetaChange.
 * - 본인이 채팅방 나가기/외부 강퇴 → onChildRemoved → onRoomRemoved.
 * - 진짜 신규 방(attach 시점 이후 추가된 ID) → onChildAdded → onNewRoom.
 */
class UserChatRoomsListener(
    private val userId        : Long,
    private val onMetaChange  : (chatRoomId: Long, meta: RoomMeta) -> Unit,
    private val onNewRoom     : (chatRoomId: Long) -> Unit,
    private val onRoomRemoved : (chatRoomId: Long) -> Unit,
) {

    private val rtdb = FirebaseDatabase.getInstance()
    private var ref          : DatabaseReference?  = null
    private var listener     : ChildEventListener? = null
    private var knownRoomIds : Set<Long> = emptySet()

    fun attach(initialRoomIds: Set<Long>) {
        if (listener != null) return
        knownRoomIds = initialRoomIds
        val node = rtdb.getReference("users/$userId/chatrooms")
        val l = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key?.toLongOrNull() ?: return
                if (id in knownRoomIds) return
                onNewRoom(id)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                emit(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                snapshot.key?.toLongOrNull()?.let(onRoomRemoved)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "users/$userId/chatrooms listener cancelled", error.toException())
            }
        }
        node.addChildEventListener(l)
        ref      = node
        listener = l
    }

    fun detach() {
        val l = listener ?: return
        ref?.removeEventListener(l)
        ref      = null
        listener = null
    }

    private fun emit(snapshot: DataSnapshot) {
        val chatRoomId = snapshot.key?.toLongOrNull() ?: return
        val meta = RoomMeta(
            lastMessage     = snapshot.child("lastMessage").getValue(String::class.java).orEmpty(),
            lastMessageTime = snapshot.child("lastMessageTime").getValue(Long::class.java) ?: 0L,
            unreadCount     = snapshot.child("unreadCount").getValue(Int::class.java) ?: 0,
        )
        onMetaChange(chatRoomId, meta)
    }

    companion object {
        private const val TAG = "UserChatRoomsListener"
    }
}

/** RTDB `users/{me}/chatrooms/{roomId}` 노드의 표시용 메타 추출. */
data class RoomMeta(
    val lastMessage     : String,
    val lastMessageTime : Long,    // epoch ms. 0이면 메시지 없음.
    val unreadCount     : Int,
)
