package com.everybuddy.app.data.chat

import java.time.LocalDateTime

// ChatRoom — [변경] createdAt, unreadCount, participantIds 추가
data class ChatRoom(
    val id             : String       = "",
    val name           : String       = "",
    val lastMessage    : String       = "",  // TODO: 서버 lastMessage 필드 추가 후 연동
    val timestamp      : String       = "",  // TODO: createdAt 파싱 상대시간
    val createdAt      : String       = "",  // ISO-8601 (서버 createdAt)
    val unreadCount    : Int          = 0,   // 읽지 않은 메시지 수 → 배지 표시
    val isMuted        : Boolean      = false,
    val isPinned       : Boolean      = false,
    val participantIds : List<Long>   = emptyList(),
)

// ChatRoomDto → ChatRoom 변환
fun com.everybuddy.app.data.dto.ChatRoomDto.toChatRoom(): ChatRoom = ChatRoom(
    id             = chatRoomId.toString(),
    name           = roomName,
    createdAt      = createdAt,
    unreadCount    = unreadCount,
    participantIds = participantIds,
)

// ChatListUiState — [변경] errorMessage 추가
data class ChatListUiState(
    val rooms           : List<ChatRoom> = emptyList(),
    val isLoading       : Boolean        = false,
    val errorMessage    : String?        = null,
    val searchQuery     : String         = "",
    val activeFilter    : ChatFilter     = ChatFilter.ALL,
    val contextMenuRoom : ChatRoom?      = null,
)

enum class ChatFilter(val label: String) {
    ALL("전체"),
    UNREAD("읽지 않은"),
    FAVORITE("즐겨찾기"),
}

enum class MessageType { TEXT, VOICE, IMAGE }

data class ChatMessage(
    val id               : String        = "",
    val roomId           : String        = "",
    val senderId         : String        = "",
    val senderName       : String        = "",
    val type             : MessageType   = MessageType.TEXT,
    val text             : String        = "",
    val voiceUrl         : String        = "",
    val voiceDurationSec : Int           = 0,
    val timestamp        : LocalDateTime = LocalDateTime.now(),
    val isTranslated     : Boolean       = false,
    val translatedText   : String        = "",
)

// ChatRoomUiState — [변경] translatingMessageIds, contextMenuMessage, savedToastVisible 추가
data class ChatRoomUiState(
    val room                  : ChatRoom             = ChatRoom(),
    val messages              : List<ChatMessage>    = emptyList(),
    val inputText             : String               = "",
    val isRecording           : Boolean              = false,
    val recordingSeconds      : Int                  = 0,
    val recordingAmplitudes   : List<Float>          = emptyList(),
    val playingMessageId      : String?              = null,
    val isMediaPanelOpen      : Boolean              = false,
    val isAutoTranslate       : Boolean              = false,
    val showTranslation       : Map<String, Boolean> = emptyMap(),
    val translatingMessageIds : Set<String>          = emptySet(),
    val contextMenuMessage    : ChatMessage?         = null,
    val savedToastVisible     : Boolean              = false,
)

private val BASE_DATE = LocalDateTime.of(2026, 1, 25, 0, 0)

val dummyChatRooms: List<ChatRoom> = listOf(
    ChatRoom(
        id          = "r_woowonjai",
        name        = "우원재",
        lastMessage = "난 지금 눈을 감아야 해. 내일의 나는 달라져야 해.",
        timestamp   = "오후 11:53",
        unreadCount = 0,
    ),
    ChatRoom(id = "r01", name = "Potter",   lastMessage = "안녕하세요!", timestamp = "방금",    unreadCount = 3),
    ChatRoom(id = "r02", name = "오혁",     lastMessage = "좋아요",      timestamp = "1시간 전", unreadCount = 0),
    ChatRoom(id = "r03", name = "Hermione", lastMessage = "See you!",   timestamp = "어제",    unreadCount = 1),
)

val dummyMessages: Map<String, List<ChatMessage>> = mapOf(
    "r_woowonjai" to listOf(
        ChatMessage(
            id = "ww_m01", roomId = "r_woowonjai",
            senderId = "u_woowonjai", senderName = "우원재",
            type = MessageType.TEXT,
            text = "We're livin' in a different time zone. Have a good night",
            translatedText = "우리는 다른 시간대에 살고 있습니다.\n좋은 밤 되세요.",
            timestamp = BASE_DATE.withHour(19).withMinute(41),
        ),
        ChatMessage(
            id = "ww_m02", roomId = "r_woowonjai",
            senderId = "u_woowonjai", senderName = "우원재",
            type = MessageType.VOICE,
            text = "We are who we are\nWe a-a-are who we a-a-are\nohahh\nDon't you know who we are?",
            translatedText = "우리는 우리야.\n우리느으은 우리느으으으은\n우리가 누구인지 몰라?",
            voiceUrl = "", voiceDurationSec = 24,
            timestamp = BASE_DATE.withHour(19).withMinute(42),
        ),
        ChatMessage(
            id = "ww_m03", roomId = "r_woowonjai",
            senderId = "me", senderName = "나",
            type = MessageType.TEXT,
            text = "난 지금 눈을 감아야 해. 내일의 나는 달라져야 해.",
            timestamp = BASE_DATE.withHour(23).withMinute(51),
        ),
        ChatMessage(
            id = "ww_m04", roomId = "r_woowonjai",
            senderId = "me", senderName = "나",
            type = MessageType.VOICE,
            voiceUrl = "", voiceDurationSec = 10,
            timestamp = BASE_DATE.withHour(23).withMinute(53),
        ),
    ),
    "r01" to listOf(
        ChatMessage(id = "m01", roomId = "r01", senderId = "u01", senderName = "Potter",
            type = MessageType.TEXT, text = "Hello!", timestamp = LocalDateTime.now().minusMinutes(5)),
        ChatMessage(id = "m02", roomId = "r01", senderId = "me", senderName = "나",
            type = MessageType.TEXT, text = "안녕하세요!", timestamp = LocalDateTime.now()),
    ),
    "r02" to listOf(
        ChatMessage(id = "m03", roomId = "r02", senderId = "u08", senderName = "오혁",
            type = MessageType.TEXT, text = "좋아요", timestamp = LocalDateTime.now()),
    ),
)
