package com.everybuddy.app.ui.chat

// =============================================================================
// ChatRoomViewModel.kt  (업데이트)
//
// 변경사항:
//   - onToggleAutoTranslate() 추가
//   - onPauseRecording() TODO stub 추가
//   - 코드 정리 + 주석 보강
// =============================================================================

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.chat.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val voiceRecorder : VoiceRecorder,
    private val voicePlayer   : VoicePlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatRoomUiState())
    val uiState: StateFlow<ChatRoomUiState> = _uiState.asStateFlow()

    init {
        // 녹음 타이머 구독
        viewModelScope.launch {
            voiceRecorder.seconds.collect { sec ->
                _uiState.update { it.copy(recordingSeconds = sec) }
            }
        }
        // 녹음 진폭 구독 (파형 UI)
        viewModelScope.launch {
            voiceRecorder.amplitudes.collect { amps ->
                _uiState.update { it.copy(recordingAmplitudes = amps) }
            }
        }
        // 재생 상태 구독
        viewModelScope.launch {
            voicePlayer.state.collect { playerState ->
                when (playerState) {
                    is PlayerState.Playing  ->
                        _uiState.update { it.copy(playingMessageId = playerState.messageId) }
                    is PlayerState.Finished,
                    is PlayerState.Idle,
                    is PlayerState.Paused   ->
                        _uiState.update { it.copy(playingMessageId = null) }
                }
            }
        }
    }

    // ── 방 로드 ───────────────────────────────────────────────
    fun loadRoom(roomId: String) {
        val room     = dummyChatRooms.find { it.id == roomId } ?: return
        val messages = dummyMessages[roomId] ?: emptyList()
        _uiState.update { it.copy(room = room, messages = messages) }
        // TODO: 실제 메시지 로드
        // viewModelScope.launch { chatRepository.getMessages(roomId, page=0) }
    }

    // ── 텍스트 입력 ───────────────────────────────────────────
    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    // ── 텍스트 전송 ───────────────────────────────────────────
    fun onSendText() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val msg = ChatMessage(
            id         = UUID.randomUUID().toString(),
            roomId     = _uiState.value.room.id,
            senderId   = "me",
            senderName = "나",
            type       = MessageType.TEXT,
            text       = text,
            timestamp  = LocalDateTime.now(),
        )
        _uiState.update { state ->
            state.copy(messages = state.messages + msg, inputText = "")
        }
        // TODO: ChatRepository.sendTextMessage(roomId, text)
    }

    // ── 음성 녹음 시작 ─────────────────────────────────────────
    fun onStartRecording() {
        // TODO: RECORD_AUDIO 권한 체크 — ChatRoomScreen에서 accompanist-permissions로 처리 후 호출
        val started = voiceRecorder.startRecording()
        if (started) {
            _uiState.update { it.copy(isRecording = true, isMediaPanelOpen = false) }
        }
    }

    // ── 음성 녹음 일시정지 ─────────────────────────────────────
    fun onPauseRecording() {
        // TODO: VoiceRecorder.pause() 구현 후 연결
        //       현재 MediaRecorder는 API 24+부터 pause() 지원
        //       if (Build.VERSION.SDK_INT >= 24) recorder.pause()
    }

    // ── 음성 녹음 정지 + 전송 ─────────────────────────────────
    fun onStopRecording() {
        val filePath    = voiceRecorder.stopRecording() ?: run {
            _uiState.update { it.copy(isRecording = false) }
            return
        }
        val durationSec = _uiState.value.recordingSeconds

        val msg = ChatMessage(
            id               = UUID.randomUUID().toString(),
            roomId           = _uiState.value.room.id,
            senderId         = "me",
            senderName       = "나",
            type             = MessageType.VOICE,
            voiceUrl         = filePath,    // 로컬 경로 → Firebase Storage URL로 교체 예정
            voiceDurationSec = durationSec,
            timestamp        = LocalDateTime.now(),
        )
        _uiState.update { state ->
            state.copy(messages = state.messages + msg, isRecording = false)
        }

        // TODO: Firebase Storage 업로드 후 voiceUrl 업데이트
        // viewModelScope.launch {
        //     val url     = firebaseStorageRepo.uploadVoice(filePath)
        //     val updated = msg.copy(voiceUrl = url)
        //     chatRepository.sendFileMessage(roomId, "", File(filePath))
        // }
    }

    // ── 음성 녹음 취소 ─────────────────────────────────────────
    fun onCancelRecording() {
        voiceRecorder.cancelRecording()
        _uiState.update { it.copy(isRecording = false) }
    }

    // ── 음성 재생 / 정지 ──────────────────────────────────────
    fun onPlayVoice(messageId: String) {
        if (_uiState.value.playingMessageId == messageId) {
            voicePlayer.stop()
            return
        }
        val msg = _uiState.value.messages.find { it.id == messageId } ?: return
        val url = msg.voiceUrl.ifEmpty { return }
        // TODO: 로컬 파일 경로(file://) vs 원격 URL(https://) 분기 처리
        voicePlayer.play(messageId, url)
    }

    // ── 번역 토글 (개별 메시지) ───────────────────────────────
    fun onToggleTranslation(messageId: String) {
        val state   = _uiState.value
        val current = state.showTranslation[messageId] ?: state.isAutoTranslate

        if (!current) {
            val msg = state.messages.find { it.id == messageId } ?: return
            if (msg.translatedText.isEmpty()) {
                // translatedText 없음 → 로딩 표시 후 번역 API 호출 (TODO: POST /api/v1/translate)
                _uiState.update { it.copy(translatingMessageIds = it.translatingMessageIds + messageId) }
                viewModelScope.launch {
                    delay(1200) // TODO: 실제 번역 API 응답 대기
                    _uiState.update { it.copy(
                        translatingMessageIds = it.translatingMessageIds - messageId,
                        showTranslation       = it.showTranslation + (messageId to true),
                    )}
                }
                return
            }
        }

        _uiState.update { s ->
            s.copy(showTranslation = s.showTranslation + (messageId to !current))
        }
    }

    // ── 자동번역 전체 토글 ────────────────────────────────────
    fun onToggleAutoTranslate() {
        _uiState.update { it.copy(isAutoTranslate = !it.isAutoTranslate) }
        // TODO: DataStore에 isAutoTranslate 설정 저장 (앱 재시작 후 유지)
    }

    // ── 미디어 패널 토글 ──────────────────────────────────────
    fun onToggleMediaPanel() {
        _uiState.update { it.copy(isMediaPanelOpen = !it.isMediaPanelOpen) }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecorder.release()
        voicePlayer.release()
    }

    // 말풍선 꾹 누르기 → 컨텍스트 메뉴 열기
    fun onLongPressMessage(message: ChatMessage) {
        _uiState.update { it.copy(contextMenuMessage = message) }
    }

    // 컨텍스트 메뉴 닫기
    fun onDismissContextMenu() {
        _uiState.update { it.copy(contextMenuMessage = null) }
    }

    // "저장되었습니다." 토스트 표시
    fun onScriptSaved() {
        _uiState.update { it.copy(savedToastVisible = true) }
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(savedToastVisible = false) }
        }
    }
}
