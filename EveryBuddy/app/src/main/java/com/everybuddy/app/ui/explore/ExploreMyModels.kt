package com.everybuddy.app.ui.explore

import androidx.compose.ui.graphics.Color
import com.everybuddy.app.ui.onboarding.InterestCategory
import com.everybuddy.app.ui.onboarding.sampleTags

// 국적 코드(KOREA/USA/JAPAN/CHINA/FRANCE/CZECH) → 표시 변환.
// 백엔드 enum 값을 그대로 받아 변환. 알 수 없는 값은 🌐 / 코드 그대로 반환.
fun countryFlag(code: String): String = when (code.uppercase()) {
    "KOREA"  -> "🇰🇷"
    "USA"    -> "🇺🇸"
    "JAPAN"  -> "🇯🇵"
    "CHINA"  -> "🇨🇳"
    "FRANCE" -> "🇫🇷"
    "CZECH"  -> "🇨🇿"
    else     -> "🌐"
}

fun countryName(code: String): String = when (code.uppercase()) {
    "KOREA"  -> "한국"
    "USA"    -> "미국"
    "JAPAN"  -> "일본"
    "CHINA"  -> "중국"
    "FRANCE" -> "프랑스"
    "CZECH"  -> "체코"
    else     -> code
}

object AppColors {
    val Accent      = Color(0xFF0167FF)
    val TextPri     = Color(0xFF000000)
    val TextSec     = Color(0xFF797979)
    val TextRed     = Color(0xFFFF3333)
    val Border      = Color(0xFFE5E5E5)
    val TagBg       = Color(0xFFEFF4FF)
    val TagText     = Color(0xFF0167FF)
    val TagBgGray   = Color(0xFFF0F0F0)
    val TagTextGray = Color(0xFF555555)
    val Online      = Color(0xFF4CAF50)
    val CardBg      = Color(0xFFF5F7FF)
    val BannerBg    = Color(0xFFEEF4FF)
}

data class DiscoverUser(
    val userId          : Long,
    val name            : String,
    val profileImageUrl : String?     = null,  // TODO: Coil AsyncImage
    val country         : String      = "KOREA",  // 국적 코드 (KOREA, USA, JAPAN, ...)
    val bio             : String      = "",
    val languages       : List<UserLanguage> = emptyList(),
    val tags            : List<UserTag>      = emptyList(),
    val lastSeenAt      : String      = "",    // ISO 날짜 문자열
    val isOnline        : Boolean     = false, // lastSeenAt 기반 파생
    val age             : Int         = 25,    // 나이
    val consecutiveDays : Int         = 0,     // 연속 출석일
) {
    /** 한줄소개 15자 제한 */
    fun bioPreview15() = if (bio.length > 15) bio.take(15) + "…" else bio

    /** 상위 3개 태그 */
    fun top3Tags() = tags.take(3)

    /** 카드용 상위 2개 태그 + 나머지 +N */
    fun top2TagsAndRest(): Pair<List<UserTag>, Int> {
        val top2 = tags.take(2)
        val rest = (tags.size - 2).coerceAtLeast(0)
        return top2 to rest
    }

    fun countryFlag() = countryFlag(country)
    fun countryName() = countryName(country)

    /** 언어 코드 표시 */
    fun languageLabels() = languages.map { it.displayCode() }
}

data class UserLanguage(
    val language : String,  // "ENGLISH", "JAPANESE", "KOREAN", ...
    val level    : Int = 1, // 1~5
) {
    fun displayCode() = when (language.uppercase()) {
        "ENGLISH"  -> "EN"
        "JAPANESE" -> "JP"
        "KOREAN"   -> "KR"
        "CHINESE"  -> "CN"
        "FRENCH"   -> "FR"
        "CZECH"    -> "CS"
        "SPANISH"  -> "ES"
        else       -> language.take(2).uppercase()
    }
    fun flagEmoji() = when (language.uppercase()) {
        "ENGLISH"  -> "🇺🇸"
        "JAPANESE" -> "🇯🇵"
        "KOREAN"   -> "🇰🇷"
        "CHINESE"  -> "🇨🇳"
        "FRENCH"   -> "🇫🇷"
        "CZECH"    -> "🇨🇿"
        else -> "🌐"
    }
}

data class UserTag(
    val tag      : String,   // "SPORTS", "TRAVEL", "COOKING", ...
    val category : String,   // "HOBBY", "PERSONALITY", "FOOD", "ENTERTAINMENT"
    val emoji    : String = "",
    val displayName: String = tag, // 한국어 표시명
)

enum class GenderFilter(val label: String) { FEMALE("여성"), MALE("남성"), ALL("모든 성별") }
enum class ActivityFilter(val label: String, val desc: String) {
    ONLINE("현재활동중인 친구", "지금 이 앱에서 활발히 활동 중이에요."),
    RECENT("최근 접속한 친구", "최근 24시간 이내에 접속했어요."),
}

data class FilterSettings(
    val gender         : GenderFilter        = GenderFilter.ALL,
    val minAge         : Int                 = 15,
    val maxAge         : Int                 = 30,
    val country        : String?             = null,   // null = 모든 국적
    val languages      : List<String>        = emptyList(),  // 서버 코드("ENGLISH" 등), 빈 리스트 = 모든 언어
    val activityFilters: Set<ActivityFilter> = emptySet(),
    val selectedTags   : List<UserTag>       = emptyList(),  // 최대 15개
    val isOnline       : Boolean             = false,
    val recentlyActive : Boolean             = false,
) {
    fun isEmpty() = gender == GenderFilter.ALL &&
            minAge == 15 && maxAge == 30 &&
            country == null && languages.isEmpty() &&
            activityFilters.isEmpty() && selectedTags.isEmpty()
}

data class MyProfile(
    val userId          : Long    = 1L,
    val name            : String  = "홍길동",
    val profileImageUrl : String? = null,   // TODO: Coil
    val age             : Int     = 23,
    val gender          : String  = "여성",
    val country         : String  = "USA",
    val bio             : String  = "일본어 배우는 중이에요.\n가볍게 대화하면서 연습해요 😊",
    val learningLanguages: List<UserLanguage> = listOf(
        UserLanguage("ENGLISH", 2),
        UserLanguage("JAPANESE", 3),
    ),
    val tags            : List<UserTag> = listOf(
        UserTag("TRAVEL",       "HOBBY",         "✈️", "여행"),
        UserTag("PHOTOGRAPHY",  "HOBBY",         "📷", "사진찍기"),
        UserTag("RUNNING",      "HOBBY",         "🏃", "러닝"),
        UserTag("CAFE_TOUR",    "HOBBY",         "☕", "카페탐방"),
        UserTag("MOVIE",        "ENTERTAINMENT", "🎬", "영화보기"),
    ),
    val birthday        : String  = "",   // "yyyy.MM.dd" — 로드 전 빈 값
    val consecutiveDays : Int     = 0,    // 연속 출석일. 1일 이상일 때만 뱃지 표시(0이면 숨김)
) {
    fun countryFlag() = countryFlag(country)
    fun countryName() = countryName(country)
}

object ExploreDemo {

    // 온보딩에서 설정한 내 정보 (마이페이지 / 필터 기준)
    val myProfile = MyProfile()

    // 온보딩 sampleTags에서 파생 — 항상 동기화 유지
    val allTags: List<UserTag> = sampleTags.map { tag ->
        UserTag(
            tag         = tag.apiValue,
            category    = when (tag.category) {
                InterestCategory.HOBBY          -> "HOBBY"
                InterestCategory.MBTI           -> "PERSONALITY"
                InterestCategory.FOOD           -> "FOOD"
                InterestCategory.ENTERTAINMENT  -> "ENTERTAINMENT"
            },
            emoji       = tag.emoji,
            displayName = tag.label,
        )
    }

    // 탐색 카드 랜덤 세트 (6개씩)
    val randomCards = listOf(
        DiscoverUser(
            userId = 10, name = "Olivia", profileImageUrl = "olivia_card",
            country = "USA", bio = "Hello! I'm from the U.S. 😊",
            languages = listOf(UserLanguage("ENGLISH", 5), UserLanguage("KOREAN", 1)),
            tags = listOf(UserTag("PHOTOGRAPHY","HOBBY","📷","사진찍기"), UserTag("RUNNING","HOBBY","🏃","러닝"), UserTag("TRAVEL","HOBBY","✈️","여행")),
            isOnline = true, age = 26, consecutiveDays = 7,
        ),
        DiscoverUser(
            userId = 11, name = "홍현준", profileImageUrl = "user_hong",
            country = "USA", bio = "영어 배우고 싶어요",
            languages = listOf(UserLanguage("ENGLISH", 2), UserLanguage("JAPANESE", 1)),
            tags = listOf(UserTag("WORKOUT","HOBBY","🏋️","운동하기"), UserTag("COOKING","HOBBY","🍳","요리"), UserTag("FOOD_TOUR","FOOD","📍","맛집탐방")),
            isOnline = true, age = 31, consecutiveDays = 5,
        ),
        DiscoverUser(
            userId = 12, name = "손나은", profileImageUrl = "user_son",
            country = "USA", bio = "영어 배우고 싶어요",
            languages = listOf(UserLanguage("ENGLISH", 2), UserLanguage("JAPANESE", 1)),
            tags = listOf(UserTag("HIKING","HOBBY","⛰️","등산"), UserTag("COOKING","HOBBY","🍳","요리"), UserTag("FOOD_TOUR","FOOD","📍","맛집탐방")),
            isOnline = false, age = 24, consecutiveDays = 3,
        ),
        DiscoverUser(
            userId = 13, name = "Alex", profileImageUrl = "user_alex",
            country = "CZECH", bio = "Ahoj! 😊",
            languages = listOf(UserLanguage("CZECH", 5), UserLanguage("FRENCH", 3)),
            tags = listOf(UserTag("CAMPING","HOBBY","⛺","캠핑"), UserTag("READING","HOBBY","📚","독서"), UserTag("COFFEE","FOOD","☕","커피러버")),
            isOnline = false, age = 38, consecutiveDays = 1,
        ),
        DiscoverUser(
            userId = 14, name = "はるか", profileImageUrl = "user_haruka",
            country = "JAPAN", bio = "日本語で話しましょう！",
            languages = listOf(UserLanguage("JAPANESE", 5), UserLanguage("ENGLISH", 2)),
            tags = listOf(UserTag("MUSIC","ENTERTAINMENT","🎵","음악감상"), UserTag("COOKING","HOBBY","🍳","요리")),
            isOnline = true, age = 27, consecutiveDays = 12,
        ),
        DiscoverUser(
            userId = 15, name = "김민준", profileImageUrl = "user_kim",
            country = "KOREA", bio = "영어 배우고 싶어요",
            languages = listOf(UserLanguage("ENGLISH", 3), UserLanguage("JAPANESE", 1)),
            tags = listOf(UserTag("PHOTOGRAPHY","HOBBY","📷","사진찍기"), UserTag("RUNNING","HOBBY","🏃","러닝"), UserTag("TRAVEL","HOBBY","✈️","여행")),
            isOnline = true, age = 23, consecutiveDays = 30,
        ),
    )

    // 상위 태그 매칭 사용자 목록 (현활 → 가나다순)
    val tagMatchUsers = randomCards.sortedWith(
        compareByDescending<DiscoverUser> { it.isOnline }.thenBy { it.name }
    )

    // 내가 배우는 언어("ENGLISH") 배우고 싶은 사용자 목록
    val learningLangUsers = tagMatchUsers

    // 필터 적용 결과 (가나다순)
    val filterUsers = randomCards.sortedBy { it.name }

    // 온보딩 국적 목록
    val countryList = listOf("모든 국적", "한국", "미국", "일본", "중국", "프랑스", "체코")
    val languageList = listOf("모든 언어", "영어", "일본어", "한국어", "중국어", "프랑스어")
}
