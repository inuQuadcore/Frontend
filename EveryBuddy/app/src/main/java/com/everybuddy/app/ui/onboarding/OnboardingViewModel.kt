package com.everybuddy.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.auth.AuthDataHolder
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.GoogleRegisterRequest
import com.everybuddy.app.data.dto.LanguageLevel
import com.everybuddy.app.data.dto.LoginResponse
import com.everybuddy.app.data.dto.RegisterRequest
import com.everybuddy.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 온보딩 전체(Step1~8)의 상태를 관리하는 ViewModel.
 * Step8(앱 소개) 마지막 화면에서 register()를 호출해
 * SignUp 화면에서 저장된 인증 정보 + 온보딩 입력 데이터를 한 번에 서버로 전송함.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authDataHolder: AuthDataHolder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onStateChange(new: OnboardingUiState) {
        _uiState.value = new
    }

    fun onNext() {
        _uiState.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(8)) }
    }

    fun onBack() {
        _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(1)) }
    }

    /**
     * 온보딩 마지막 화면(Step8)에서 "회원가입 완료" 버튼 클릭 시 호출.
     * AuthDataHolder의 loginId/password/checked + 온보딩에서 수집한 정보를
     * RegisterRequest로 조합하여 POST /api/v1/auth/register 호출.
     * 성공 시 registerSuccess = true → OnboardingScreen의 LaunchedEffect가 onFinish() 호출.
     */
    fun register() {
        val state = _uiState.value
        val birthday = if (state.birthYear != null && state.birthMonth != null && state.birthDay != null)
            "%04d-%02d-%02d".format(state.birthYear, state.birthMonth, state.birthDay)
        else ""

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 구글 신규가입 흐름이면 googleRegister, 아니면 일반 register 호출
            val result: ApiResult<*> = if (authDataHolder.isGoogleSignup) {
                val req = GoogleRegisterRequest(
                    tempToken         = authDataHolder.tempToken ?: "",
                    name              = state.name,
                    checked           = authDataHolder.checked,   // 약관 동의 화면 추가 시 true로 set됨
                    country           = state.selectedCountry?.code ?: "",
                    birthday          = birthday,
                    gender            = state.gender?.apiValue ?: "",
                    bio               = state.bio,
                    tags              = state.selectedTags.map { it.apiValue },
                    primaryLanguage   = state.selectedMyLang?.apiValue ?: "",
                    interestLanguages = state.selectedLearnLangs.map { LanguageLevel(it.apiValue, it.level.coerceAtLeast(1)) },
                )
                authRepository.googleRegister(req)
            } else {
                val req = RegisterRequest(
                    loginId           = authDataHolder.loginId,
                    password          = authDataHolder.password,
                    name              = state.name,
                    checked           = authDataHolder.checked,
                    country           = state.selectedCountry?.code ?: "",
                    birthday          = birthday,
                    gender            = state.gender?.apiValue ?: "",
                    bio               = state.bio,
                    tags              = state.selectedTags.map { it.apiValue },
                    primaryLanguage   = state.selectedMyLang?.apiValue ?: "",
                    interestLanguages = state.selectedLearnLangs.map { LanguageLevel(it.apiValue, it.level.coerceAtLeast(1)) },
                )
                authRepository.register(req)
            }

            when (result) {
                is ApiResult.Success<*> -> {
                    // 구글 가입(googleRegister)은 LoginResponse를 반환하며 토큰이 이미 저장됨 → MAIN으로 직행
                    // 일반 가입(register)은 Unit을 반환하며 별도 로그인 필요 → LOGIN으로
                    val autoLoggedIn = result.data is LoginResponse
                    authDataHolder.clear()
                    _uiState.update {
                        it.copy(
                            isLoading       = false,
                            registerSuccess = true,
                            autoLoggedIn    = autoLoggedIn,
                        )
                    }
                }
                is ApiResult.Error -> {
                    val msg = when (result.code) {
                        409  -> "이미 가입된 이메일입니다."
                        else -> result.message
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "네트워크 연결을 확인해주세요.") }
                }
            }
        }
    }
}
