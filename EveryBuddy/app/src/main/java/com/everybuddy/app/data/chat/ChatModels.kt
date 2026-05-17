package com.everybuddy.app.data.chat

import com.everybuddy.app.R
import com.everybuddy.app.data.cache.UserSummary
import com.everybuddy.app.ui.chat.CaptureOption
import java.time.LocalDateTime

data class ChatParticipantUi(
    val id                 : Long    = 0,
    val profileImageUrl    : String? = null,
    val profileDrawableRes : Int?    = null,
)

// ChatRoom — [변경] createdAt, unreadCount, participantIds, participants 추가
data class ChatRoomUi(
    val id                 : String                  = "",
    val name               : String                  = "",
    val lastMessage        : String                  = "",
    val timestamp          : String                  = "",
    val lastMessageTime    : Long                    = 0L,   // epoch ms. RTDB 노드 값 보관 (RelativeTimeFormatter로 timestamp 갱신용).
    val createdAt          : String                  = "",
    val unreadCount        : Int                     = 0,
    val isGroup            : Boolean                 = false,  // false 기본 — isGroup 누락 시 fail-safe로 멤버 초대 차단
    val isMuted            : Boolean                 = false,
    val isPinned           : Boolean                 = false,
    val isStarred          : Boolean                 = false,
    val participantIds     : List<Long>              = emptyList(),
    val profileImageUrl    : String?                 = null,
    val profileDrawableRes : Int?                    = null,
    // API 연동 시 서버에서 받은 참여자 프로필 목록으로 교체
    val participants       : List<ChatParticipantUi> = emptyList(),
)

/**
 * ChatRoom(DTO) → ChatRoomUi 변환.
 * @param displayName 표시용 이름. ChatRoomDisplayName.resolve 결과를 전달.
 *                    빈 문자열이면 서버 roomName으로 폴백.
 */
fun com.everybuddy.app.data.dto.ChatRoom.toChatRoomUi(
    displayName: String = "",
): ChatRoomUi = ChatRoomUi(
    id             = chatRoomId.toString(),
    name           = displayName.ifEmpty { roomName },
    createdAt      = createdAt,
    unreadCount    = unreadCount ?: 0,
    isGroup        = isGroup,
    participantIds = participantIds,
)

data class ChatFolder(
    val id          : String       = System.currentTimeMillis().toString(),
    val name        : String       = "",
    val order       : Int          = 0,
    val chatRoomIds : List<String> = emptyList(),
)

data class ChatListUiState(
    val rooms              : List<ChatRoomUi>   = emptyList(),
    val isLoading          : Boolean          = false,
    val isRefreshing       : Boolean          = false,
    val errorMessage       : String?          = null,
    val isSearchOpen       : Boolean          = false,
    val searchQuery        : String           = "",
    val activeFilter       : ChatFilter       = ChatFilter.ALL,
    val contextMenuRoom    : ChatRoomUi?        = null,
    val infoRoom           : ChatRoomUi?        = null,
    val folders            : List<ChatFolder> = emptyList(),
    val activeFolderId     : String?          = null,
    val isFolderManageOpen : Boolean          = false,
    val isFolderCreateOpen : Boolean          = false,
    val editingFolder      : ChatFolder?      = null,
)

enum class ChatFilter(val label: String) {
    ALL("전체"),
    UNREAD("읽지 않은"),
    FAVORITE("즐겨찾기"),
}

enum class MessageType { TEXT, VOICE, IMAGE }

data class ChatMessage(
    val id               : String          = "",
    val roomId           : String          = "",
    val senderId         : String          = "",
    val senderName       : String          = "",
    val type             : MessageType     = MessageType.TEXT,
    val text             : String          = "",
    val voiceUrl         : String          = "",
    val voiceDurationSec : Int             = 0,
    val timestamp        : LocalDateTime   = LocalDateTime.now(),
    val editedAt         : LocalDateTime?  = null,                  // null이면 미수정, 값 있으면 "(수정됨)" 표시
    val status           : String          = "SENT",                // "SENT" | "PENDING" | "FAILED" — 송신 인디케이터
    val isTranslated     : Boolean         = false,
    val translatedText   : String          = "",
    val isStatusReply    : Boolean         = false,
    val statusPreview    : String          = "",
)

data class ChatRoomUiState(
    val room                      : ChatRoomUi           = ChatRoomUi(),
    val messages                  : List<ChatMessage>    = emptyList(),
    val userSummaries             : Map<Long, UserSummary> = emptyMap(),  // senderId → UserSummary. UI는 senderId로 lookup해서 이름/프로필 표시.
    val inputText                 : String               = "",
    val myUserId                  : Long                 = 0L,            // ContextMenu isOwnMessage 판정용. 0 = 미인증/로드 전.

    // 번역
    val isAutoTranslate           : Boolean              = false,
    val showTranslation           : Map<String, Boolean> = emptyMap(),
    val translatingMessageIds     : Set<String>          = emptySet(),

    // 음성 녹음
    val isRecording               : Boolean              = false,
    val isRecordingPaused         : Boolean              = false,
    val recordingSeconds          : Int                  = 0,
    val recordingAmplitudes       : List<Float>          = emptyList(),

    // 음성 재생
    val playingMessageId          : String?              = null,

    // 미디어 패널
    val isMediaPanelOpen          : Boolean              = false,

    // 사진 선택 시트
    val isPhotoPickerOpen         : Boolean              = false,
    val selectedPhotoIndices      : Set<Int>             = emptySet(),

    // 롱프레스 컨텍스트 메뉴 (단일 말풍선)
    val contextMenuMessage        : ChatMessage?         = null,

    // 단일 말풍선 스크립트 저장 시트
    val scriptSaveMessage         : ChatMessage?         = null,

    // 저장 완료 토스트
    val savedToastVisible         : Boolean              = false,

    // 대화 선택 저장 (미디어 패널 → 대화저장)
    val isConversationSelectOpen  : Boolean              = false,
    val conversationSaveMessages  : List<ChatMessage>    = emptyList(),
    val conversationCaptureOption : CaptureOption        = CaptureOption.COMBINED,
)

private val BASE_DATE = LocalDateTime.of(2026, 1, 25, 0, 0)

val dummyChatRooms: List<ChatRoomUi> = listOf(
    ChatRoomUi(
        id                 = "r_woowonjai",
        name               = "우원재",
        lastMessage        = "난 지금 눈을 감아야 해. 내일의 나는 달라져야 해.",
        timestamp          = "오후 11:53",
        unreadCount        = 0,
        profileDrawableRes = R.drawable.im_woo,
    ),
    ChatRoomUi(id = "r01", name = "Potter",   lastMessage = "안녕하세요!", timestamp = "방금",    unreadCount = 3),
    ChatRoomUi(id = "r02", name = "오혁",     lastMessage = "좋아요",      timestamp = "1시간 전", unreadCount = 0),
    ChatRoomUi(id = "r03", name = "Hermione", lastMessage = "See you!",   timestamp = "어제",    unreadCount = 1),
)

val dummyMessages: Map<String, List<ChatMessage>> = mapOf(
    "r_woowonjai" to listOf(
        ChatMessage(
            id = "ww_m01", roomId = "r_woowonjai",
            senderId = "u_woowonjai", senderName = "우원재",
            type = MessageType.TEXT,
            text = "You make me feel brand new",
            translatedText = "네 덕분에 새 사람이 된 것 같아",
            timestamp = BASE_DATE.withHour(19).withMinute(41),
        ),
        ChatMessage(
            id = "ww_m02", roomId = "r_woowonjai",
            senderId = "u_woowonjai", senderName = "우원재",
            type = MessageType.VOICE,
            text = "We're livin' in a different time zone. Have a good night",
            translatedText = "우리는 다른 시간대에 살잖아.\n잘 자.",
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
