package com.everybuddy.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.everybuddy.app.R
import com.everybuddy.app.ui.theme.*
import androidx.compose.ui.focus.FocusDirection

// PretendardFamily 는 ui/theme/Font.kt 에서 정의

// LoginScreen - Hilt ViewModel 진입점
/**
 * LoginScreen
 *
 * Hilt ViewModel을 주입받는 최상위 진입점.
 * NavGraph에서 이 함수를 직접 호출함.
 *
 * - loginSuccess가 true가 되면 LaunchedEffect가 감지하여 onLoginSuccess() 호출
 * - 실제 UI 구성은 LoginScreenContent에 위임 (Preview 가능하도록 분리)
 */
@Composable
fun LoginScreen(
    onLoginSuccess     : () -> Unit,
    onSignUpClick      : () -> Unit,
    onGoogleLoginClick : () -> Unit,
    viewModel          : LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // 로그인 성공 → NavGraph로 이동 트리거
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) onLoginSuccess()
    }

    LoginScreenContent(
        uiState                 = uiState,
        onLoginIdChange         = viewModel::onLoginIdChange,
        onPasswordChange        = viewModel::onPasswordChange,
        onTogglePasswordVisible = viewModel::onTogglePasswordVisibility,
        onLoginClick            = { focusManager.clearFocus(); viewModel.onLoginClick() },
        onSignUpClick           = onSignUpClick,
        onGoogleLoginClick      = onGoogleLoginClick,
    )
}

// LoginScreenContent - UI 구성
/**
 * 412dp
 * padding start = 28, end = 28
 * Spacer 110dp
 * logo 155x155dp, boarder = 2dp, pad = 2dp
 * Spacer 32dp
 * title: "에브리버디에 오신 것을 환영해요 :)" 24sp Bold
 * Spacer 8dp
 * 에러 Box 18dp height
 * Spacer 24dp
 * 이메일 입력 필드 54dp
 * Spacer 12dp
 * 비밀번호 입력 필드 54dp
 * Spacer 12dp
 * 로그인 버튼 54dp r = 15dp, #0167FF
 * Spacer 24dp
 * --------------or-------------- EBGray2
 * Spacer 24dp
 * 구글 로그인 버튼 54dp r = 15dp, #CCE9E9E9
 * Spacer 8dp
 * "회원가입하기" #404040
 *
 * 외부 영역 클릭 시 키보드 닫기 (focusManager.clearFocus)
 */

@Composable
fun LoginScreenContent(
    uiState                 : LoginUiState,
    onLoginIdChange         : (String) -> Unit,
    onPasswordChange        : (String) -> Unit,
    onTogglePasswordVisible : () -> Unit,
    onLoginClick            : () -> Unit,
    onSignUpClick           : () -> Unit,
    onGoogleLoginClick      : () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    // 바깥 클릭 --> 키보드 닫기
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EBWhite)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { focusManager.clearFocus() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 28.dp, top = 20.dp, end = 28.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(110.dp))

            // 1. 로고 이미지 (ic_logo.xml)
            Image(
                painter            = painterResource(id = R.drawable.ic_logo),
                contentDescription = "EveryBuddy 로고",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.size(155.dp),
            )

            Spacer(Modifier.height(32.dp))

            // 2. 타이틀 텍스트
            Text(
                text  = "에브리버디에 오신 것을\n환영해요 :)",
                style = TextStyle(
                    fontSize   = 24.sp,
                    lineHeight = 32.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(800),
                    color      = Color(0xFF000000),
                    textAlign  = TextAlign.Center,
                )
            )

            Spacer(Modifier.height(32.dp))

            // 3. 에러 메시지(조건부)
            // 높이(height)를 고정해 레이아웃 shift 방지
            Box(
                modifier          = Modifier.width(276.dp).height(18.dp),
                contentAlignment  = Alignment.Center
            ) {
                if (uiState.errorMessage != null) {
                    Text(
                        text  = uiState.errorMessage,
                        style = TextStyle(
                            fontSize   = 14.sp,
                            lineHeight = 16.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight(600),
                            color      = Color(0xFFFF2B01),
                            textAlign  = TextAlign.Center,
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 4. 이메일 입력 필드
            // leadingIcon: Email 아이콘
            EBTextField(
                value           = uiState.loginId,
                onValueChange   = onLoginIdChange,
                placeholder     = "Your email",
                leadingIcon     = {
                    Image(
                        painter            = painterResource(R.drawable.ic_email),
                        contentDescription = null,
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.size(width = 20.dp, height = 18.dp),
                    )
                },
                isError         = uiState.errorMessage != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )

            Spacer(Modifier.height(12.dp))

            // 5. 비밀번호 입력 필드
            // 열쇠 아이콘: leadingIcon(Group)
            // 눈 아이콘  : trailingIcon(ic_eyes_on/off.xml)
            EBTextField(
                value                = uiState.password,
                onValueChange        = onPasswordChange,
                placeholder          = "Password",
                leadingIcon          = {
                    Image(
                        painter            = painterResource(R.drawable.ic_key),
                        contentDescription = null,
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.size(width = 18.dp, height = 20.dp),
                    )
                },
                trailingIcon         = {
                    IconButton(onClick = onTogglePasswordVisible) {
                        Image(
                            painter            = painterResource(
                                if (uiState.isPasswordVisible) R.drawable.ic_eyes_off
                                else                           R.drawable.ic_eyes_on
                            ),
                            contentDescription = if (uiState.isPasswordVisible) "비밀번호 숨기기" else "비밀번호 보기",
                            contentScale       = ContentScale.Fit,
                            modifier           = Modifier.size(24.dp),
                        )
                    }
                },
                visualTransformation = if (uiState.isPasswordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                isError              = uiState.errorMessage != null,
                keyboardOptions      = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done,
                ),
                keyboardActions      = KeyboardActions(
                    onDone = { focusManager.clearFocus(); onLoginClick() },
                ),
            )

            Spacer(Modifier.height(12.dp))

            // 6. 로그인 버튼 (로딩 중 CircularProgressIndicator 표시)
            Button(
                onClick        = onLoginClick,
                modifier       = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape          = RoundedCornerShape(15.dp),
                colors         = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0167FF),
                    contentColor   = Color.White,
                ),
                contentPadding = PaddingValues(0.dp),
                enabled        = !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text  = "로그인",
                        style = TextStyle(
                            fontSize   = 16.sp,
                            lineHeight = 20.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight(600),
                            color      = Color.White,
                            textAlign  = TextAlign.Center,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 7. or 구분선
            OrDivider(dividerColor = Color(0xFFE4E6EC))

            Spacer(Modifier.height(24.dp))

            // 8. 구글 로그인 버튼
            // ic_google.xml (구글 로고)
            OutlinedButton(
                onClick        = onGoogleLoginClick,
                modifier       = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape          = RoundedCornerShape(15.dp),
                border         = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xCCE9E9E9)),
                contentPadding = PaddingValues(0.dp),
                colors         = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xCCFFFFFF),
                    contentColor   = Color(0xFF404040),
                ),
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Image(
                        painter            = painterResource(R.drawable.ic_google),
                        contentDescription = "Google",
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = "구글로 로그인하기",
                        style = TextStyle(
                            fontSize   = 16.sp,
                            lineHeight = 20.8.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight(600),
                            color      = Color(0xFF404040),
                            textAlign  = TextAlign.Center,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))


            // 9. 회원가입 링크
            Text(
                text     = "회원가입하기",
                style    = TextStyle(
                    fontSize   = 14.sp,
                    lineHeight = 18.2.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = Color(0xFF404040),
                    textAlign  = TextAlign.Center,
                ),
                modifier = Modifier.clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onSignUpClick() },
            )
        }
    }
}

// 공통 컴포넌트
// EBTextField
/**
 * Figma(첫 화면2 기준):
 *   height        = 54.dp
 *   cornerRadius  = 15.dp
 *   background    = #F5F5F5 (EBGray3)
 *   border idle   = Transparent
 *   border focus  = #898989 (EBGray1)
 *   error = #FF2B01 (EBRed)
 */
@Composable
fun EBTextField(
    value                : String,
    onValueChange        : (String) -> Unit,
    placeholder          : String,
    modifier             : Modifier                  = Modifier,
    leadingIcon          : (@Composable () -> Unit)? = null,
    trailingIcon         : (@Composable () -> Unit)? = null,
    visualTransformation : VisualTransformation      = VisualTransformation.None,
    isError              : Boolean                   = false,
    keyboardOptions      : KeyboardOptions           = KeyboardOptions.Default,
    keyboardActions      : KeyboardActions           = KeyboardActions.Default,
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        placeholder          = {
            Text(
                text  = placeholder,
                style = TextStyle(
                    fontSize   = 14.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(400),
                    color      = Color(0xFF999999),
                ),
            )
        },
        leadingIcon          = leadingIcon,
        trailingIcon         = trailingIcon,
        visualTransformation = visualTransformation,
        isError              = isError,
        singleLine           = true,
        modifier             = modifier.fillMaxWidth().height(54.dp),
        shape                = RoundedCornerShape(15.dp),
        colors               = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color(0xFFF5F5F5),
            focusedContainerColor   = Color(0xFFF5F5F5),
            errorContainerColor     = Color(0xFFF5F5F5),
            unfocusedBorderColor    = Color.Transparent,
            focusedBorderColor      = Color(0xFF898989),
            errorBorderColor        = Color(0xFFFF2B01),
            cursorColor             = Color(0xFF0167FF),
        ),
        textStyle = TextStyle(
            fontSize   = 14.sp,
            fontFamily = PretendardFamily,
            fontWeight = FontWeight(400),
            color      = Color(0xFF1B1B1B),
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

/** OrDivider — 가로선 + "or" */
@Composable
fun OrDivider(dividerColor: Color = Color(0xFFE4E6EC)) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = dividerColor)
        Text(
            text  = "  or  ",
            style = TextStyle(
                fontSize   = 12.sp,
                fontFamily = PretendardFamily,
                fontWeight = FontWeight(400),
                color      = Color(0xFF898989),
            ),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = dividerColor)
    }
}


// Previews
@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "기본 상태")
@Composable
private fun Preview_Default() {
    EveryBuddyTheme {
        LoginScreenContent(
            uiState                 = LoginUiState(),
            onLoginIdChange         = {},
            onPasswordChange        = {},
            onTogglePasswordVisible = {},
            onLoginClick            = {},
            onSignUpClick           = {},
            onGoogleLoginClick      = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "에러 — 빈 입력")
@Composable
private fun Preview_ErrorEmpty() {
    EveryBuddyTheme {
        LoginScreenContent(
            uiState                 = LoginUiState(errorMessage = "아이디 또는 비밀번호를 입력해주세요."),
            onLoginIdChange         = {},
            onPasswordChange        = {},
            onTogglePasswordVisible = {},
            onLoginClick            = {},
            onSignUpClick           = {},
            onGoogleLoginClick      = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "로딩 상태")
@Composable
private fun Preview_Loading() {
    EveryBuddyTheme {
        LoginScreenContent(
            uiState                 = LoginUiState(isLoading = true),
            onLoginIdChange         = {},
            onPasswordChange        = {},
            onTogglePasswordVisible = {},
            onLoginClick            = {},
            onSignUpClick           = {},
            onGoogleLoginClick      = {},
        )
    }
}
