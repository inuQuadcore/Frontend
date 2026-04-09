package com.everybuddy.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.chat.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
        // 녹음 타이머 + 진폭 구독
        viewModelScope.launch {
            voiceRecorder.seconds.collect { sec ->
                _uiState.update { it.copy(recordingSeconds = sec) }
            }
        }
        viewModelScope.launch {
            voiceRecorder.amplitudes.collect { amps ->
                _uiState.update { it.copy(recordingAmplitudes = amps) }
            }
        }
        // 재생 상태 구독
        viewModelScope.launch {
            voicePlayer.state.collect { playerState ->
                when (playerState) {
                    is PlayerState.Playing  -> _uiState.update { it.copy(playingMessageId = playerState.messageId) }
                    is PlayerState.Finished,
                    is PlayerState.Idle,
                    is PlayerState.Paused   -> _uiState.update { it.copy(playingMessageId = null) }
                }
            }
        }
    }

    // ── 방 로드 ───────────────────────────────
    fun loadRoom(roomId: String) {
        val room     = dummyChatRooms.find { it.id == roomId } ?: return
        val messages = dummyMessages[roomId] ?: emptyList()
        _uiState.update { it.copy(room = room, messages = messages) }
    }

    // ── 텍스트 입력 ───────────────────────────
    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    // ── 텍스트 전송 ───────────────────────────
    fun onSendText() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val msg = ChatMessage(
            id        = UUID.randomUUID().toString(),
            roomId    = _uiState.value.room.id,
            senderId  = "me",
            senderName = "나",
            type      = MessageType.TEXT,
            text      = text,
            timestamp = LocalDateTime.now(),
        )
        _uiState.update { state ->
            state.copy(
                messages  = state.messages + msg,
                inputText = "",
            )
        }

        // TODO: Firebase Realtime Database에 저장
        // viewModelScope.launch { firebaseRepo.sendMessage(msg) }
    }

    // ── 음성 녹음 시작 ────────────────────────
    fun onStartRecording() {
        val started = voiceRecorder.startRecording()
        if (started) {
            _uiState.update { it.copy(isRecording = true, isMediaPanelOpen = false) }
        }
    }

    // ── 음성 녹음 정지 + 전송 ─────────────────
    fun onStopRecording() {
        val filePath = voiceRecorder.stopRecording() ?: run {
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
            voiceUrl         = filePath,    // 로컬 경로 → 나중에 Firebase Storage URL로 교체
            voiceDurationSec = durationSec,
            timestamp        = LocalDateTime.now(),
        )

        _uiState.update { state ->
            state.copy(
                messages    = state.messages + msg,
                isRecording = false,
            )
        }

        // TODO: Firebase Storage에 업로드 후 URL 업데이트
        // viewModelScope.launch {
        //     val url = firebaseStorageRepo.uploadVoice(filePath)
        //     val updated = msg.copy(voiceUrl = url)
        //     firebaseRepo.sendMessage(updated)
        // }
    }

    // ── 음성 녹음 취소 ────────────────────────
    fun onCancelRecording() {
        voiceRecorder.cancelRecording()
        _uiState.update { it.copy(isRecording = false) }
    }

    // ── 음성 재생 ─────────────────────────────
    fun onPlayVoice(messageId: String) {
        val current = _uiState.value.playingMessageId
        if (current == messageId) {
            // 같은 메시지 → 정지
            voicePlayer.stop()
            return
        }
        val msg = _uiState.value.messages.find { it.id == messageId } ?: return
        val url = msg.voiceUrl.ifEmpty { return } // TODO: 실제 URL 필요
        voicePlayer.play(messageId, url)
    }

    // ── 번역 토글 ─────────────────────────────
    fun onToggleTranslation(messageId: String) {
        _uiState.update { state ->
            val current = state.showTranslation[messageId] ?: state.isAutoTranslate
            state.copy(
                showTranslation = state.showTranslation + (messageId to !current)
            )
        }
    }

    // ── 미디어 패널 토글 ──────────────────────
    fun onToggleMediaPanel() {
        _uiState.update { it.copy(isMediaPanelOpen = !it.isMediaPanelOpen) }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecorder.release()
        voicePlayer.release()
    }
}
