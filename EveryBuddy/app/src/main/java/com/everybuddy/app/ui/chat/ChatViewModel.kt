package com.everybuddy.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.chat.*
import com.everybuddy.app.data.network.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ChatViewModel
 *
 * 채팅 리스트 화면의 상태와 비즈니스 로직을 담당.
 * ChatRoomViewModel은 별도 파일(ChatRoomViewModel.kt)로 분리.
 *
 * TODO API: GET /api/v1/채팅방 연동 → loadChatRooms()에서 더미 데이터 제거
 * TODO 실시간: WebSocket/SSE 연결 → 새 메시지 수신 시 채팅 목록 자동 갱신
 * TODO 정렬: 채팅방 목록 최근 메시지 기준 정렬 로직 추가
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _listState = MutableStateFlow(ChatListUiState())
    val listState: StateFlow<ChatListUiState> = _listState.asStateFlow()

    init { loadChatRooms() }

    /**
     * 채팅방 목록 로드.
     * TODO API: chatRepository.getChatRooms() 호출 후 결과를 rooms에 매핑
     * TODO 더미: dummyChatRooms를 실 API 연동 후 제거
     */
    fun loadChatRooms() {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true) }
            // TODO: when (val result = chatRepository.getChatRooms()) {
            //     is AuthResult.Success -> rooms = result.data?.map { ... } ?: emptyList()
            //     is AuthResult.Error   -> errorMessage = result.message
            // }
            _listState.update { it.copy(isLoading = false) }
        }
    }

    fun onFilterSelect(filter: ChatFilter) {
        _listState.update { it.copy(activeFilter = filter) }
    }

    fun onSearchQueryChange(query: String) {
        _listState.update { it.copy(searchQuery = query) }
    }

    /**
     * TODO UI: 검색창 열기/닫기 애니메이션 + 포커스 처리
     */
    fun onSearchToggle() { /* TODO */ }

    /**
     * TODO 기능: 알림 끄기(isMuted), 상단 고정(isPinned), 채팅방 나가기(Firebase)
     *             각 action별 API 연동 필요
     */
    fun onMenuAction(action: String, room: ChatRoom) { /* TODO */ }
}