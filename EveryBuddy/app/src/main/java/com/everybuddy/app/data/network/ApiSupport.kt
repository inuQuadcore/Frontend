package com.everybuddy.app.data.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.everybuddy.app.di.TokenKeys
import com.everybuddy.app.data.dto.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val accessToken:           Flow<String?> = dataStore.data.map { it[TokenKeys.ACCESS_TOKEN] }
    val refreshToken:          Flow<String?> = dataStore.data.map { it[TokenKeys.REFRESH_TOKEN] }
    val accessTokenExpiresAt:  Flow<String?> = dataStore.data.map { it[TokenKeys.ACCESS_TOKEN_EXPIRES_AT] }
    val refreshTokenExpiresAt: Flow<String?> = dataStore.data.map { it[TokenKeys.REFRESH_TOKEN_EXPIRES_AT] }
    val userId:                Flow<Long?>   = dataStore.data.map { it[TokenKeys.USER_ID]?.toLongOrNull() }

    suspend fun saveToken(
        accessToken           : String,
        refreshToken          : String,
        accessTokenExpiresAt  : String,
        refreshTokenExpiresAt : String,
        userId                : Long,
    ) {
        dataStore.edit { prefs ->
            prefs[TokenKeys.ACCESS_TOKEN]              = accessToken
            prefs[TokenKeys.REFRESH_TOKEN]             = refreshToken
            prefs[TokenKeys.ACCESS_TOKEN_EXPIRES_AT]   = accessTokenExpiresAt
            prefs[TokenKeys.REFRESH_TOKEN_EXPIRES_AT]  = refreshTokenExpiresAt
            prefs[TokenKeys.USER_ID]                   = userId.toString()
        }
    }

    suspend fun saveTempToken(token: String) {
        dataStore.edit { prefs ->
            prefs[TokenKeys.ACCESS_TOKEN] = token
        }
    }

    suspend fun clearToken() {
        dataStore.edit { prefs ->
            prefs.remove(TokenKeys.ACCESS_TOKEN)
            prefs.remove(TokenKeys.REFRESH_TOKEN)
            prefs.remove(TokenKeys.ACCESS_TOKEN_EXPIRES_AT)
            prefs.remove(TokenKeys.REFRESH_TOKEN_EXPIRES_AT)
            prefs.remove(TokenKeys.USER_ID)
        }
    }
}

object ApiErrorHandler {

    fun toUserMessage(error: ApiResult.Error): String = when (error.name) {
        "BAD_CREDENTIALS"            -> "이메일 또는 비밀번호가 올바르지 않아요."
        "DUPLICATED_USER"            -> "이미 가입된 이메일이에요."
        "JWT_ENTRY_POINT"            -> "로그인이 필요해요."
        "JWT_EXPIRED"                -> "로그인이 만료됐어요. 다시 로그인해주세요."
        "JWT_INVALID"                -> "인증 정보가 유효하지 않아요."
        "CANNOT_ADD_SELF"            -> "자기 자신은 친구로 추가할 수 없어요."
        "ALREADY_FRIEND"             -> "이미 친구인 사용자예요."
        "CANNOT_BLOCK_SELF"          -> "자기 자신은 차단할 수 없어요."
        "ALREADY_BLOCKED"            -> "이미 차단한 사용자예요."
        "BLOCK_NOT_FOUND"            -> "차단 관계를 찾을 수 없어요."
        "USER_NOT_FOUND"             -> "사용자를 찾을 수 없어요."
        "USER_DELETED"               -> "탈퇴한 사용자예요."
        "USER_NOT_IN_CHATROOM"       -> "해당 채팅방에 접근할 수 없어요."
        "INVALID_INPUT_VALUE"        -> "입력값을 확인해주세요."
        "INVALID_FILE_TYPE"          -> "지원하지 않는 파일 형식이에요."
        "EMPTY_FILE"                 -> "빈 파일은 업로드할 수 없어요."
        "FILE_SIZE_EXCEEDED"         -> "파일 크기는 최대 10MB까지 가능해요."
        "CHATROOM_NOT_FOUND"         -> "채팅방을 찾을 수 없어요."
        "MESSAGE_NOT_FOUND"          -> "메시지를 찾을 수 없어요."
        "MESSAGE_EDIT_TIME_EXCEEDED" -> "수정·삭제 가능 시간(5분)이 지났어요."
        "MESSAGE_ALREADY_DELETED"    -> "이미 삭제된 메시지예요."
        "CANNOT_EDIT_FILE_MESSAGE"   -> "파일 메시지는 수정할 수 없어요."
        "NOT_MESSAGE_OF_USER"        -> "자신의 메시지만 수정·삭제할 수 있어요."
        "INVALID_IMAGE_FORMAT"       -> "지원하지 않는 이미지 형식이에요."
        "TYPE_MISMATCH"              -> "입력 형식이 올바르지 않아요."
        "STATUS_MESSAGE_NOT_FOUND"   -> "상태메시지를 찾을 수 없어요."
        "STATUS_MESSAGE_ALREADY_EXISTS" -> "이미 상태메시지가 있어요. 수정해주세요."
        "STATUS_MESSAGE_EXPIRED"     -> "24시간이 지나 수정할 수 없어요."
        "PARSE_ERROR"                -> "서버 응답을 처리할 수 없어요."
        else -> error.message.ifBlank { "알 수 없는 오류가 발생했어요. (${error.name})" }
    }

    fun toUserMessage(error: ApiResult.NetworkError): String =
        "네트워크 연결을 확인해주세요."

    fun requiresLogout(error: ApiResult.Error): Boolean =
        error.name in listOf("JWT_ENTRY_POINT", "JWT_EXPIRED", "JWT_INVALID")
}
