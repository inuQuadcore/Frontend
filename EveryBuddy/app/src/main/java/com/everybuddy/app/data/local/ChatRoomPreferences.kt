package com.everybuddy.app.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 채팅방 단위 클라 로컬 플래그 영속화 (mute / pin / star).
 * 백엔드 API 없음 — 디바이스 종속. 향후 백엔드 API 추가 시 서버 동기화로 전환.
 *
 * 키 패턴: `{flag}:{chatRoomId}` (예: `mute:42`, `pin:42`, `star:42`).
 * SharedPreferences 단순 사용. 채팅방 수 적어 검색 비효율 무관.
 */
@Singleton
class ChatRoomPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chat_room_flags", Context.MODE_PRIVATE)

    fun isMuted(roomId: String): Boolean         = prefs.getBoolean(key("mute",     roomId), false)
    fun isPinned(roomId: String): Boolean        = prefs.getBoolean(key("pin",      roomId), false)
    fun isStarred(roomId: String): Boolean       = prefs.getBoolean(key("star",     roomId), false)
    fun isAutoTranslate(roomId: String): Boolean = prefs.getBoolean(key("autoTrans", roomId), false)

    fun setMuted(roomId: String, value: Boolean)         = prefs.edit().putBoolean(key("mute",     roomId), value).apply()
    fun setPinned(roomId: String, value: Boolean)        = prefs.edit().putBoolean(key("pin",      roomId), value).apply()
    fun setStarred(roomId: String, value: Boolean)       = prefs.edit().putBoolean(key("star",     roomId), value).apply()
    fun setAutoTranslate(roomId: String, value: Boolean) = prefs.edit().putBoolean(key("autoTrans", roomId), value).apply()

    fun saveReply(roomId: String, msgId: String, text: String) {
        prefs.edit()
            .putString(key("replyId",   roomId), msgId)
            .putString(key("replyText", roomId), text)
            .apply()
    }

    fun getSavedReplyId(roomId: String): String?   = prefs.getString(key("replyId",   roomId), null)
    fun getSavedReplyText(roomId: String): String? = prefs.getString(key("replyText", roomId), null)

    fun clearReply(roomId: String) {
        prefs.edit()
            .remove(key("replyId",   roomId))
            .remove(key("replyText", roomId))
            .apply()
    }

    private fun key(flag: String, roomId: String) = "$flag:$roomId"
}
