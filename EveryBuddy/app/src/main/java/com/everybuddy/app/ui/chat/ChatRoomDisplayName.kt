package com.everybuddy.app.ui.chat

import com.everybuddy.app.data.cache.UserSummaryCache
import com.everybuddy.app.data.dto.ChatRoom

/**
 * ChatRoom에서 화면에 표시할 이름을 결정.
 * 정책 (`isGroup` 기준 — 단톡방에서 인원 빠져 2명 남은 케이스 대응):
 * - 1:1방 (`isGroup == false`): 서버 roomName 무시, UserSummaryCache로 상대 이름 표시.
 *   (서버 roomName은 생성자가 박은 값이라 양쪽 사용자가 다르게 봐서 정합성 X)
 * - 그룹방 (`isGroup == true`): 서버 roomName 그대로 표시.
 */
object ChatRoomDisplayName {

    suspend fun resolve(
        room       : ChatRoom,
        myUserId   : Long,
        userCache  : UserSummaryCache,
    ): String {
        if (room.isGroup) return room.roomName
        val otherId = room.participantIds.firstOrNull { it != myUserId } ?: return room.roomName
        return userCache.get(otherId)?.name ?: room.roomName
    }
}
