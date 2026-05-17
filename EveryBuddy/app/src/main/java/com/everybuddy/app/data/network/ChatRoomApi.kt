package com.everybuddy.app.data.network

import com.everybuddy.app.data.dto.ChatRoom
import com.everybuddy.app.data.dto.CreateChatRoomRequest
import com.everybuddy.app.data.dto.InviteParticipantsRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatRoomApi {

    /**
     * GET /api/v1/chatrooms — 내 채팅방 목록 조회
     * 내가 참여 중인 모든 채팅방 목록
     * 200: [ { chatRoomId, roomName, createdAt, participantIds[], unreadCount } ]
     * 401: 인증 필요
     */
    @GET("api/v1/chatrooms")
    suspend fun getChatRooms(): Response<List<ChatRoom>>

    /**
     * POST /api/v1/chatrooms — 채팅방 생성
     * 새로운 채팅방을 생성하고 참여자를 초대
     * Request: { roomName, participantIds[] }
     * 200: { chatRoomId, roomName, createdAt, participantIds[], unreadCount } (unreadCount는 null)
     * 400: errors.roomName / errors.participantIds
     * 401: 인증 | 404: 사용자를 찾을 수 없음
     */
    @POST("api/v1/chatrooms")
    suspend fun createChatRoom(
        @Body request: CreateChatRoomRequest,
    ): Response<ChatRoom>

    /**
     * DELETE /api/v1/chatrooms/{chatRoomId}/participants/me — 채팅방 나가기.
     * 본인만 빠지고 채팅방·다른 참여자는 그대로 유지.
     * 204: 성공 (본문 없음) | 403: USER_NOT_IN_CHATROOM
     */
    @DELETE("api/v1/chatrooms/{chatRoomId}/participants/me")
    suspend fun leaveChatRoom(
        @Path("chatRoomId") chatRoomId: Long,
    ): Response<Unit>

    /**
     * POST /api/v1/chatrooms/{chatRoomId}/participants — 멤버 초대 (그룹방만).
     * 1:1방(isGroup=false)에서 호출 시 403 CANNOT_INVITE_TO_DIRECT.
     * 204: 성공 | 400/403/404/409/410: 각종 에러 (chatroom.md 참고).
     */
    @POST("api/v1/chatrooms/{chatRoomId}/participants")
    suspend fun inviteParticipants(
        @Path("chatRoomId") chatRoomId : Long,
        @Body request                  : InviteParticipantsRequest,
    ): Response<Unit>
}
