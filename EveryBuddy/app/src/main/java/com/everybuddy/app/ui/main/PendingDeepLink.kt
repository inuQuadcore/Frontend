package com.everybuddy.app.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * FCM 트레이 알림 클릭 시 Activity 시작 → MainScreen 진입 사이를 잇는 단방향 큐.
 * MainActivity가 Intent에서 파싱해 set, MainScreen이 LaunchedEffect로 1회 consume.
 *
 * - chatRoomId: 채팅 푸시 클릭 시 채팅방 직접 진입
 * - friendUserId: 친구추가 푸시 클릭 시 친구 탭 진입 + 해당 fromUserId 프로필 자동 표시
 *   (친구 목록에 매칭되면 프로필, 매칭 실패 시 친구 탭만 열림 — fallback)
 */
object PendingDeepLink {
    var chatRoomId  : String? by mutableStateOf(null)
    var friendUserId: Long?   by mutableStateOf(null)
}
