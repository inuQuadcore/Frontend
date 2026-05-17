package com.everybuddy.app.ui.friend

/**
 * 친구 도메인 잔존 상수.
 * dummy chatRooms/DemoChatRoom/DemoChatMsg는 C14에서 제거됨 — 답장 흐름은 실제 chat REST 사용.
 * MY_USER_ID/MY_NAME은 FriendViewModel.submitStatus(dummy status post)/visibleStatusMessages에서
 * 본인 status 식별용으로 사용 중. 향후 TokenManager.userId로 교체 + StatusMessage 도메인 정리 시 제거.
 */
object FriendDemoData {
    const val MY_USER_ID = 0L
    const val MY_NAME    = "나"
}