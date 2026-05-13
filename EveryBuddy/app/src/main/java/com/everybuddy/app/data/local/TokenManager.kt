package com.everybuddy.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.everybuddy.app.di.TokenKeys
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
