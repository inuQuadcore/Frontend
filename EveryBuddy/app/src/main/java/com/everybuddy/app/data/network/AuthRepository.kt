package com.everybuddy.app.data.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.everybuddy.app.di.PrefKeys          // data.network → di로 이동
import com.everybuddy.app.di.dataStore          // data.network → di로 이동
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api    : AuthApi,
    @ApplicationContext private val context: Context,
) {
    private val gson = Gson()

    // 저장된 토큰 Flow
    val accessToken: Flow<String?> = context.dataStore.data
        .map { it[PrefKeys.ACCESS_TOKEN] }

    // 로그인
    suspend fun login(loginId: String, password: String): AuthResult<LoginResponse> {
        return try {
            val res = api.login(LoginRequest(loginId, password))
            if (res.isSuccessful) {
                val body = res.body()!!
                // JWT + userId DataStore 저장
                context.dataStore.edit { prefs ->
                    prefs[PrefKeys.ACCESS_TOKEN] = body.accessToken
                    prefs[PrefKeys.USER_ID]      = body.userId.toString()
                }
                AuthResult.Success(body)
            } else {
                val err = parseError(res.errorBody()?.string())
                AuthResult.Error(res.code(), err)
            }
        } catch (e: kotlin.Exception) {
            AuthResult.Exception(e)
        }
    }

    // 회원가입
    suspend fun register(req: RegisterRequest): AuthResult<Unit> {
        return try {
            val res = api.register(req)
            if (res.isSuccessful) {
                AuthResult.Success(Unit)
            } else {
                val err = parseError(res.errorBody()?.string())
                AuthResult.Error(res.code(), err)
            }
        } catch (e: kotlin.Exception) {
            AuthResult.Exception(e)
        }
    }

    // Firebase 토큰 발급
    suspend fun firebaseToken(): AuthResult<String> {
        return try {
            val res = api.firebaseToken()
            if (res.isSuccessful) {
                AuthResult.Success(res.body()!!.firebaseToken)
            } else {
                val err = parseError(res.errorBody()?.string())
                AuthResult.Error(res.code(), err)
            }
        } catch (e: kotlin.Exception) {
            AuthResult.Exception(e)
        }
    }

    // 로그아웃 (로컬 토큰 삭제)
    suspend fun logout() {
        context.dataStore.edit { it.clear() }
    }

    // 에러 파싱 헬퍼
    private fun parseError(body: String?): String {
        return try {
            gson.fromJson(body, ErrorResponse::class.java).message
        } catch (_: Exception) {
            "알 수 없는 오류가 발생했습니다."
        }
    }
}
