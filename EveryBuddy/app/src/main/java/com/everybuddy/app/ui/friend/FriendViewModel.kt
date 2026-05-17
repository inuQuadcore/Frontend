package com.everybuddy.app.ui.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.Friend
import com.everybuddy.app.data.dto.userMessage
import com.everybuddy.app.data.firebase.PresenceRepository
import com.everybuddy.app.data.repository.BlockRepository
import com.everybuddy.app.data.repository.ChatRoomRepository
import com.everybuddy.app.data.repository.FriendRepository
import com.everybuddy.app.data.repository.MessageRepository
import com.everybuddy.app.data.repository.UserRepository
import com.everybuddy.app.ui.explore.UserDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendUiState(
    // 전체 데이터
    val friends             : List<FriendProfile>   = emptyList(),
    val isLoading           : Boolean               = false,
    val statusMessages      : List<StatusMessage>   = emptyList(),
    val myStatusMessage     : StatusMessage?        = null,   // 내 상태메시지 (null = 미작성)

    // 정렬
    val sortOption          : FriendSortOption      = FriendSortOption.GANADA,

    // 검색
    val searchQuery         : String                = "",
    val isSearchActive      : Boolean               = false,

    // 상태메시지 더보기 (하위 페이지)
    val isStatusPageOpen    : Boolean               = false,

    // 상태메시지 작성 화면
    val isWritingStatus     : Boolean               = false,
    val draftStatusText     : String                = "",

    // 상태메시지 팝업 (탭 시 상세 팝업)
    val expandedStatus      : StatusMessage?        = null,   // null = 팝업 없음
    val isReplying          : Boolean               = false,  // 답장 입력창 열림
    val replyText           : String                = "",
    val replySent           : Boolean               = false,  // "전송 완료!" 토스트

    // 프로필 화면
    val selectedFriend      : FriendProfile?        = null,
    val selectedFriendDetail: UserDetail?           = null,

    // 팔로우 상태
    val followedFriendIds   : Set<Long>             = emptySet(),

    // 토스트
    val isRefreshing        : Boolean               = false,
    val toastMessage        : String?               = null,
)

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val friendRepository    : FriendRepository,
    private val blockRepository     : BlockRepository,
    private val presenceRepository  : PresenceRepository,
    private val chatRoomRepository  : ChatRoomRepository,
    private val messageRepository   : MessageRepository,
    private val userRepository      : UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendUiState())
    val uiState: StateFlow<FriendUiState> = _uiState.asStateFlow()

    init {
        loadFriends()
        observePresence()
    }

    /** RTDB presence/ 변경 시 친구 isOnline 일괄 갱신. */
    private fun observePresence() {
        viewModelScope.launch {
            presenceRepository.onlineIds.collect { ids ->
                _uiState.update { state ->
                    state.copy(friends = state.friends.map { it.copy(isOnline = it.id in ids) })
                }
            }
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val r = friendRepository.getFriends()) {
                is ApiResult.Success -> {
                    val ids = presenceRepository.onlineIds.value
                    _uiState.update {
                        it.copy(
                            friends   = r.data.friends.map { f -> f.toFriendProfile(ids) },
                            isLoading = false,
                        )
                    }
                }
                is ApiResult.Error, is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isLoading = false, toastMessage = r.userMessage()) }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            when (val r = friendRepository.getFriends()) {
                is ApiResult.Success -> {
                    val ids = presenceRepository.onlineIds.value
                    _uiState.update {
                        it.copy(
                            friends      = r.data.friends.map { f -> f.toFriendProfile(ids) },
                            isRefreshing = false,
                        )
                    }
                }
                is ApiResult.Error, is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isRefreshing = false, toastMessage = r.userMessage()) }
            }
        }
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update { it.copy(isSearchActive = active, searchQuery = if (!active) "" else it.searchQuery) }
    }

    fun updateSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    /** 검색 결과 친구 목록 — 초성 포함 */
    fun filteredFriends(): List<FriendProfile> {
        val q = _uiState.value.searchQuery
        if (q.isBlank()) return sortedFriends()
        return _uiState.value.friends.filter { friend ->
            KoreanChosung.matches(q, friend.name) ||
                    KoreanChosung.matches(q, friend.bio)
        }
    }

    /** 검색 결과 상태메시지 목록 — 초성 포함 */
    fun filteredStatusMessages(): List<StatusMessage> {
        val q = _uiState.value.searchQuery
        if (q.isBlank()) return emptyList()
        return _uiState.value.statusMessages.filter { sm ->
            KoreanChosung.matches(q, sm.authorName) ||
                    KoreanChosung.matches(q, sm.content)
        }
    }

    fun changeSortOption(option: FriendSortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    private fun sortedFriends(): List<FriendProfile> {
        val list = _uiState.value.friends
        return when (_uiState.value.sortOption) {
            FriendSortOption.GANADA      -> list.sortedBy { it.name }
            FriendSortOption.RECENT      -> list  // TODO: 마지막 메시지 시간 기준
            FriendSortOption.ONLINE_FIRST-> list.sortedByDescending { it.isOnline }
        }
    }

    fun openStatusDetail(sm: StatusMessage) {
        _uiState.update { it.copy(expandedStatus = sm, isReplying = false, replyText = "", replySent = false) }
    }

    fun closeStatusDetail() {
        _uiState.update { it.copy(expandedStatus = null, isReplying = false, replyText = "", replySent = false) }
    }

    fun openReply() {
        _uiState.update { it.copy(isReplying = true) }
    }

    fun cancelReply() {
        _uiState.update { it.copy(isReplying = false, replyText = "") }
    }

    fun updateReplyText(text: String) {
        _uiState.update { it.copy(replyText = text) }
    }

    /**
     * 상태메시지 답장 전송.
     * 1:1방 idempotent — createChatRoom 호출하면 기존/신규 chatRoomId 받음. 클라 캐시 lookup 불필요.
     * statusPreview는 인용 카드 표시용 — 백엔드 message API의 옵션 필드.
     */
    fun sendReply() {
        val state  = _uiState.value
        val target = state.expandedStatus ?: return
        val text   = state.replyText.trim().ifBlank { return }
        val preview = target.preview15()

        viewModelScope.launch {
            when (val createResult = chatRoomRepository.createChatRoom(target.authorName, isGroup = false, listOf(target.authorId))) {
                is ApiResult.Success -> {
                    val chatRoomId = createResult.data?.chatRoomId
                    if (chatRoomId == null) {
                        _uiState.update { it.copy(toastMessage = "채팅방 생성 실패") }
                        return@launch
                    }
                    when (val sendResult = messageRepository.sendTextMessage(chatRoomId, text, statusPreview = preview)) {
                        is ApiResult.Success -> _uiState.update { it.copy(replySent = true, replyText = "") }
                        is ApiResult.Error, is ApiResult.NetworkError ->
                            _uiState.update { it.copy(toastMessage = sendResult.userMessage()) }
                    }
                }
                is ApiResult.Error, is ApiResult.NetworkError ->
                    _uiState.update { it.copy(toastMessage = createResult.userMessage()) }
            }
        }
    }

    fun consumeReplySent() {
        _uiState.update { it.copy(replySent = false) }
    }

    fun openStatusPage()  { _uiState.update { it.copy(isStatusPageOpen = true)  } }
    fun closeStatusPage() { _uiState.update { it.copy(isStatusPageOpen = false) } }

    fun openWriteStatus() {
        val existing = _uiState.value.myStatusMessage?.content ?: ""
        _uiState.update { it.copy(isWritingStatus = true, draftStatusText = existing) }
    }

    fun closeWriteStatus() {
        _uiState.update { it.copy(isWritingStatus = false, draftStatusText = "") }
    }

    fun updateDraftStatus(text: String) {
        if (text.length <= 150) _uiState.update { it.copy(draftStatusText = text) }
    }

    /** 작성 완료 — 내 상태메시지 업데이트, 목록 맨 앞으로 이동 */
    fun submitStatus() {
        val text = _uiState.value.draftStatusText.trim().ifBlank { return }
        val newSm = StatusMessage(
            id           = "sm_my",
            authorId     = FriendDemoData.MY_USER_ID,
            authorName   = FriendDemoData.MY_NAME,
            profileImageUrl = null,
            content      = text,
            createdAt    = System.currentTimeMillis(),
            isMyMessage  = true,
        )
        // 기존 목록에서 내 메시지 제거 후 맨 앞에 삽입
        val updated = _uiState.value.statusMessages
            .filter { it.authorId != FriendDemoData.MY_USER_ID }
            .toMutableList()
            .apply { add(0, newSm) }

        _uiState.update {
            it.copy(
                myStatusMessage  = newSm,
                statusMessages   = updated,
                isWritingStatus  = false,
                draftStatusText  = "",
                toastMessage     = "상태메시지가 등록되었습니다.",
            )
        }
    }

    fun selectFriend(friend: FriendProfile) {
        _uiState.update { it.copy(selectedFriend = friend, selectedFriendDetail = null) }
        viewModelScope.launch {
            val res = userRepository.getUserProfile(friend.id)
            if (res is ApiResult.Success) {
                _uiState.update { s ->
                    s.copy(
                        selectedFriendDetail = UserDetail(
                            age             = res.data.age,
                            gender          = res.data.gender,
                            consecutiveDays = res.data.consecutiveDays,
                            isFriend        = s.selectedFriend?.isFriend ?: true,
                        ),
                    )
                }
            }
        }
    }
    fun clearSelectedFriend() { _uiState.update { it.copy(selectedFriend = null, selectedFriendDetail = null) } }

    /**
     * fromUserId로 친구 매칭 후 프로필 표시 (푸시 클릭 라우팅용).
     * friends 비어있으면 한 번 새로 가져옴. 그래도 매칭 실패면 no-op.
     */
    fun selectFriendByUserId(userId: Long) {
        viewModelScope.launch {
            _uiState.value.friends.find { it.id == userId }?.let {
                selectFriend(it); return@launch
            }
            when (val r = friendRepository.getFriends()) {
                is ApiResult.Success -> {
                    val ids = presenceRepository.onlineIds.value
                    val fresh = r.data.friends.map { it.toFriendProfile(ids) }
                    _uiState.update { it.copy(friends = fresh) }
                    fresh.find { it.id == userId }?.let { selectFriend(it) }
                }
                is ApiResult.Error, is ApiResult.NetworkError -> Unit
            }
        }
    }

    fun onFollowToggle(id: Long) {
        _uiState.update { s ->
            val ids = s.followedFriendIds
            s.copy(followedFriendIds = if (ids.contains(id)) ids - id else ids + id)
        }
    }

    fun addFriendById(friendId: Long) {
        viewModelScope.launch {
            when (val r = friendRepository.addFriend(friendId)) {
                is ApiResult.Success -> {
                    _uiState.update { s ->
                        s.copy(selectedFriend = s.selectedFriend?.let { if (it.id == friendId) it.copy(isFriend = true) else it })
                    }
                    loadFriends()
                }
                is ApiResult.Error, is ApiResult.NetworkError ->
                    _uiState.update { it.copy(toastMessage = r.userMessage()) }
            }
        }
    }

    fun removeFriendById(friendId: Long) {
        viewModelScope.launch {
            when (val r = friendRepository.removeFriend(friendId)) {
                is ApiResult.Success -> {
                    _uiState.update { s ->
                        s.copy(selectedFriend = s.selectedFriend?.let { if (it.id == friendId) it.copy(isFriend = false) else it })
                    }
                    loadFriends()
                }
                is ApiResult.Error, is ApiResult.NetworkError ->
                    _uiState.update { it.copy(toastMessage = r.userMessage()) }
            }
        }
    }

    fun blockFriendById(friendId: Long) {
        viewModelScope.launch {
            when (val r = blockRepository.blockUser(friendId)) {
                is ApiResult.Success -> {
                    _uiState.update { s ->
                        s.copy(
                            friends       = s.friends.filter { it.id != friendId },
                            selectedFriend = null,
                            toastMessage  = "차단되었습니다.",
                        )
                    }
                }
                is ApiResult.Error, is ApiResult.NetworkError ->
                    _uiState.update { it.copy(toastMessage = r.userMessage()) }
            }
        }
    }

    fun consumeToast() { _uiState.update { it.copy(toastMessage = null) } }

    fun visibleStatusMessages(): List<StatusMessage> =
        _uiState.value.statusMessages.filter {
            it.isVisible() && it.authorId != FriendDemoData.MY_USER_ID
        }
}

/**
 * Friend(DTO) → FriendProfile(UI) 매핑.
 * isOnline은 RTDB `presence/` 구독 결과에서 채움 ([PresenceRepository]).
 */
private fun Friend.toFriendProfile(onlineIds: Set<Long>) = FriendProfile(
    id                = userId,
    name              = name,
    profileImageUrl   = profileImageUrl,
    nationality       = country,
    nativeLanguages   = emptyList(),
    learningLanguages = languages.map { it.language },
    interests         = tags.map { it.tag },
    bio               = bio,
    isOnline          = userId in onlineIds,
    isFriend          = true,
)
