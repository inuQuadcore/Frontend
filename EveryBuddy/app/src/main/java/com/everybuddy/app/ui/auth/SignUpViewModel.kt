package com.everybuddy.app.ui.auth

import androidx.lifecycle.ViewModel
import com.everybuddy.app.data.network.AuthDataHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 이메일/비밀번호 유효성 검사만 담당.
 * API 호출은 온보딩 완료 시점에 OnboardingViewModel에서 수행함.
 * 검사 통과 후 AuthDataHolder에 인증 정보를 저장하고 signUpSuccess = true로 전환.
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authDataHolder: AuthDataHolder,
) : ViewModel() {

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
     * "회원 정보 작성" 버튼 클릭 시 호출.
     * 유효성 검사만 수행하고, 통과 시 AuthDataHolder에 인증 정보 저장 후 온보딩으로 이동.
     * 실제 API 호출은 온보딩 마지막 화면(Step8)에서 OnboardingViewModel.register()가 담당.
     */
    fun onSignUpClick() {
        val s = _uiState.value

        if (!s.isAgreed) {
            _uiState.update { it.copy(errorMessage = "이용약관에 동의해주세요.") }
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

        authDataHolder.set(
            loginId  = s.email,
            password = s.password,
            checked  = s.isAgreed,
        )
        _uiState.update { it.copy(signUpSuccess = true) }
    }
}
