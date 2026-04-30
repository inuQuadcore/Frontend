package com.everybuddy.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * Hilt로 주입되며, AuthRepository.register()를 통해 회원가입 API를 호출함.
 *
 * 현재 회원가입 API 구조 (RegisterRequest):
 *  - loginId, password: 이 화면에서 수집
 *  - name, birthday, gender, country, bio, tags, languages:
 *    온보딩에서 수집 → 추후 PATCH /user/profile 로 업데이트 예정
 *
 * TODO: OnboardingViewModel과 연동하여 온보딩 데이터를 RegisterRequest에 주입하거나,
 *       회원가입 후 온보딩 완료 시점에 PATCH API 분리 호출 방식 중 하나로 결정 필요.
 *
 */

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    companion object {
        // true = 시뮬레이션 모드 (API 호출 없이 즉시 성공 처리)
        // false = 실제 서버 연동
        const val DEBUG_SIMULATION = true
    }

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    /**
     * 이메일 입력 변경 시 호출.
     * android.util.Patterns.EMAIL_ADDRESS로 형식 검증 후
     * emailStatus(IDLE/SUCCESS/ERROR)와 emailMessage 업데이트.
     */
    fun onEmailChange(value: String) {
        val isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()
        _uiState.update {
            it.copy(
                email        = value,
                emailStatus  = when {
                    value.isEmpty() -> FieldStatus.IDLE
                    isValid         -> FieldStatus.SUCCESS
                    else            -> FieldStatus.ERROR
                },
                emailMessage = when {
                    value.isEmpty() -> null
                    isValid         -> "사용 가능한 이메일입니다."
                    else            -> "올바른 이메일 형식이 아닙니다."
                },
                errorMessage = null,
            )
        }
    }
    /**
     * 비밀번호 입력 변경 시 호출.
     * 세 가지 규칙을 실시간으로 검증하여 각 플래그 업데이트:
     *  - hasUpperCase: 영문 대소문자 포함 여부
     *  - hasNoSpecial: 특수문자(@#$?%&*) 미포함 여부
     *  - hasNoRepeat: 같은 문자 3회 이상 연속 반복 없음 + 숫자 포함 여부
     * 세 규칙 모두 충족 + 8자 이상일 때 passwordStatus = SUCCESS.
     */
    fun onPasswordChange(value: String) {
        val hasUpper  = value.any { it.isLetter() }
        val noSpecial = value.none { it in "@#/$?%&*" }
        val noRepeat  = !Regex("(.)\\1{2,}").containsMatchIn(value) && value.any { it.isDigit() }
        val allValid  = hasUpper && noSpecial && noRepeat && value.length >= 8

        _uiState.update {
            it.copy(
                password       = value,
                hasUpperCase   = hasUpper,
                hasNoSpecial   = noSpecial,
                hasNoRepeat    = noRepeat,
                passwordStatus = when {
                    value.isEmpty() -> FieldStatus.IDLE
                    allValid        -> FieldStatus.SUCCESS
                    else            -> FieldStatus.ERROR
                },
                errorMessage   = null,
            )
        }
    }

    /**
     * 비밀번호 확인 입력 변경 시 호출.
     * 현재 password 값과 일치 여부를 비교하여 confirmStatus 업데이트.
     */
    fun onPasswordConfirmChange(value: String) {
        val matches = value == _uiState.value.password
        _uiState.update {
            it.copy(
                passwordConfirm = value,
                confirmStatus   = when {
                    value.isEmpty() -> FieldStatus.IDLE
                    matches         -> FieldStatus.SUCCESS
                    else            -> FieldStatus.ERROR
                },
                confirmMessage  = when {
                    value.isEmpty() -> null
                    matches         -> "비밀번호가 일치합니다."
                    else            -> "비밀번호가 일치하지 않습니다."
                },
            )
        }
    }

    /** 비밀번호 눈 아이콘 토글 */
    fun onTogglePasswordVisible() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    /** 비밀번호 확인 눈 아이콘 토글 */
    fun onToggleConfirmVisible() {
        _uiState.update { it.copy(isConfirmVisible = !it.isConfirmVisible) }
    }

    /** 이용약관 동의 체크박스 토글 */
    fun onAgreementToggle() {
        _uiState.update { it.copy(isAgreed = !it.isAgreed) }
    }

    /**
     * 회원가입 버튼 클릭 시 호출.
     *
     * 사전 검증:
     *  1. 약관 동의 여부 확인
     *  2. 이메일 형식 확인 (emailStatus == SUCCESS)
     *  3. 비밀번호 규칙 확인 (passwordStatus == SUCCESS)
     *  4. 비밀번호 일치 확인 (confirmStatus == SUCCESS)
     *
     * API 호출 (authRepository.register()):
     *  - 성공 → signUpSuccess = true (NavGraph에서 온보딩으로 이동 트리거)
     *  - 400  → "입력 정보를 다시 확인해주세요."
     *  - 409  → "이미 가입된 이메일입니다." (이메일 중복)
     *  - 네트워크 오류 → "네트워크 연결을 확인해주세요."
     *
     * name, birthday, gender, country, bio, tags, languages는
     * 온보딩 연동 전 임시 기본값으로 전송됨. 추후 OnboardingViewModel과 연동 필요.
     */
    fun onSignUpClick() {
        val s = _uiState.value

        if (!s.isAgreed) {
            _uiState.update { it.copy(errorMessage = "이용약관에 동의해주세요.") } // 약관 미동의 시 에러 메시지 없던 문제 → 추가
            return
        }
        if (s.emailStatus != FieldStatus.SUCCESS) {
            _uiState.update { it.copy(errorMessage = "올바른 이메일을 입력해주세요.") }
            return
        }
        if (s.passwordStatus != FieldStatus.SUCCESS) {
            _uiState.update { it.copy(errorMessage = "비밀번호 규칙을 확인해주세요.") }
            return
        }
        if (s.confirmStatus != FieldStatus.SUCCESS) {
            _uiState.update { it.copy(errorMessage = "비밀번호가 일치하지 않습니다.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (DEBUG_SIMULATION) {
                // 시뮬레이션: API 없이 즉시 성공
                _uiState.update { it.copy(isLoading = false, signUpSuccess = true) }
                return@launch
            }

            // 실제 API 호출 (DEBUG_SIMULATION = false 시 동작)
            // TODO 온보딩 연동 시 아래 임시 기본값들을 OnboardingViewModel의 실제 데이터로 교체
            val req = RegisterRequest(
                loginId   = s.email,
                password  = s.password,
                name      = "임시닉네임",   // 온보딩 Step1에서 수집
                country   = "KOREA",       // 온보딩 Step2에서 수집
                birthday  = "2000-01-01",  // 온보딩 Step1에서 수집
                gender    = "MALE",        // 온보딩 Step1에서 수집
                bio       = "안녕하세요",   // 온보딩 Step6에서 수집
                tags      = emptyList(),   // 온보딩 Step5에서 수집
                languages = listOf(LanguageLevel("KOREAN", 5)), // 온보딩 Step3에서 수집
                checked   = true,
            )

            when (val result = authRepository.register(req)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, signUpSuccess = true) }
                }
                is AuthResult.Error -> {
                    val msg = when (result.code) {
                        400  -> "입력 정보를 다시 확인해주세요."
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
