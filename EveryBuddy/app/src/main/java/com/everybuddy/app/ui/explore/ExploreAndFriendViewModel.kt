package com.everybuddy.app.ui.explore

import androidx.lifecycle.ViewModel
import com.everybuddy.app.data.dummy.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// 친구 탭 (FriendScreen.kt 전용 - 구버전 심플 뷰)
// TODO: FriendMainScreen으로 완전 교체 후 이 파일 제거 예정

data class FriendUiState(
    val isLoading   : Boolean         = false,
    val friends     : List<DummyUser> = friendUsers,
    val isSearchMode: Boolean         = false,
    val searchQuery : String          = "",
    val currentSort : String          = "최신순",
)

val FriendUiState.filteredFriends: List<DummyUser>
    get() = if (searchQuery.isEmpty()) friends
            else friends.filter { it.name.contains(searchQuery, ignoreCase = true) }

@HiltViewModel
class FriendViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(FriendUiState())
    val uiState: StateFlow<FriendUiState> = _uiState.asStateFlow()

    fun onSearchToggle() {
        _uiState.update { it.copy(isSearchMode = !it.isSearchMode) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    // TODO: sort 값에 따라 friends 재정렬 로직 추가
    fun onSortChange(sort: String) {
        _uiState.update { it.copy(currentSort = sort) }
    }
}
