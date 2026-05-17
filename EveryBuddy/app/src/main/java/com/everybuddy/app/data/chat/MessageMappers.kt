package com.everybuddy.app.data.chat

import com.everybuddy.app.data.dto.Message
import com.everybuddy.app.data.local.ChatMessageEntity
import com.everybuddy.app.data.local.epochMsToKstLocalDateTime
import com.everybuddy.app.data.local.parseRestLocalDateTime
import com.google.firebase.database.DataSnapshot

/**
 * Message(DTO) ↔ ChatMessageEntity(Room) ↔ ChatMessage(UI) 변환 책임.
 * 시간은 KST LocalDateTime으로 통일 (TimeConverter helper 사용).
 */

/** REST 응답 Message → Room ChatMessageEntity. userName은 stale 방지로 저장 안 함. */
fun Message.toChatMessageEntity(
    chatRoomId : Long,
    status     : String = "SENT",
): ChatMessageEntity = ChatMessageEntity(
    messageId     = messageId,
    chatRoomId    = chatRoomId,
    senderId      = userId,
    messageType   = messageType,
    content       = content,
    sendAt        = parseRestLocalDateTime(sendAt),
    editedAt      = editedAt?.let(::parseRestLocalDateTime),
    statusPreview = statusPreview,
    status        = status,
    fileUrl       = fileUrl,
    fileName      = fileName,
    fileSize      = fileSize,
    mediaType     = mediaType,
)

/** Room ChatMessageEntity → UI ChatMessage. 송신자 이름/프로필은 UI에서 senderId로 lookup. */
fun ChatMessageEntity.toChatMessage(): ChatMessage = ChatMessage(
    id        = messageId.toString(),
    roomId    = chatRoomId.toString(),
    senderId  = senderId.toString(),
    type      = resolveType(messageType, mediaType),
    text      = content.orEmpty(),
    voiceUrl  = fileUrl.orEmpty(),
    // 음성 길이: 백엔드 응답에 duration 없음. 재생 시점에 ExoPlayer가 메타 추출.
    voiceDurationSec = 0,
    timestamp     = sendAt,
    editedAt      = editedAt,
    status        = status,
    isStatusReply = statusPreview != null,
    statusPreview = statusPreview.orEmpty(),
)

/** messageType + mediaType 조합 → UI MessageType. 미디어 enum 확장 시 여기 업데이트. */
private fun resolveType(messageType: String, mediaType: String?): MessageType = when {
    messageType == "TEXT"                             -> MessageType.TEXT
    messageType == "FILE" && mediaType == "AUDIO"     -> MessageType.VOICE
    messageType == "FILE" && mediaType == "IMAGE"     -> MessageType.IMAGE
    // TODO: VIDEO/DOCUMENT용 enum 추가 시 분기. 현재는 IMAGE로 통합 표시.
    else                                              -> MessageType.IMAGE
}

/** RTDB messages/{chatRoomId}/{messageId} 스냅샷 → Room ChatMessageEntity. */
fun DataSnapshot.toChatMessageEntity(chatRoomId: Long): ChatMessageEntity? {
    val messageId = key?.toLongOrNull() ?: return null
    val userId      = child("userId").getValue(Long::class.java)   ?: return null
    // RTDB userName 노드는 표시용으로 사용 X — 송신자 이름은 senderId로 UserSummaryCache lookup.
    val messageType = child("messageType").getValue(String::class.java) ?: "TEXT"
    val content     = child("content").getValue(String::class.java)
    val sendAtMs    = child("sendAt").getValue(Long::class.java)   ?: 0L
    val editedAtMs  = child("editedAt").getValue(Long::class.java)
    return ChatMessageEntity(
        messageId     = messageId,
        chatRoomId    = chatRoomId,
        senderId      = userId,
        messageType   = messageType,
        content       = content,
        sendAt        = epochMsToKstLocalDateTime(sendAtMs),
        editedAt      = editedAtMs?.let(::epochMsToKstLocalDateTime),
        statusPreview = child("statusPreview").getValue(String::class.java),
        status        = "SENT",
        fileUrl       = child("fileUrl").getValue(String::class.java),
        fileName      = child("fileName").getValue(String::class.java),
        fileSize      = child("fileSize").getValue(Long::class.java),
        mediaType     = child("mediaType").getValue(String::class.java),
    )
}
