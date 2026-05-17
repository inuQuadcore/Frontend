package com.everybuddy.app.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.Notification
import com.everybuddy.app.data.dto.userMessage
import com.everybuddy.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val notifications : List<Notification> = emptyList(),
    val isLoading     : Boolean            = false,
    val isAppending   : Boolean            = false,
    val hasNext       : Boolean            = false,
    val nextCursor    : Long?              = null,
    val errorMessage  : String?            = null,
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init { loadFirst() }

    fun loadFirst() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = repository.list(before = null)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        notifications = r.data.notifications,
                        hasNext       = r.data.hasNext,
                        nextCursor    = r.data.nextCursor,
                        isLoading     = false,
                    )
                }
                is ApiResult.Error, is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = r.userMessage()) }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasNext || state.isAppending || state.nextCursor == null) return
        _uiState.update { it.copy(isAppending = true) }
        viewModelScope.launch {
            when (val r = repository.list(before = state.nextCursor)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        notifications = it.notifications + r.data.notifications,
                        hasNext       = r.data.hasNext,
                        nextCursor    = r.data.nextCursor,
                        isAppending   = false,
                    )
                }
                is ApiResult.Error, is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isAppending = false, errorMessage = r.userMessage()) }
            }
        }
    }

    /** 단건 읽음 처리 — 로컬 state 먼저 갱신(optimistic), API는 백그라운드. */
    fun markRead(notificationId: Long) {
        _uiState.update { s ->
            s.copy(notifications = s.notifications.map {
                if (it.notificationId == notificationId) it.copy(isRead = true) else it
            })
        }
        viewModelScope.launch { repository.markRead(notificationId) }
    }

    /** 전체 읽음 처리 — 로컬 state 먼저 갱신(optimistic), API는 백그라운드. */
    fun markAllRead() {
        _uiState.update { s ->
            s.copy(notifications = s.notifications.map { it.copy(isRead = true) })
        }
        viewModelScope.launch { repository.markAllRead() }
    }

    fun consumeError() { _uiState.update { it.copy(errorMessage = null) } }
}
