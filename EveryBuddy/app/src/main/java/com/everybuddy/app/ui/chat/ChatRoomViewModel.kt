package com.everybuddy.app.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.BuildConfig
import com.everybuddy.app.data.chat.*
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.local.MessageDao
import com.everybuddy.app.data.local.TokenManager
import com.everybuddy.app.data.local.formatRestLocalDateTime
import com.everybuddy.app.data.firebase.ChatMessageListener
import com.everybuddy.app.data.firebase.ViewingManager
import com.everybuddy.app.data.repository.MessageRepository
import com.everybuddy.app.ui.friend.FriendDemoData
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val voiceRecorder       : VoiceRecorder,
    private val voicePlayer         : VoicePlayer,
    private val messageRepository   : MessageRepository,
    private val messageDao          : MessageDao,
    private val tokenManager        : TokenManager,
    private val fileMessageUploader : FileMessageUploader,
    private val viewingManager      : ViewingManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatRoomUiState())
    val uiState: StateFlow<ChatRoomUiState> = _uiState.asStateFlow()

    /** RTDB messages listener 시작점. 본인 입장 이전 메시지 격리용. */
    private var enterChatRoomAt: Long = 0L
    private val rtdb = FirebaseDatabase.getInstance()

    /** 현재 구독 중인 메시지 listener. 채팅방 이동/onCleared 시 detach. */
    private var messageListener: ChatMessageListener? = null

    /** 현재 viewing 중인 채팅방 ID. onCleared에서 leave 호출용. */
    private var viewingChatRoomId: Long? = null

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

    fun loadRoom(roomId: String, roomName: String = "") {
        // 채팅방 이동/재진입 시 이전 listener 정리
        messageListener?.detach()
        messageListener = null

        // friend dummy 답장 흐름 — C13/C14에서 통째 제거 예정. 일단 호환성 유지.
        if (BuildConfig.USE_DUMMY_DATA && roomId.startsWith("reply_")) {
            val friendId = roomId.removePrefix("reply_")
            val demoRoom = FriendDemoData.chatRooms.find { it.friendId == friendId }
            val room     = ChatRoomUi(id = roomId, name = demoRoom?.friendName ?: friendId)
            val messages = demoRoom?.messages?.mapIndexed { i, msg ->
                ChatMessage(
                    id         = "reply_msg_$i",
                    roomId     = roomId,
                    senderId   = if (msg.isMine) "me" else friendId,
                    senderName = if (msg.isMine) "나" else (demoRoom.friendName),
                    type       = MessageType.TEXT,
                    text       = msg.text,
                    isStatusReply  = msg.isStatusReply,
                    statusPreview  = msg.originalStatusPreview,
                    timestamp  = LocalDateTime.now().minusMinutes((demoRoom.messages.size - i).toLong()),
                )
            } ?: emptyList()
            _uiState.update { it.copy(room = room, messages = messages) }
            return
        }

        val chatRoomId = roomId.toLongOrNull()
        // toLong 실패 = dummy ID (예: "r01"). USE_DUMMY_DATA 분기 처리.
        if (chatRoomId == null) {
            if (BuildConfig.USE_DUMMY_DATA) {
                val dummyRoom = dummyChatRooms.find { it.id == roomId }
                _uiState.update {
                    it.copy(
                        room     = dummyRoom ?: ChatRoomUi(id = roomId, name = roomName),
                        messages = dummyMessages[roomId] ?: emptyList(),
                    )
                }
            }
            return
        }

        _uiState.update { it.copy(room = ChatRoomUi(id = roomId, name = roomName)) }

        // Room flow collect → UI state.messages
        viewModelScope.launch {
            messageDao.observeRoom(chatRoomId, limit = 50, offset = 0).collect { entities ->
                val messages = entities.map { it.toChatMessage() }
                _uiState.update { it.copy(messages = messages) }
            }
        }

        // enterChatRoomAt read → REST sync → RTDB listener attach + viewing 등록
        viewModelScope.launch {
            val myUserId = tokenManager.userId.firstOrNull() ?: return@launch
            enterChatRoomAt = readEnterChatRoomAt(myUserId, chatRoomId)
            syncMessagesFromServer(chatRoomId)
            attachMessageListener(chatRoomId)
            viewingManager.enter(myUserId, chatRoomId)
            viewingChatRoomId = chatRoomId
        }
    }

    private fun attachMessageListener(chatRoomId: Long) {
        val listener = ChatMessageListener(
            chatRoomId      = chatRoomId,
            enterChatRoomAt = enterChatRoomAt,
            onUpsert        = { entity ->
                viewModelScope.launch { messageDao.upsert(entity) }
            },
            onRemoved       = { messageId ->
                viewModelScope.launch { messageDao.delete(messageId) }
            },
        )
        listener.attach()
        messageListener = listener
    }

    /** RTDB users/{me}/chatrooms/{roomId}/enterChatRoomAt 단건 read (epoch ms). 실패 시 0. */
    private suspend fun readEnterChatRoomAt(myUserId: Long, chatRoomId: Long): Long {
        return try {
            val snap = rtdb.getReference("users/$myUserId/chatrooms/$chatRoomId/enterChatRoomAt")
                .get()
                .await()
            snap.getValue(Long::class.java) ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /** GET /messages/chatrooms/{roomId}?since= 호출 + Room 동기화. 백엔드가 enterChatRoomAt 자동 필터. */
    private suspend fun syncMessagesFromServer(chatRoomId: Long) {
        val since = messageDao.lastSentAt(chatRoomId)?.let(::formatRestLocalDateTime)
        when (val result = messageRepository.syncMessages(chatRoomId, since)) {
            is ApiResult.Success -> {
                val data = result.data ?: return
                val toUpsert = (data.newMessages + data.updatedMessages)
                    .map { it.toChatMessageEntity(chatRoomId) }
                if (toUpsert.isNotEmpty()) messageDao.upsertAll(toUpsert)
                if (data.deletedIds.isNotEmpty()) messageDao.deleteAll(data.deletedIds)
            }
            else -> { /* 에러는 silent fail — Room의 옛 메시지만 표시 */ }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onSendText() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val chatRoomId = _uiState.value.room.id.toLongOrNull()
        if (chatRoomId == null) {
            // dummy 채팅방 (USE_DUMMY_DATA 흐름): 기존처럼 로컬 메시지 표시
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
            return
        }

        // 입력창 즉시 비우고 송신. 본인 메시지 화면 표시는 RTDB 도착(C10) 또는 sync 재호출 때.
        _uiState.update { it.copy(inputText = "") }
        viewModelScope.launch {
            messageRepository.sendTextMessage(chatRoomId, text)
            // 성공/실패 처리는 C21 (PENDING/재시도)에서 본격화. 지금은 silent fail.
        }
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
        val filePath = voiceRecorder.stopRecording() ?: run {
            _uiState.update { it.copy(isRecording = false) }
            return
        }
        _uiState.update { it.copy(isRecording = false, isRecordingPaused = false) }

        val chatRoomId = _uiState.value.room.id.toLongOrNull()
        if (chatRoomId == null) {
            // dummy 채팅방: 기존 로컬 메시지 흐름 유지
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
            _uiState.update { state -> state.copy(messages = state.messages + msg) }
            return
        }

        viewModelScope.launch {
            // VoiceRecorder는 MPEG_4 컨테이너 + AAC 인코딩 (.m4a)
            fileMessageUploader.upload(chatRoomId, File(filePath), mimeType = "audio/mp4")
        }
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

    fun onDeleteMessage(messageId: String) {
        _uiState.update { it.copy(contextMenuMessage = null) }

        val msgIdLong = messageId.toLongOrNull()
        if (msgIdLong == null) {
            // dummy 메시지: 기존 로컬 제거
            _uiState.update { it.copy(messages = it.messages.filter { m -> m.id != messageId }) }
            return
        }

        viewModelScope.launch {
            messageRepository.deleteMessage(msgIdLong)
            // 실제 Room 제거는 RTDB onChildRemoved (C10) 또는 다음 sync에 반영.
            // 백엔드 에러(403/409)는 C21에서 토스트.
        }
    }

    fun onEditMessage(messageId: String, newText: String) {
        _uiState.update { it.copy(contextMenuMessage = null) }

        val msgIdLong = messageId.toLongOrNull()
        if (msgIdLong == null) {
            // dummy 메시지: 기존 로컬 갱신
            _uiState.update { state ->
                state.copy(messages = state.messages.map { if (it.id == messageId) it.copy(text = newText) else it })
            }
            return
        }

        viewModelScope.launch {
            messageRepository.editMessage(msgIdLong, newText)
            // 응답 Message는 editedAt 포함. Room 갱신은 RTDB onChildChanged 또는 다음 sync에 반영.
        }
    }

    fun onCameraCapture() {
        val msg = ChatMessage(
            id         = UUID.randomUUID().toString(),
            roomId     = _uiState.value.room.id,
            senderId   = "me",
            senderName = "나",
            type       = MessageType.IMAGE,
            text       = "[카메라 사진]",
            timestamp  = LocalDateTime.now(),
        )
        _uiState.update { it.copy(messages = it.messages + msg, isMediaPanelOpen = false) }
    }

    fun onFilePicked(uri: String) {
        _uiState.update { it.copy(isMediaPanelOpen = false) }

        val chatRoomId = _uiState.value.room.id.toLongOrNull()
        if (chatRoomId == null) {
            // dummy 채팅방: 기존 로컬 메시지 흐름 유지
            val msg = ChatMessage(
                id         = UUID.randomUUID().toString(),
                roomId     = _uiState.value.room.id,
                senderId   = "me",
                senderName = "나",
                type       = MessageType.IMAGE,
                text       = "[파일] ${uri.substringAfterLast('/')}",
                voiceUrl   = uri,
                timestamp  = LocalDateTime.now(),
            )
            _uiState.update { it.copy(messages = it.messages + msg) }
            return
        }

        viewModelScope.launch {
            fileMessageUploader.upload(chatRoomId, Uri.parse(uri))
            // 결과 토스트/재시도는 C21에서.
        }
    }

    fun onToggleMuteRoom() {
        _uiState.update { it.copy(room = it.room.copy(isMuted = !it.room.isMuted)) }
    }

    override fun onCleared() {
        super.onCleared()
        messageListener?.detach()
        messageListener = null
        viewingChatRoomId?.let { viewingManager.leave(it) }
        viewingChatRoomId = null
        voiceRecorder.release()
        voicePlayer.release()
    }
}
