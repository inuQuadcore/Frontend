package com.everybuddy.app.data.network

/**
 * AuthResult
 *
 * API 호출 결과를 타입 안전하게 래핑하는 sealed class.
 * AuthRepository, ChatRepository 모든 API 호출의 반환 타입으로 사용.
 *
 * Success  : 정상 응답 (body가 null일 수 있는 204 No Content 포함)
 * Error    : HTTP 에러 응답 (4xx, 5xx)
 * Exception: 네트워크 오류, 타임아웃 등 예외 상황
 */
sealed class AuthResult<out T> {
    data class Success<T>(val data: T?)             : AuthResult<T>()
    data class Error(val code: Int, val message: String) : AuthResult<Nothing>()
    data class Exception(val e: kotlin.Exception)   : AuthResult<Nothing>()
}
