package com.everybuddy.app.data.dummy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedUserRepository @Inject constructor() {

    /** 전체 유저 목록 (isFriend 상태 포함) — MutableStateFlow로 실시간 변경 알림 */
    private val _users = MutableStateFlow(allDummyUsers.toMutableList())
    val users: StateFlow<List<DummyUser>> = _users.asStateFlow()

    /** 친구 목록 (isFriend = true) */
    val friends: List<DummyUser>
        get() = _users.value.filter { it.isFriend }
}