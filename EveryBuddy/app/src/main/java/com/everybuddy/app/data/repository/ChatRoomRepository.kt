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
     * roomName: 항상 필수 (NotBlank). 1:1방은 호출자가 상대 이름을, 그룹방은 사용자 입력값을 전달.
     * isGroup: 클라가 UI에 따라 결정. 1:1방=false + participantIds.size==1, 그룹방=true.
     * 1:1방은 백엔드 idempotent — 이미 있으면 기존 chatRoomId 반환.
     * 에러: 400(roomName/isGroup/participantIds 검증 실패) | 401 | 404(유저없음)
     */
    suspend fun createChatRoom(
        roomName       : String,
        isGroup        : Boolean,
        participantIds : List<Long>,
    ): ApiResult<ChatRoom> = safeApiCall(gson, {
        api.createChatRoom(CreateChatRoomRequest(roomName, isGroup, participantIds))
    })

    /**
     * 채팅방 나가기 — DELETE /api/v1/chatrooms/{chatRoomId}/participants/me
     * 본인만 빠짐. RTDB 정리는 서버 자동 (users/{me}/chatrooms/{roomId} 노드 삭제).
     * 에러: 403(USER_NOT_IN_CHATROOM)
     */
    suspend fun leaveChatRoom(chatRoomId: Long): ApiResult<Unit> =
        safeApiCall(gson, { api.leaveChatRoom(chatRoomId) }) { ApiResult.Success(Unit) }
}
