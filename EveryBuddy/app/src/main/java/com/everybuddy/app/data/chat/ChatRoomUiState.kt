package com.everybuddy.app.data.chat

import com.everybuddy.app.ui.chat.CaptureOption

data class ChatRoomUiState(
    val room                      : ChatRoom              = ChatRoom(),
    val messages                  : List<ChatMessage>     = emptyList(),
    val inputText                 : String                = "",

    // 번역
    val isAutoTranslate           : Boolean               = false,
    val showTranslation           : Map<String, Boolean>  = emptyMap(),
    val translatingMessageIds     : Set<String>           = emptySet(),

    // 음성 녹음
    val isRecording               : Boolean               = false,
    val isRecordingPaused         : Boolean               = false,
    val recordingSeconds          : Int                   = 0,
    val recordingAmplitudes       : List<Float>           = emptyList(),

    // 음성 재생
    val playingMessageId          : String?               = null,

    // 미디어 패널
    val isMediaPanelOpen          : Boolean               = false,

    // 사진 선택 시트
    // - isPhotoPickerOpen: 미디어 패널 "사진" 탭 클릭 시 true
    // - selectedPhotoIndices: 선택된 사진 순서 (최대 10개, 선택 순서대로 번호 표시)
    val isPhotoPickerOpen         : Boolean               = false,
    val selectedPhotoIndices      : Set<Int>              = emptySet(),

    // 롱프레스 컨텍스트 메뉴 (단일 말풍선)
    val contextMenuMessage        : ChatMessage?          = null,

    // 단일 말풍선 스크립트 저장 시트
    val scriptSaveMessage         : ChatMessage?          = null,

    // 저장 완료 토스트
    val savedToastVisible         : Boolean               = false,

    // 대화 선택 저장 (미디어 패널 → 대화저장)
    val isConversationSelectOpen  : Boolean               = false,
    val conversationSaveMessages  : List<ChatMessage>     = emptyList(),
    val conversationCaptureOption : CaptureOption         = CaptureOption.COMBINED,
)
