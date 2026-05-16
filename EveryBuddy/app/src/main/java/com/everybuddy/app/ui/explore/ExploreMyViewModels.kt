package com.everybuddy.app.ui.explore

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.BuildConfig
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.local.TokenManager
import com.everybuddy.app.data.repository.AuthRepository
import com.everybuddy.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

// ExploreViewModel — 탐색 탭
data class ExploreUiState(
    val selectedTab        : Int               = 0,   // 0=추천친구, 1=필터
    val cardSet            : List<DiscoverUser> = if (BuildConfig.USE_DUMMY_DATA) ExploreDemo.randomCards else emptyList(),
    val currentCardIndex   : Int               = 0,
    val tagMatchUsers      : List<DiscoverUser> = if (BuildConfig.USE_DUMMY_DATA) ExploreDemo.tagMatchUsers else emptyList(),
    val learningLangUsers  : List<DiscoverUser> = if (BuildConfig.USE_DUMMY_DATA) ExploreDemo.learningLangUsers else emptyList(),
    val isRefreshing       : Boolean           = false,
    val filterSettings     : FilterSettings    = FilterSettings(),
    val filterResults      : List<DiscoverUser> = emptyList(),
    val isFilterApplied    : Boolean           = false,
    val filterHasNext      : Boolean           = false,
    val isFilterScreenOpen : Boolean           = false,
    val selectedUser       : DiscoverUser?     = null,
    val isProfileOpen      : Boolean           = false,
    val followedUserIds    : Set<Long>         = emptySet(),
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

    fun onFollowToggle(userId: Long) {
        _uiState.update { s ->
            val ids = s.followedUserIds
            s.copy(followedUserIds = if (ids.contains(userId)) ids - userId else ids + userId)
        }
    }
}

// MyViewModel — 마이페이지
data class MyUiState(
    val profile         : MyProfile    = if (BuildConfig.USE_DUMMY_DATA) ExploreDemo.myProfile else MyProfile(),
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
    val isSaving        : Boolean      = false,  // 저장/삭제 API 진행 중 — 버튼 중복 클릭 방지용
    val pendingImageUri : String?      = null,   // 갤러리에서 새로 고른 이미지 URI (저장 시 multipart로 업로드)
)

@HiltViewModel
class MyViewModel @Inject constructor(
    @ApplicationContext private val appContext : Context,
    private val userRepository : UserRepository,
    private val tokenManager   : TokenManager,
    private val authRepository : AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    private val _logoutComplete = MutableStateFlow(false)
    val logoutComplete: StateFlow<Boolean> = _logoutComplete.asStateFlow()

    init { loadMyProfile() }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _logoutComplete.value = true
        }
    }

    private fun loadMyProfile() {
        viewModelScope.launch {
            val userId = tokenManager.userId.first() ?: return@launch

            val profileDeferred = async { userRepository.getUserProfile(userId) }
            val tagsDeferred    = async { userRepository.getUserTags(userId) }
            val langsDeferred   = async { userRepository.getUserLanguages(userId) }

            val apiProfile = (profileDeferred.await() as? ApiResult.Success)?.data ?: return@launch
            val tags = (tagsDeferred.await() as? ApiResult.Success)?.data
                ?.map { UserTag(it.tag, it.category) }
                ?: _uiState.value.profile.tags
            val langs = (langsDeferred.await() as? ApiResult.Success)?.data?.languages
                ?.map { UserLanguage(it.language, it.level) }
                ?: _uiState.value.profile.learningLanguages

            _uiState.update { s ->
                s.copy(profile = s.profile.copy(
                    userId            = userId,
                    name              = apiProfile.name,
                    profileImageUrl   = apiProfile.profileImageUrl,
                    country           = apiProfile.country,
                    age               = apiProfile.age,
                    // API "yyyy-MM-dd" → UI 표시 "yyyy.MM.dd". null/빈값이면 빈 문자열
                    birthday          = apiProfile.birthday?.replace("-", ".").orEmpty(),
                    gender            = when (apiProfile.gender.uppercase()) {
                                            "MALE" -> "남성"; "FEMALE" -> "여성"; else -> apiProfile.gender
                                        },
                    bio               = apiProfile.bio,
                    consecutiveDays   = apiProfile.consecutiveDays,
                    tags              = tags,
                    learningLanguages = langs,
                ))
            }
        }
    }

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

    fun closeEdit() { _uiState.update { it.copy(isEditMode = false, pendingImageUri = null) } }

    fun updateEditName(v: String)     { _uiState.update { it.copy(editName     = v) } }
    fun updateEditBio(v: String)      { if (v.length <= 150) _uiState.update { it.copy(editBio = v) } }
    fun updateEditBirthday(v: String) { _uiState.update { it.copy(editBirthday = v) } }
    fun updateEditGender(v: String)   { _uiState.update { it.copy(editGender   = v) } }
    fun updateEditCountry(v: String)  { _uiState.update { it.copy(editCountry  = v) } }
    fun setPendingImageUri(uri: String?) { _uiState.update { it.copy(pendingImageUri = uri) } }

    fun saveEdit() {
        val s = _uiState.value
        if (s.isSaving) return   // 응답 대기 중 중복 클릭 방지
        val apiGender    = when (s.editGender) { "남성" -> "MALE"; "여성" -> "FEMALE"; else -> s.editGender }
        // UI 입력 "2005.11.30" → API 요구 "2005-11-30". 빈 값은 null로 (PATCH에서 미변경)
        val apiBirthday  = s.editBirthday.takeIf { it.isNotBlank() }?.replace(".", "-")
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            // 갤러리에서 새로 고른 이미지가 있으면 cache dir에 임시 파일로 복사 후 multipart로 전송
            val pendingUri = s.pendingImageUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            val (imageFile, imageMime) = pendingUri?.let { uri ->
                val mime = appContext.contentResolver.getType(uri) ?: "image/jpeg"
                uriToTempFile(uri) to mime
            } ?: (null to "image/jpeg")

            val res = userRepository.updateMyProfile(
                name          = s.editName,
                bio           = s.editBio,
                birthday      = apiBirthday,
                gender        = apiGender,
                country       = s.editCountry,
                profileImage  = imageFile,
                imageMimeType = imageMime,
            )
            // 업로드 결과와 무관하게 임시 파일은 즉시 정리
            imageFile?.delete()

            when (res) {
                is ApiResult.Success      -> {
                    loadMyProfile()   // 서버 응답으로 동기화
                    _uiState.update { it.copy(
                        isEditMode      = false,
                        isSaving        = false,
                        pendingImageUri = null,
                        toastMessage    = "프로필이 저장되었습니다.",
                    ) }
                }
                is ApiResult.Error        -> _uiState.update { it.copy(
                    isSaving     = false,
                    toastMessage = res.message,
                ) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(
                    isSaving     = false,
                    toastMessage = "네트워크 연결을 확인해주세요.",
                ) }
            }
        }
    }

    // 갤러리 URI를 cache dir의 임시 File로 복사. 실패 시 null.
    private suspend fun uriToTempFile(uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val tempFile = File.createTempFile("upload_", ".img", appContext.cacheDir)
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            tempFile
        }.getOrNull()
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
    fun saveTagEdit() {
        val s = _uiState.value
        if (s.isSaving) return
        val tags = s.editingTags
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val res = userRepository.updateMyTags(tags.map { it.tag })
            when (res) {
                is ApiResult.Success      -> _uiState.update { it.copy(
                    profile       = it.profile.copy(tags = tags),
                    isTagEditOpen = false,
                    isSaving      = false,
                ) }
                is ApiResult.Error        -> _uiState.update { it.copy(
                    isSaving     = false,
                    toastMessage = res.message,
                ) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(
                    isSaving     = false,
                    toastMessage = "네트워크 연결을 확인해주세요.",
                ) }
            }
        }
    }

    // 언어 상세
    fun openLanguage(code: String)  { _uiState.update { it.copy(openLanguageCode = code) } }
    fun closeLanguage()             { _uiState.update { it.copy(openLanguageCode = null) } }

    fun saveLanguageLevel(language: String, level: Int) {
        val s = _uiState.value
        if (s.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val res = userRepository.updateMyLanguageLevel(language, level)
            when (res) {
                is ApiResult.Success      -> _uiState.update { st ->
                    st.copy(
                        profile = st.profile.copy(
                            learningLanguages = st.profile.learningLanguages.map {
                                if (it.language.equals(language, ignoreCase = true)) it.copy(level = level) else it
                            },
                        ),
                        openLanguageCode = null,
                        isSaving         = false,
                    )
                }
                is ApiResult.Error        -> _uiState.update { it.copy(
                    isSaving     = false,
                    toastMessage = res.message,
                ) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(
                    isSaving     = false,
                    toastMessage = "네트워크 연결을 확인해주세요.",
                ) }
            }
        }
    }

    // 서브 메뉴
    fun openSubMenu(key: String)    { _uiState.update { it.copy(openSubMenu = key) } }
    fun closeSubMenu()              { _uiState.update { it.copy(openSubMenu = null) } }

    // 토스트 소비
    fun consumeToast() { _uiState.update { it.copy(toastMessage = null) } }
}
