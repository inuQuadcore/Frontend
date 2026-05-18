package com.everybuddy.app.ui.explore

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Explore 필터 설정 영속화 (디바이스 종속).
 *
 * ViewModel state만으론 process death/시스템 메모리 회수 시 날아감.
 * SharedPreferences + Gson으로 FilterSettings 직렬화 보존.
 * 필터 결과 list는 API 산출물이라 저장 X — 복원 시 재호출.
 */
@Singleton
class ExploreFilterPreferences @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("explore_filter", Context.MODE_PRIVATE)

    fun load(): FilterSettings? {
        val json = prefs.getString(KEY_SETTINGS, null) ?: return null
        return try {
            gson.fromJson(json, FilterSettings::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun isApplied(): Boolean = prefs.getBoolean(KEY_APPLIED, false)

    fun save(settings: FilterSettings, isApplied: Boolean) {
        prefs.edit()
            .putString(KEY_SETTINGS, gson.toJson(settings))
            .putBoolean(KEY_APPLIED, isApplied)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_SETTINGS = "settings"
        private const val KEY_APPLIED  = "isApplied"
    }
}
