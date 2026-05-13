package com.everybuddy.app.data.repository

import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.ChatRoom
import com.everybuddy.app.data.dto.CreateChatRoomRequest
import com.everybuddy.app.data.network.ChatRoomApi
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRoomRepository @Inject constructor(
    private val api  : ChatRoomApi,
    private val gson : Gson,
) {
    /**
     * 내 채팅방 목록 조회 — GET /api/v1/chatrooms
     * 에러: 401(인증)
     */
    suspend fun getChatRooms(): ApiResult<List<ChatRoom>> =
        safeApiCall(gson, { api.getChatRooms() })

    /**
     * 채팅방 생성 — POST /api/v1/chatrooms
     * 에러: 400(errors.roomName / errors.participantIds) | 401 | 404(유저없음)
     */
    suspend fun createChatRoom(
        roomName       : String,
        participantIds : List<Long>,
    ): ApiResult<ChatRoom> = safeApiCall(gson, {
        api.createChatRoom(CreateChatRoomRequest(roomName, participantIds))
    })
}
