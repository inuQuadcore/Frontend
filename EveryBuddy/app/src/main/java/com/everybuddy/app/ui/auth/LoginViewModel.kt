package com.everybuddy.app.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.repository.AuthRepository
import com.everybuddy.app.data.network.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * LoginUiState
 *
 * 로그인 화면의 UI 상태를 담는 불변 데이터 클래스.
 * ViewModel이 StateFlow로 노출하며 Compose UI가 구독함.
 */

data class LoginUiState(
    val loginId          : String  = "",
    val password         : String  = "",
    val isPasswordVisible: Boolean = false,
    /** API 요청 중 로딩 상태 (true일 때 로그인 버튼 비활성화 + Indicator 표시) */
    val isLoading        : Boolean = false,
    val errorMessage     : String? = null,
    /** 로그인 성공 여부 — LaunchedEffect에서 감지하여 NavGraph 이동 트리거 */
    val loginSuccess     : Boolean = false,
)

sealed class LoginNavEvent {
    object ToMain        : LoginNavEvent()   // 기존 유저 → MAIN
    object ToOnboarding  : LoginNavEvent()   // 신규 유저 → ONBOARDING
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    companion object {
        // true = 시뮬레이션 모드 (API 호출 없이 즉시 성공 처리)
        // false = 실제 서버 연동
        const val DEBUG_SIMULATION = false
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navEvent  = MutableStateFlow<LoginNavEvent?>(null)
    val navEvent: StateFlow<LoginNavEvent?> = _navEvent.asStateFlow()

    fun onGoogleLogin(activityContext: Context, webClientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.googleLogin(activityContext, webClientId)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _navEvent.value = if (result.data == true) LoginNavEvent.ToOnboarding
                                      else                     LoginNavEvent.ToMain
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is AuthResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.e.localizedMessage ?: "네트워크 오류가 발생했습니다.") }
                }
            }
        }
    }

    fun onNavEventConsumed() {
        _navEvent.value = null
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    fun onLoginIdChange(value: String) {
        _uiState.update { it.copy(loginId = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    /** 비밀번호 눈 아이콘 클릭 시 표시/숨김 토글. */
    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onLoginClick() {
        val state = _uiState.value

        // 빈 칸 검사
        if (state.loginId.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "아이디 또는 비밀번호를 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (DEBUG_SIMULATION) {
                // 시뮬레이션: API 없이 즉시 성공
                _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                return@launch
            }

            // 실제 API 호출 (DEBUG_SIMULATION = false 시 동작)
            when (val result = authRepository.login(state.loginId, state.password)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                is AuthResult.Error -> {
                    val msg = when (result.code) {
                        401  -> "아이디 또는 비밀번호가 올바르지 않습니다."
                        404  -> "등록되지 않은 이메일입니다. 회원가입을 진행해주세요."
                        409  -> "이미 로그인된 상태입니다. 잠시 후 다시 시도해주세요."
                        else -> result.message
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
                is AuthResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = "네트워크 연결을 확인해주세요.",
                        )
                    }
                }
            }
        }
    }
}
