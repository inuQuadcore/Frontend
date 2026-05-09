package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

// Unique-to-ApiDtos helpers used by ApiRepositories
data class LanguageDto(
    @SerializedName("language") val language : String,
    @SerializedName("level")    val level    : Int,
)

data class OnboardingLanguage(
    @SerializedName("language") val language : String,
    @SerializedName("level")    val level    : Int,
)

// POST /api/v1/messages (multipart JSON part) — different name from AppDtos SendMessageRequestDto
data class SendMessageRequest(
    @SerializedName("chatRoomId") val chatRoomId : Long,
    @SerializedName("content")    val content    : String,
)

// Common error response shape returned by the backend
data class ApiErrorResponse(
    @SerializedName("code")    val code    : Int,
    @SerializedName("name")    val name    : String,
    @SerializedName("message") val message : String,
    @SerializedName("errors")  val errors  : Map<String, String>? = null,
)

// API result wrapper used by all repositories
sealed class ApiResult<out T> {
    data class Success<T>(val data: T)                   : ApiResult<T>()
    data class Error(val code: Int, val name: String, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val e: Throwable)            : ApiResult<Nothing>()
}
