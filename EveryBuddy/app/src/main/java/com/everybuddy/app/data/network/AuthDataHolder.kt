package com.everybuddy.app.data.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * SignUp 화면에서 수집한 인증 정보 또는 구글 신규가입의 tempToken을
 * Onboarding 완료 시점까지 메모리에 보관하는 홀더.
 * 회원가입 API는 온보딩 마지막 화면에서 모든 데이터가 모인 후 단 한 번 호출됨.
 *
 * 흐름 판정: tempToken이 비어있지 않으면 구글 신규가입, 아니면 일반 가입.
 */
@Singleton
class AuthDataHolder @Inject constructor() {
    var loginId   : String  = ""
    var password  : String  = ""
    var checked   : Boolean = false
    var tempToken : String? = null   // 구글 신규가입 흐름에서만 세팅 (10분 유효)

    /** 일반 가입 — SignUp 화면에서 호출 */
    fun set(loginId: String, password: String, checked: Boolean) {
        this.loginId   = loginId
        this.password  = password
        this.checked   = checked
        this.tempToken = null
    }

    /** 구글 신규가입 — googleLogin 성공 시(isNewUser=true) 호출 */
    fun setGoogleSignup(tempToken: String) {
        this.loginId   = ""
        this.password  = ""
        this.checked   = false
        this.tempToken = tempToken
    }

    /** 현재 가입 흐름이 구글 신규가입인지 판정 */
    val isGoogleSignup: Boolean get() = !tempToken.isNullOrEmpty()

    fun clear() {
        loginId = ""; password = ""; checked = false; tempToken = null
    }
}
