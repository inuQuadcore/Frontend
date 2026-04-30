package com.everybuddy.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.everybuddy.app.ui.main.MainScreen
import com.everybuddy.app.ui.auth.LoginScreen
import com.everybuddy.app.ui.auth.SignUpScreen
import com.everybuddy.app.ui.onboarding.OnboardingScreen
import com.everybuddy.app.data.chat.ChatRoom

// 최상위 라우트
object Route {
    const val LOGIN      = "login"
    const val SIGN_UP    = "sign_up"
    const val ONBOARDING = "onboarding"
    const val MAIN       = "main"          // ← BottomNav 포함 메인 화면
    // TODO: CHAT_ROOM = "chat_room/{roomId}"  — 채팅방 화면 (MainScreen 내부 NavHost로 이동 예정)
    // TODO: USER_PROFILE = "user_profile/{userId}" — 유저 프로필 상세
}

/**
 * AppNavGraph
 *
 * 앱 전체 최상위 네비게이션 그래프.
 * 로그인 → 회원가입 → 온보딩 → 메인(BottomNav)
 *
 * MainScreen 내부의 탭별 이동은 MainScreen.kt의 내부 NavHost가 담당.
 *
 * TODO 자동 로그인: 앱 시작 시 DataStore의 JWT 토큰 존재 여부 확인 →
 *                    토큰 있으면 startDest = Route.MAIN으로 직행
 *                    (SplashScreen 또는 MainActivity에서 처리 권장)
 * TODO Google OAuth: onGoogleLoginClick → Google One Tap SDK 연동 →
 *                      신규 사용자 = ONBOARDING, 기존 사용자 = MAIN 분기
 * TODO 딥링크: 푸시 알림 클릭 시 특정 채팅방으로 바로 진입하는 딥링크 처리
 */
@Composable
fun AppNavGraph(
    navController : NavHostController = rememberNavController(),
    startDest     : String            = Route.LOGIN,
) {
    NavHost(
        navController    = navController,
        startDestination = startDest,
    ) {

        // 로그인
        composable(Route.LOGIN) {
            LoginScreen(
                onLoginSuccess     = {
                    navController.navigate(Route.ONBOARDING) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                },
                onSignUpClick      = { navController.navigate(Route.SIGN_UP) },
                onGoogleLoginClick = {
                    // TODO Google OAuth: 구글 이메일 인증 서버 연동 후 구현 예정
                    //   신규 사용자 = ONBOARDING, 기존 사용자 = MAIN 분기
                },
            )
        }

        // 회원가입
        composable(Route.SIGN_UP) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.popBackStack() // 회원가입 완료 → 로그인 화면으로 복귀
                },
            )
        }

        // 온보딩
        composable(Route.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Route.MAIN) {
                        popUpTo(Route.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        // 메인 (BottomNavigation)
        // MainScreen 내부에서 탭 전환 및 세부 화면 이동 처리
        composable(Route.MAIN) {
            MainScreen()
        }
    }
}
