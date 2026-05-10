package com.everybuddy.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuddy.app.ui.theme.*

// Data — UiState + FieldStatus
// SignUpViewModel.kt에서 참조하기 위해 동일 파일에 위치

/**
 * SignUpUiState
 *
 * 회원가입 화면의 전체 UI 상태.
 * 이메일/비밀번호 유효성 검증 결과(FieldStatus)와 각 피드백 메시지를 함께 관리.
 */

data class SignUpUiState(
    val email              : String       = "",
    val password           : String       = "",
    val passwordConfirm    : String       = "",
    val isPasswordVisible  : Boolean      = false,
    val isConfirmVisible   : Boolean      = false,
    val isAgreed           : Boolean      = false,
    val isLoading          : Boolean      = false,
    val signUpSuccess      : Boolean      = false,   // ← NavGraph 트리거
    /** 이메일 유효성 상태 (IDLE/SUCCESS/ERROR) */
    val emailStatus        : FieldStatus  = FieldStatus.IDLE,
    val emailMessage       : String?      = null,

    /** 비밀번호 규칙1: 영문 대소문자 포함 여부 */
    val hasUpperCase       : Boolean     = false,
    /** 비밀번호 규칙2: 특수문자(@#$?%&*) 미포함 여부 */
    val hasNoSpecial       : Boolean     = false,
    /** 비밀번호 규칙3: 반복 문자 없음 + 숫자 포함 여부 */
    val hasNoRepeat        : Boolean     = false,
    /** 비밀번호 유효성 상태 */
    val passwordStatus     : FieldStatus = FieldStatus.IDLE,
    /** 비밀번호 확인 유효성 상태 */
    val confirmStatus      : FieldStatus = FieldStatus.IDLE,
    /** 비밀번호 확인 피드백 메시지 */
    val confirmMessage     : String?     = null,
    /** 전체 에러 메시지 (null = 에러 없음) */
    val errorMessage       : String?     = null,
)
/**
 * FieldStatus
 *
 * 입력 필드의 유효성 검증 상태를 나타내는 enum.
 * - IDLE   : 아직 입력 전 (기본 상태, 아이콘 없음)
 * - SUCCESS: 유효한 입력 (초록 체크 아이콘)
 * - ERROR  : 유효하지 않은 입력 (빨간 X 아이콘)
 */
enum class FieldStatus { IDLE, SUCCESS, ERROR }


// 진입점
@Composable
fun SignUpScreen(
    onSignUpSuccess : () -> Unit,
    viewModel       : SignUpViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // 회원가입 성공 → 온보딩으로 이동
    LaunchedEffect(uiState.signUpSuccess) {
        if (uiState.signUpSuccess) onSignUpSuccess()
    }

    SignUpScreenContent(
        uiState                 = uiState,
        onEmailChange           = viewModel::onEmailChange,
        onPasswordChange        = viewModel::onPasswordChange,
        onPasswordConfirmChange = viewModel::onPasswordConfirmChange,
        onTogglePasswordVisible = viewModel::onTogglePasswordVisible,
        onToggleConfirmVisible  = viewModel::onToggleConfirmVisible,
        onAgreementToggle       = viewModel::onAgreementToggle,
        onSignUpClick           = viewModel::onSignUpClick,
    )
}

// Content

/**
 * SignUpScreenContent
 *
 * 회원가입 화면 UI 구성. verticalScroll 적용 (소프트 키보드 올라올 때 스크롤 가능).
 * 레이아웃 (위→아래):
 *  - 로고 플레이스홀더 (EverybuddyLogoPlaceholder)
 *  - 타이틀 "everybuddy 회원가입"
 *  - 이메일 입력 (EBTextField + 유효성 아이콘 + 피드백 메시지)
 *  - 비밀번호 입력 (EBTextField + 눈 아이콘 + 3개 규칙 체크리스트)
 *  - 비밀번호 확인 입력 (EBTextField + 눈 아이콘 + 피드백 메시지)
 *  - 이용약관 동의 박스 (AgreementBox)
 *  - 에러 메시지
 *  - 회원가입 완료하기 버튼
 *
 */
@Composable
fun SignUpScreenContent(
    uiState                 : SignUpUiState,
    onEmailChange           : (String) -> Unit,
    onPasswordChange        : (String) -> Unit,
    onPasswordConfirmChange : (String) -> Unit,
    onTogglePasswordVisible : () -> Unit,
    onToggleConfirmVisible  : () -> Unit,
    onAgreementToggle       : () -> Unit,
    onSignUpClick           : () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val scrollState  = rememberScrollState()

    Scaffold(containerColor = EBWhite) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)  // 키보드 올라올 때 스크롤 가능
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { focusManager.clearFocus() }
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // TODO: ic_logo.xml

            Spacer(Modifier.height(12.dp))

            Text(
                text  = "everybuddy 회원가입",
                style = TextStyle(
                    fontSize   = 20.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(700),
                    color      = Color(0xFF1B1B1B),
                ),
            )

            Spacer(Modifier.height(32.dp))

            // 이메일 입력
            FieldLabel("이메일")
            Spacer(Modifier.height(8.dp))
            EBTextField(
                value           = uiState.email,
                onValueChange   = onEmailChange,
                placeholder     = "everybuddy@inu.ac.kr",
                trailingIcon    = {
                    // 유효성 상태별 아이콘 (초록 체크, 빨간 엑스)
                    when (uiState.emailStatus) {
                        FieldStatus.SUCCESS -> StatusIcon(success = true)
                        FieldStatus.ERROR   -> StatusIcon(success = false)
                        else -> {}
                    }
                },
                isError         = uiState.emailStatus == FieldStatus.ERROR,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
            )
            // 이메일 피드백 메시지 (성공/에러)
            uiState.emailMessage?.let { msg ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = msg,
                    style = TextStyle(
                        fontSize   = 12.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(500),
                        color      = if (uiState.emailStatus == FieldStatus.SUCCESS)
                            Color(0xFF00C853) else Color(0xFFFF2B01),
                    ),
                )
            }

            Spacer(Modifier.height(20.dp))

            // 비밀번호 입력
            FieldLabel("비밀번호")
            Spacer(Modifier.height(8.dp))
            EBTextField(
                value                = uiState.password,
                onValueChange        = onPasswordChange,
                placeholder          = "비밀번호",
                trailingIcon         = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onTogglePasswordVisible) {
                            Icon(
                                imageVector        = if (uiState.isPasswordVisible)
                                    Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                tint               = Color(0xFF999999),
                            )
                        }
                        // 유효성 아이콘
                        when (uiState.passwordStatus) {
                            FieldStatus.SUCCESS -> StatusIcon(success = true)
                            FieldStatus.ERROR   -> StatusIcon(success = false)
                            else -> {}
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                },
                visualTransformation = if (uiState.isPasswordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                isError              = uiState.passwordStatus == FieldStatus.ERROR,
                keyboardOptions      = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Next,
                ),
                keyboardActions      = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
            )

            // 비밀번호 규칙 체크리스트 (3개)
            // - 충족: 초록 체크 아이콘 + 초록 텍스트
            // - 에러(입력 후 미충족): 빨간 X 아이콘 + 빨간 텍스트
            // - 기본: 회색 원 아이콘 + 회색 텍스트
            PasswordRule(
                text    = "영문 대소문자 사용",
                checked = uiState.hasUpperCase,
                isError = uiState.passwordStatus == FieldStatus.ERROR && !uiState.hasUpperCase,
            )
            PasswordRule(
                text    = "@, #, /, $, ?, %, &, * 등 특수문자 사용 불가",
                checked = uiState.hasNoSpecial,
                isError = uiState.passwordStatus == FieldStatus.ERROR && !uiState.hasNoSpecial,
            )
            PasswordRule(
                text    = "반복되지 않는 숫자 사용",
                checked = uiState.hasNoRepeat,
                isError = uiState.passwordStatus == FieldStatus.ERROR && !uiState.hasNoRepeat,
            )

            Spacer(Modifier.height(20.dp))

            // 비밀번호 재확인
            FieldLabel("비밀번호 확인")
            Spacer(Modifier.height(8.dp))
            EBTextField(
                value                = uiState.passwordConfirm,
                onValueChange        = onPasswordConfirmChange,
                placeholder          = "비밀번호 확인",
                trailingIcon         = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleConfirmVisible) {
                            Icon(
                                imageVector        = if (uiState.isConfirmVisible)
                                    Icons.Outlined.Visibility
                                else Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                tint               = Color(0xFF999999),
                            )
                        }
                        when (uiState.confirmStatus) {
                            FieldStatus.SUCCESS -> StatusIcon(success = true)
                            FieldStatus.ERROR   -> StatusIcon(success = false)
                            else                -> {}
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                },
                visualTransformation = if (uiState.isConfirmVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                isError              = uiState.confirmStatus == FieldStatus.ERROR,
                keyboardOptions      = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done,
                ),
                keyboardActions      = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
            )
            // 비밀번호 확인 피드백 메시지
            uiState.confirmMessage?.let { msg ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = msg,
                    style = TextStyle(
                        fontSize   = 12.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(500),
                        color      = if (uiState.confirmStatus == FieldStatus.SUCCESS)
                            Color(0xFF00C853) else Color(0xFFFF2B01),
                    ),
                )
            }

            Spacer(Modifier.height(24.dp))

            // 이용약관 동의
            FieldLabel("사용자 동의")
            Spacer(Modifier.height(8.dp))
            AgreementBox(
                isAgreed      = uiState.isAgreed,
                onToggle      = onAgreementToggle,
            )

            // 전체 에러 메시지 (약관 미동의, 유효성 검사 실패 등)
            uiState.errorMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = msg,
                    style = TextStyle(
                        fontSize   = 13.sp,
                        fontFamily = PretendardFamily,
                        color      = Color(0xFFFF2B01),
                        textAlign  = TextAlign.Center,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(32.dp))

            // 회원가입 완료 버튼
            // 활성화 조건: isAgreed == true && !isLoading
            // (ViewModel에서 추가 유효성 검사 수행)
            Button(
                onClick  = onSignUpClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape    = RoundedCornerShape(15.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF0167FF),  // 활성 상태 버튼 항상 회색 표시 문제: 0xFFE0E0E0 → 0xFF0167FF
                    contentColor           = Color.White,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor   = Color(0xFF999999),
                ),
                enabled  = uiState.isAgreed && !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    LoadingIndicator(modifier = Modifier.size(20.dp), tint = Color.White)
                } else {
                    Text(
                        text  = "회원 정보 작성",
                        style = TextStyle(
                            fontSize   = 16.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight(600),
                            color      = Color.White,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// 외... 서브 컴포넌트
// FieldLabel - 이메일, 비밀번호, 비밀번호 확인, 사용자 동의 섹션의 라벨 텍스트
@Composable
private fun FieldLabel(text: String) {
    Text(
        text     = text,
        style    = TextStyle(
            fontSize   = 14.sp,
            fontFamily = PretendardFamily,
            fontWeight = FontWeight(600),
            color      = Color(0xFF1B1B1B),
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * StatusIcon
 *
 * 이메일/비밀번호/비밀번호 확인 필드의 유효성 상태 아이콘.
 * success = true  → 초록 체크 원 (Icons.Outlined.CheckCircle)
 * success = false → 빨간 에러 원 (Icons.Outlined.Error)
 */
@Composable
private fun StatusIcon(success: Boolean) {
    Icon(
        imageVector        = if (success) Icons.Outlined.CheckCircle else Icons.Outlined.Error,
        contentDescription = null,
        tint               = if (success) Color(0xFF00C853) else Color(0xFFFF2B01),
        modifier           = Modifier.size(20.dp),
    )
}

/**
 * PasswordRule
 *
 * 비밀번호 규칙 체크 항목 한 줄.
 * - checked = true : 초록 체크 원 + 초록 텍스트
 * - isError = true : 빨간 원 + 빨간 텍스트
 * - 기본           : 회색 원 테두리 + 회색 텍스트
 */
@Composable
private fun PasswordRule(text: String, checked: Boolean, isError: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Icon(
            imageVector        = if (checked) Icons.Outlined.CheckCircle
            else         Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint               = when {
                checked -> Color(0xFF00C853)
                isError -> Color(0xFFFF2B01)
                else    -> Color(0xFF999999)
            },
            modifier           = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text  = text,
            style = TextStyle(
                fontSize   = 12.sp,
                fontFamily = PretendardFamily,
                fontWeight = FontWeight(400),
                color      = when {
                    checked -> Color(0xFF00C853)
                    isError -> Color(0xFFFF2B01)
                    else    -> Color(0xFF999999)
                },
            ),
        )
    }
}

/**
 * AgreementBox
 *
 * 이용약관 동의 체크박스 + 약관 텍스트를 담은 박스.
 * 체크박스 색상: 체크 시 #0167FF(파랑), 미체크 시 #CCCCCC(회색).
 */
@Composable
private fun AgreementBox(
    isAgreed      : Boolean,
    onToggle      : () -> Unit,
) {
    val agreementText = "본 서비스 이용약관에 동의합니다. \n에브리버디는 회원가입 시 수집한 개인정보를 서비스 제공 목적으로만 활용하며, 관계 법령에 따라 안전하게 관리합니다. \n자세한 내용은 개인정보처리방침을 확인해 주세요."
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE4E6EC), RoundedCornerShape(12.dp))
            .background(Color(0xFFF9F9F9), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked         = isAgreed,
                onCheckedChange = { onToggle() },
                colors          = CheckboxDefaults.colors(
                    checkedColor   = Color(0xFF0167FF),
                    uncheckedColor = Color(0xFFCCCCCC),
                ),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text  = agreementText,
                style = TextStyle(
                    fontSize   = 13.sp,
                    lineHeight = 20.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(400),
                    color      = Color(0xFF404040),
                ),
            )
        }
    }
}

// Previews
@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "기본 상태")
@Composable
private fun Preview_Default() {
    EveryBuddyTheme {
        SignUpScreenContent(
            uiState                 = SignUpUiState(),
            onEmailChange           = {},
            onPasswordChange        = {},
            onPasswordConfirmChange = {},
            onTogglePasswordVisible = {},
            onToggleConfirmVisible  = {},
            onAgreementToggle       = {},
            onSignUpClick           = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "전체 성공 상태")
@Composable
private fun Preview_Success() {
    EveryBuddyTheme {
        SignUpScreenContent(
            uiState = SignUpUiState(
                email           = "everybuddy@inu.ac.kr",
                emailStatus     = FieldStatus.SUCCESS,
                emailMessage    = "사용 가능한 이메일입니다.",
                password        = "pwEverybuddy2026!",
                hasUpperCase    = true,
                hasNoSpecial    = true,
                hasNoRepeat     = true,
                passwordStatus  = FieldStatus.SUCCESS,
                passwordConfirm = "pwEverybuddy2026!",
                confirmStatus   = FieldStatus.SUCCESS,
                confirmMessage  = "비밀번호가 일치합니다.",
                isAgreed        = true,
            ),
            onEmailChange           = {},
            onPasswordChange        = {},
            onPasswordConfirmChange = {},
            onTogglePasswordVisible = {},
            onToggleConfirmVisible  = {},
            onAgreementToggle       = {},
            onSignUpClick           = {},
        )
    }
}
