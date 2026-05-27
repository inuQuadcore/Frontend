package com.everybuddy.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.cache.UserSummary
import com.everybuddy.app.data.cache.UserSummaryCache
import com.everybuddy.app.data.chat.*
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.ChatRoom
import com.everybuddy.app.data.firebase.RoomMeta
import com.everybuddy.app.data.firebase.UserChatRoomsListener
import com.everybuddy.app.data.local.ChatRoomPreferences
import com.everybuddy.app.data.local.FolderDao
import com.everybuddy.app.data.local.FolderRoomEntity
import com.everybuddy.app.data.local.MessageDao
import com.everybuddy.app.data.local.TokenManager
import com.everybuddy.app.data.local.kstLocalDateTimeToEpochMs
import com.everybuddy.app.data.local.parseRestLocalDateTime
import com.everybuddy.app.data.repository.ChatRoomRepository
import com.everybuddy.app.ui.friend.KoreanChosung
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
    private val chatRoomRepository : ChatRoomRepository,
    private val tokenManager       : TokenManager,
    private val userSummaryCache   : UserSummaryCache,
    private val folderDao          : FolderDao,
    private val messageDao         : MessageDao,
    private val roomPreferences    : ChatRoomPreferences,
) : ViewModel() {

    private val _listState = MutableStateFlow(ChatListUiState())
    val listState: StateFlow<ChatListUiState> = _listState.asStateFlow()

    /** RTDB `users/{me}/chatrooms/` 메타 구독. loadChatRooms 후 attach. */
    private var userChatRoomsListener: UserChatRoomsListener? = null

    init {
        loadChatRooms()
        viewModelScope.launch { refreshFolders() }
    }

    /** Room에 영속화된 폴더 + 매핑을 ChatFolder list로 로드해 uiState에 채움. */
    private suspend fun refreshFolders() {
        val folders = folderDao.getFolders().map { entity ->
            val roomIds = folderDao.getRoomsInFolder(entity.id)
            entity.toChatFolder(roomIds)
        }
        _listState.update { it.copy(folders = folders) }
    }

    override fun onCleared() {
        super.onCleared()
        userChatRoomsListener?.detach()
        userChatRoomsListener = null
    }

    private fun attachUserChatRoomsListener() {
        if (userChatRoomsListener != null) return
        viewModelScope.launch {
            val myUserId = tokenManager.userId.firstOrNull() ?: return@launch
            val initialIds = _listState.value.rooms.mapNotNull { it.id.toLongOrNull() }.toSet()
            val listener = UserChatRoomsListener(
                userId         = myUserId,
                onMetaChange   = ::applyRoomMeta,
                onNewRoom      = { loadChatRooms() },
                onRoomRemoved  = ::removeRoom,
            )
            listener.attach(initialIds)
            userChatRoomsListener = listener
        }
    }

    private fun applyRoomMeta(chatRoomId: Long, meta: RoomMeta) {
        val idStr = chatRoomId.toString()
        _listState.update { state ->
            state.copy(rooms = state.rooms.map { room ->
                if (room.id != idStr) room
                else room.copy(
                    lastMessage     = meta.lastMessage,
                    lastMessageTime = meta.lastMessageTime,
                    timestamp       = RelativeTimeFormatter.format(meta.lastMessageTime),
                    unreadCount     = meta.unreadCount,
                )
            })
        }
    }

    private fun removeRoom(chatRoomId: Long) {
        val idStr = chatRoomId.toString()
        _listState.update { state -> state.copy(rooms = state.rooms.filter { it.id != idStr }) }
        // Room의 옛 메시지 정리 — 재초대 후 enterChatRoomAt 이전 메시지 누수 방지.
        // 본인 나가기/외부 강퇴 모두 RTDB onChildRemoved로 들어와 한 경로로 처리.
        viewModelScope.launch { messageDao.deleteAllByRoom(chatRoomId) }
    }

    /**
     * 채팅방 DTO 리스트 → UI 모델 변환.
     * - displayName/profileImageUrl/participants는 DTO의 participants 풀 객체에서 직접 추출 (toChatRoomUi 내부 처리).
     * - 로컬 mute/pin/star 플래그 적용.
     * - participants를 UserSummaryCache에 prefetch — 메시지 송신자 표시 시 cache hit 보장.
     */
    private suspend fun toChatRoomUis(rooms: List<ChatRoom>): List<ChatRoomUi> {
        val myUserId = tokenManager.userId.firstOrNull() ?: 0L
        rooms.flatMap { it.participants }.forEach { p ->
            userSummaryCache.put(p.userId, UserSummary(p.userId, p.name, p.profileImageUrl))
        }
        return rooms.map { room ->
            val idStr = room.chatRoomId.toString()
            val lastMsgEpoch = room.lastMessageTime?.let { kstLocalDateTimeToEpochMs(parseRestLocalDateTime(it)) }
            room.toChatRoomUi(myUserId = myUserId, lastMessageEpochMs = lastMsgEpoch).copy(
                timestamp = lastMsgEpoch?.let(RelativeTimeFormatter::format).orEmpty(),
                isMuted   = roomPreferences.isMuted(idStr),
                isPinned  = roomPreferences.isPinned(idStr),
                isStarred = roomPreferences.isStarred(idStr),
            )
        }
    }

    // 채팅방 목록 로드 — GET /api/v1/chatrooms
    fun loadChatRooms() {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = chatRoomRepository.getChatRooms()) {
                is ApiResult.Success -> {
                    val rooms = result.data?.let { toChatRoomUis(it) } ?: emptyList()
                    _listState.update { it.copy(isLoading = false, rooms = rooms) }
                    attachUserChatRoomsListener()
                }

                is ApiResult.Error -> {
                    // TODO: 401 JWT 만료 시 로그아웃 처리
                    _listState.update {
                        it.copy(isLoading = false, errorMessage = result.message, rooms = emptyList())
                    }
                }

                is ApiResult.NetworkError -> {
                    _listState.update {
                        it.copy(isLoading = false, errorMessage = result.e.localizedMessage, rooms = emptyList())
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _listState.update { it.copy(isRefreshing = true) }
            when (val result = chatRoomRepository.getChatRooms()) {
                is ApiResult.Success -> {
                    val rooms = result.data?.let { toChatRoomUis(it) } ?: emptyList()
                    _listState.update { it.copy(isRefreshing = false, errorMessage = null, rooms = rooms) }
                }
                is ApiResult.Error -> {
                    _listState.update { it.copy(isRefreshing = false, errorMessage = result.message) }
                }
                is ApiResult.NetworkError -> {
                    _listState.update { it.copy(isRefreshing = false, errorMessage = result.e.localizedMessage) }
                }
            }
        }
    }

    // 채팅방 생성 — POST /api/v1/chatrooms
    // roomName: 항상 필수. 1:1방은 호출자가 상대 이름을, 그룹방은 사용자 입력값을 전달.
    // isGroup: UI 흐름에 따라 — 1:1방=false, 그룹방=true.
    fun createChatRoom(
        roomName       : String,
        isGroup        : Boolean,
        participantIds : List<Long>,
        onSuccess      : (ChatRoomUi) -> Unit,
        onError        : (String) -> Unit,
    ) {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true) }

            when (val result = chatRoomRepository.createChatRoom(roomName, isGroup, participantIds)) {
                is ApiResult.Success -> {
                    val createdDto = result.data
                    val created = createdDto?.let { toChatRoomUis(listOf(it)).firstOrNull() }
                    if (created != null) {
                        // 1:1방 idempotent — 기존 방 반환된 경우 중복 방지: 같은 id 제거 후 맨 앞에 추가
                        _listState.update { state ->
                            state.copy(
                                isLoading = false,
                                rooms     = listOf(created) + state.rooms.filter { it.id != created.id },
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
        _listState.update { state ->
            if (state.isSearchOpen)
                state.copy(isSearchOpen = false, searchQuery = "")
            else
                state.copy(isSearchOpen = true)
        }
    }

    fun onContextMenu(room: ChatRoomUi) {
        _listState.update { it.copy(contextMenuRoom = room) }
    }

    fun onDismissContextMenu() {
        _listState.update { it.copy(contextMenuRoom = null) }
    }

    fun onMenuAction(action: String, room: ChatRoomUi) {
        val current = _listState.value
        when (action) {
            "toggle_mute" -> {
                val next = !room.isMuted
                roomPreferences.setMuted(room.id, next)
                _listState.update { it.copy(rooms = current.rooms.map { r -> if (r.id == room.id) r.copy(isMuted = next) else r }) }
            }
            "toggle_pin" -> {
                val next = !room.isPinned
                roomPreferences.setPinned(room.id, next)
                _listState.update { it.copy(rooms = current.rooms.map { r -> if (r.id == room.id) r.copy(isPinned = next) else r }) }
            }
            "toggle_star" -> {
                val next = !room.isStarred
                roomPreferences.setStarred(room.id, next)
                _listState.update { it.copy(rooms = current.rooms.map { r -> if (r.id == room.id) r.copy(isStarred = next) else r }) }
            }
            "leave" -> {
                val chatRoomId = room.id.toLongOrNull()
                if (chatRoomId == null) {
                    // dummy 채팅방: 기존 동작 (로컬에서만 제거)
                    _listState.update { it.copy(rooms = current.rooms.filter { r -> r.id != room.id }) }
                } else {
                    // 낙관적 제거 + 실패 시 복구 안 함 (RTDB onChildRemoved가 추후 확정).
                    _listState.update { it.copy(rooms = current.rooms.filter { r -> r.id != room.id }) }
                    viewModelScope.launch { chatRoomRepository.leaveChatRoom(chatRoomId) }
                }
            }
            "info" -> {
                _listState.update { it.copy(infoRoom = room) }
            }
        }
        onDismissContextMenu()
    }

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

    fun toggleStar(roomId: String) {
        _listState.update { state ->
            state.copy(rooms = state.rooms.map {
                if (it.id == roomId) it.copy(isStarred = !it.isStarred) else it
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
        viewModelScope.launch {
            // 신규면 마지막 order에 추가, 기존이면 order 유지.
            val existing = _listState.value.folders.find { it.id == folder.id }
            val order    = existing?.order ?: _listState.value.folders.size
            val toSave   = folder.copy(order = order)

            folderDao.upsertFolder(toSave.toEntity())
            folderDao.clearFolder(toSave.id)
            toSave.toRoomEntities().forEach { folderDao.addRoomToFolder(it) }

            refreshFolders()
        }
    }

    fun deleteFolder(id: String) {
        viewModelScope.launch {
            folderDao.deleteFolder(id)   // FolderRoomEntity는 별도 — 명시 삭제 필요
            folderDao.clearFolder(id)
            refreshFolders()
            _listState.update { state ->
                state.copy(activeFolderId = if (state.activeFolderId == id) null else state.activeFolderId)
            }
        }
    }

    fun reorderFolders(ordered: List<ChatFolder>) {
        viewModelScope.launch {
            ordered.forEachIndexed { idx, f -> folderDao.updateOrder(f.id, idx) }
            refreshFolders()
        }
    }

    fun onFolderTabSelect(folderId: String?) {
        _listState.update { it.copy(activeFolderId = folderId) }
    }

    fun openFolderManage() { _listState.update { it.copy(isFolderManageOpen = true) } }
    fun closeFolderManage() { _listState.update { it.copy(isFolderManageOpen = false) } }

    fun openFolderCreate(folder: ChatFolder? = null) {
        _listState.update { it.copy(isFolderCreateOpen = true, editingFolder = folder) }
    }
    fun closeFolderCreate() {
        _listState.update { it.copy(isFolderCreateOpen = false, editingFolder = null) }
    }

    val filteredRooms: StateFlow<List<ChatRoomUi>> = listState.map { state ->
        state.rooms
            .filter { room ->
                val matchQuery = state.searchQuery.isEmpty() ||
                        KoreanChosung.matches(state.searchQuery, room.name) ||
                        KoreanChosung.matches(state.searchQuery, room.lastMessage)
                val matchFilter = when {
                    state.activeFolderId != null -> {
                        val folder = state.folders.find { it.id == state.activeFolderId }
                        folder?.chatRoomIds?.contains(room.id) ?: false
                    }
                    else -> when (state.activeFilter) {
                        ChatFilter.ALL      -> true
                        ChatFilter.UNREAD   -> room.unreadCount > 0
                        ChatFilter.FAVORITE -> room.isStarred
                    }
                }
                matchQuery && matchFilter
            }
            .sortedWith(compareByDescending<ChatRoomUi> { it.isPinned }.thenByDescending { it.lastMessageTime })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
