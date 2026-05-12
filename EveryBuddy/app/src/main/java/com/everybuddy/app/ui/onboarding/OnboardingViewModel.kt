package com.everybuddy.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.network.AuthDataHolder
import com.everybuddy.app.data.network.AuthRepository
import com.everybuddy.app.data.network.AuthResult
import com.everybuddy.app.data.network.LanguageLevel
import com.everybuddy.app.data.network.RegisterRequest
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

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.register(req)) {
                is AuthResult.Success -> {
                    authDataHolder.clear()
                    _uiState.update { it.copy(isLoading = false, registerSuccess = true) }
                }
                is AuthResult.Error -> {
                    val msg = when (result.code) {
                        409  -> "이미 가입된 이메일입니다."
                        else -> result.message
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
                is AuthResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "네트워크 연결을 확인해주세요.") }
                }
            }
        }
    }
}
