// 모델 + 가상 로직 구현
// ChatRoom(얘를 ㅃㄹ해야만), ChatMessage, ChatFilter, UiState, 더미 데이터 전체
package com.everybuddy.app.data.chat

import java.time.LocalDateTime

// 채팅방 모델

data class ChatRoom(
    val id          : String  = "",
    val name        : String  = "",
    val lastMessage : String  = "",
    val timestamp   : String  = "",
    val isMuted     : Boolean = false,
    val isPinned    : Boolean = false,
)

// 채팅 리스트 상태
data class ChatListUiState(
    val rooms          : List<ChatRoom> = emptyList(),
    val isLoading      : Boolean        = false,
    val searchQuery    : String         = "",
    val activeFilter   : ChatFilter     = ChatFilter.ALL,
    val contextMenuRoom: ChatRoom?      = null,
)

enum class ChatFilter(val label: String) {
    ALL("전체"),
    UNREAD("읽지 않은"),
    FAVORITE("즐겨찾기"),
}


// 메시지 모델

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


// 채팅방 화면 UiState

data class ChatRoomUiState(
    val room                : ChatRoom               = ChatRoom(),
    val messages            : List<ChatMessage>      = emptyList(),
    val inputText           : String                 = "",
    val isRecording         : Boolean                = false,
    val recordingSeconds    : Int                    = 0,
    val recordingAmplitudes : List<Float>            = emptyList(),
    val playingMessageId    : String?                = null,
    val isMediaPanelOpen    : Boolean                = false,
    val isAutoTranslate     : Boolean                = false,
    val showTranslation     : Map<String, Boolean>   = emptyMap(),
)


// 더미 데이터

val dummyChatRooms: List<ChatRoom> = listOf(
    ChatRoom(id = "r01", name = "Potter",  lastMessage = "안녕하세요!",  timestamp = "방금"),
    ChatRoom(id = "r02", name = "오혁",    lastMessage = "좋아요",       timestamp = "1시간 전"),
    ChatRoom(id = "r03", name = "Hermione", lastMessage = "See you!",   timestamp = "어제"),
)

val dummyMessages: Map<String, List<ChatMessage>> = mapOf(
    "r01" to listOf(
        ChatMessage(
            id         = "m01", roomId = "r01",
            senderId   = "u01", senderName = "Potter",
            type       = MessageType.TEXT, text = "Hello!",
            timestamp  = LocalDateTime.now().minusMinutes(5),
        ),
        ChatMessage(
            id         = "m02", roomId = "r01",
            senderId   = "me",  senderName = "나",
            type       = MessageType.TEXT, text = "안녕하세요!",
            timestamp  = LocalDateTime.now(),
        ),
    ),
    "r02" to listOf(
        ChatMessage(
            id         = "m03", roomId = "r02",
            senderId   = "u08", senderName = "오혁",
            type       = MessageType.TEXT, text = "좋아요",
            timestamp  = LocalDateTime.now(),
        ),
    ),
)
