package com.everybuddy.app.data.repository

import com.everybuddy.app.data.dto.ApiErrorResponse
import com.everybuddy.app.data.dto.ApiResult
import com.google.gson.Gson
import retrofit2.Response

internal fun <T> Response<T>.toApiResult(
    gson: Gson,
    onSuccess: (T?) -> ApiResult<T>,
): ApiResult<T> {
    return if (isSuccessful) {
        onSuccess(body())
    } else {
        try {
            val err = gson.fromJson(errorBody()?.string(), ApiErrorResponse::class.java)
            ApiResult.Error(err.code, err.name, err.message)
        } catch (e: Exception) {
            ApiResult.Error(code(), "PARSE_ERROR", "응답 파싱 오류")
        }
    }
}

internal suspend fun <T> safeApiCall(
    gson     : Gson,
    block    : suspend () -> Response<T>,
    onSuccess: (T?) -> ApiResult<T> = { ApiResult.Success(it!!) },
): ApiResult<T> = try {
    block().toApiResult(gson, onSuccess)
} catch (e: Exception) {
    ApiResult.NetworkError(e)
}
