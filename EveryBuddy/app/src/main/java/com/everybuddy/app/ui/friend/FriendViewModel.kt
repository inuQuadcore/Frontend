package com.everybuddy.app.ui.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.BuildConfig
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.Friend
import com.everybuddy.app.data.dto.userMessage
import com.everybuddy.app.data.repository.FriendRepository
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
    val statusMessages      : List<StatusMessage>   = if (BuildConfig.DEBUG) FriendDemoData.statusMessages.toList() else emptyList(),
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

    // 팔로우 상태
    val followedFriendIds   : Set<Long>             = emptySet(),

    // 토스트
    val toastMessage        : String?               = null,
)

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendUiState())
    val uiState: StateFlow<FriendUiState> = _uiState.asStateFlow()

    init { loadFriends() }

    fun loadFriends() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val r = friendRepository.getFriends()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(friends = r.data.friends.map { f -> f.toFriendProfile() }, isLoading = false)
                }
                is ApiResult.Error, is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isLoading = false, toastMessage = r.userMessage()) }
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

    fun selectFriend(friend: FriendProfile) { _uiState.update { it.copy(selectedFriend = friend) } }
    fun clearSelectedFriend() { _uiState.update { it.copy(selectedFriend = null) } }

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

    fun consumeToast() { _uiState.update { it.copy(toastMessage = null) } }

    fun visibleStatusMessages(): List<StatusMessage> =
        _uiState.value.statusMessages.filter {
            it.isVisible() && it.authorId != FriendDemoData.MY_USER_ID
        }
}

// Friend(DTO) → FriendProfile(UI) 매핑.
// isOnline은 RTDB presence/{userId} 구독으로 별도 채움 — 별도 작업으로 분리.
private fun Friend.toFriendProfile() = FriendProfile(
    id                = userId,
    name              = name,
    profileImageUrl   = profileImageUrl,
    nationality       = country,
    nativeLanguages   = emptyList(),
    learningLanguages = languages.map { it.language },
    interests         = tags.map { it.tag },
    bio               = bio,
    isOnline          = false,
    isFriend          = true,
)
