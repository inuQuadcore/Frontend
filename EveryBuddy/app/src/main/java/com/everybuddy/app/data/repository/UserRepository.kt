package com.everybuddy.app.data.repository

// =============================================================================
// UserRepository_v2.kt
// 기존 UserRepository를 이 파일로 교체하세요.
// 신규 추가:
//   getUserProfile()       — GET /api/v1/users/{userId}
//   getUserTags()          — GET /api/v1/users/{userId}/tags
//   getUserLanguages()     — GET /api/v1/users/{userId}/languages
//   updateMyLanguageLevel()— PATCH /api/v1/users/me/languages
// =============================================================================

import com.everybuddy.app.data.dto.*
import com.everybuddy.app.data.network.ApiService
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api  : ApiService,
    private val gson : Gson,
) {

    // ── 조회 ─────────────────────────────────────────────────────────────────

    /**
     * 유저 프로필 조회 — GET /api/v1/users/{userId}
     * 본인 userId 전달 시 본인 프로필 조회
     * 200: { profileImageUrl, country, name, age, gender, bio }
     * 에러: 401(인증) | 404(유저없음) | 410(탈퇴)
     */
    suspend fun getUserProfile(userId: Long): ApiResult<UserPublicProfileResponse> =
        safeApiCall(gson, { api.getUserProfile(userId) })

    /**
     * 유저 태그 조회 — GET /api/v1/users/{userId}/tags
     * 200: [ { tag, category } ]
     * 에러: 401(인증) | 404(유저없음) | 410(탈퇴)
     */
    suspend fun getUserTags(userId: Long): ApiResult<List<TagDto>> =
        safeApiCall(gson, { api.getUserTags(userId) })

    /**
     * 유저 관심 언어 목록 조회 — GET /api/v1/users/{userId}/languages
     * 본인 userId 전달 시 isOwner = true → 수준 수정 버튼 표시에 활용
     * 200: { languages: [ { language, level } ], isOwner: Boolean }
     * 에러: 401(인증) | 404(유저없음) | 410(탈퇴)
     */
    suspend fun getUserLanguages(userId: Long): ApiResult<UserLanguagesResponse> =
        safeApiCall(gson, { api.getUserLanguages(userId) })

    // ── 수정 ─────────────────────────────────────────────────────────────────

    /**
     * 관심 언어 수준 수정 — PATCH /api/v1/users/me/languages
     * 관심 언어 목록에 있는 언어만 수정 가능 (없으면 404 USER_LANGUAGE_NOT_FOUND)
     * level 범위: 1~5 (초과 시 400 errors.level)
     * 에러: 400(잘못된언어/수준범위) | 401(인증) | 404(USER_LANGUAGE_NOT_FOUND) | 410(탈퇴)
     */
    suspend fun updateMyLanguageLevel(
        language : String,
        level    : Int,
    ): ApiResult<Unit> = safeApiCall(gson, {
        api.updateMyLanguageLevel(UpdateLanguageLevelRequest(language, level))
    }) { ApiResult.Success(Unit) }

    /**
     * 내 태그 전체 교체 — PUT /api/v1/users/me/tags
     * 빈 리스트 전달 시 전체 삭제
     * 에러: 400(잘못된입력) | 401(인증) | 404(유저없음) | 410(탈퇴)
     */
    suspend fun updateMyTags(tags: List<String>): ApiResult<Unit> =
        safeApiCall(gson, { api.updateMyTags(UpdateTagsRequest(tags)) }) {
            ApiResult.Success(Unit)
        }

    /**
     * 내 프로필 수정 — PATCH /api/v1/users/me (multipart)
     * 모든 필드 선택적. 이미지 없을 시 profileImage = null
     * profileImage 제한: 최대 5MB, jpg/jpeg/png/gif/webp/heic
     * 에러: 400(bio150초과/잘못된성별·국적/날짜형식/이미지형식/빈파일)
     *       401(인증) | 404(유저없음) | 410(탈퇴) | 413(5MB초과)
     */
    suspend fun updateMyProfile(
        name          : String? = null,
        birthday      : String? = null,  // "yyyy-MM-dd"
        gender        : String? = null,  // "MALE" | "FEMALE"
        country       : String? = null,
        bio           : String? = null,  // 최대 150자
        profileImage  : File?   = null,
        imageMimeType : String  = "image/jpeg",
    ): ApiResult<UserProfileResponse> {
        val requestJson = gson.toJson(
            UpdateProfileRequest(name, birthday, gender, country, bio)
        ).toRequestBody("application/json".toMediaType())

        val imagePart = profileImage?.let {
            MultipartBody.Part.createFormData(
                name     = "profileImage",   // ⚠️ 스웨거 파트명: "profileImage"
                filename = it.name,
                body     = it.asRequestBody(imageMimeType.toMediaType()),
            )
        }
        return safeApiCall(gson, { api.updateMyProfile(requestJson, imagePart) })
    }

    /**
     * 회원 탈퇴 — DELETE /api/v1/users/me
     * 에러: 401(인증) | 404(유저없음)
     */
    suspend fun deleteMyAccount(): ApiResult<Unit> =
        safeApiCall(gson, { api.deleteMyAccount() }) { ApiResult.Success(Unit) }
}
