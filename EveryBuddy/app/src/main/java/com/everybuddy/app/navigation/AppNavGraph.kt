package com.everybuddy.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.everybuddy.app.ui.main.MainScreen
import com.everybuddy.app.ui.auth.ConsentScreen
import com.everybuddy.app.ui.auth.LoginScreen
import com.everybuddy.app.ui.auth.SignUpScreen
import com.everybuddy.app.ui.onboarding.OnboardingScreen

// 최상위 라우트
object Route {
    const val SIGN_UP    = "sign_up"
    const val CONSENT    = "consent"   // 구글 신규가입 약관 동의 (일반 가입은 SignUpScreen 내부에 약관 포함)
    const val ONBOARDING = "onboarding"
    const val LOGIN      = "login"
    const val MAIN       = "main"
}

// AppNavGraph — 회원가입 → 온보딩 → 로그인 → 메인
// TODO: 자동 로그인 (SplashScreen에서 JWT 토큰 존재 시 Route.MAIN으로 직행)
// TODO: 딥링크 — 푸시 알림 클릭 시 특정 채팅방 진입
@Composable
fun AppNavGraph(
    navController : NavHostController = rememberNavController(),
    startDest     : String            = Route.LOGIN,
) {
    NavHost(
        navController    = navController,
        startDestination = startDest,
    ) {
        // 회원가입
        composable(Route.SIGN_UP) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Route.ONBOARDING) {
                        popUpTo(Route.SIGN_UP) { inclusive = true }
                    }
                },
            )
        }

        // 구글 신규가입 약관 동의
        composable(Route.CONSENT) {
            ConsentScreen(
                onConsent = {
                    navController.navigate(Route.ONBOARDING) {
                        popUpTo(Route.CONSENT) { inclusive = true }
                    }
                },
                onCancel  = {
                    navController.popBackStack()   // LoginScreen으로 복귀
                },
            )
        }

        // 온보딩
        composable(Route.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        // 로그인
        composable(Route.LOGIN) {
            LoginScreen(
                onLoginSuccess  = {
                    navController.navigate(Route.MAIN) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                },
                onSignUpClick   = { navController.navigate(Route.SIGN_UP) },
                onGoogleNewUser = {
                    // 구글 신규유저 → 약관 동의 → 온보딩
                    // LoginScreen은 backstack에 유지 (Consent에서 뒤로가기 시 복귀 대상)
                    navController.navigate(Route.CONSENT)
                },
            )
        }

        composable(Route.MAIN) {
            MainScreen()
        }
    }
}
