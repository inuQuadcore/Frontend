package com.everybuddy.app.ui.chat

import androidx.lifecycle.ViewModel
import com.everybuddy.app.data.chat.ChatFilter
import com.everybuddy.app.data.chat.ChatListUiState
import com.everybuddy.app.data.chat.ChatRoom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ChatListViewModel
 *
 * NOTE: 현재 ChatListScreen은 ChatViewModel을 사용함.
 * TODO 리팩터: ChatViewModel에서 채팅방 리스트 로직을 이 ViewModel로 이전하거나,
 *              ChatViewModel을 제거하고 이 파일로 통합할 것.
 *
 * TODO API: GET /api/v1/채팅방 연동 → loadChatRooms()에서 실 API 데이터로 교체
 * TODO 실시간: Firebase Realtime Database 또는 WebSocket → 채팅방 목록 자동 갱신
 */
@HiltViewModel
class ChatListViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    fun onFilterClick(filter: ChatFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun onContextMenu(room: ChatRoom) {
        _uiState.update { it.copy(contextMenuRoom = room) }
    }

    fun onDismissMenu() {
        _uiState.update { it.copy(contextMenuRoom = null) }
    }

    fun onMenuAction(action: String, room: ChatRoom) {
        when (action) {
            "채팅방 정보"  -> { /* TODO 기능: 채팅방 설정 화면 이동 */ }
            "알림 끄기"   -> {
                // TODO API: PATCH /api/v1/채팅방/{roomId}/mute → isMuted 토글
                //           ChatRoom.isMuted 필드 추가 후 rooms 목록 갱신
            }
            "상단 고정"   -> {
                // TODO API: PATCH /api/v1/채팅방/{roomId}/pin → isPinned 토글
                //           고정된 방은 목록 최상단 정렬
            }
            "채팅방 나가기" -> {
                // TODO API: DELETE /api/v1/채팅방/{roomId} 또는 Firebase removeValue()
                //           성공 시 rooms 목록에서 해당 방 제거
            }
        }
        onDismissMenu()
    }
}
