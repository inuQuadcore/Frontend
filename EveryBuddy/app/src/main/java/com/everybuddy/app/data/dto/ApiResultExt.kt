package com.everybuddy.app.data.dto

fun ApiResult<*>.userMessage(): String? = when (this) {
    is ApiResult.Success      -> null
    is ApiResult.Error        -> message
    is ApiResult.NetworkError -> "네트워크 연결을 확인해주세요."
}

fun ApiResult.Error.requiresLogout(): Boolean =
    name in listOf("JWT_ENTRY_POINT", "JWT_EXPIRED", "JWT_INVALID")
