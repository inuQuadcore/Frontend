package com.everybuddy.app.data.network

// =============================================================================
// ApiService.kt
// 스웨거 문서 기반 전체 REST API 인터페이스
// Base URL: https://api.everybuddy.cloud/
//
// ─ 포함된 API 그룹 ──────────────────────────────────────────────────────────
//   Friend API    : 친구 추가/삭제/목록 조회
//   Block API     : 사용자 차단/차단 해제
//   Discover API  : 랜덤 탐색, 필터 탐색
//   Message API   : 메시지 전송/수정/삭제/읽음/채팅방 동기화
//   User API      : 프로필 조회(타인/본인), 태그 조회, 언어 조회/수준 수정,
//                   프로필 수정(이미지 포함), 태그 전체 교체, 회원 탈퇴
//   Chatroom API  : 내 채팅방 목록 조회, 채팅방 생성
//   Status API    : 상태메시지 작성/수정/삭제/내것조회/친구목록조회
// =============================================================================

import com.everybuddy.app.data.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // =========================================================================
    // Friend API  (이미지 5~8)
    // =========================================================================

    /**
     * POST /api/v1/friends/{toUserId} — 친구 추가
     * 200: 성공 | 400: 자기 자신 추가 | 401: 인증 필요
     * 404: 유저 없음 | 409: 이미 친구 | 410: 탈퇴한 유저
     */
    @POST("api/v1/friends/{toUserId}")
    suspend fun addFriend(
        @Path("toUserId") toUserId: Long,
    ): Response<Unit>

    /**
     * DELETE /api/v1/friends/{toUserId} — 친구 삭제
     * TODO: 스웨거 확인 후 추가
     */
    @DELETE("api/v1/friends/{toUserId}")
    suspend fun removeFriend(
        @Path("toUserId") toUserId: Long,
    ): Response<Unit>

    /**
     * GET /api/v1/friends — 내 친구 전체 목록 조회
     * 200: { friends: [ {userId, name, profileImageUrl, country, bio, languages[], tags[]} ] }
     * 401: 인증 필요 | 404: 유저 없음
     */
    @GET("api/v1/friends")
    suspend fun getFriends(): Response<FriendListResponse>

    // =========================================================================
    // Block API  (이미지 1~4)
    // =========================================================================

    /**
     * POST /api/v1/blocks/{userId} — 사용자 차단
     * 200: 성공 | 400: 자기 자신 차단 | 401: 인증 필요
     * 404: 유저 없음 | 409: 이미 차단 | 410: 탈퇴한 유저
     */
    @POST("api/v1/blocks/{userId}")
    suspend fun blockUser(
        @Path("userId") userId: Long,
    ): Response<Unit>

    /**
     * DELETE /api/v1/blocks/{userId} — 차단 해제
     * 204: 성공 | 401: 인증 필요 | 404: 차단 관계 없음
     */
    @DELETE("api/v1/blocks/{userId}")
    suspend fun unblockUser(
        @Path("userId") userId: Long,
    ): Response<Unit>

    // =========================================================================
    // Discover API  (이미지 7~11 from 이전 대화)
    // =========================================================================

    /**
     * GET /api/v1/discover/random — 랜덤 유저 탐색
     * 친구·차단 관계를 제외한 랜덤 유저 6명 반환
     * 200: { users: [{userId, name, profileImageUrl, country, bio, languages[], tags[], lastSeenAt}] }
     * 401: 인증 필요
     */
    @GET("api/v1/discover/random")
    suspend fun discoverRandom(): Response<DiscoverResponse>

    /**
     * GET /api/v1/discover/filter — 필터 유저 탐색 (커서 기반 페이징)
     * 조건: isOnline(현재 온라인), recentlyActive(24시간 이내), 두 조건 동시 적용 가능
     * 200: { users[], hasNext, nextCursor }
     * 400: 잘못된 파라미터 | 401: 인증 필요
     */
    @GET("api/v1/discover/filter")
    suspend fun discoverFilter(
        @Query("gender")         gender         : String?  = null,
        @Query("country")        country        : String?  = null,
        @Query("minAge")         minAge         : Long?    = null,
        @Query("maxAge")         maxAge         : Long?    = null,
        @Query("languages")      languages      : List<String>? = null,
        @Query("tags")           tags           : List<String>? = null,
        @Query("isOnline")       isOnline       : Boolean? = null,
        @Query("recentlyActive") recentlyActive : Boolean? = null,
        @Query("lastUserId")     lastUserId     : Long?    = null,   // 커서
        @Query("size")           size           : Int      = 20,
    ): Response<DiscoverFilterResponse>

    // =========================================================================
    // Message API  (이미지 9~17)
    // =========================================================================

    /**
     * POST /api/v1/messages — 메시지 전송 (텍스트 또는 파일)
     * 파일 포함 시 파일 메시지, 없으면 텍스트 메시지
     * 지원 파일: 이미지(jpg/jpeg/png/gif/webp/heic), 비디오(mp4/mov/avi/webm),
     *            오디오(mp3/wav/m4a/aac), 문서(pdf/txt/doc/docx/xls/xlsx/ppt/pptx), 압축(zip/rar)
     * 최대 크기: 10MB
     * 204: 전송 성공 | 400: 잘못된 입력 / 지원 안 하는 파일 형식
     * 401: 인증 | 403: 채팅방 접근 권한 없음
     * 404: 유저 또는 채팅방 없음 | 413: 파일 크기 초과
     */
    @Multipart
    @POST("api/v1/messages")
    suspend fun sendMessage(
        @Part("request") request : RequestBody,   // JSON: { chatRoomId, content }
        @Part file       : MultipartBody.Part? = null,
    ): Response<Unit>

    /**
     * POST /api/v1/messages/{messageId}/read — 메시지 읽음 처리
     * 특정 메시지까지 읽음 처리
     * 204: 성공 | 401: 인증 | 403: 채팅방 접근 권한 | 404: 메시지 없음
     */
    @POST("api/v1/messages/{messageId}/read")
    suspend fun readMessage(
        @Path("messageId") messageId: Long,
    ): Response<Unit>

    /**
     * DELETE /api/v1/messages/{messageId} — 메시지 삭제
     * 자신이 전송한 메시지만, 전송 후 5분 이내만 삭제 가능
     * 204: 성공 | 401: 인증 | 403: 권한 없음 또는 5분 초과
     * 404: 메시지 없음 | 409: 이미 삭제된 메시지
     */
    @DELETE("api/v1/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("messageId") messageId: Long,
    ): Response<Unit>

    /**
     * PATCH /api/v1/messages/{messageId} — 텍스트 메시지 수정
     * 자신이 전송한 텍스트 메시지만, 전송 후 5분 이내만 수정 가능
     * 파일 메시지는 수정 불가 (400 CANNOT_EDIT_FILE_MESSAGE)
     * 200: { messageId, userId, userName, messageType, content, sendAt,
     *        fileUrl, fileName, fileSize, mediaType }
     * 400: 본문 누락 / 파일 메시지 수정 불가
     * 401: 인증 | 403: 타인 메시지 또는 5분 초과 | 404: 메시지 없음 | 409: 이미 삭제
     */
    @PATCH("api/v1/messages/{messageId}")
    suspend fun editMessage(
        @Path("messageId") messageId : Long,
        @Body request                : EditMessageRequest,
    ): Response<MessageDto>

    /**
     * GET /api/v1/messages/chatrooms/{chatRoomId} — 채팅방 메시지 동기화
     * since 이후 새 메시지, 수정된 메시지, 삭제된 메시지 ID 반환
     * since 없으면 전체 메시지 반환 (채팅방 최초 진입 시)
     * 채팅방 접속할 때 한 번 호출 → 이후 실시간은 Firebase Realtime DB로 갱신
     * 200: { newMessages[], updatedMessages[], deletedIds[] }
     * 401: 인증 | 403: 채팅방 접근 권한 없음 | 404: 채팅방 없음
     */
    @GET("api/v1/messages/chatrooms/{chatRoomId}")
    suspend fun syncMessages(
        @Path("chatRoomId") chatRoomId : Long,
        @Query("since")     since      : String? = null,  // ISO 8601 (예: "2026-04-07T10:00:00")
    ): Response<SyncMessagesResponse>

    // =========================================================================
    // User API
    // =========================================================================

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
    ): Response<List<TagDto>>

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

    // =========================================================================
    // Chatroom API
    // =========================================================================

    /**
     * GET /api/v1/chatrooms — 내 채팅방 목록 조회
     * 내가 참여 중인 모든 채팅방 목록
     * 200: [ { chatRoomId, roomName, createdAt, participantIds[], unreadCount } ]
     * 401: 인증 필요
     */
    @GET("api/v1/chatrooms")
    suspend fun getChatRooms(): Response<List<ChatRoomDto>>

    /**
     * POST /api/v1/chatrooms — 채팅방 생성
     * 새로운 채팅방을 생성하고 참여자를 초대
     * Request: { roomName, participantIds[] }
     * 200: { chatRoomId, roomName, createdAt, participantIds[], unreadCount }
     * 400: errors.roomName / errors.participantIds
     * 401: 인증 | 404: 사용자를 찾을 수 없음
     */
    @POST("api/v1/chatrooms")
    suspend fun createChatRoom(
        @Body request: CreateChatRoomRequest,
    ): Response<ChatRoomDto>

    // =========================================================================
    // Status Message API
    // =========================================================================

    /**
     * POST /api/v1/status-messages — 상태메시지 작성
     * 1인당 하나만 작성 가능 (이미 있으면 409 STATUS_MESSAGE_ALREADY_EXISTS)
     * Request: { "content": "오늘 날씨 너무 좋다!" }
     * 200: 성공 | 401: 인증 | 409: 이미 상태메시지 존재
     */
    @POST("api/v1/status-messages")
    suspend fun postStatusMessage(
        @Body request: StatusMessageRequest,
    ): Response<Unit>

    /**
     * PATCH /api/v1/status-messages — 상태메시지 수정
     * 24시간 이내에만 수정 가능 (만료 시 400 STATUS_MESSAGE_EXPIRED)
     * Request: { "content": "오늘 날씨 너무 좋다!" }
     * 200: 성공
     * 400: STATUS_MESSAGE_EXPIRED (24시간 만료)
     * 401: 인증 | 404: STATUS_MESSAGE_NOT_FOUND
     */
    @PATCH("api/v1/status-messages")
    suspend fun updateStatusMessage(
        @Body request: StatusMessageRequest,
    ): Response<Unit>

    /**
     * DELETE /api/v1/status-messages — 상태메시지 삭제
     * 204: 성공 | 401: 인증 | 404: STATUS_MESSAGE_NOT_FOUND
     */
    @DELETE("api/v1/status-messages")
    suspend fun deleteStatusMessage(): Response<Unit>

    /**
     * GET /api/v1/status-messages/me — 내 상태메시지 조회
     * 200: { statusMessageId, content, timeAgo }
     * 401: 인증 | 404: STATUS_MESSAGE_NOT_FOUND
     */
    @GET("api/v1/status-messages/me")
    suspend fun getMyStatusMessage(): Response<MyStatusMessageResponse>

    /**
     * GET /api/v1/status-messages/friends — 친구 상태메시지 목록 조회
     * 최신순, 커서 기반 무한스크롤
     * 본인 상태메시지 미포함 (별도로 GET /me 호출 후 목록 첫 번째에 추가)
     * 200: { statusMessages[], nextCursor, hasNext }
     * 401: 인증 | 404: STATUS_MESSAGE_NOT_FOUND (커서에 해당하는 메시지 없음)
     */
    @GET("api/v1/status-messages/friends")
    suspend fun getFriendStatusMessages(
        @Query("cursor") cursor : Long? = null,  // 이전 페이지 마지막 statusMessageId
        @Query("size")   size   : Int   = 20,
    ): Response<FriendStatusMessagesResponse>
}
