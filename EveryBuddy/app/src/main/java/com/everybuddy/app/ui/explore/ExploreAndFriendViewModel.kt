package com.everybuddy.app.ui.explore

import androidx.lifecycle.ViewModel
import com.everybuddy.app.data.dummy.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// 탐색 탭

/**
 * ExploreUiState — 탐색 탭 전체 상태
 *
 * TODO API: GET /api/v1/users → 서버에서 추천 유저 목록 수신 후 users 대체
 * TODO 더미: exploreUsers 더미 데이터 → API 연동 후 제거
 * TODO 필터: selectedCategory → 서버 필터 파라미터로 매핑
 */
data class ExploreUiState(
    val isLoading       : Boolean         = false,
    val users           : List<DummyUser> = exploreUsers,
    val selectedCategory: String          = "전체",
)

/**
 * ExploreViewModel — 탐색 탭 ViewModel
 *
 * TODO API: SharedUserRepository 또는 별도 UserRepository로 유저 목록 로드
 * TODO 검색: 검색 UI 연동 후 onSearchQueryChange에서 users 필터링
 * TODO 탭: 탭(추천 친구 | 필터) 선택에 따라 users 재정렬/필터링
 * TODO 친구 추가: onFriendToggle → POST /api/v1/friends/{userId}
 */
@HiltViewModel
class ExploreViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    /**
     * TODO API: POST /api/v1/friends/{userId} 친구 추가/해제
     *             성공 시 users 목록에서 해당 유저의 isFriend 상태 토글
     */
    fun onFriendToggle(userId: String) {
        println("Friend Toggle: $userId") // TODO: API 연동 후 제거
    }

    /**
     * TODO UI: 검색창 열기/닫기 애니메이션 처리
     */
    fun onSearchToggle() {
        // TODO: _uiState.update { it.copy(isSearchMode = !it.isSearchMode) }
    }

    /**
     * TODO 검색: 입력값으로 users 목록 실시간 필터링
     *             또는 서버 검색 API(GET /api/v1/users?q=query) 호출
     */
    fun onSearchQueryChange(query: String) {
        // TODO: _uiState.update { it.copy(users = exploreUsers.filter { u -> u.name.contains(query) }) }
    }

    fun onTabSelect(tab: String) {
        _uiState.update { it.copy(selectedCategory = tab) }
    }
}


// 친구 탭

/**
 * FriendUiState — 친구 탭 전체 상태
 *
 * TODO API: GET /api/v1/friends → 서버에서 친구 목록 수신 후 friends 대체
 * TODO 더미: friendUsers 더미 데이터 → API 연동 후 제거
 * TODO 정렬: currentSort → 서버 정렬 파라미터(최신순/이름순 등)로 매핑
 */
data class FriendUiState(
    val isLoading   : Boolean         = false,
    val friends     : List<DummyUser> = friendUsers,
    val isSearchMode: Boolean         = false,
    val searchQuery : String          = "",
    val currentSort : String          = "최신순",
)

/** 검색어 기반 필터링 — TODO: 초성 검색 지원 추가 */
val FriendUiState.filteredFriends: List<DummyUser>
    get() = if (searchQuery.isEmpty()) friends
            else friends.filter { it.name.contains(searchQuery, ignoreCase = true) }

/**
 * FriendViewModel — 친구 탭 ViewModel
 *
 * TODO API: SharedUserRepository 또는 별도 FriendRepository로 친구 목록 로드
 * TODO 정렬: onSortChange → 최신순/이름순 등 정렬 옵션 적용
 * TODO 삭제: 친구 삭제 기능 (DELETE /api/v1/friends/{userId})
 */
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

    /**
     * TODO 정렬: sort 값(최신순/이름순)에 따라 friends 재정렬 로직 추가
     */
    fun onSortChange(sort: String) {
        _uiState.update { it.copy(currentSort = sort) }
    }
}
