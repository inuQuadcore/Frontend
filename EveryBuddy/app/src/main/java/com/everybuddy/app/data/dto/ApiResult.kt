package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

// 서버 에러 응답 본문 모양
data class ApiErrorResponse(
    @SerializedName("code")    val code    : Int,
    @SerializedName("name")    val name    : String,
    @SerializedName("message") val message : String,
    @SerializedName("errors")  val errors  : Map<String, String>? = null,
)

// 모든 API 호출 결과 래퍼
sealed class ApiResult<out T> {
    data class Success<T>(val data: T)                                     : ApiResult<T>()
    data class Error(val code: Int, val name: String, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val e: Throwable)                              : ApiResult<Nothing>()
}

// ViewModel용 — 서버 message 그대로 노출. NetworkError만 클라이언트 문구.
fun ApiResult<*>.userMessage(): String? = when (this) {
    is ApiResult.Success      -> null
    is ApiResult.Error        -> message
    is ApiResult.NetworkError -> "네트워크 연결을 확인해주세요."
}

// JWT_* 코드는 로그아웃 라우팅 트리거
fun ApiResult.Error.requiresLogout(): Boolean =
    name in listOf("JWT_ENTRY_POINT", "JWT_EXPIRED", "JWT_INVALID")
