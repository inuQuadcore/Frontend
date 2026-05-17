package com.everybuddy.app.data.cache

import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.repository.UserRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** 송신자/참여자 표시용 최소 정보. */
data class UserSummary(
    val userId          : Long,
    val name            : String,
    val profileImageUrl : String?,
)

/**
 * userId → UserSummary 메모리 캐시.
 * - 캐시 miss 시 GET /api/v1/users/{userId} 1회 호출 후 저장.
 * - 메시지 송신자 프로필, 1:1방 상대 이름, 그룹방 참여자 표시 등에서 공통 사용.
 * - 앱 세션 동안 유지. 프로필 변경 반영 필요 시 invalidate(userId) 호출.
 */
@Singleton
class UserSummaryCache @Inject constructor(
    private val userRepository: UserRepository,
) {
    private val cache = ConcurrentHashMap<Long, UserSummary>()

    /** 캐시 hit → 즉시 반환, miss → API 호출 후 캐시. fetch 실패 시 null. */
    suspend fun get(userId: Long): UserSummary? {
        cache[userId]?.let { return it }
        return when (val result = userRepository.getUserProfile(userId)) {
            is ApiResult.Success -> {
                val data = result.data ?: return null
                val summary = UserSummary(
                    userId          = userId,
                    name            = data.name,
                    profileImageUrl = data.profileImageUrl,
                )
                cache[userId] = summary
                summary
            }
            else -> null
        }
    }

    /** 캐시에 있으면 반환, 없으면 null (네트워크 호출 X). */
    fun peek(userId: Long): UserSummary? = cache[userId]

    /** 다른 API 응답에서 받은 user 정보를 prefetch — 이후 get() 시 cache hit. */
    fun put(userId: Long, summary: UserSummary) {
        cache[userId] = summary
    }

    fun invalidate(userId: Long) {
        cache.remove(userId)
    }

    fun clear() {
        cache.clear()
    }
}
