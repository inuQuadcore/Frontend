package com.everybuddy.app.data.firebase

import android.util.Log
import com.everybuddy.app.data.chat.toChatMessageEntity
import com.everybuddy.app.data.local.ChatMessageEntity
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query

/**
 * 채팅방 메시지 실시간 구독.
 * RTDB `messages/{chatRoomId}/` 노드를 enterChatRoomAt 이후 + 최신 N개로 구독.
 *
 * 사용: 채팅방 진입 시 [attach], 이탈 또는 ViewModel onCleared 시 [detach].
 * 한 인스턴스는 한 채팅방 한 구독을 책임. 채팅방 이동 시 detach + 새 인스턴스 권장.
 *
 * - 본인 입장 이전 메시지 격리 ([rtdb.md] 카톡식): `orderByChild("sendAt").startAt(enterChatRoomAt)`
 * - 최신 N개만 구독: `limitToLast(LIMIT)` — 다운로드 효율 + 5분 수정 윈도우 안 옛 메시지 변경 이벤트 커버
 * - onChildAdded/Changed: Room upsert (PK기반 멱등)
 * - onChildRemoved: 거의 안 옴 (서버는 보통 노드 유지 + content "삭제된 메시지입니다"로 갱신 — onChildChanged로 옴)
 */
class ChatMessageListener(
    private val chatRoomId       : Long,
    private val enterChatRoomAt  : Long,
    private val onUpsert         : (ChatMessageEntity) -> Unit,
    private val onRemoved        : (messageId: Long) -> Unit,
) {

    private val rtdb = FirebaseDatabase.getInstance()
    private var query    : Query?              = null
    private var listener : ChildEventListener? = null

    fun attach() {
        if (listener != null) return

        val q = rtdb.getReference("messages/$chatRoomId")
            .orderByChild("sendAt")
            .startAt(enterChatRoomAt.toDouble())
            .limitToLast(LIMIT)

        val l = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.toChatMessageEntity(chatRoomId)?.let(onUpsert)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.toChatMessageEntity(chatRoomId)?.let(onUpsert)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                snapshot.key?.toLongOrNull()?.let(onRemoved)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // sendAt 변경으로 정렬 위치만 바뀐 경우 — 데이터 변화 없으므로 무시.
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "messages/$chatRoomId listener cancelled", error.toException())
            }
        }

        q.addChildEventListener(l)
        query    = q
        listener = l
    }

    fun detach() {
        val l = listener ?: return
        query?.removeEventListener(l)
        query    = null
        listener = null
    }

    companion object {
        private const val TAG   = "ChatMessageListener"
        private const val LIMIT = 50
    }
}
