package com.everybuddy.app.ui.explore

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// ExploreViewModel — 탐색 탭
data class ExploreUiState(
    val selectedTab        : Int               = 0,   // 0=추천친구, 1=필터
    val cardSet            : List<DiscoverUser> = ExploreDemo.randomCards,
    val currentCardIndex   : Int               = 0,
    val tagMatchUsers      : List<DiscoverUser> = ExploreDemo.tagMatchUsers,
    val learningLangUsers  : List<DiscoverUser> = ExploreDemo.learningLangUsers,
    val isRefreshing       : Boolean           = false,
    val filterSettings     : FilterSettings    = FilterSettings(),
    val filterResults      : List<DiscoverUser> = emptyList(),
    val isFilterApplied    : Boolean           = false,
    val filterHasNext      : Boolean           = false,
    val isFilterScreenOpen : Boolean           = false,
    val selectedUser       : DiscoverUser?     = null,
    val isProfileOpen      : Boolean           = false,
)

@HiltViewModel
class ExploreViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    fun selectTab(tab: Int) { _uiState.update { it.copy(selectedTab = tab) } }

    // 카드 인터렉션
    fun nextCard() {
        val state = _uiState.value
        val next  = state.currentCardIndex + 1
        if (next >= state.cardSet.size) {
            _uiState.update { it.copy(cardSet = state.cardSet.shuffled(), currentCardIndex = 0) }
        } else {
            _uiState.update { it.copy(currentCardIndex = next) }
        }
    }

    fun prevCard() {
        val state = _uiState.value
        if (state.currentCardIndex > 0) {
            _uiState.update { it.copy(currentCardIndex = state.currentCardIndex - 1) }
        }
    }

    // TODO: 실제 API 호출 → GET /api/v1/discover/random
    fun refresh() {
        _uiState.update { it.copy(
            cardSet          = it.cardSet.shuffled(),
            currentCardIndex = 0,
            isRefreshing     = false,
        ) }
    }

    // 필터 화면 열기/닫기
    fun openFilterScreen()  { _uiState.update { it.copy(isFilterScreenOpen = true) } }
    fun closeFilterScreen() { _uiState.update { it.copy(isFilterScreenOpen = false) } }

    // TODO: 실제 API 호출 → GET /api/v1/discover/filter
    fun applyFilter(settings: FilterSettings) {
        val results = ExploreDemo.filterUsers.let { list ->
            var r = list
            if (settings.gender != GenderFilter.ALL) r = r.take(4)
            if (settings.isOnline) r = r.filter { it.isOnline }
            r.sortedBy { it.name }
        }
        _uiState.update { it.copy(
            filterSettings     = settings,
            filterResults      = results,
            isFilterApplied    = !settings.isEmpty(),
            isFilterScreenOpen = false,
            selectedTab        = 1,
        ) }
    }

    fun resetFilter() {
        _uiState.update { it.copy(
            filterSettings  = FilterSettings(),
            filterResults   = emptyList(),
            isFilterApplied = false,
        ) }
    }

    // 상대 프로필 팝업
    fun openProfile(user: DiscoverUser) { _uiState.update { it.copy(selectedUser = user, isProfileOpen = true) } }
    fun closeProfile()                  { _uiState.update { it.copy(selectedUser = null, isProfileOpen = false) } }
}

// MyViewModel — 마이페이지
data class MyUiState(
    val profile         : MyProfile    = ExploreDemo.myProfile,
    val isEditMode      : Boolean      = false,
    val editName        : String       = "",
    val editBio         : String       = "",
    val editBirthday    : String       = "",
    val editGender      : String       = "",
    val editCountry     : String       = "",
    val toastMessage    : String?      = null,
    val isTagEditOpen   : Boolean      = false,
    val editingTags     : List<UserTag> = emptyList(),
    val openLanguageCode: String?      = null,
    val openSubMenu     : String?      = null,   // "guide" | "notice" | "settings" | "version"
)

@HiltViewModel
class MyViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    // 프로필 수정
    fun openEdit() {
        val p = _uiState.value.profile
        _uiState.update { it.copy(
            isEditMode   = true,
            editName     = p.name,
            editBio      = p.bio,
            editBirthday = p.birthday,
            editGender   = p.gender,
            editCountry  = p.country,
        ) }
    }

    fun closeEdit() { _uiState.update { it.copy(isEditMode = false) } }

    fun updateEditName(v: String)     { _uiState.update { it.copy(editName     = v) } }
    fun updateEditBio(v: String)      { if (v.length <= 150) _uiState.update { it.copy(editBio = v) } }
    fun updateEditBirthday(v: String) { _uiState.update { it.copy(editBirthday = v) } }
    fun updateEditGender(v: String)   { _uiState.update { it.copy(editGender   = v) } }
    fun updateEditCountry(v: String)  { _uiState.update { it.copy(editCountry  = v) } }

    // TODO: 실제 API 호출 → PUT /api/v1/users/profile
    fun saveEdit() {
        val s = _uiState.value
        val updated = s.profile.copy(
            name     = s.editName,
            bio      = s.editBio,
            birthday = s.editBirthday,
            gender   = s.editGender,
            country  = s.editCountry,
        )
        _uiState.update { it.copy(profile = updated, isEditMode = false, toastMessage = "프로필이 저장되었습니다.") }
    }

    // 태그 편집
    fun openTagEdit() {
        _uiState.update { it.copy(isTagEditOpen = true, editingTags = it.profile.tags) }
    }
    fun closeTagEdit() { _uiState.update { it.copy(isTagEditOpen = false) } }
    fun toggleTag(tag: UserTag) {
        val current = _uiState.value.editingTags.toMutableList()
        if (current.any { it.tag == tag.tag }) current.removeAll { it.tag == tag.tag }
        else if (current.size < 15) current.add(tag)
        _uiState.update { it.copy(editingTags = current) }
    }
    // TODO: 실제 API 호출 → PUT /api/v1/users/tags
    fun saveTagEdit() {
        _uiState.update { it.copy(profile = it.profile.copy(tags = it.editingTags), isTagEditOpen = false) }
    }

    // 언어 상세
    fun openLanguage(code: String)  { _uiState.update { it.copy(openLanguageCode = code) } }
    fun closeLanguage()             { _uiState.update { it.copy(openLanguageCode = null) } }

    // 서브 메뉴
    fun openSubMenu(key: String)    { _uiState.update { it.copy(openSubMenu = key) } }
    fun closeSubMenu()              { _uiState.update { it.copy(openSubMenu = null) } }

    // 토스트 소비
    fun consumeToast() { _uiState.update { it.copy(toastMessage = null) } }
}
