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
import com.everybuddy.app.ui.splash.SplashScreen

object Route {
    const val SPLASH     = "splash"
    const val SIGN_UP    = "sign_up"
    const val ONBOARDING = "onboarding"
    const val LOGIN      = "login"
    const val MAIN       = "main"
}

// AppNavGraph — 스플래시 → 로그인/자동로그인 → 회원가입 → 온보딩 → 메인
// SPLASH: 저장된 JWT 토큰 확인 → 있으면 MAIN, 없으면 LOGIN
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
        // 스플래시 — JWT 토큰 확인 후 MAIN 또는 LOGIN으로 자동 이동
        composable(Route.SPLASH) {
            SplashScreen(
                onNavigate = { dest ->
                    navController.navigate(dest) {
                        popUpTo(Route.SPLASH) { inclusive = true }
                    }
                }
            )
        }

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

        // 온보딩 — 회원가입 완료(register API) 후 로그인 화면으로
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
                    navController.navigate(Route.ONBOARDING) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.MAIN) {
            MainScreen(
                onLogout = {
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.MAIN) { inclusive = true }
                    }
                }
            )
        }
    }
}
