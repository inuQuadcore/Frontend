package com.everybuddy.app.data.network

import com.everybuddy.app.data.dto.ChatRoom
import com.everybuddy.app.data.dto.CreateChatRoomRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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
}
