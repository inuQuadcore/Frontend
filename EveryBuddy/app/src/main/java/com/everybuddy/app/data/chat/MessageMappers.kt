package com.everybuddy.app.data.chat

import com.everybuddy.app.data.dto.Message
import com.everybuddy.app.data.local.ChatMessageEntity
import com.everybuddy.app.data.local.MessageCachedFields
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
    type      = resolveType(messageType, mediaType, fileName),
    text      = content.orEmpty(),
    voiceUrl  = fileUrl.orEmpty(),
    voiceDurationSec = voiceDuration ?: 0,
    timestamp        = sendAt,
    editedAt         = editedAt,
    status           = status,
    translatedText   = translatedText.orEmpty(),
    sourceText       = sourceText.orEmpty(),
    isStatusReply    = statusPreview != null,
    statusPreview    = statusPreview.orEmpty(),
    localFilePath    = localFilePath,
    fileName         = fileName.orEmpty(),
    fileSize         = fileSize ?: 0L,
)

private val VIDEO_EXTS = setOf("mp4", "mov", "avi", "mkv", "webm", "3gp", "ts")
private val AUDIO_EXTS = setOf("mp3", "aac", "wav", "m4a", "ogg", "flac", "opus")
private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp")

private fun resolveType(messageType: String, mediaType: String?, fileName: String?): MessageType {
    if (messageType == "TEXT") return MessageType.TEXT
    if (messageType != "FILE") return MessageType.TEXT

    val mt = mediaType?.uppercase()
    if (mt != null) {
        return when {
            mt.contains("VIDEO") -> MessageType.VIDEO
            mt.contains("AUDIO") -> MessageType.VOICE
            mt.contains("IMAGE") -> MessageType.IMAGE
            else                 -> MessageType.FILE
        }
    }

    val ext = fileName?.substringAfterLast('.', "")?.lowercase()
    return when (ext) {
        in VIDEO_EXTS -> MessageType.VIDEO
        in AUDIO_EXTS -> MessageType.VOICE
        in IMAGE_EXTS -> MessageType.IMAGE
        else          -> MessageType.FILE
    }
}

/**
 * 서버/RTDB에서 받은 entity에 기존 DB 캐시(번역·로컬 파일·음성 길이)를 덮어쓰지 않도록 보존.
 * toChatMessageEntity 변환 결과에는 이 필드들이 null이라 OnConflictStrategy.REPLACE가 번역을 지운다.
 */
fun ChatMessageEntity.preserveCache(cached: MessageCachedFields?): ChatMessageEntity =
    if (cached == null) this
    else copy(
        translatedText = translatedText ?: cached.translatedText,
        sourceText     = sourceText     ?: cached.sourceText,
        localFilePath  = localFilePath  ?: cached.localFilePath,
        voiceDuration  = voiceDuration  ?: cached.voiceDuration,
    )

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
