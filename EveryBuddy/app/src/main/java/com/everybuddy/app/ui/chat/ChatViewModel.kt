package com.everybuddy.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.chat.*
import com.everybuddy.app.data.network.AuthResult
import com.everybuddy.app.data.network.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ChatViewModel — 채팅 리스트 화면 상태 및 비즈니스 로직
 *
 * [변경] API 스펙 확정 반영:
 *   - loadChatRooms(): GET /api/v1/chatrooms 실제 호출 + 에러 처리
 *   - createChatRoom(): POST /api/v1/chatrooms 연동 (400 필드 에러 처리 포함)
 *   - 더미 데이터는 API 실패 시 폴백으로만 사용
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _listState = MutableStateFlow(ChatListUiState())
    val listState: StateFlow<ChatListUiState> = _listState.asStateFlow()

    init { loadChatRooms() }

    // ─────────────────────────────────────────────────────────────────────────
    // 채팅방 목록 로드 — GET /api/v1/chatrooms
    // ─────────────────────────────────────────────────────────────────────────
    fun loadChatRooms() {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = chatRepository.getChatRooms()) {
                is AuthResult.Success -> {
                    val rooms = result.data
                        ?.map { it.toChatRoom() }
                        ?: emptyList()
                    _listState.update { it.copy(isLoading = false, rooms = rooms) }
                }

                is AuthResult.Error -> {
                    // 401: JWT 만료 → 로그인 화면으로 이동 필요
                    // TODO: 401 시 로그아웃 처리 — AppNavGraph에서 Route.LOGIN으로 navigate
                    _listState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = result.message,
                            // API 실패 시 더미 데이터 폴백 (개발 단계)
                            rooms        = dummyChatRooms,
                        )
                    }
                }

                is AuthResult.Exception -> {
                    // 네트워크 오류 — 더미 데이터 폴백
                    _listState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = result.e.localizedMessage,
                            rooms        = dummyChatRooms,
                        )
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 채팅방 생성 — POST /api/v1/chatrooms
    //
    // @param roomName       채팅방 이름 (필수)
    // @param participantIds 참여자 ID 목록 (나 자신 제외, 서버가 자동 추가)
    //
    // 에러 처리:
    //   400 INVALID_INPUT_VALUE — errors.roomName / errors.participantIds
    //   401 JWT_ENTRY_POINT     — 로그인 필요
    //   404 USER_NOT_FOUND      — 해당 유저 없음
    // ─────────────────────────────────────────────────────────────────────────
    fun createChatRoom(
        roomName       : String,
        participantIds : List<Long>,
        onSuccess      : (ChatRoom) -> Unit,
        onError        : (String) -> Unit,
    ) {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true) }

            when (val result = chatRepository.createChatRoom(roomName, participantIds)) {
                is AuthResult.Success -> {
                    val created = result.data?.toChatRoom()
                    if (created != null) {
                        // 목록 맨 앞에 추가
                        _listState.update { state ->
                            state.copy(
                                isLoading = false,
                                rooms     = listOf(created) + state.rooms,
                            )
                        }
                        onSuccess(created)
                    } else {
                        _listState.update { it.copy(isLoading = false) }
                        onError("채팅방 생성에 실패했습니다.")
                    }
                }

                is AuthResult.Error -> {
                    _listState.update { it.copy(isLoading = false) }
                    // 400: 필드별 에러 메시지 조합
                    val errorMsg = when (result.code) {
                        400  -> result.message  // "채팅방 이름을 입력해주세요." 등
                        401  -> "로그인이 필요합니다."
                        404  -> "해당 유저를 찾을 수 없습니다."
                        else -> result.message
                    }
                    onError(errorMsg)
                }

                is AuthResult.Exception -> {
                    _listState.update { it.copy(isLoading = false) }
                    onError(result.e.localizedMessage ?: "네트워크 오류가 발생했습니다.")
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 필터 선택
    // ─────────────────────────────────────────────────────────────────────────
    fun onFilterSelect(filter: ChatFilter) {
        _listState.update { it.copy(activeFilter = filter) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 검색
    // ─────────────────────────────────────────────────────────────────────────
    fun onSearchQueryChange(query: String) {
        _listState.update { it.copy(searchQuery = query) }
    }

    fun onSearchToggle() {
        // TODO: 검색창 AnimatedVisibility 토글 + 포커스 처리
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 컨텍스트 메뉴 (채팅방 롱클릭)
    // ─────────────────────────────────────────────────────────────────────────
    fun onContextMenu(room: ChatRoom) {
        _listState.update { it.copy(contextMenuRoom = room) }
    }

    fun onDismissContextMenu() {
        _listState.update { it.copy(contextMenuRoom = null) }
    }

    fun onMenuAction(action: String, room: ChatRoom) {
        when (action) {
            "알림 끄기"   -> {
                // TODO: PATCH /api/v1/chatrooms/{roomId}/mute
            }
            "상단 고정"   -> {
                // TODO: PATCH /api/v1/chatrooms/{roomId}/pin
            }
            "채팅방 나가기" -> {
                // TODO: DELETE /api/v1/chatrooms/{roomId}
            }
        }
        onDismissContextMenu()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 필터링된 채팅방 목록 (검색어 + 필터 적용)
    // ─────────────────────────────────────────────────────────────────────────
    val filteredRooms: StateFlow<List<ChatRoom>> = listState.map { state ->
        state.rooms
            .filter { room ->
                // 검색어 필터
                val matchQuery = state.searchQuery.isEmpty() ||
                        room.name.contains(state.searchQuery, ignoreCase = true) ||
                        room.lastMessage.contains(state.searchQuery, ignoreCase = true)
                // 탭 필터
                val matchFilter = when (state.activeFilter) {
                    ChatFilter.ALL      -> true
                    ChatFilter.UNREAD   -> room.unreadCount > 0
                    ChatFilter.FAVORITE -> room.isPinned
                }
                matchQuery && matchFilter
            }
            // 고정 방 최상단 정렬 → 최근 메시지 순 (timestamp 미구현이므로 현재 유지)
            .sortedWith(compareByDescending<ChatRoom> { it.isPinned })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
