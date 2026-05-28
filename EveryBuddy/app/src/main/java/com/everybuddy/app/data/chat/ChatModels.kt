package com.everybuddy.app.data.chat

import com.everybuddy.app.data.cache.UserSummary
import com.everybuddy.app.ui.chat.CaptureOption
import java.time.LocalDateTime

data class ChatParticipantUi(
    val id                 : Long    = 0,
    val profileImageUrl    : String? = null,
    val profileDrawableRes : Int?    = null,
)

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
    val profileImageUrl    : String?                 = null,   // 1:1방 상대 프로필. 그룹방은 null (participants로 모자이크).
    val profileDrawableRes : Int?                    = null,
    val participants       : List<ChatParticipantUi> = emptyList(),
)

/**
 * ChatRoom(DTO) → ChatRoomUi 변환.
 * @param myUserId 본인 ID. 1:1방 상대 추출용. 0이면 상대 추출 X (displayName도 서버 roomName으로 폴백).
 * @param lastMessageEpochMs RTDB epoch ms 변환 결과. null이면 0L.
 */
fun com.everybuddy.app.data.dto.ChatRoom.toChatRoomUi(
    myUserId           : Long = 0L,
    lastMessageEpochMs : Long? = null,
): ChatRoomUi {
    val displayName        = ChatRoomDisplayName.resolve(this, myUserId).ifEmpty { roomName }
    val otherInOneToOne    = if (!isGroup) participants.firstOrNull { it.userId != myUserId } else null
    val participantUis     = participants.map {
        ChatParticipantUi(id = it.userId, profileImageUrl = it.profileImageUrl)
    }
    return ChatRoomUi(
        id              = chatRoomId.toString(),
        name            = displayName,
        lastMessage     = lastMessage.orEmpty(),
        lastMessageTime = lastMessageEpochMs ?: 0L,
        createdAt       = createdAt,
        unreadCount     = unreadCount ?: 0,
        isGroup         = isGroup,
        profileImageUrl = otherInOneToOne?.profileImageUrl,
        participants    = participantUis,
    )
}

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

enum class MessageType { TEXT, VOICE, IMAGE, VIDEO, FILE }

data class VideoSubtitle(
    val startMs    : Long,
    val endMs      : Long,
    val original   : String,
    val translated : String,
)

data class ChatMessage(
    val id               : String          = "",
    val roomId           : String          = "",
    val senderId         : String          = "",
    val type             : MessageType     = MessageType.TEXT,
    val text             : String          = "",
    val voiceUrl         : String          = "",
    val voiceDurationSec : Int             = 0,
    val timestamp        : LocalDateTime   = LocalDateTime.now(),
    val editedAt         : LocalDateTime?  = null,                  // null이면 미수정, 값 있으면 "(수정됨)" 표시
    val status           : String          = "SENT",                // "SENT" | "PENDING" | "FAILED" — 송신 인디케이터
    val translatedText   : String          = "",
    val sourceText       : String          = "",                    // 음성 번역 응답의 STT 결과. 텍스트 메시지는 빈 문자열.
    val isStatusReply    : Boolean         = false,
    val statusPreview    : String          = "",
    val localFilePath    : String?         = null,                  // 영속화된 미디어 로컬 경로. null이면 voiceUrl/fileUrl로 fallback.
    val fileName         : String          = "",
    val fileSize         : Long            = 0L,
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
    val translationError          : String?              = null,           // 토스트 1회 노출 후 consume

    // 음성 녹음
    val isRecording               : Boolean              = false,
    val isRecordingPaused         : Boolean              = false,
    val recordingSeconds          : Int                  = 0,
    val recordingAmplitudes       : List<Float>          = emptyList(),

    // 음성 재생
    val playingMessageId          : String?              = null,
    val playPositionMs            : Long                 = 0L,

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

    // 이미지 풀스크린 뷰어 — localFilePath 또는 fileUrl. null이면 닫힘.
    val fullscreenImage           : String?              = null,

    // 채팅 내 메시지 답장 — 롱프레스 "답장하기" 선택 시 설정. null이면 답장 모드 아님.
    val replyToMessage            : ChatMessage?         = null,

    // 영상 재생 + 자막
    val videoPlayerMessageId  : String?                            = null,
    val videoSubtitles        : Map<String, List<VideoSubtitle>>  = emptyMap(),
    val subtitleLoadingIds    : Set<String>                        = emptySet(),
    val videoDownloadingIds   : Set<String>                        = emptySet(),

    val isRefreshing          : Boolean                            = false,
)
