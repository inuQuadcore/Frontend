package com.everybuddy.app.data.network

import com.everybuddy.app.data.dto.UpdateLanguageLevelRequest
import com.everybuddy.app.data.dto.UpdateTagsRequest
import com.everybuddy.app.data.dto.UserLanguagesResponse
import com.everybuddy.app.data.dto.UserProfileResponse
import com.everybuddy.app.data.dto.UserPublicProfileResponse
import com.everybuddy.app.data.dto.UserTag
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface UserApi {

    /**
     * GET /api/v1/users/{userId} — 유저 프로필 조회
     * 특정 유저의 프로필 조회. 본인 userId 전달 시 본인 프로필 반환
     * 200: { profileImageUrl, country, name, age, gender, bio }
     * 401: 인증 | 404: 유저 없음 | 410: 탈퇴한 유저
     */
    @GET("api/v1/users/{userId}")
    suspend fun getUserProfile(
        @Path("userId") userId: Long,
    ): Response<UserPublicProfileResponse>

    /**
     * GET /api/v1/users/{userId}/tags — 유저 태그 조회
     * 특정 유저의 태그 목록 조회. 본인 userId 전달 시 본인 태그 반환
     * 200: [ { tag, category } ]
     * 401: 인증 | 404: 유저 없음 | 410: 탈퇴한 유저
     */
    @GET("api/v1/users/{userId}/tags")
    suspend fun getUserTags(
        @Path("userId") userId: Long,
    ): Response<List<UserTag>>

    /**
     * GET /api/v1/users/{userId}/languages — 유저 관심 언어 목록 조회
     * 특정 유저의 관심 언어 목록 조회
     * 본인 userId 전달 시 isOwner = true 반환 → 수준 수정 버튼 노출 여부에 활용
     * 200: { languages: [ { language, level } ], isOwner: Boolean }
     * 401: 인증 | 404: 유저 없음 | 410: 탈퇴한 유저
     */
    @GET("api/v1/users/{userId}/languages")
    suspend fun getUserLanguages(
        @Path("userId") userId: Long,
    ): Response<UserLanguagesResponse>

    /**
     * PATCH /api/v1/users/me/languages — 내 관심 언어 수준 수정
     * 관심 언어 목록에 있는 언어의 수준(1~5)을 수정
     * Request: { "language": "ENGLISH", "level": 3 }
     * 204: 성공
     * 400: 잘못된 언어 값 / 수준 범위 초과 (errors.level = "언어 수준은 5 이하여야 합니다.")
     * 401: 인증 | 404: USER_LANGUAGE_NOT_FOUND (관심 언어 목록에 없는 언어)
     * 410: 탈퇴한 유저
     */
    @PATCH("api/v1/users/me/languages")
    suspend fun updateMyLanguageLevel(
        @Body request: UpdateLanguageLevelRequest,
    ): Response<Unit>

    /**
     * PUT /api/v1/users/me/tags — 내 태그 목록 수정
     * 전달된 목록으로 전체 교체 (빈 리스트 전송 시 전체 삭제)
     * Request: { "tags": ["SPORTS", "INTJ", "MOVIES"] }
     * 204: 성공 | 400: 잘못된 입력 | 401: 인증 | 404: 유저 없음 | 410: 탈퇴한 유저
     */
    @PUT("api/v1/users/me/tags")
    suspend fun updateMyTags(
        @Body request: UpdateTagsRequest,
    ): Response<Unit>

    /**
     * PATCH /api/v1/users/me — 내 프로필 수정 (multipart)
     * 이름, 생일, 성별, 국적, 자기소개, 프로필 이미지 수정 (모든 필드 선택적)
     * profileImage: 최대 5MB, 허용 형식: jpg/jpeg/png/gif/webp/heic
     * 200: { userId, name, profileImageUrl, birthday, gender, country, bio }
     * 400: 자기소개 150자 초과 / 잘못된 성별·국적 / 날짜 형식 / 지원 안 하는 이미지 형식 / 빈 파일
     * 401: 인증 | 404: 유저 없음 | 410: 삭제된 사용자 | 413: 이미지 크기 초과
     */
    @Multipart
    @PATCH("api/v1/users/me")
    suspend fun updateMyProfile(
        @Part("request")    request      : RequestBody,         // JSON: UpdateProfileRequest
        @Part profileImage  : MultipartBody.Part? = null,       // 선택적 프로필 이미지
    ): Response<UserProfileResponse>

    /**
     * DELETE /api/v1/users/me — 회원 탈퇴
     * 현재 로그인한 유저 탈퇴 처리
     * 204: 성공 | 401: 인증 | 404: 유저 없음
     */
    @DELETE("api/v1/users/me")
    suspend fun deleteMyAccount(): Response<Unit>

    /**
     * POST /api/v1/users/me/attendance — 출석 기록
     * KST 자정 기준으로 연속 출석일수 갱신. 같은 날 여러 번 호출해도 no-op.
     * 204: 성공 | 401: 인증 | 404: 유저 없음 | 410: 탈퇴한 유저
     */
    @POST("api/v1/users/me/attendance")
    suspend fun markAttendance(): Response<Unit>
}
