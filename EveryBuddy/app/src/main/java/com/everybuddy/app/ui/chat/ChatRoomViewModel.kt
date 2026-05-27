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
import com.everybuddy.app.data.repository.videoTranslateUserMessage
import com.everybuddy.app.di.ApplicationScope
import android.media.MediaMetadataRetriever
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
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
    private val mediaFileStore      : MediaFileStore,
    @ApplicationScope private val appScope: CoroutineScope,
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

    /** 방금 녹음 완료한 음성의 길이(초). 해당 메시지가 RTDB에서 도착하면 적용 후 0으로 리셋. */
    private var pendingVoiceDurationSec: Int = 0

    /** ExoPlayer STATE_READY 시 추출한 재생 길이 캐시 (messageId → sec). 세션 단위 인메모리. */
    private val voiceDurationCache = mutableMapOf<String, Int>()

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
                    is PlayerState.Paused   -> _uiState.update { it.copy(playingMessageId = null, playPositionMs = 0L) }
                }
            }
        }
        viewModelScope.launch {
            voicePlayer.positionMs.collect { ms ->
                _uiState.update { it.copy(playPositionMs = ms) }
            }
        }
        viewModelScope.launch {
            voicePlayer.durationReady.collect { pair ->
                if (pair == null) return@collect
                val (msgId, sec) = pair
                if (sec > 0 && voiceDurationCache[msgId] != sec) {
                    voiceDurationCache[msgId] = sec
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == msgId && msg.voiceDurationSec == 0) msg.copy(voiceDurationSec = sec) else msg
                            }
                        )
                    }
                    msgId.toLongOrNull()?.let { id -> messageDao.updateVoiceDuration(id, sec) }
                }
            }
        }
        // 메시지 senderId + 참여자 ID 합집합 변화 감지 → 누락된 UserSummary 병렬 fetch.
        viewModelScope.launch {
            _uiState
                .map { state ->
                    val senderIds      = state.messages.mapNotNull { it.senderId.toLongOrNull() }.toSet()
                    val participantIds = state.room.participants.map { it.id }.toSet()
                    senderIds + participantIds
                }
                .distinctUntilChanged()
                .collect { ids -> fetchMissingSummaries(ids) }
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
        val myUid = state.myUserId
        if (state.isAutoTranslate) {
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
        }
        // isAutoTranslate와 무관하게 watermark는 항상 갱신 — OFF 중 도착한 메시지도 추적해야 ON 전환 후 watermark가 정확함
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
        autoTranslateWatermark = -1
        autoTranslateInitialized = false

        val chatRoomId = roomId.toLongOrNull() ?: return   // 잘못된 ID — 무시

        val savedAutoTranslate  = chatRoomPreferences.isAutoTranslate(roomId)
        val savedShowTranslation = chatRoomPreferences.getShowTranslation(roomId)
        val savedReplyText = chatRoomPreferences.getSavedReplyText(roomId)
        val savedReplyId   = chatRoomPreferences.getSavedReplyId(roomId)
        val savedReply = if (savedReplyText != null && savedReplyId != null) {
            ChatMessage(id = savedReplyId, text = savedReplyText)
        } else null
        _uiState.update {
            it.copy(
                room            = ChatRoomUi(id = roomId, name = roomName, isGroup = isGroup),
                isAutoTranslate = savedAutoTranslate,
                showTranslation = savedShowTranslation,
                replyToMessage  = savedReply,
            )
        }

        // Room flow collect → UI state.messages.
        // RTDB가 limitToLast(50)로 캐시 유입을 제한하니 채팅방당 메시지 수 적음 — 전체 load.
        viewModelScope.launch {
            messageDao.observeRoomAll(chatRoomId).collect { entities ->
                var messages = entities.map { it.toChatMessage() }
                val myId = _uiState.value.myUserId
                if (pendingVoiceDurationSec > 0 && myId != null) {
                    val idx = messages.indexOfLast { it.type == MessageType.VOICE && it.senderId.toLongOrNull() == myId && it.voiceDurationSec == 0 }
                    if (idx >= 0) {
                        messages = messages.toMutableList().also { it[idx] = it[idx].copy(voiceDurationSec = pendingVoiceDurationSec) }
                        pendingVoiceDurationSec = 0
                    }
                }
                // 인메모리 캐시에서 음성 길이 패치
                if (voiceDurationCache.isNotEmpty()) {
                    messages = messages.map { msg ->
                        val cached = voiceDurationCache[msg.id]
                        if (msg.type == MessageType.VOICE && cached != null && msg.voiceDurationSec == 0) msg.copy(voiceDurationSec = cached) else msg
                    }
                }
                _uiState.update { it.copy(messages = messages) }
            }
        }

        // enterChatRoomAt read → REST sync → RTDB listener attach + viewing 등록 → read 표시
        viewModelScope.launch {
            val myUserId = tokenManager.userId.firstOrNull() ?: return@launch
            enterChatRoomAt = readEnterChatRoomAt(myUserId, chatRoomId)
            syncMessagesFromServer(chatRoomId)
            attachMessageListener(chatRoomId)
            viewingManager.enter(myUserId, chatRoomId)
            viewingChatRoomId = chatRoomId
            markChatRoomAsRead(chatRoomId)
        }

        loadParticipants(chatRoomId)
    }

    private fun loadParticipants(chatRoomId: Long) {
        viewModelScope.launch {
            val myUserId = tokenManager.userId.firstOrNull() ?: return@launch
            when (val result = chatRoomRepository.getChatRooms()) {
                is ApiResult.Success -> {
                    val room = result.data.firstOrNull { it.chatRoomId == chatRoomId } ?: return@launch
                    val participantUis = room.participants.map { p ->
                        com.everybuddy.app.data.chat.ChatParticipantUi(
                            id              = p.userId,
                            profileImageUrl = p.profileImageUrl,
                        )
                    }
                    _uiState.update { state ->
                        state.copy(room = state.room.copy(participants = participantUis))
                    }
                }
                else -> { /* silent fail */ }
            }
        }
    }

    private fun attachMessageListener(chatRoomId: Long) {
        val listener = ChatMessageListener(
            chatRoomId      = chatRoomId,
            enterChatRoomAt = enterChatRoomAt,
            onUpsert        = { entity ->
                viewModelScope.launch { messageDao.upsertPreservingClientFields(entity) }
            },
            onRemoved       = { messageId ->
                viewModelScope.launch { messageDao.delete(messageId) }
            },
        )
        listener.attach()
        messageListener = listener
    }

    private var lastMarkedReadId: Long = -1L

    private suspend fun markChatRoomAsRead(chatRoomId: Long) {
        val last = messageDao.lastMessageWithSender(chatRoomId) ?: return
        if (last.messageId == lastMarkedReadId) return
        val myUserId = _uiState.value.myUserId
        if (myUserId != 0L && last.senderId == myUserId) return
        messageRepository.readMessage(last.messageId)
        lastMarkedReadId = last.messageId
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
                if (toUpsert.isNotEmpty()) messageDao.upsertAllPreservingClientFields(toUpsert)
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

        val chatRoomId  = _uiState.value.room.id.toLongOrNull() ?: return
        val myUserId    = _uiState.value.myUserId
        val replyMsg = _uiState.value.replyToMessage
        val statusPreview = replyMsg?.let { msg ->
            val raw = msg.text.ifBlank { "[음성 메시지]" }
            if (raw.length > 20) raw.take(20) + "…" else raw
        }
        chatRoomPreferences.clearReply(_uiState.value.room.id)
        _uiState.update { it.copy(inputText = "", replyToMessage = null) }
        viewModelScope.launch {
            when (val result = messageRepository.sendTextMessage(chatRoomId, text, statusPreview)) {
                is ApiResult.Success -> { /* RTDB push로 본인 메시지 도착 */ }
                is ApiResult.Error, is ApiResult.NetworkError -> {
                    insertFailedTextMessage(chatRoomId, myUserId, text)
                }
            }
        }
    }

    fun startReply(msg: ChatMessage) {
        val roomId = _uiState.value.room.id
        chatRoomPreferences.saveReply(roomId, msg.id, msg.text)
        _uiState.update { it.copy(replyToMessage = msg, contextMenuMessage = null) }
    }

    fun cancelReply() {
        chatRoomPreferences.clearReply(_uiState.value.room.id)
        _uiState.update { it.copy(replyToMessage = null) }
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

        pendingVoiceDurationSec = try {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(filePath)
                val ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                (ms / 1000).toInt()
            }
        } catch (_: Exception) { 0 }

        viewModelScope.launch {
            // VoiceRecorder는 MPEG_4 컨테이너 + AAC 인코딩 (.m4a)
            fileMessageUploader.upload(chatRoomId, File(filePath), mimeType = "audio/mp4")
        }
    }

    fun onCancelRecording() {
        voiceRecorder.cancelRecording()
        _uiState.update { it.copy(isRecording = false, isRecordingPaused = false) }
    }

    /**
     * 이미지 메시지가 화면에 처음 노출될 때 호출 — 백그라운드로 영속화.
     * 이미 localFilePath 있거나 fileUrl 비어있으면 noop. Coil의 디스크 캐시와 별도로
     * 우리 filesDir에 영구 저장해 presigned 만료/오프라인 대응.
     */
    fun onImageAppeared(messageId: String) {
        val msg = _uiState.value.messages.find { it.id == messageId } ?: return
        if (!msg.localFilePath.isNullOrBlank() && File(msg.localFilePath).exists()) return
        val url       = msg.voiceUrl.ifBlank { return }   // ChatMessage.voiceUrl == entity.fileUrl
        val msgIdLong = messageId.toLongOrNull() ?: return
        viewModelScope.launch {
            val ext  = mediaFileStore.extFromUrlOrName(url, fileName = null, fallback = "jpg")
            if (mediaFileStore.exists(msgIdLong, ext)) {
                messageDao.updateLocalFilePath(msgIdLong, mediaFileStore.pathFor(msgIdLong, ext).absolutePath)
                return@launch
            }
            mediaFileStore.downloadAndPersist(url, msgIdLong, ext)?.let { file ->
                messageDao.updateLocalFilePath(msgIdLong, file.absolutePath)
            }
        }
    }

    /** 이미지 탭 — 풀스크린 뷰어 오픈. localFilePath 우선, 없으면 fileUrl. */
    fun onTapImage(messageId: String) {
        val msg = _uiState.value.messages.find { it.id == messageId } ?: return
        val target = msg.localFilePath?.takeIf { File(it).exists() } ?: msg.voiceUrl.ifBlank { return }
        _uiState.update { it.copy(fullscreenImage = target) }
    }

    fun onCloseFullscreenImage() {
        _uiState.update { it.copy(fullscreenImage = null) }
    }

    fun onOpenVideoPlayer(messageId: String) {
        _uiState.update { it.copy(videoPlayerMessageId = messageId) }
        loadVideoSubtitles(messageId)
    }

    fun onCloseVideoPlayer() {
        _uiState.update { it.copy(videoPlayerMessageId = null) }
    }

    private fun loadVideoSubtitles(messageId: String) {
        if (_uiState.value.videoSubtitles.containsKey(messageId)) return
        if (messageId in _uiState.value.subtitleLoadingIds) return
        val message = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        _uiState.update { it.copy(subtitleLoadingIds = it.subtitleLoadingIds + messageId) }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val file = resolveVideoFileForTranslation(message)
            if (file == null) {
                _uiState.update { state ->
                    state.copy(
                        subtitleLoadingIds = state.subtitleLoadingIds - messageId,
                        translationError   = "영상 파일을 불러올 수 없습니다.",
                    )
                }
                return@launch
            }

            try {
                translateRepository.translateVideoStream(file).collect { segment ->
                    if (segment.error != null) {
                        _uiState.update { state ->
                            state.copy(
                                subtitleLoadingIds = state.subtitleLoadingIds - messageId,
                                subtitleProgress   = state.subtitleProgress - messageId,
                                translationError   = segment.error,
                            )
                        }
                        return@collect
                    }
                    val newSub = VideoSubtitle(
                        startMs    = (segment.startSeconds * 1000).toLong(),
                        endMs      = (segment.endSeconds * 1000).toLong(),
                        original   = segment.sourceText,
                        translated = segment.translatedText,
                    )
                    _uiState.update { state ->
                        state.copy(
                            videoSubtitles     = state.videoSubtitles +
                                (messageId to (state.videoSubtitles[messageId].orEmpty() + newSub)),
                            subtitleProgress   = when {
                                segment.isFinal                -> state.subtitleProgress - messageId
                                segment.totalSegments > 0     -> state.subtitleProgress +
                                    (messageId to (segment.index to segment.totalSegments))
                                else                           -> state.subtitleProgress
                            },
                            subtitleLoadingIds = if (segment.isFinal)
                                state.subtitleLoadingIds - messageId
                            else
                                state.subtitleLoadingIds,
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { state ->
                    state.copy(
                        subtitleLoadingIds = state.subtitleLoadingIds - messageId,
                        subtitleProgress   = state.subtitleProgress - messageId,
                        translationError   = "번역 연결이 끊어졌습니다.",
                    )
                }
            }
        }
    }

    private suspend fun resolveVideoFileForTranslation(message: ChatMessage): File? {
        // 이미 로컬에 영속된 파일이 있으면 바로 사용
        message.localFilePath?.let { path ->
            val f = File(path)
            if (f.exists() && f.length() > 0) return f
        }
        val mid = message.id.toLongOrNull() ?: return null
        val ext = mediaFileStore.extFromUrlOrName(
            url      = message.voiceUrl.takeIf { it.isNotBlank() },
            fileName = message.fileName.takeIf { it.isNotBlank() },
            fallback = "mp4",
        )
        // mediaFileStore 영속 경로 확인
        val persisted = mediaFileStore.pathFor(mid, ext)
        if (persisted.exists() && persisted.length() > 0) return persisted
        // S3에서 다운로드 후 영속화 (이후 재생에도 재사용)
        val url = message.voiceUrl.ifBlank { return null }
        return mediaFileStore.downloadAndPersist(url, mid, ext)
    }

    fun onPlayVoice(messageId: String) {
        if (_uiState.value.playingMessageId == messageId) { voicePlayer.stop(); return }
        val msg = _uiState.value.messages.find { it.id == messageId } ?: return
        val msgIdLong = messageId.toLongOrNull() ?: return

        // 로컬 영속 파일이 있으면 즉시 재생.
        msg.localFilePath?.takeIf { File(it).exists() }?.let { local ->
            voicePlayer.play(messageId, local)
            return
        }

        // 없으면 voiceUrl로 다운로드 후 재생 (다음 재생부터 캐시 활용).
        val url = msg.voiceUrl.ifEmpty { return }
        viewModelScope.launch {
            val ext  = mediaFileStore.extFromUrlOrName(url, fileName = null, fallback = "m4a")
            val file = mediaFileStore.downloadAndPersist(url, msgIdLong, ext)
            if (file != null) {
                messageDao.updateLocalFilePath(msgIdLong, file.absolutePath)
                voicePlayer.play(messageId, file.absolutePath)
            } else {
                // 다운로드 실패 시 URL 스트리밍으로 fallback (오프라인이거나 presigned 만료 케이스).
                voicePlayer.play(messageId, url)
            }
        }
    }

    fun onToggleTranslation(messageId: String) {
        val state   = _uiState.value
        val current = state.showTranslation[messageId] ?: state.isAutoTranslate
        val msg     = state.messages.find { it.id == messageId } ?: return

        if (msg.translatedText.isNotEmpty()) {
            val newShow = !current
            _uiState.update { s -> s.copy(showTranslation = s.showTranslation + (messageId to newShow)) }
            chatRoomPreferences.setShowTranslation(state.room.id, messageId, newShow)
            return
        }
        if (current) return

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
                    // 1) localFilePath 있으면 즉시 사용.
                    // 2) 없으면 voiceUrl로 다운로드 → 영속화 → 그 파일로 번역.
                    val file: File? = msg.localFilePath?.let { File(it).takeIf(File::exists) }
                        ?: run {
                            val url = msg.voiceUrl.ifBlank { return@launch finishTranslating(messageId) }
                            val ext = mediaFileStore.extFromUrlOrName(url, fileName = null, fallback = "m4a")
                            mediaFileStore.downloadAndPersist(url, msgIdLong, ext)?.also { dl ->
                                messageDao.updateLocalFilePath(msgIdLong, dl.absolutePath)
                            }
                        }
                    if (file == null) {
                        ApiResult.Error(-1, "VOICE_DOWNLOAD_FAILED", "음성 파일을 받지 못했습니다.")
                    } else {
                        when (val r = translateRepository.translateSpeech(file)) {
                            is ApiResult.Success      -> ApiResult.Success(r.data.translatedText to r.data.sourceText)
                            is ApiResult.Error        -> r
                            is ApiResult.NetworkError -> r
                        }
                    }
                }
                else -> return@launch finishTranslating(messageId)
            }
            when (result) {
                is ApiResult.Success -> {
                    val (translated, source) = result.data
                    messageDao.updateTranslation(msgIdLong, translated, source)
                    if (autoShow) chatRoomPreferences.setShowTranslation(_uiState.value.room.id, messageId, true)
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
        _uiState.update { it.copy(isAutoTranslate = turningOn) }
        _uiState.value.room.id.takeIf { it.isNotEmpty() }?.let { roomId ->
            chatRoomPreferences.setAutoTranslate(roomId, turningOn)
        }
        if (!turningOn) return
        // ON 전환 시 — 현재 화면에 보이는 미번역 상대 메시지를 watermark 무관하게 즉시 번역 요청
        val state = _uiState.value
        val myUid = state.myUserId
        state.messages.forEach { msg ->
            if (msg.senderId.toLongOrNull() == myUid) return@forEach
            if (msg.translatedText.isNotEmpty()) return@forEach
            if (msg.id in state.translatingMessageIds) return@forEach
            val hasContent = when (msg.type) {
                MessageType.TEXT  -> msg.text.isNotBlank()
                MessageType.VOICE -> msg.voiceUrl.isNotBlank()
                else              -> false
            }
            if (!hasContent) return@forEach
            translateMessage(msg.id, autoShow = false)
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
            uris.forEach { uri ->
                when (val result = fileMessageUploader.upload(chatRoomId, uri)) {
                    is ApiResult.Error        -> _uiState.update { it.copy(translationError = result.message ?: "파일 전송에 실패했습니다.") }
                    is ApiResult.NetworkError -> _uiState.update { it.copy(translationError = "네트워크 오류로 전송에 실패했습니다.") }
                    is ApiResult.Success      -> {}
                }
            }
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
        viewingChatRoomId?.let { roomId ->
            appScope.launch { markChatRoomAsRead(roomId) }
            viewingManager.leave(roomId)
        }
        viewingChatRoomId = null
        voiceRecorder.release()
        voicePlayer.release()
    }
}
