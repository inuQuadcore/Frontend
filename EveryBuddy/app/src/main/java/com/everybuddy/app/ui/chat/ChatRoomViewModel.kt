package com.everybuddy.app.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.cache.UserSummaryCache
import com.everybuddy.app.data.chat.*
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.local.ChatRoomPreferences
import com.everybuddy.app.data.local.MessageDao
import com.everybuddy.app.data.local.TokenManager
import com.everybuddy.app.data.local.formatRestLocalDateTime
import com.everybuddy.app.data.firebase.ChatMessageListener
import com.everybuddy.app.data.firebase.ViewingManager
import com.everybuddy.app.data.local.ChatMessageEntity
import com.everybuddy.app.data.repository.ChatRoomRepository
import com.everybuddy.app.data.repository.MessageRepository
import com.everybuddy.app.data.repository.TranslateRepository
import com.everybuddy.app.data.repository.translateUserMessage
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val voiceRecorder       : VoiceRecorder,
    private val voicePlayer         : VoicePlayer,
    private val messageRepository   : MessageRepository,
    private val chatRoomRepository  : ChatRoomRepository,
    private val messageDao          : MessageDao,
    private val tokenManager        : TokenManager,
    private val fileMessageUploader : FileMessageUploader,
    private val viewingManager      : ViewingManager,
    private val userSummaryCache    : UserSummaryCache,
    private val translateRepository : TranslateRepository,
    private val chatRoomPreferences : ChatRoomPreferences,
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

    /** 자동번역 watermark — 이 messageId 이상만 자동 번역 대상. 채팅방 진입 시 기존 메시지 제외 + OFF→ON 토글 시 갱신. */
    private var autoTranslateWatermark: Long = -1
    private var autoTranslateInitialized: Boolean = false

    init {
        viewModelScope.launch {
            tokenManager.userId.firstOrNull()?.let { uid ->
                _uiState.update { it.copy(myUserId = uid) }
            }
        }
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
        // 메시지에 등장하는 senderId 집합 변화 감지 → 누락된 UserSummary 병렬 fetch.
        viewModelScope.launch {
            _uiState
                .map { state -> state.messages.mapNotNull { it.senderId.toLongOrNull() }.toSet() }
                .distinctUntilChanged()
                .collect { senderIds -> fetchMissingSummaries(senderIds) }
        }
        // 자동번역 ON 상태에서 새 메시지 도착 시 자동 번역 트리거 (텍스트 + 음성, 상대 메시지만)
        viewModelScope.launch {
            _uiState
                .map { it.messages }
                .distinctUntilChanged()
                .collect { messages -> maybeAutoTranslate(messages) }
        }
    }

    /**
     * 자동번역 watermark 이상의 새 상대 메시지를 백그라운드로 번역.
     * 첫 호출(채팅방 진입 시 Room 캐시 emit)에서는 watermark만 setting하고 skip — 기존 메시지는 대상 X.
     */
    private fun maybeAutoTranslate(messages: List<com.everybuddy.app.data.chat.ChatMessage>) {
        if (!autoTranslateInitialized) {
            autoTranslateWatermark = messages.mapNotNull { it.id.toLongOrNull() }.maxOrNull() ?: -1L
            autoTranslateInitialized = true
            return
        }
        val state = _uiState.value
        if (!state.isAutoTranslate) return
        val myUid = state.myUserId
        messages.forEach { msg ->
            val id = msg.id.toLongOrNull() ?: return@forEach
            if (id <= autoTranslateWatermark) return@forEach
            if (msg.senderId.toLongOrNull() == myUid) return@forEach
            if (msg.translatedText.isNotEmpty()) return@forEach
            if (msg.id in _uiState.value.translatingMessageIds) return@forEach
            val hasContent = when (msg.type) {
                MessageType.TEXT  -> msg.text.isNotBlank()
                MessageType.VOICE -> msg.voiceUrl.isNotBlank()
                else              -> false
            }
            if (!hasContent) return@forEach
            translateMessage(msg.id, autoShow = false)
        }
        messages.mapNotNull { it.id.toLongOrNull() }.maxOrNull()?.let {
            if (it > autoTranslateWatermark) autoTranslateWatermark = it
        }
    }

    private suspend fun fetchMissingSummaries(senderIds: Set<Long>) {
        val current = _uiState.value.userSummaries.keys
        val missing = senderIds - current
        if (missing.isEmpty()) return
        val fetched = coroutineScope {
            missing.map { id -> async { id to userSummaryCache.get(id) } }.awaitAll()
        }.mapNotNull { (id, summary) -> summary?.let { id to it } }.toMap()
        if (fetched.isEmpty()) return
        _uiState.update { state -> state.copy(userSummaries = state.userSummaries + fetched) }
    }

    fun loadRoom(roomId: String, roomName: String = "", isGroup: Boolean = false) {
        // 채팅방 이동/재진입 시 이전 listener 정리
        messageListener?.detach()
        messageListener = null

        val chatRoomId = roomId.toLongOrNull() ?: return   // 잘못된 ID — 무시

        val savedAutoTranslate = chatRoomPreferences.isAutoTranslate(roomId)
        _uiState.update {
            it.copy(
                room            = ChatRoomUi(id = roomId, name = roomName, isGroup = isGroup),
                isAutoTranslate = savedAutoTranslate,
            )
        }

        // Room flow collect → UI state.messages.
        // RTDB가 limitToLast(50)로 캐시 유입을 제한하니 채팅방당 메시지 수 적음 — 전체 load.
        viewModelScope.launch {
            messageDao.observeRoomAll(chatRoomId).collect { entities ->
                val messages = entities.map { it.toChatMessage() }
                _uiState.update { it.copy(messages = messages) }
            }
        }

        // enterChatRoomAt read → REST sync → RTDB listener attach + viewing 등록 → read 표시
        viewModelScope.launch {
            val myUserId = tokenManager.userId.firstOrNull() ?: return@launch
            enterChatRoomAt = readEnterChatRoomAt(myUserId, chatRoomId)
            syncMessagesFromServer(chatRoomId)
            attachMessageListener(chatRoomId, myUserId)
            viewingManager.enter(myUserId, chatRoomId)
            viewingChatRoomId = chatRoomId
            markChatRoomAsRead(chatRoomId)
        }
    }

    private fun attachMessageListener(chatRoomId: Long, myUserId: Long) {
        val listener = ChatMessageListener(
            chatRoomId      = chatRoomId,
            enterChatRoomAt = enterChatRoomAt,
            onUpsert        = { entity ->
                viewModelScope.launch {
                    messageDao.upsert(entity)
                    // 본인이 viewing 중 + 타인 메시지면 즉시 read 표시 (본인 메시지는 unreadCount 영향 없음)
                    if (viewingChatRoomId == chatRoomId && entity.senderId != myUserId) {
                        markChatRoomAsRead(chatRoomId)
                    }
                }
            },
            onRemoved       = { messageId ->
                viewModelScope.launch { messageDao.delete(messageId) }
            },
        )
        listener.attach()
        messageListener = listener
    }

    /** 채팅방의 마지막 SENT 메시지 ID로 POST /messages/{id}/read — RTDB unreadCount=0 reset. */
    private suspend fun markChatRoomAsRead(chatRoomId: Long) {
        val lastMessageId = messageDao.lastMessageId(chatRoomId) ?: return
        messageRepository.readMessage(lastMessageId)
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

        val chatRoomId = _uiState.value.room.id.toLongOrNull() ?: return
        val myUserId   = _uiState.value.myUserId

        _uiState.update { it.copy(inputText = "") }
        viewModelScope.launch {
            when (val result = messageRepository.sendTextMessage(chatRoomId, text)) {
                is ApiResult.Success -> { /* RTDB push로 본인 메시지 도착 */ }
                is ApiResult.Error, is ApiResult.NetworkError -> {
                    insertFailedTextMessage(chatRoomId, myUserId, text)
                }
            }
        }
    }

    /** 송신 실패 메시지를 Room에 FAILED status로 저장. 음수 tempId로 PK 충돌 회피. */
    private suspend fun insertFailedTextMessage(chatRoomId: Long, senderId: Long, text: String) {
        val tempId = -System.currentTimeMillis()
        messageDao.upsert(
            ChatMessageEntity(
                messageId   = tempId,
                chatRoomId  = chatRoomId,
                senderId    = senderId,
                messageType = "TEXT",
                content     = text,
                sendAt      = java.time.LocalDateTime.now(com.everybuddy.app.data.local.KST),
                status      = "FAILED",
            )
        )
    }

    /** FAILED 메시지 재시도. 기존 FAILED row 삭제 후 다시 송신. */
    fun retryMessage(messageId: String) {
        val tempId = messageId.toLongOrNull()?.takeIf { it < 0 } ?: return
        val msg    = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        val chatRoomId = _uiState.value.room.id.toLongOrNull() ?: return

        viewModelScope.launch {
            messageDao.delete(tempId)
            when (val result = messageRepository.sendTextMessage(chatRoomId, msg.text)) {
                is ApiResult.Success -> { /* RTDB로 도착 */ }
                is ApiResult.Error, is ApiResult.NetworkError -> {
                    insertFailedTextMessage(chatRoomId, _uiState.value.myUserId, msg.text)
                }
            }
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

        val chatRoomId = _uiState.value.room.id.toLongOrNull() ?: return

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
        val msg     = state.messages.find { it.id == messageId } ?: return

        // 이미 번역 캐시가 있으면 토글만 (재호출 X)
        if (msg.translatedText.isNotEmpty()) {
            _uiState.update { s -> s.copy(showTranslation = s.showTranslation + (messageId to !current)) }
            return
        }
        // 캐시 없는 상태에서 OFF → OFF 토글은 의미 없음. 표시 X.
        if (current) return

        // 캐시 없음 + ON 전환 → API 호출
        translateMessage(messageId, autoShow = true)
    }

    /**
     * 메시지 번역 API 호출 후 Room 캐시 저장. Room flow가 emit 갱신 → UI translatedText 채워짐.
     * autoShow=true면 번역 완료 시 showTranslation도 true로.
     */
    private fun translateMessage(messageId: String, autoShow: Boolean) {
        val msg = _uiState.value.messages.find { it.id == messageId } ?: return
        val msgIdLong = messageId.toLongOrNull() ?: return
        if (msg.translatedText.isNotEmpty()) return
        if (messageId in _uiState.value.translatingMessageIds) return

        _uiState.update { it.copy(translatingMessageIds = it.translatingMessageIds + messageId) }
        viewModelScope.launch {
            val result: ApiResult<Pair<String, String?>> = when (msg.type) {
                MessageType.TEXT -> {
                    val text = msg.text.ifBlank { return@launch finishTranslating(messageId) }
                    when (val r = translateRepository.translateText(text)) {
                        is ApiResult.Success      -> ApiResult.Success(r.data.translatedText to null)
                        is ApiResult.Error        -> r
                        is ApiResult.NetworkError -> r
                    }
                }
                MessageType.VOICE -> {
                    val url = msg.voiceUrl.ifBlank { return@launch finishTranslating(messageId) }
                    when (val r = translateRepository.translateSpeechFromUrl(url)) {
                        is ApiResult.Success      -> ApiResult.Success(r.data.translatedText to r.data.sourceText)
                        is ApiResult.Error        -> r
                        is ApiResult.NetworkError -> r
                    }
                }
                else -> return@launch finishTranslating(messageId)
            }
            when (result) {
                is ApiResult.Success -> {
                    val (translated, source) = result.data
                    messageDao.updateTranslation(msgIdLong, translated, source)
                    _uiState.update {
                        it.copy(
                            translatingMessageIds = it.translatingMessageIds - messageId,
                            showTranslation = if (autoShow) it.showTranslation + (messageId to true) else it.showTranslation,
                        )
                    }
                }
                is ApiResult.Error, is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            translatingMessageIds = it.translatingMessageIds - messageId,
                            translationError      = result.translateUserMessage(),
                        )
                    }
                }
            }
        }
    }

    private fun finishTranslating(messageId: String) {
        _uiState.update { it.copy(translatingMessageIds = it.translatingMessageIds - messageId) }
    }

    fun consumeTranslationError() {
        _uiState.update { it.copy(translationError = null) }
    }

    fun onToggleAutoTranslate() {
        val turningOn = !_uiState.value.isAutoTranslate
        if (turningOn) {
            // OFF→ON 토글 시 watermark를 현재 최신 messageId로 갱신 — 기존 메시지는 자동 번역 대상 X
            autoTranslateWatermark = _uiState.value.messages.mapNotNull { it.id.toLongOrNull() }.maxOrNull()
                ?: autoTranslateWatermark
        }
        _uiState.update { it.copy(isAutoTranslate = turningOn) }
        // 채팅방 단위 영속화 — 다음 진입 시 복원
        _uiState.value.room.id.takeIf { it.isNotEmpty() }?.let { roomId ->
            chatRoomPreferences.setAutoTranslate(roomId, turningOn)
        }
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
        _uiState.update { it.copy(isMediaPanelOpen = false) }
    }

    fun onFilePicked(uri: String) {
        _uiState.update { it.copy(isMediaPanelOpen = false) }

        val chatRoomId = _uiState.value.room.id.toLongOrNull() ?: return

        viewModelScope.launch {
            fileMessageUploader.upload(chatRoomId, Uri.parse(uri))
            // 결과 토스트/재시도는 C21에서.
        }
    }

    /** 시스템 PhotoPicker 결과 — Uri 목록을 받아 각각 업로드. */
    fun onPhotosPicked(uris: List<Uri>) {
        _uiState.update { it.copy(isMediaPanelOpen = false, isPhotoPickerOpen = false) }

        val chatRoomId = _uiState.value.room.id.toLongOrNull() ?: return
        if (uris.isEmpty()) return

        viewModelScope.launch {
            uris.forEach { uri -> fileMessageUploader.upload(chatRoomId, uri) }
        }
    }

    fun onToggleMuteRoom() {
        _uiState.update { it.copy(room = it.room.copy(isMuted = !it.room.isMuted)) }
    }

    /** 멤버 초대 — 그룹방만. 1:1방은 UI에서 초대 버튼 자체를 숨기므로 호출 경로 없음 + 백엔드 403 거부도 받음. */
    fun inviteMembers(participantIds: List<Long>) {
        val room = _uiState.value.room
        if (!room.isGroup) return
        val chatRoomId = room.id.toLongOrNull() ?: return
        if (participantIds.isEmpty()) return
        viewModelScope.launch {
            chatRoomRepository.inviteParticipants(chatRoomId, participantIds)
            // 성공 시 새 멤버는 RTDB users/{me}/chatrooms/{roomId} 노드로 자동 노출.
        }
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
