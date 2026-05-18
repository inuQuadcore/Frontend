package com.everybuddy.app.ui.explore

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.cache.UserSummaryCache
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.userMessage
import com.everybuddy.app.data.firebase.PresenceRepository
import com.everybuddy.app.data.local.TokenManager
import com.everybuddy.app.data.repository.AuthRepository
import com.everybuddy.app.data.repository.BlockRepository
import com.everybuddy.app.data.repository.DiscoverRepository
import com.everybuddy.app.data.repository.FriendRepository
import com.everybuddy.app.data.repository.TranslateRepository
import com.everybuddy.app.data.repository.UserRepository
import com.everybuddy.app.ui.onboarding.sampleTags
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
import com.everybuddy.app.data.dto.DiscoverUser as DiscoverUserDto
import com.everybuddy.app.data.dto.LanguageLevel as LanguageLevelDto
import com.everybuddy.app.data.dto.UserPublicProfileResponse
import com.everybuddy.app.data.dto.UserTag as UserTagDto

// ExploreViewModel — 탐색 탭
data class ExploreUiState(
    val selectedTab        : Int               = 0,   // 0=추천친구, 1=필터
    val cardSet            : List<DiscoverUser> = emptyList(),
    val currentCardIndex   : Int               = 0,
    val tagMatchUsers      : List<DiscoverUser> = emptyList(),
    val tagMatchHasNext    : Boolean            = false,
    val tagMatchNextCursor : Long?              = null,
    val isTagMatchRefreshing  : Boolean         = false,
    val isTagMatchLoadingMore : Boolean         = false,
    val learningLangUsers     : List<DiscoverUser> = emptyList(),
    val learningLangHasNext   : Boolean         = false,
    val learningLangNextCursor: Long?           = null,
    val isLearningLangRefreshing  : Boolean     = false,
    val isLearningLangLoadingMore : Boolean     = false,
    val myFirstTag         : UserTag?          = null,   // 내 첫 번째 태그 — 관심사 섹션 헤더 + /filter?tags=
    val myPrimaryLanguage  : String?           = null,   // 내 주 언어(isPrimary) — 언어 섹션 헤더 + /filter?languages=
    val isRefreshing       : Boolean           = false,
    val filterSettings     : FilterSettings    = FilterSettings(),
    val filterResults      : List<DiscoverUser> = emptyList(),
    val isFilterApplied    : Boolean           = false,
    val filterHasNext      : Boolean           = false,
    val filterNextCursor   : Long?             = null,
    val isFilterLoading    : Boolean           = false,
    val isFilterLoadingMore: Boolean           = false,
    val isFilterScreenOpen : Boolean           = false,
    val selectedUser       : DiscoverUser?     = null,
    val selectedUserDetail : UserDetail?       = null,
    val isProfileOpen      : Boolean           = false,
    val translatedBio      : String?           = null,
    val isBioTranslating   : Boolean           = false,
)

// API에 없는 필드: age, consecutiveDays는 기본값. isOnline은 RTDB에서 받음.
private fun DiscoverUserDto.toUi(): DiscoverUser = DiscoverUser(
    userId          = userId,
    name            = name,
    profileImageUrl = profileImageUrl,
    country         = country,
    bio             = bio,
    languages       = languages.map { it.toUi() },
    tags            = tags.map { it.toUi() },
    lastSeenAt      = lastSeenAt.orEmpty(),
)

private fun LanguageLevelDto.toUi(): UserLanguage = UserLanguage(
    language = language,
    level    = level,
)

private fun UserTagDto.toUi(): UserTag {
    val sample = sampleTags.firstOrNull { it.apiValue == tag }
    return UserTag(
        tag         = tag,
        category    = category,
        emoji       = sample?.emoji ?: "",
        displayName = sample?.label ?: tag,
    )
}

private fun UserPublicProfileResponse.toUserDetail(): UserDetail = UserDetail(
    age             = age,
    gender          = gender,
    consecutiveDays = consecutiveDays,
    isFriend        = isFriend ?: false,
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val discoverRepository  : DiscoverRepository,
    private val userRepository      : UserRepository,
    private val friendRepository    : FriendRepository,
    private val presenceRepository  : PresenceRepository,
    private val tokenManager        : TokenManager,
    private val filterPreferences   : ExploreFilterPreferences,
    private val translateRepository : TranslateRepository,
    private val blockRepository     : BlockRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        restoreFilter()
        refresh()
        observePresence()
        loadMyContext()
    }

    /**
     * 내 첫 번째 태그 + 주 언어(isPrimary) 로딩 후 두 추천 섹션 첫 페이지 받음.
     * - tagMatchUsers: /filter?tags=<내 첫 태그>
     * - learningLangUsers: /filter?languages=<내 주 언어>
     * 더보기 화면 진입 후 스크롤 끝 도달 시 loadMoreTagMatch/LearningLang으로 다음 페이지.
     */
    private fun loadMyContext() {
        viewModelScope.launch {
            val userId = tokenManager.userId.first() ?: return@launch
            val tagsDeferred = async { userRepository.getUserTags(userId) }
            val langsDeferred = async { userRepository.getUserLanguages(userId) }

            val allTagDtos  = (tagsDeferred.await() as? ApiResult.Success)?.data ?: emptyList()
            val firstTagDto = allTagDtos.firstOrNull()
            val firstTagUi  = firstTagDto?.toUi()
            val primaryLang = (langsDeferred.await() as? ApiResult.Success)?.data?.languages
                ?.firstOrNull { it.isPrimary }?.language

            _uiState.update { it.copy(myFirstTag = firstTagUi, myPrimaryLanguage = primaryLang) }

            firstTagDto?.let  { fetchTagMatchPage(it.tag, lastUserId = null, isFirstPage = true) }
            primaryLang?.let  { fetchLearningLangPage(it,  lastUserId = null, isFirstPage = true) }
        }
    }

    /** 관심사 추천 섹션 새로고침 — 더보기 화면 PullToRefresh. 첫 페이지부터 다시. */
    fun refreshTagMatch() {
        val tag = _uiState.value.myFirstTag?.tag ?: return
        _uiState.update { it.copy(isTagMatchRefreshing = true) }
        viewModelScope.launch { fetchTagMatchPage(tag, lastUserId = null, isFirstPage = true) }
    }

    /** 더보기 화면 스크롤 끝 도달 시 다음 페이지. */
    fun loadMoreTagMatch() {
        val state = _uiState.value
        if (state.isTagMatchLoadingMore || !state.tagMatchHasNext) return
        val cursor = state.tagMatchNextCursor ?: return
        val tag    = state.myFirstTag?.tag ?: return
        _uiState.update { it.copy(isTagMatchLoadingMore = true) }
        viewModelScope.launch { fetchTagMatchPage(tag, lastUserId = cursor, isFirstPage = false) }
    }

    private suspend fun fetchTagMatchPage(tag: String, lastUserId: Long?, isFirstPage: Boolean) {
        val res = discoverRepository.discoverFilter(tags = listOf(tag), lastUserId = lastUserId)
        when (res) {
            is ApiResult.Success -> {
                val newUsers = res.data.users.map { it.toUi() }.applyPresence(presenceRepository.onlineIds.value)
                _uiState.update { s ->
                    s.copy(
                        tagMatchUsers         = if (isFirstPage) newUsers else s.tagMatchUsers + newUsers,
                        tagMatchHasNext       = res.data.hasNext,
                        tagMatchNextCursor    = res.data.nextCursor,
                        isTagMatchRefreshing  = false,
                        isTagMatchLoadingMore = false,
                    )
                }
            }
            is ApiResult.Error, is ApiResult.NetworkError ->
                _uiState.update { it.copy(isTagMatchRefreshing = false, isTagMatchLoadingMore = false) }
        }
    }

    fun refreshLearningLang() {
        val lang = _uiState.value.myPrimaryLanguage ?: return
        _uiState.update { it.copy(isLearningLangRefreshing = true) }
        viewModelScope.launch { fetchLearningLangPage(lang, lastUserId = null, isFirstPage = true) }
    }

    fun loadMoreLearningLang() {
        val state = _uiState.value
        if (state.isLearningLangLoadingMore || !state.learningLangHasNext) return
        val cursor = state.learningLangNextCursor ?: return
        val lang   = state.myPrimaryLanguage ?: return
        _uiState.update { it.copy(isLearningLangLoadingMore = true) }
        viewModelScope.launch { fetchLearningLangPage(lang, lastUserId = cursor, isFirstPage = false) }
    }

    private suspend fun fetchLearningLangPage(lang: String, lastUserId: Long?, isFirstPage: Boolean) {
        val res = discoverRepository.discoverFilter(languages = listOf(lang), lastUserId = lastUserId)
        when (res) {
            is ApiResult.Success -> {
                val newUsers = res.data.users.map { it.toUi() }.applyPresence(presenceRepository.onlineIds.value)
                _uiState.update { s ->
                    s.copy(
                        learningLangUsers         = if (isFirstPage) newUsers else s.learningLangUsers + newUsers,
                        learningLangHasNext       = res.data.hasNext,
                        learningLangNextCursor    = res.data.nextCursor,
                        isLearningLangRefreshing  = false,
                        isLearningLangLoadingMore = false,
                    )
                }
            }
            is ApiResult.Error, is ApiResult.NetworkError ->
                _uiState.update { it.copy(isLearningLangRefreshing = false, isLearningLangLoadingMore = false) }
        }
    }

    /** 영속 저장된 필터 복원. isApplied=true면 결과 list까지 자동 재조회. */
    private fun restoreFilter() {
        val saved = filterPreferences.load() ?: return
        val applied = filterPreferences.isApplied()
        _uiState.update { it.copy(
            filterSettings  = saved,
            isFilterApplied = applied,
            selectedTab     = if (applied) 1 else it.selectedTab,
            isFilterLoading = applied,
        ) }
        if (applied) {
            viewModelScope.launch { fetchFilterPage(saved, lastUserId = null, isFirstPage = true) }
        }
    }

    /** RTDB presence/ 변경 시 모든 user list의 isOnline 일괄 갱신. */
    private fun observePresence() {
        viewModelScope.launch {
            presenceRepository.onlineIds.collect { ids ->
                _uiState.update { state ->
                    state.copy(
                        cardSet           = state.cardSet.applyPresence(ids),
                        tagMatchUsers     = state.tagMatchUsers.applyPresence(ids),
                        learningLangUsers = state.learningLangUsers.applyPresence(ids),
                        filterResults     = state.filterResults.applyPresence(ids),
                        selectedUser      = state.selectedUser?.let { it.copy(isOnline = it.userId in ids) },
                    )
                }
            }
        }
    }

    private fun List<DiscoverUser>.applyPresence(ids: Set<Long>): List<DiscoverUser> =
        map { it.copy(isOnline = it.userId in ids) }

    fun selectTab(tab: Int) { _uiState.update { it.copy(selectedTab = tab) } }

    // 카드 인터렉션. 끝까지 도달하면 새 6명 fetch.
    fun nextCard() {
        val state = _uiState.value
        val next  = state.currentCardIndex + 1
        if (next >= state.cardSet.size) {
            refresh()
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

    /** PullToRefresh 진입점. 랜덤 카드 6명만 다시 받아옴 — 추천 섹션 fetch는 loadMyContext()가 책임. */
    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val onlineIds = presenceRepository.onlineIds.value
            when (val res = discoverRepository.discoverRandom()) {
                is ApiResult.Success -> _uiState.update { it.copy(
                    cardSet          = res.data.users.map { it.toUi() }.applyPresence(onlineIds),
                    currentCardIndex = 0,
                    isRefreshing     = false,
                ) }
                is ApiResult.Error, is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    // 필터 화면 열기/닫기
    fun openFilterScreen()  { _uiState.update { it.copy(isFilterScreenOpen = true) } }
    fun closeFilterScreen() { _uiState.update { it.copy(isFilterScreenOpen = false) } }

    fun applyFilter(settings: FilterSettings) {
        _uiState.update { it.copy(
            filterSettings      = settings,
            filterResults       = emptyList(),
            filterNextCursor    = null,
            filterHasNext       = false,
            isFilterApplied     = true,
            isFilterScreenOpen  = false,
            isFilterLoading     = true,
            selectedTab         = 1,
        ) }
        filterPreferences.save(settings, isApplied = true)
        viewModelScope.launch {
            fetchFilterPage(settings, lastUserId = null, isFirstPage = true)
        }
    }

    fun loadMoreFilterResults() {
        val state = _uiState.value
        if (state.isFilterLoadingMore || !state.filterHasNext) return
        val cursor = state.filterNextCursor ?: return
        _uiState.update { it.copy(isFilterLoadingMore = true) }
        viewModelScope.launch {
            fetchFilterPage(state.filterSettings, lastUserId = cursor, isFirstPage = false)
        }
    }

    private suspend fun fetchFilterPage(settings: FilterSettings, lastUserId: Long?, isFirstPage: Boolean) {
        val res = discoverRepository.discoverFilter(
            gender         = settings.gender.toApiCode(),
            country        = settings.country,
            minAge         = settings.minAge.toLong().takeIf { !settings.isAgeFullRange },
            maxAge         = settings.maxAge.toLong().takeIf { !settings.isAgeFullRange },
            languages      = settings.languages.ifEmpty { null },
            tags           = settings.selectedTags.map { it.tag }.ifEmpty { null },
            isOnline       = settings.isOnline.takeIf { it },
            recentlyActive = settings.recentlyActive.takeIf { it },
            lastUserId     = lastUserId,
        )
        when (res) {
            is ApiResult.Success -> {
                val newUsers = res.data.users.map { it.toUi() }
                _uiState.update { s ->
                    s.copy(
                        filterResults       = if (isFirstPage) newUsers else s.filterResults + newUsers,
                        filterHasNext       = res.data.hasNext,
                        filterNextCursor    = res.data.nextCursor,
                        isFilterLoading     = false,
                        isFilterLoadingMore = false,
                    )
                }
            }
            is ApiResult.Error, is ApiResult.NetworkError -> {
                _uiState.update { it.copy(
                    isFilterLoading     = false,
                    isFilterLoadingMore = false,
                ) }
            }
        }
    }

    fun resetFilter() {
        _uiState.update { it.copy(
            filterSettings   = FilterSettings(),
            filterResults    = emptyList(),
            filterNextCursor = null,
            filterHasNext    = false,
            isFilterApplied  = false,
        ) }
        filterPreferences.clear()
    }

    fun openProfile(user: DiscoverUser) {
        _uiState.update { it.copy(
            selectedUser       = user,
            selectedUserDetail = null,
            isProfileOpen      = true,
            translatedBio      = null,
            isBioTranslating   = false,
        ) }
        viewModelScope.launch {
            val res = userRepository.getUserProfile(user.userId)
            if (res is ApiResult.Success) {
                _uiState.update { it.copy(selectedUserDetail = res.data.toUserDetail()) }
            }
        }
    }

    fun translateBio(text: String) {
        if (_uiState.value.isBioTranslating) return
        _uiState.update { it.copy(isBioTranslating = true) }
        viewModelScope.launch {
            when (val r = translateRepository.translateText(text)) {
                is ApiResult.Success -> _uiState.update { it.copy(translatedBio = r.data.translatedText, isBioTranslating = false) }
                else                 -> _uiState.update { it.copy(isBioTranslating = false) }
            }
        }
    }

    fun clearBioTranslation() {
        _uiState.update { it.copy(translatedBio = null) }
    }

    fun closeProfile() {
        _uiState.update { it.copy(
            selectedUser       = null,
            selectedUserDetail = null,
            isProfileOpen      = false,
            translatedBio      = null,
            isBioTranslating   = false,
        ) }
    }

    fun addFriend(userId: Long) {
        viewModelScope.launch {
            when (friendRepository.addFriend(userId)) {
                is ApiResult.Success -> {
                    _uiState.update { s ->
                        s.copy(selectedUserDetail = s.selectedUserDetail?.copy(isFriend = true))
                    }
                }
                else -> Unit
            }
        }
    }

    fun removeFriend(userId: Long) {
        viewModelScope.launch {
            when (friendRepository.removeFriend(userId)) {
                is ApiResult.Success -> {
                    _uiState.update { s ->
                        s.copy(selectedUserDetail = s.selectedUserDetail?.copy(isFriend = false))
                    }
                }
                else -> Unit
            }
        }
    }

    fun blockUser(userId: Long) {
        viewModelScope.launch {
            when (blockRepository.blockUser(userId)) {
                is ApiResult.Success -> closeProfile()
                else -> Unit
            }
        }
    }
}

// MyViewModel — 마이페이지
data class BlockedFriendItem(
    val userId      : Long,
    val name        : String,
    val nationality : String,
    val languages   : List<String>,
)

data class RemovedFriendItem(
    val userId      : Long,
    val name        : String,
    val nationality : String,
    val languages   : List<String>,
)

data class MyUiState(
    val profile          : MyProfile    = MyProfile(),
    val isEditMode       : Boolean      = false,
    val editName         : String       = "",
    val editBio          : String       = "",
    val editBirthday     : String       = "",
    val editGender       : String       = "",
    val editCountry      : String       = "",
    val toastMessage     : String?      = null,
    val isTagEditOpen    : Boolean      = false,
    val editingTags      : List<UserTag> = emptyList(),
    val openLanguageCode : String?      = null,
    val openSubMenu      : String?      = null,   // "guide" | "notice" | "settings" | "version"
    val isSaving         : Boolean      = false,
    val pendingImageUri  : String?      = null,
    val blockedFriends    : List<BlockedFriendItem>  = emptyList(),
    val isLoadingBlocked  : Boolean                  = false,
    val blockedError      : String?                  = null,
    val removedFriends    : List<RemovedFriendItem>  = emptyList(),
    val isLoadingRemoved  : Boolean                  = false,
    val removedError      : String?                  = null,
)

@HiltViewModel
class MyViewModel @Inject constructor(
    @ApplicationContext private val appContext    : Context,
    private val userRepository     : UserRepository,
    private val tokenManager       : TokenManager,
    private val authRepository     : AuthRepository,
    private val blockRepository    : BlockRepository,
    private val friendRepository   : FriendRepository,
    private val userSummaryCache   : UserSummaryCache,
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

    fun deleteMyAccount() {
        val s = _uiState.value
        if (s.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val res = userRepository.deleteMyAccount()
            when (res) {
                is ApiResult.Success      -> {
                    authRepository.cleanupAfterAccountDeletion()
                    _logoutComplete.value = true
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

    private fun loadMyProfile() {
        viewModelScope.launch {
            val userId = tokenManager.userId.first() ?: return@launch

            val profileDeferred = async { userRepository.getUserProfile(userId) }
            val tagsDeferred    = async { userRepository.getUserTags(userId) }
            val langsDeferred   = async { userRepository.getUserLanguages(userId) }

            val apiProfile = (profileDeferred.await() as? ApiResult.Success)?.data ?: return@launch
            val tags = (tagsDeferred.await() as? ApiResult.Success)?.data
                ?.map { dto ->
                    val sample = sampleTags.firstOrNull { s -> s.apiValue == dto.tag }
                    UserTag(
                        tag         = dto.tag,
                        category    = dto.category,
                        emoji       = sample?.emoji ?: "",
                        displayName = sample?.label ?: dto.tag,
                    )
                }
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
            ExploreDemo.myProfile = ExploreDemo.myProfile.copy(tags = tags, learningLanguages = langs)
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
                uriToTempFile(uri, mime) to mime
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
                    // 본인 이름/프로필 변경 즉시 채팅방 등 모든 화면 반영 — UserSummaryCache invalidate
                    tokenManager.userId.first()?.let { userSummaryCache.invalidate(it) }
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
    private suspend fun uriToTempFile(uri: Uri, mimeType: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            val ext = when (mimeType.lowercase()) {
                "image/jpeg" -> ".jpg"
                "image/png"  -> ".png"
                "image/gif"  -> ".gif"
                "image/webp" -> ".webp"
                "image/heic" -> ".heic"
                else         -> ".jpg"
            }
            val tempFile = File.createTempFile("upload_", ext, appContext.cacheDir)
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

    fun loadBlockedUsers() {
        _uiState.update { it.copy(isLoadingBlocked = true, blockedError = null) }
        viewModelScope.launch {
            when (val res = blockRepository.getBlockedUsers()) {
                is ApiResult.Success -> {
                    val items = res.data?.blockedUsers?.map { u ->
                        BlockedFriendItem(
                            userId      = u.userId,
                            name        = u.name,
                            nationality = countryName(u.country ?: ""),
                            languages   = u.languages?.map { l -> UserLanguage(l.language, l.level).displayCode() } ?: emptyList(),
                        )
                    } ?: emptyList()
                    _uiState.update { it.copy(blockedFriends = items, isLoadingBlocked = false) }
                }
                is ApiResult.Error        -> _uiState.update { it.copy(isLoadingBlocked = false, blockedError = res.userMessage()) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoadingBlocked = false, blockedError = res.userMessage()) }
            }
        }
    }

    fun unblockFriend(userId: Long) {
        viewModelScope.launch {
            when (blockRepository.unblockUser(userId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(blockedFriends = it.blockedFriends.filter { f -> f.userId != userId }) }
                    loadBlockedUsers()
                }
                is ApiResult.Error, is ApiResult.NetworkError -> Unit
            }
        }
    }

    fun loadRemovedFriends() {
        _uiState.update { it.copy(isLoadingRemoved = true, removedError = null) }
        viewModelScope.launch {
            when (val res = friendRepository.getRemovedFriends()) {
                is ApiResult.Success -> {
                    val items = res.data?.removedFriends?.map { u ->
                        RemovedFriendItem(
                            userId      = u.userId,
                            name        = u.name,
                            nationality = countryName(u.country ?: ""),
                            languages   = u.languages?.map { l -> UserLanguage(l.language, l.level).displayCode() } ?: emptyList(),
                        )
                    } ?: emptyList()
                    _uiState.update { it.copy(removedFriends = items, isLoadingRemoved = false) }
                }
                is ApiResult.Error        -> _uiState.update { it.copy(isLoadingRemoved = false, removedError = res.userMessage()) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoadingRemoved = false, removedError = res.userMessage()) }
            }
        }
    }

    fun restoreFriend(userId: Long) {
        viewModelScope.launch {
            when (friendRepository.addFriend(userId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(
                        removedFriends = it.removedFriends.filter { f -> f.userId != userId },
                    ) }
                }
                is ApiResult.Error, is ApiResult.NetworkError -> Unit
            }
        }
    }

    // 서브 메뉴
    fun openSubMenu(key: String)    { _uiState.update { it.copy(openSubMenu = key) } }
    fun closeSubMenu()              { _uiState.update { it.copy(openSubMenu = null) } }

    // 토스트 소비
    fun consumeToast() { _uiState.update { it.copy(toastMessage = null) } }
}
