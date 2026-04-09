// val org.gradle.accessors.dm.LibrariesForLibs.AndroidxComposeLibraryAccessors.material: Any

plugins {
    // Add the Google services Gradle plugin
    alias(libs.plugins.google.services)

    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // Hilt
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.everybuddy.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.everybuddy.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // 스웨거 연동 의존성 추가
    // Retrofit + OkHttp (API 통신)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

// DataStore (JWT 토큰 저장)
    implementation(libs.androidx.datastore.preferences)

// Gson
    implementation("com.google.code.gson:gson:2.10.1")


// ════════════════════════════════════════════════════════════════
// API 연동 흐름 요약
// ════════════════════════════════════════════════════════════════
//
// [로그인]
// LoginScreen → LoginViewModel.onLoginClick()
//   → AuthRepository.login(loginId, password)
//     → POST https://api.everybuddy.cloud/api/v1/auth/login
//       성공(200): JWT → DataStore 저장 → loginSuccess = true → ChatList 이동
//       실패(401): "아이디 또는 비밀번호가 올바르지 않습니다."
//       실패(404): "등록되지 않은 이메일입니다."
//
// [회원가입]
// SignUpScreen → SignUpViewModel.onSignUpClick()
//   → AuthRepository.register(req)
//     → POST https://api.everybuddy.cloud/api/v1/auth/register
//       성공(200): signUpSuccess = true → Onboarding 이동
//       실패(400): "입력 정보를 다시 확인해주세요."
//       실패(409): "이미 가입된 이메일입니다."
//
// [이후 모든 API 요청]
// OkHttp 인터셉터가 DataStore에서 JWT를 읽어 자동으로 헤더에 첨부:
// Authorization: Bearer eyJhbGciOi...

    // Androidx Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Material Icons Extended(Email, Lock, Visibility 등)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Hilt (DI)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // DataStore (토큰 저장)
    implementation(libs.androidx.datastore.preferences.v114)

    // Retrofit (API 통신)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor.v4120)


    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)

    // 라이브러리 추가: ExoPlayer (Media3) — 음성 메시지 재생
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

// ════════════════════════════════════════════════════════
// 런타임 권한 요청 (ChatRoomScreen.kt 에서 녹음 전 처리)
// ════════════════════════════════════════════════════════
//
// onStartRecording() 호출 전에 RECORD_AUDIO 권한 확인:
//
// val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
//
// if (permissionState.status.isGranted) {
//     viewModel.onStartRecording()
// } else {
//     permissionState.launchPermissionRequest()
// }
//
// 의존성 추가 필요:
// implementation("com.google.accompanist:accompanist-permissions:0.34.0")
}