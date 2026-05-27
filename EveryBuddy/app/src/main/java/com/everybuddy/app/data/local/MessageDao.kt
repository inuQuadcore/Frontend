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

    /**
     * 서버 응답/RTDB 스냅샷에 없는 클라 전용 필드(번역 캐시, 영속 파일 경로, 음성 길이)를
     * 보존하면서 upsert. REST sync · RTDB onChildAdded 재발사 시 기존 캐시가 null로 덮어써지는 것 방지.
     */
    suspend fun upsertPreservingClientFields(entity: ChatMessageEntity) {
        val existing = clientFieldsFor(entity.messageId)
        val merged = if (existing == null) entity else entity.copy(
            translatedText = existing.translatedText ?: entity.translatedText,
            sourceText     = existing.sourceText     ?: entity.sourceText,
            localFilePath  = existing.localFilePath  ?: entity.localFilePath,
            voiceDuration  = existing.voiceDuration  ?: entity.voiceDuration,
        )
        upsert(merged)
    }

    suspend fun upsertAllPreservingClientFields(entities: List<ChatMessageEntity>) {
        if (entities.isEmpty()) return
        val existingById = clientFieldsByIds(entities.map { it.messageId }).associateBy { it.messageId }
        val merged = entities.map { e ->
            val ex = existingById[e.messageId] ?: return@map e
            e.copy(
                translatedText = ex.translatedText ?: e.translatedText,
                sourceText     = ex.sourceText     ?: e.sourceText,
                localFilePath  = ex.localFilePath  ?: e.localFilePath,
                voiceDuration  = ex.voiceDuration  ?: e.voiceDuration,
            )
        }
        upsertAll(merged)
    }

    @Query("SELECT messageId, translatedText, sourceText, localFilePath, voiceDuration FROM chat_messages WHERE messageId = :messageId")
    suspend fun clientFieldsFor(messageId: Long): ClientFieldsSnapshot?

    @Query("SELECT messageId, translatedText, sourceText, localFilePath, voiceDuration FROM chat_messages WHERE messageId IN (:messageIds)")
    suspend fun clientFieldsByIds(messageIds: List<Long>): List<ClientFieldsSnapshot>

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

    /** 마지막 메시지의 messageId + senderId — 자기 메시지 read 스킵 판정용. */
    @Query("""
        SELECT messageId, senderId FROM chat_messages
        WHERE chatRoomId = :chatRoomId AND status = 'SENT'
        ORDER BY sendAt DESC LIMIT 1
    """)
    suspend fun lastMessageWithSender(chatRoomId: Long): LastMessageInfo?

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

    /** 음성 메시지 재생 길이 영속화. ExoPlayer STATE_READY 시 추출 후 호출. */
    @Query("UPDATE chat_messages SET voiceDuration = :duration WHERE messageId = :messageId")
    suspend fun updateVoiceDuration(messageId: Long, duration: Int)

    /** PENDING(음수 tempId) 메시지의 messageId를 서버 PK로 교체할 때 path 캐리오버 용도 readback. */
    @Query("SELECT localFilePath FROM chat_messages WHERE messageId = :messageId")
    suspend fun localFilePath(messageId: Long): String?
}

data class LastMessageInfo(val messageId: Long, val senderId: Long)

data class ClientFieldsSnapshot(
    val messageId      : Long,
    val translatedText : String?,
    val sourceText     : String?,
    val localFilePath  : String?,
    val voiceDuration  : Int?,
)
