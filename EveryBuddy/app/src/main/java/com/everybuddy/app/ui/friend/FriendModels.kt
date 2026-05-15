package com.everybuddy.app.ui.friend

import java.util.UUID

import androidx.compose.ui.graphics.Color

object FriendColors {
    val Accent      = Color(0xFF0167FF)
    val TextPri     = Color(0xFF000000)
    val TextSec     = Color(0xFF797979)
    val TextRed     = Color(0xFFFF3333)
    val Border      = Color(0xFFE5E5E5)
    val BgSection   = Color(0xFFF8F8F8)
    val Online      = Color(0xFF4CAF50)   // 현재활동중 뱃지
    val TagBg       = Color(0xFFEFF4FF)
    val TagText     = Color(0xFF0167FF)
    val SendBtnBg   = Color(0xFF0167FF)
}

data class FriendProfile(
    val id              : Long,
    val name            : String,
    val profileImageUrl : String?   = null,   // TODO: Coil AsyncImage
    val nationality     : String    = "KR",   // 국적 플래그 이모지 코드
    val nativeLanguages : List<String> = listOf("KR"),  // 모국어 (예: KR, EN, JP)
    val learningLanguages: List<String> = listOf("EN"), // 배우는 언어
    val interests       : List<String> = emptyList(), // 관심사 태그 (온보딩 설정)
    val bio             : String    = "",     // 자기소개
    val isOnline        : Boolean   = false,  // 현재활동중
    val isFriend        : Boolean   = true,
    val isFollowing     : Boolean   = false,
)

data class StatusMessage(
    val id          : String = UUID.randomUUID().toString(),
    val authorId    : Long,
    val authorName  : String,
    val profileImageUrl: String? = null,  // TODO: Coil
    val content     : String,             // 최대 150자
    val createdAt   : Long = System.currentTimeMillis(),  // epoch millis
    val isMyMessage : Boolean = false,
) {
    /** 경과 시간 표시 문자열 (예: "2시간 전") */
    fun elapsedText(): String {
        val diffMs  = System.currentTimeMillis() - createdAt
        val minutes = diffMs / 60_000
        val hours   = diffMs / 3_600_000
        return when {
            minutes < 1  -> "방금"
            hours   < 1  -> "${minutes}분 전"
            hours   < 24 -> "${hours}시간 전"
            else         -> "${hours / 24}일 전"
        }
    }

    /** 24시간 이내인지 여부 */
    fun isVisible(): Boolean =
        System.currentTimeMillis() - createdAt < 24 * 3_600_000L

    /** 25자 제한 (친구 목록 미리보기) */
    fun preview25(): String =
        if (content.length > 25) content.take(25) + "…" else content

    /** 80자 제한 (내 상태메시지 미리보기) */
    fun preview80(): String =
        if (content.length > 80) content.take(80) + "…" else content

    /** 15자 제한 (채팅방 인용 미리보기) */
    fun preview15(): String =
        if (content.length > 15) content.take(15) + "…" else content
}

enum class FriendSortOption(val label: String) {
    GANADA("가나다순"),
    RECENT("최근 대화순"),
    ONLINE_FIRST("온라인 우선"),
}

object KoreanChosung {
    private val CHOSUNG = arrayOf(
        "ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ",
        "ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ"
    )

    /** 문자열에서 초성 배열 추출 */
    fun extractChosung(text: String): String = buildString {
        for (ch in text) {
            val code = ch.code
            if (code in 0xAC00..0xD7A3) {
                val offset = code - 0xAC00
                append(CHOSUNG[offset / (21 * 28)])
            } else {
                append(ch)
            }
        }
    }

    /**
     * 쿼리가 대상에 포함되는지 검사
     * - 일반 문자열 포함 (대소문자 무시)
     * - 초성만 입력해도 매칭
     */
    fun matches(query: String, target: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim()
        // 1. 일반 포함 검사
        if (target.contains(q, ignoreCase = true)) return true
        // 2. 초성 검사 — 쿼리가 초성(ㄱ~ㅎ)으로만 이루어진 경우
        val isChosungQuery = q.all { it in 'ㄱ'..'ㅎ' }
        if (isChosungQuery) {
            val targetChosung = extractChosung(target)
            if (targetChosung.contains(q)) return true
        }
        return false
    }
}

fun countryCodeToFlag(code: String): String {
    return code.uppercase().map { char ->
        Character.toCodePoint('\uD83C', '\uDDE6' + (char - 'A') + 0x1F1E6 - 0x1F1E6)
    }.joinToString("") { String(Character.toChars(it + 0x1F1E6 - 'A'.code + 0x1F1E6 - 0x1F1E6) ) }
        .ifEmpty { code }  // fallback: 원래 코드
}

/** 언어코드 → 표시 텍스트 */
fun languageLabel(code: String) = when (code.uppercase()) {
    "KR" -> "KR"
    "EN" -> "EN"
    "JP" -> "JP"
    "CN" -> "CN"
    "FR" -> "FR"
    "ES" -> "ES"
    "DE" -> "DE"
    else -> code.uppercase()
}

/** 언어코드 → 국기 이모지 (간단 버전) */
fun languageFlag(code: String) = when (code.uppercase()) {
    "KR" -> "🇰🇷"
    "EN" -> "🇺🇸"
    "JP" -> "🇯🇵"
    "CN" -> "🇨🇳"
    "FR" -> "🇫🇷"
    "ES" -> "🇪🇸"
    "DE" -> "🇩🇪"
    else -> "🌐"
}
