package com.everybuddy.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 채팅 메시지 로컬 캐시.
 * - 서버 메시지는 messageId가 양수 (서버 PK).
 * - PENDING 송신 메시지는 음수 tempId (PK 제약 회피용, 송신 성공 시 row 교체).
 * - sendAt/editedAt은 KST LocalDateTime으로 통일 저장 (TimeConverter 참고).
 */
@Entity(
    tableName = "chat_messages",
    indices   = [Index("chatRoomId"), Index("sendAt")],
)
data class ChatMessageEntity(
    @PrimaryKey val messageId   : Long,
    val chatRoomId : Long,
    val senderId   : Long,
    val senderName : String,
    val messageType: String,                          // "TEXT" | "FILE"
    val content    : String?,
    val sendAt     : LocalDateTime,
    val editedAt   : LocalDateTime?  = null,
    val status     : String          = "SENT",        // "SENT" | "PENDING" | "FAILED"
    val fileUrl    : String?         = null,
    val fileName   : String?         = null,
    val fileSize   : Long?           = null,
    val mediaType  : String?         = null,          // "IMAGE" | "VIDEO" | "AUDIO" | "DOCUMENT"
)
