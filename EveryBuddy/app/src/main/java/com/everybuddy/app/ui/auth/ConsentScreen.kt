package com.everybuddy.app.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.everybuddy.app.data.auth.AuthDataHolder
import com.everybuddy.app.ui.theme.EBWhite
import com.everybuddy.app.ui.theme.EveryBuddyTheme
import com.everybuddy.app.ui.theme.PretendardFamily
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ConsentScreen — 구글 신규가입 흐름의 약관 동의 화면
 *
 * 흐름: LoginScreen → 구글 로그인 → (isNewUser=true) → ConsentScreen → Onboarding
 *
 * 동의 체크 후 "다음" → AuthDataHolder.checked = true 설정 후 Onboarding 진입.
 * 시스템 뒤로가기 → AuthDataHolder.clear() 후 LoginScreen으로 복귀 (흐름 취소).
 *
 * 일반 가입(SignUpScreen)은 자체에 약관 체크박스를 갖고 있어 이 화면을 거치지 않음.
 */
@HiltViewModel
class ConsentViewModel @Inject constructor(
    private val authDataHolder: AuthDataHolder,
) : ViewModel() {

    private val _isAgreed = MutableStateFlow(false)
    val isAgreed: StateFlow<Boolean> = _isAgreed.asStateFlow()

    fun toggleAgreed() {
        _isAgreed.update { !it }
    }

    /** "다음" 클릭 — 동의 사실을 AuthDataHolder에 기록. Onboarding 완료 시 googleRegister 호출에 사용됨. */
    fun onConsent() {
        authDataHolder.checked = true
    }

    /** 시스템 뒤로가기 — 구글 가입 흐름 취소. tempToken/checked 초기화. */
    fun onCancel() {
        authDataHolder.clear()
    }
}

@Composable
fun ConsentScreen(
    onConsent : () -> Unit,
    onCancel  : () -> Unit,
    viewModel : ConsentViewModel = hiltViewModel(),
) {
    val isAgreed by viewModel.isAgreed.collectAsState()

    // 시스템 뒤로가기 가로채기 — AuthDataHolder 클리어 후 LoginScreen 복귀
    BackHandler {
        viewModel.onCancel()
        onCancel()
    }

    ConsentScreenContent(
        isAgreed = isAgreed,
        onToggle = viewModel::toggleAgreed,
        onNext   = {
            viewModel.onConsent()
            onConsent()
        },
    )
}

@Composable
fun ConsentScreenContent(
    isAgreed : Boolean,
    onToggle : () -> Unit,
    onNext   : () -> Unit,
) {
    val scrollState = rememberScrollState()

    Scaffold(containerColor = EBWhite) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // TODO: ic_logo.xml — SignUpScreen과 동일

            Spacer(Modifier.height(12.dp))

            Text(
                text  = "약관 동의",
                style = TextStyle(
                    fontSize   = 20.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(700),
                    color      = Color(0xFF1B1B1B),
                ),
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text      = "서비스 이용을 위해 약관에 동의해 주세요.",
                style     = TextStyle(
                    fontSize   = 14.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(400),
                    color      = Color(0xFF888888),
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            AgreementBox(
                isAgreed = isAgreed,
                onToggle = onToggle,
            )

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(40.dp))

            Button(
                onClick  = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape    = RoundedCornerShape(15.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF0167FF),
                    contentColor           = Color.White,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor   = Color(0xFF999999),
                ),
                enabled  = isAgreed,
            ) {
                Text(
                    text  = "다음",
                    style = TextStyle(
                        fontSize   = 16.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(600),
                        color      = Color.White,
                    ),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "약관 동의 — 미체크")
@Composable
private fun PreviewConsentUnchecked() {
    EveryBuddyTheme {
        ConsentScreenContent(isAgreed = false, onToggle = {}, onNext = {})
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "약관 동의 — 체크")
@Composable
private fun PreviewConsentChecked() {
    EveryBuddyTheme {
        ConsentScreenContent(isAgreed = true, onToggle = {}, onNext = {})
    }
}
