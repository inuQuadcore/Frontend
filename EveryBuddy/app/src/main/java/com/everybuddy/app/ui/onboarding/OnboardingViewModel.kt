package com.everybuddy.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * OnboardingViewModel
 *
 * 온보딩 전체(Step1~8)의 상태를 관리하는 ViewModel.
 * Hilt로 주입되며, OnboardingScreen에서 hiltViewModel()로 획득함.
 *
 * 현재 구조: 상태 변경 로직을 Screen 단에서 직접 수행하고,
 * ViewModel은 상태 저장 + 단계 이동만 담당 (Stateless-like 패턴).
 *
 * TODO: 온보딩 완료 시 수집된 데이터를 API(PATCH /user/profile)로 전송하는
 *       submitOnboarding() 함수 추가 필요 → AuthRepository 연동
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    /** 온보딩 UI 전체 상태. StateFlow로 노출하여 Screen이 구독함. */
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Screen에서 직접 상태 객체를 copy()하여 전달하는 패턴.
     * 각 Step의 입력(이름, 생일, 국가, 언어, 태그, bio 등)이
     * 이 함수를 통해 ViewModel로 올라옴.
     */
    fun onStateChange(new: OnboardingUiState) {
        _uiState.value = new
    }

    /**
     * 다음 단계로 이동.
     * coerceAtMost(8): Step8(앱 소개)을 초과하지 않도록 제한.
     */
    fun onNext() {
        _uiState.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(8)) }
    }

    /**
     * 이전 단계로 이동.
     * coerceAtLeast(1): Step1 미만으로 내려가지 않도록 제한.
     */
    fun onBack() {
        _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(1)) }
    }

    /**
     * 온보딩 완료 후 수집된 데이터를 서버에 전송.
     *
     * TODO API: PATCH /api/v1/user/profile 연동
     *   - AuthRepository 또는 UserRepository 주입 필요
     *   - 전송 항목: name, birthday, gender, country, bio, tags, languages
     *   - 성공 시 onFinish 콜백 호출 → AppNavGraph에서 MAIN으로 이동
     *   - 실패 시 에러 메시지 표시
     *
     * TODO 연동: SignUpViewModel과 공유하는 방법 결정
     *   방법 A: 회원가입 시 온보딩 데이터까지 한 번에 전송 (RegisterRequest 확장)
     *   방법 B: 회원가입 후 온보딩 완료 시점에 PATCH 분리 호출 (현재 설계)
     */
    fun submitProfile() {
        val state           = uiState.value
        val genderForServer = state.gender?.apiValue ?: ""

        viewModelScope.launch {
            // TODO: when (val result = userRepository.updateProfile(
            //     name      = state.name,
            //     birthday  = state.birthday,
            //     gender    = genderForServer,
            //     country   = state.country?.code ?: "",
            //     bio       = state.bio,
            //     tags      = state.selectedTags.map { it.apiValue },
            //     languages = state.myLanguages.map { it.code },
            // )) {
            //     is AuthResult.Success   -> { /* onFinish 호출 */ }
            //     is AuthResult.Error     -> { /* errorMessage 표시 */ }
            //     is AuthResult.Exception -> { /* 네트워크 오류 */ }
            // }
            println("서버로 전송할 성별: $genderForServer") // TODO: 제거
        }
    }
}
