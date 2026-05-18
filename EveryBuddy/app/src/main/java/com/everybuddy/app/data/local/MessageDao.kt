package com.everybuddy.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE messageId = :messageId")
    suspend fun delete(messageId: Long)

    @Query("DELETE FROM chat_messages WHERE messageId IN (:messageIds)")
    suspend fun deleteAll(messageIds: List<Long>)

    /** 채팅방 나가기/강퇴 시 호출 — 재초대 후 옛 메시지 누수 방지. */
    @Query("DELETE FROM chat_messages WHERE chatRoomId = :chatRoomId")
    suspend fun deleteAllByRoom(chatRoomId: Long)

    /** 채팅방의 메시지를 sendAt 오름차순으로 limit/offset 페이지네이션. */
    @Query("""
        SELECT * FROM chat_messages
        WHERE chatRoomId = :chatRoomId
        ORDER BY sendAt ASC
        LIMIT :limit OFFSET :offset
    """)
    fun observeRoom(chatRoomId: Long, limit: Int, offset: Int): Flow<List<ChatMessageEntity>>

    /** 채팅방 전체 메시지 sendAt ASC. RTDB가 limitToLast(50)로 제한해 보내니 1000개 미만 가정. */
    @Query("SELECT * FROM chat_messages WHERE chatRoomId = :chatRoomId ORDER BY sendAt ASC")
    fun observeRoomAll(chatRoomId: Long): Flow<List<ChatMessageEntity>>

    /** SENT 상태의 마지막 sendAt — REST sync의 since 파라미터에 사용. */
    @Query("""
        SELECT sendAt FROM chat_messages
        WHERE chatRoomId = :chatRoomId AND status = 'SENT'
        ORDER BY sendAt DESC LIMIT 1
    """)
    suspend fun lastSentAt(chatRoomId: Long): LocalDateTime?

    /** SENT 상태의 마지막 messageId — POST /messages/{id}/read 호출에 사용. */
    @Query("""
        SELECT messageId FROM chat_messages
        WHERE chatRoomId = :chatRoomId AND status = 'SENT'
        ORDER BY sendAt DESC LIMIT 1
    """)
    suspend fun lastMessageId(chatRoomId: Long): Long?

    /** SENT 상태 메시지 개수 — 로컬 페이지네이션 hasMore 판정. */
    @Query("""
        SELECT COUNT(*) FROM chat_messages
        WHERE chatRoomId = :chatRoomId AND status = 'SENT'
    """)
    suspend fun countSent(chatRoomId: Long): Int

    /** 번역 결과 영구 캐싱. sourceText는 음성 메시지의 STT 결과(텍스트는 null로 호출). */
    @Query("""
        UPDATE chat_messages
        SET translatedText = :translatedText, sourceText = :sourceText
        WHERE messageId = :messageId
    """)
    suspend fun updateTranslation(messageId: Long, translatedText: String, sourceText: String?)

    /** 미디어 영속화 경로 갱신. 송신 업로드 성공 또는 수신 첫 다운로드 직후 호출. */
    @Query("UPDATE chat_messages SET localFilePath = :path WHERE messageId = :messageId")
    suspend fun updateLocalFilePath(messageId: Long, path: String?)

    /** PENDING(음수 tempId) 메시지의 messageId를 서버 PK로 교체할 때 path 캐리오버 용도 readback. */
    @Query("SELECT localFilePath FROM chat_messages WHERE messageId = :messageId")
    suspend fun localFilePath(messageId: Long): String?
}
