package com.everybuddy.app.ui.friend

import java.util.UUID

object FriendDemoData {

    const val MY_USER_ID = 0L
    const val MY_NAME    = "나"

    data class DemoChatRoom(
        val id         : String,
        val friendId   : String,
        val friendName : String,
        val messages   : MutableList<DemoChatMsg> = mutableListOf(),
    )

    data class DemoChatMsg(
        val id      : String = UUID.randomUUID().toString(),
        val text    : String,
        val isMine  : Boolean,
        val isStatusReply: Boolean = false,  // 상태메시지 답장 여부
        val originalStatusPreview: String = "",  // 채팅방 내 상단 인용 텍스트
    )

    val chatRooms: MutableList<DemoChatRoom> = mutableListOf()
}

private fun UUID.randomUUID() = java.util.UUID.randomUUID()