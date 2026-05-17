package com.everybuddy.app.data.firebase

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

/**
 * 본인 채팅방 목록 메타데이터 실시간 구독.
 * RTDB `users/{userId}/chatrooms/` 노드의 각 채팅방 메타(lastMessage/lastMessageTime/unreadCount 등)를
 * onChildAdded/Changed/Removed 콜백으로 전달.
 *
 * - GET /chatrooms 호출 후 attach. 초기에 onChildAdded로 기존 채팅방 메타가 1회씩 발사됨.
 * - 본인이 채팅방 나가기 시 onChildRemoved.
 * - 메시지 송수신/read 처리 시 서버 multi-path update → onChildChanged.
 */
class UserChatRoomsListener(
    private val userId      : Long,
    private val onMetaChange: (chatRoomId: Long, meta: RoomMeta) -> Unit,
    private val onRoomRemoved: (chatRoomId: Long) -> Unit,
) {

    private val rtdb = FirebaseDatabase.getInstance()
    private var ref      : DatabaseReference?  = null
    private var listener : ChildEventListener? = null

    fun attach() {
        if (listener != null) return
        val node = rtdb.getReference("users/$userId/chatrooms")
        val l = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                emit(snapshot)
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
