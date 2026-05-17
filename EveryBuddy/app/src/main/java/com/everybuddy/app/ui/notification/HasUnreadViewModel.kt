package com.everybuddy.app.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 알림 미읽 여부(GET /has-unread)를 전역으로 들고 다니는 ViewModel.
 * MainScreen에서 hiltViewModel()로 받아 각 탭 헤더 뱃지에 반영.
 */
@HiltViewModel
class HasUnreadViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val _hasUnread = MutableStateFlow(false)
    val hasUnread: StateFlow<Boolean> = _hasUnread.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            when (val r = repository.hasUnread()) {
                is ApiResult.Success -> _hasUnread.value = r.data.hasUnread
                is ApiResult.Error, is ApiResult.NetworkError -> Unit
            }
        }
    }
}
