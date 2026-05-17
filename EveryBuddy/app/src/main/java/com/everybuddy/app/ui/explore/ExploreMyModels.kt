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

// 프로필 진입 시 GET /users/{id}로 보강되는 정보. DiscoverUser와 합쳐 UserProfileScreen 표시
data class UserDetail(
    val age             : Int,
    val gender          : String,  // "MALE"/"FEMALE", UI에서 한글 변환
    val consecutiveDays : Int,
    val isFriend        : Boolean,
) {
    fun genderLabel(): String = when (gender.uppercase()) {
        "MALE"   -> "남성"
        "FEMALE" -> "여성"
        else     -> ""
    }
}

enum class GenderFilter(val label: String) {
    FEMALE("여성"), MALE("남성"), ALL("모든 성별");

    fun toApiCode(): String? = when (this) {
        MALE   -> "MALE"
        FEMALE -> "FEMALE"
        ALL    -> null
    }
}
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
    val userId          : Long    = 0L,
    val name            : String  = "",
    val profileImageUrl : String? = null,
    val age             : Int     = 0,
    val gender          : String  = "",
    val country         : String  = "",
    val bio             : String  = "",
    val learningLanguages: List<UserLanguage> = emptyList(),
    val tags            : List<UserTag>       = emptyList(),
    val birthday        : String  = "",
    val consecutiveDays : Int     = 0,
) {
    fun countryFlag() = countryFlag(country)
    fun countryName() = countryName(country)
}

object ExploreDemo {

    // 온보딩에서 설정한 내 정보 (마이페이지 / 필터 기준). MyViewModel 로드 후 갱신됨.
    var myProfile = MyProfile()

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

    // 온보딩 국적 목록
    val countryList = listOf("모든 국적", "한국", "미국", "일본", "중국", "프랑스", "체코")
    val languageList = listOf("모든 언어", "영어", "일본어", "한국어", "중국어", "프랑스어")
}
