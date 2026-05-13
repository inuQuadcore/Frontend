package com.everybuddy.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.BuildConfig
import com.everybuddy.app.data.chat.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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

    fun loadRoom(roomId: String) {
        val dummyRoom = if (BuildConfig.DEBUG) dummyChatRooms.find { it.id == roomId } else null
        val room      = dummyRoom ?: ChatRoom(id = roomId)
        val messages  = if (BuildConfig.DEBUG) dummyMessages[roomId] ?: emptyList() else emptyList()
        _uiState.update { it.copy(room = room, messages = messages) }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

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
        _uiState.update { state -> state.copy(messages = state.messages + msg, inputText = "") }
    }

    fun onStartRecording() {
        val started = voiceRecorder.startRecording()
        if (started) _uiState.update { it.copy(isRecording = true, isMediaPanelOpen = false) }
    }

    fun onPauseRecording() {
        val paused = _uiState.value.isRecordingPaused
        if (paused) { voiceRecorder.resumeRecording(); _uiState.update { it.copy(isRecordingPaused = false) } }
        else        { voiceRecorder.pauseRecording();  _uiState.update { it.copy(isRecordingPaused = true)  } }
    }

    fun onStopRecording() {
        val filePath    = voiceRecorder.stopRecording() ?: run { _uiState.update { it.copy(isRecording = false) }; return }
        val durationSec = _uiState.value.recordingSeconds
        val msg = ChatMessage(
            id               = UUID.randomUUID().toString(),
            roomId           = _uiState.value.room.id,
            senderId         = "me",
            senderName       = "나",
            type             = MessageType.VOICE,
            voiceUrl         = filePath,
            voiceDurationSec = durationSec,
            timestamp        = LocalDateTime.now(),
        )
        _uiState.update { state -> state.copy(messages = state.messages + msg, isRecording = false, isRecordingPaused = false) }
    }

    fun onCancelRecording() {
        voiceRecorder.cancelRecording()
        _uiState.update { it.copy(isRecording = false, isRecordingPaused = false) }
    }

    fun onPlayVoice(messageId: String) {
        if (_uiState.value.playingMessageId == messageId) { voicePlayer.stop(); return }
        val msg = _uiState.value.messages.find { it.id == messageId } ?: return
        val url = msg.voiceUrl.ifEmpty { return }
        voicePlayer.play(messageId, url)
    }

    fun onToggleTranslation(messageId: String) {
        val state   = _uiState.value
        val current = state.showTranslation[messageId] ?: state.isAutoTranslate
        if (!current) {
            val msg = state.messages.find { it.id == messageId } ?: return
            if (msg.translatedText.isEmpty()) {
                _uiState.update { it.copy(translatingMessageIds = it.translatingMessageIds + messageId) }
                viewModelScope.launch {
                    delay(1200)
                    _uiState.update { it.copy(
                        translatingMessageIds = it.translatingMessageIds - messageId,
                        showTranslation       = it.showTranslation + (messageId to true),
                    )}
                }
                return
            }
        }
        _uiState.update { s -> s.copy(showTranslation = s.showTranslation + (messageId to !current)) }
    }

    fun onToggleAutoTranslate() {
        _uiState.update { it.copy(isAutoTranslate = !it.isAutoTranslate) }
    }

    fun onToggleMediaPanel() {
        _uiState.update { it.copy(isMediaPanelOpen = !it.isMediaPanelOpen, isPhotoPickerOpen = false) }
    }

    fun onOpenPhotoPicker() {
        _uiState.update { it.copy(isPhotoPickerOpen = true, isMediaPanelOpen = false) }
    }

    fun onClosePhotoPicker() {
        _uiState.update { it.copy(isPhotoPickerOpen = false) }
    }

    // 선택된 사진 인덱스 토글 (최대 10개)
    fun onTogglePhotoSelection(index: Int) {
        val current = _uiState.value.selectedPhotoIndices
        val updated = if (current.contains(index)) {
            current - index
        } else if (current.size < 10) {
            current + index
        } else {
            current  // 10개 제한 초과 → 무시
        }
        _uiState.update { it.copy(selectedPhotoIndices = updated) }
    }

    fun onSendSelectedPhotos() {
        val selected = _uiState.value.selectedPhotoIndices
        if (selected.isEmpty()) return
        // TODO: 실제 갤러리 URI → Firebase Storage 업로드 후 IMAGE 메시지 전송
        _uiState.update { it.copy(isPhotoPickerOpen = false, selectedPhotoIndices = emptySet()) }
    }

    fun onLongPressMessage(message: ChatMessage) {
        _uiState.update { it.copy(contextMenuMessage = message) }
    }

    fun onDismissContextMenu() {
        _uiState.update { it.copy(contextMenuMessage = null) }
    }

    fun onStartScriptSave(messageId: String) {
        val msg = _uiState.value.messages.find { it.id == messageId } ?: return
        _uiState.update { it.copy(scriptSaveMessage = msg, contextMenuMessage = null) }
    }

    fun onDismissScriptSave() {
        _uiState.update { it.copy(scriptSaveMessage = null) }
    }

    fun onScriptSaved() {
        _uiState.update { it.copy(scriptSaveMessage = null, savedToastVisible = true) }
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(savedToastVisible = false) }
        }
    }

    fun onOpenConversationSelect() {
        _uiState.update { it.copy(isConversationSelectOpen = true, isMediaPanelOpen = false) }
    }

    fun onDismissConversationSelect() {
        _uiState.update { it.copy(isConversationSelectOpen = false) }
    }

    fun onConversationSelected(selectedMessages: List<ChatMessage>, captureOption: CaptureOption) {
        _uiState.update { it.copy(
            isConversationSelectOpen  = false,
            conversationSaveMessages  = selectedMessages,
            conversationCaptureOption = captureOption,
        )}
    }

    fun onDismissConversationSave() {
        _uiState.update { it.copy(conversationSaveMessages = emptyList(), conversationCaptureOption = CaptureOption.COMBINED) }
    }

    fun onConversationSaved() {
        _uiState.update { it.copy(
            conversationSaveMessages  = emptyList(),
            conversationCaptureOption = CaptureOption.COMBINED,
            savedToastVisible         = true,
        )}
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(savedToastVisible = false) }
        }
    }

    fun onToggleMuteRoom() {
        _uiState.update { it.copy(room = it.room.copy(isMuted = !it.room.isMuted)) }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecorder.release()
        voicePlayer.release()
    }
}
