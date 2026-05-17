package com.everybuddy.app.data.chat

import com.everybuddy.app.data.dto.Message
import com.everybuddy.app.data.local.ChatMessageEntity
import com.everybuddy.app.data.local.parseRestLocalDateTime

/**
 * Message(DTO) ↔ ChatMessageEntity(Room) ↔ ChatMessage(UI) 변환 책임.
 * 시간은 KST LocalDateTime으로 통일 (TimeConverter helper 사용).
 */

/** REST 응답 Message → Room ChatMessageEntity. */
fun Message.toChatMessageEntity(
    chatRoomId : Long,
    status     : String = "SENT",
): ChatMessageEntity = ChatMessageEntity(
    messageId   = messageId,
    chatRoomId  = chatRoomId,
    senderId    = userId,
    senderName  = userName,
    messageType = messageType,
    content     = content,
    sendAt      = parseRestLocalDateTime(sendAt),
    editedAt    = editedAt?.let(::parseRestLocalDateTime),
    status      = status,
    fileUrl     = fileUrl,
    fileName    = fileName,
    fileSize    = fileSize,
    mediaType   = mediaType,
)

/** Room ChatMessageEntity → UI ChatMessage. */
fun ChatMessageEntity.toChatMessage(): ChatMessage = ChatMessage(
    id        = messageId.toString(),
    roomId    = chatRoomId.toString(),
    senderId  = senderId.toString(),
    senderName= senderName,
    type      = resolveType(messageType, mediaType),
    text      = content.orEmpty(),
    voiceUrl  = fileUrl.orEmpty(),
    // 음성 길이: 백엔드 응답에 duration 없음. 재생 시점에 ExoPlayer가 메타 추출.
    voiceDurationSec = 0,
    timestamp = sendAt,
    editedAt  = editedAt,
    status    = status,
)

/** messageType + mediaType 조합 → UI MessageType. 미디어 enum 확장 시 여기 업데이트. */
private fun resolveType(messageType: String, mediaType: String?): MessageType = when {
    messageType == "TEXT"                             -> MessageType.TEXT
    messageType == "FILE" && mediaType == "AUDIO"     -> MessageType.VOICE
    messageType == "FILE" && mediaType == "IMAGE"     -> MessageType.IMAGE
    // TODO: VIDEO/DOCUMENT용 enum 추가 시 분기. 현재는 IMAGE로 통합 표시.
    else                                              -> MessageType.IMAGE
}
