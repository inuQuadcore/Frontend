package com.everybuddy.app.data.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * SignUp 화면에서 수집한 인증 정보를 Onboarding 완료 시점까지 메모리에 보관하는 홀더.
 * 회원가입 API는 온보딩 마지막 화면에서 모든 데이터가 모인 후 단 한 번 호출됨.
 */
@Singleton
class AuthDataHolder @Inject constructor() {
    var loginId : String  = ""
    var password: String  = ""
    var checked : Boolean = false

    fun set(loginId: String, password: String, checked: Boolean) {
        this.loginId  = loginId
        this.password = password
        this.checked  = checked
    }

    fun clear() {
        loginId = ""; password = ""; checked = false
    }
}
