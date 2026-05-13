package com.everybuddy.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.BuildConfig
import com.everybuddy.app.data.chat.*
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.repository.ChatRepository
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

    // 채팅방 목록 로드 — GET /api/v1/chatrooms
    fun loadChatRooms() {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = chatRepository.getChatRooms()) {
                is ApiResult.Success -> {
                    val rooms = result.data
                        ?.map { it.toChatRoomUi() }
                        ?: emptyList()
                    _listState.update { it.copy(isLoading = false, rooms = rooms) }
                }

                is ApiResult.Error -> {
                    // 401: JWT 만료 → 로그인 화면으로 이동 필요
                    // TODO: 401 시 로그아웃 처리 — AppNavGraph에서 Route.LOGIN으로 navigate
                    _listState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = result.message,
                            rooms        = if (BuildConfig.DEBUG) dummyChatRooms else emptyList(),
                        )
                    }
                }

                is AuthResult.Exception -> {
                    // 네트워크 오류
                    _listState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = result.e.localizedMessage,
                            rooms        = if (BuildConfig.DEBUG) dummyChatRooms else emptyList(),
                        )
                    }
                }
            }
        }
    }

    // 채팅방 생성 — POST /api/v1/chatrooms
    fun createChatRoom(
        roomName       : String,
        participantIds : List<Long>,
        onSuccess      : (ChatRoomUi) -> Unit,
        onError        : (String) -> Unit,
    ) {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true) }

            when (val result = chatRepository.createChatRoom(roomName, participantIds)) {
                is ApiResult.Success -> {
                    val created = result.data?.toChatRoomUi()
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

                is ApiResult.Error -> {
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

                is ApiResult.NetworkError -> {
                    _listState.update { it.copy(isLoading = false) }
                    onError(result.e.localizedMessage ?: "네트워크 오류가 발생했습니다.")
                }
            }
        }
    }

    fun onFilterSelect(filter: ChatFilter) {
        _listState.update { it.copy(activeFilter = filter, activeFolderId = null) }
    }

    fun onSearchQueryChange(query: String) {
        _listState.update { it.copy(searchQuery = query) }
    }

    fun onSearchToggle() {
        // TODO: 검색창 AnimatedVisibility 토글 + 포커스 처리
    }

    fun onContextMenu(room: ChatRoomUi) {
        _listState.update { it.copy(contextMenuRoom = room) }
    }

    fun onDismissContextMenu() {
        _listState.update { it.copy(contextMenuRoom = null) }
    }

    fun onMenuAction(action: String, room: ChatRoom) {
        val current = _listState.value
        when (action) {
            "toggle_mute" -> {
                val updated = current.rooms.map {
                    if (it.id == room.id) it.copy(isMuted = !it.isMuted) else it
                }
                _listState.update { it.copy(rooms = updated) }
                // TODO: PATCH /api/v1/chatrooms/{roomId}/mute
            }
            "toggle_pin" -> {
                val updated = current.rooms.map {
                    if (it.id == room.id) it.copy(isPinned = !it.isPinned) else it
                }
                _listState.update { it.copy(rooms = updated) }
                // TODO: PATCH /api/v1/chatrooms/{roomId}/pin
            }
            "leave" -> {
                _listState.update { it.copy(rooms = current.rooms.filter { r -> r.id != room.id }) }
                // TODO: DELETE /api/v1/chatrooms/{roomId}
            }
            "info" -> {
                _listState.update { it.copy(infoRoom = room) }
            }
        }
        onDismissContextMenu()
    }

    val filteredRooms: StateFlow<List<ChatRoomUi>> = listState.map { state ->
    fun dismissRoomInfo() {
        _listState.update { it.copy(infoRoom = null) }
    }

    fun markRoomAsRead(roomId: String) {
        _listState.update { state ->
            state.copy(rooms = state.rooms.map {
                if (it.id == roomId) it.copy(unreadCount = 0) else it
            })
        }
    }

    fun updateRoomMute(roomId: String, isMuted: Boolean) {
        _listState.update { state ->
            state.copy(rooms = state.rooms.map {
                if (it.id == roomId) it.copy(isMuted = isMuted) else it
            })
        }
    }

    fun saveFolder(folder: ChatFolder) {
        _listState.update { state ->
            val list = state.folders.toMutableList()
            val idx  = list.indexOfFirst { it.id == folder.id }
            if (idx >= 0) list[idx] = folder else list.add(folder)
            state.copy(folders = list.toList())
        }
    }

    fun deleteFolder(id: String) {
        _listState.update { state ->
            state.copy(
                folders        = state.folders.filter { it.id != id },
                activeFolderId = if (state.activeFolderId == id) null else state.activeFolderId,
            )
        }
    }

    fun reorderFolders(ordered: List<ChatFolder>) {
        _listState.update { it.copy(folders = ordered) }
    }

    fun onFolderTabSelect(folderId: String?) {
        _listState.update { it.copy(activeFolderId = folderId) }
    }

    val filteredRooms: StateFlow<List<ChatRoom>> = listState.map { state ->
        state.rooms
            .filter { room ->
                val matchQuery = state.searchQuery.isEmpty() ||
                        room.name.contains(state.searchQuery, ignoreCase = true) ||
                        room.lastMessage.contains(state.searchQuery, ignoreCase = true)
                val matchFilter = when {
                    state.activeFolderId != null -> {
                        val folder = state.folders.find { it.id == state.activeFolderId }
                        folder?.chatRoomIds?.contains(room.id) ?: false
                    }
                    else -> when (state.activeFilter) {
                        ChatFilter.ALL      -> true
                        ChatFilter.UNREAD   -> room.unreadCount > 0
                        ChatFilter.FAVORITE -> room.isPinned
                    }
                }
                matchQuery && matchFilter
            }
            .sortedWith(compareByDescending<ChatRoom> { it.isPinned })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
