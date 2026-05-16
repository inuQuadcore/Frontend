package com.everybuddy.app.ui.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.BuildConfig
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.FriendStatusMessageDto
import com.everybuddy.app.data.dto.MyStatusMessageResponse
import com.everybuddy.app.data.dto.toDto
import com.everybuddy.app.data.dto.userMessage
import com.everybuddy.app.data.repository.StatusMessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatusUiState(
    val myStatus            : MyStatusMessageResponse?     = null,
    val friendStatuses      : List<FriendStatusMessageDto>  = emptyList(),
    val isWriteScreenOpen   : Boolean                      = false,
    val isEditMode          : Boolean                      = false,
    val draftText           : String                       = "",
    val expandedStatus      : FriendStatusMessageDto?    = null,
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
)

@HiltViewModel
class StatusMessageViewModel @Inject constructor(
    private val statusRepo: StatusMessageRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StatusUiState())
    val state: StateFlow<StatusUiState> = _state.asStateFlow()

    init { loadAll() }

    fun loadAll() {
        loadMyStatus()
        loadFriendStatuses(reset = true)
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
                    val dtos   = data.statusMessages.map { it.toDto() }
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
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, replyText = "") }

            if (BuildConfig.USE_DUMMY_DATA) {
                val statusPreview = if (target.content.length > 15) target.content.take(15) + "…" else target.content
                val friendId      = target.userId.toString()
                val existing      = FriendDemoData.chatRooms.find { it.friendId == friendId }
                val msg = FriendDemoData.DemoChatMsg(
                    text                  = replyText,
                    isMine                = true,
                    isStatusReply         = true,
                    originalStatusPreview = statusPreview,
                )
                if (existing != null) {
                    existing.messages.add(msg)
                } else {
                    FriendDemoData.chatRooms.add(
                        FriendDemoData.DemoChatRoom(
                            id         = java.util.UUID.randomUUID().toString(),
                            friendId   = friendId,
                            friendName = target.userName,
                            messages   = mutableListOf(msg),
                        )
                    )
                }
            }
            // TODO: ChatRoomRepository integration — 실제 API 전송

            delay(700)
            _state.update { it.copy(isSending = false, replySent = true) }
            delay(1500)
            _state.update { it.copy(replySent = false, expandedStatus = null, isReplying = false) }
        }
    }

    fun consumeReplySent() { _state.update { it.copy(replySent = false) } }
    fun consumeToast()     { _state.update { it.copy(toastMessage = null) } }
}
