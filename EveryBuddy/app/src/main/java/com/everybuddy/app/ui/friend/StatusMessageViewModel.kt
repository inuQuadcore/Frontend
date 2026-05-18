package com.everybuddy.app.ui.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.cache.UserSummaryCache
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.FriendStatusMessageDto
import com.everybuddy.app.data.dto.MyStatusMessageResponse
import com.everybuddy.app.data.dto.toDto
import com.everybuddy.app.data.dto.userMessage
import com.everybuddy.app.data.local.TokenManager
import com.everybuddy.app.data.repository.ChatRoomRepository
import com.everybuddy.app.data.repository.MessageRepository
import com.everybuddy.app.data.repository.StatusMessageRepository
import com.everybuddy.app.data.repository.TranslateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

data class StatusUiState(
    val myStatus            : MyStatusMessageResponse?     = null,
    val myProfileImageUrl   : String?                      = null,
    val friendStatuses      : List<FriendStatusMessageDto>  = emptyList(),
    val isWriteScreenOpen   : Boolean                      = false,
    val isEditMode          : Boolean                      = false,
    val draftText           : String                       = "",
    val expandedStatus      : FriendStatusMessageDto?      = null,
    val isMyStatusMenuOpen  : Boolean                      = false,
    val isDeleteConfirmOpen : Boolean                      = false,
    val isReplying          : Boolean                      = false,
    val isSending           : Boolean                      = false,
    val replyText           : String                       = "",
    val replySent           : Boolean                      = false,
    val isLoading           : Boolean                      = false,
    val toastMessage        : String?                      = null,
    val nextCursor          : Long?                        = null,
    val hasNext             : Boolean                      = false,
    val translatedStatuses  : Map<Long, String>            = emptyMap(),
    val translatingStatusIds: Set<Long>                    = emptySet(),
)

@HiltViewModel
class StatusMessageViewModel @Inject constructor(
    private val statusRepo         : StatusMessageRepository,
    private val chatRoomRepository : ChatRoomRepository,
    private val messageRepository  : MessageRepository,
    private val tokenManager       : TokenManager,
    private val userSummaryCache   : UserSummaryCache,
    private val translateRepository: TranslateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StatusUiState())
    val state: StateFlow<StatusUiState> = _state.asStateFlow()

    init { loadAll() }

    fun loadAll() {
        loadMyStatus()
        loadMyProfile()
        loadFriendStatuses(reset = true)
    }

    /** 본인 프로필 이미지 — UserSummaryCache hit 시 즉시, miss 시 GET /users/{myId}로 채워짐. */
    private fun loadMyProfile() {
        viewModelScope.launch {
            val myUserId = tokenManager.userId.firstOrNull() ?: return@launch
            val summary  = userSummaryCache.get(myUserId) ?: return@launch
            _state.update { it.copy(myProfileImageUrl = summary.profileImageUrl) }
        }
    }

    fun loadMyStatus() {
        viewModelScope.launch {
            when (val r = statusRepo.getMyStatusMessage()) {
                is ApiResult.Success      -> _state.update { it.copy(myStatus = r.data) }
                is ApiResult.Error        -> if (r.name == "STATUS_MESSAGE_NOT_FOUND") _state.update { it.copy(myStatus = null) }
                is ApiResult.NetworkError -> {}
            }
        }
    }

    fun loadFriendStatuses(reset: Boolean = false) {
        viewModelScope.launch {
            val cursor = if (reset) null else _state.value.nextCursor
            if (!reset && !_state.value.hasNext) return@launch
            _state.update { it.copy(isLoading = true) }
            when (val r = statusRepo.getFriendStatusMessages(cursor)) {
                is ApiResult.Success -> {
                    val data   = r.data
                    val dtos   = data.statusMessages.map { it.toDto().copy(timeAgo = it.updatedAt.toRelativeTime()) }
                    val merged = if (reset) dtos else _state.value.friendStatuses + dtos
                    _state.update { it.copy(friendStatuses = merged, nextCursor = data.nextCursor, hasNext = data.hasNext, isLoading = false) }
                }
                is ApiResult.Error, is ApiResult.NetworkError ->
                    _state.update { it.copy(isLoading = false, toastMessage = r.userMessage()) }
            }
        }
    }

    fun openWriteNew() {
        _state.update { it.copy(isWriteScreenOpen = true, isEditMode = false, draftText = "") }
    }

    fun openEditMode() {
        _state.update { it.copy(isWriteScreenOpen = true, isEditMode = true, draftText = _state.value.myStatus?.content ?: "", isMyStatusMenuOpen = false) }
    }

    fun closeWriteScreen() {
        _state.update { it.copy(isWriteScreenOpen = false, draftText = "") }
    }

    fun updateDraftText(text: String) {
        if (text.length <= 100) _state.update { it.copy(draftText = text) }
    }

    fun submitStatus() {
        val text    = _state.value.draftText.trim().ifBlank { return }
        val isEdit  = _state.value.isEditMode
        val msg     = if (isEdit) "상태메시지가 수정되었습니다." else "상태메시지가 등록되었습니다."
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val r = if (isEdit) statusRepo.updateStatusMessage(text) else statusRepo.postStatusMessage(text)
            when (r) {
                is ApiResult.Success -> {
                    loadMyStatus()
                    _state.update { it.copy(isWriteScreenOpen = false, draftText = "", isEditMode = false, isLoading = false, toastMessage = msg) }
                }
                is ApiResult.Error, is ApiResult.NetworkError ->
                    _state.update { it.copy(isLoading = false, toastMessage = r.userMessage()) }
            }
        }
    }

    fun openMyStatusMenu()  { _state.update { it.copy(isMyStatusMenuOpen = true) } }
    fun closeMyStatusMenu() { _state.update { it.copy(isMyStatusMenuOpen = false) } }

    fun openDeleteConfirm() { _state.update { it.copy(isDeleteConfirmOpen = true, isMyStatusMenuOpen = false) } }
    fun closeDeleteConfirm() { _state.update { it.copy(isDeleteConfirmOpen = false) } }

    fun deleteStatus() {
        viewModelScope.launch {
            when (val r = statusRepo.deleteStatusMessage()) {
                is ApiResult.Success      -> _state.update { it.copy(myStatus = null, isDeleteConfirmOpen = false, toastMessage = "상태메시지가 삭제되었습니다.") }
                is ApiResult.Error, is ApiResult.NetworkError ->
                    _state.update { it.copy(isDeleteConfirmOpen = false, toastMessage = r.userMessage()) }
            }
        }
    }

    fun openFriendStatus(sm: FriendStatusMessageDto) {
        _state.update { it.copy(expandedStatus = sm, isReplying = false, replyText = "", replySent = false) }
    }

    fun closeFriendStatus() {
        _state.update { it.copy(expandedStatus = null, isReplying = false, replyText = "", replySent = false) }
    }

    fun openReply()  { _state.update { it.copy(isReplying = true) } }
    fun cancelReply() { _state.update { it.copy(isReplying = false, replyText = "") } }
    fun updateReplyText(text: String) { _state.update { it.copy(replyText = text) } }

    fun sendReply() {
        if (_state.value.replyText.isBlank() || _state.value.isSending) return
        val replyText = _state.value.replyText.trim()
        val target    = _state.value.expandedStatus ?: return
        val preview   = if (target.content.length > 15) target.content.take(15) + "…" else target.content

        viewModelScope.launch {
            _state.update { it.copy(isSending = true, replyText = "") }

            val createResult = chatRoomRepository.createChatRoom(target.userName, isGroup = false, listOf(target.userId))
            if (createResult is ApiResult.Success) {
                val chatRoomId = createResult.data?.chatRoomId
                if (chatRoomId != null) {
                    messageRepository.sendTextMessage(chatRoomId, replyText, statusPreview = preview)
                    _state.update { it.copy(isSending = false, replySent = true) }
                    delay(1500)
                    _state.update { it.copy(replySent = false, expandedStatus = null, isReplying = false) }
                    return@launch
                }
            }
            _state.update { it.copy(isSending = false, toastMessage = createResult.userMessage()) }
        }
    }

    fun consumeReplySent() { _state.update { it.copy(replySent = false) } }
    fun consumeToast()     { _state.update { it.copy(toastMessage = null) } }

    fun translateStatus(statusMessageId: Long, content: String) {
        if (statusMessageId in _state.value.translatingStatusIds) return
        _state.update { it.copy(translatingStatusIds = it.translatingStatusIds + statusMessageId) }
        viewModelScope.launch {
            when (val r = translateRepository.translateText(content)) {
                is ApiResult.Success ->
                    _state.update { it.copy(
                        translatedStatuses   = it.translatedStatuses + (statusMessageId to r.data.translatedText),
                        translatingStatusIds = it.translatingStatusIds - statusMessageId,
                    )}
                else ->
                    _state.update { it.copy(translatingStatusIds = it.translatingStatusIds - statusMessageId) }
            }
        }
    }

    fun clearStatusTranslation(statusMessageId: Long) {
        _state.update { it.copy(translatedStatuses = it.translatedStatuses - statusMessageId) }
    }
}

private fun String.toRelativeTime(): String {
    val past = try {
        LocalDateTime.parse(this).atZone(ZoneId.systemDefault()).toInstant()
    } catch (_: Exception) { return this }
    val diff    = Duration.between(past, Instant.now())
    val minutes = diff.toMinutes()
    val hours   = diff.toHours()
    return when {
        minutes < 1 -> "방금"
        hours < 1   -> "${minutes}분 전"
        hours < 24  -> "${hours}시간 전"
        else        -> "${diff.toDays()}일 전"
    }
}
