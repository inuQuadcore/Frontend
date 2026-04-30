package com.everybuddy.app.data.dummy

/**
 * DummyUser — 탐색/친구/채팅 공용 더미 프로필
 *
 * @param id              고유 ID
 * @param name            이름 (영어 / 한국어 / 한자 / 일본어 / 기타)
 * @param profileImageRes 프로필 이미지 리소스 — 추후 URL로 교체
 *                        현재: ic_profile_default (placeholder)
 * @param nationCode      국적 코드 (국기 이모지 및 아이콘 매핑용)
 * @param email           이메일 (앱 내 숨김 처리 예정 — UI 미노출)
 * @param age             나이
 * @param gender          성별 ("남성" / "여성")
 * @param bio             한줄 소개
 * @param myLangs         사용 언어 + 레벨 (e.g. "EN" to 3)
 * @param learnLangs      학습 언어 코드 목록 (e.g. ["KO", "JP"])
 * @param tags            관심사 태그 목록 (apiValue 문자열)
 * @param isActive        현재활동중 여부
 * @param streakDays      연속 출석 일수
 * @param isFriend        친구 여부 (탐색→친구 추가 로직에서 toggle)
 */
data class DummyUser(
    val id              : String,
    val name            : String,
    val profileImageRes : Int            = 0,    // TODO: URL로 교체 시 String으로 변경
    val nationCode      : String,
    val email           : String,                // UI 미노출 — API 전송용
    val age             : Int,
    val gender          : String,
    val bio             : String,
    val myLangs         : Map<String, Int>,      // 언어코드 → 레벨(1~5)
    val learnLangs      : List<String>,
    val tags            : List<String>,          // 관심사 apiValue
    val isActive        : Boolean        = false,
    val streakDays      : Int            = 0,
    val isFriend        : Boolean        = false,
)

// 더미 프로필 30개
//
// 분포
//  영어 이름   : u01~u07 (GB/US, 7명)
//  한국어 이름  : u08~u16 (KR, 9명)
//  중국어 이름  : u17~u20 (CN, 4명)
//  일본어 이름  : u21~u24 (JP, 4명)
//  기타 국적    : u25~u30 (FR/ES/DE/RU/VN/IN, 6명)
//
// isFriend:
//  true  → u01,u02,u03,u05,u06,u08,u09,u11,u12,u13,u17,u19,u21,u24,u29 (15명)
//  false → 나머지 15명 (탐색 탭에서 추가 가능)

val allDummyUsers: List<DummyUser> = listOf(
    // 1. 영어 이름 (GB / US)
    //    검색 검증: 대소문자, 중복 스펠링 (Tony/Toby, Peter/Potter)

    DummyUser(
        id         = "u01",
        name       = "Potter",
        nationCode = "GB",
        email      = "harry@magic.com",
        age        = 23, gender = "남성",
        bio        = "Welcome To Hogwarts!",
        myLangs    = mapOf("EN" to 5),
        learnLangs = listOf("KO"),
        tags       = listOf("HIKING", "SPORTS", "READING"),
        isActive   = true, streakDays = 7, isFriend = true,
    ),
    DummyUser(
        id         = "u02",
        name       = "Hermione",
        nationCode = "GB",
        email      = "hermione@magic.com",
        age        = 23, gender = "여성",
        bio        = "It's Wingardium Leviosa, not Leviosah.",
        myLangs    = mapOf("EN" to 5),
        learnLangs = listOf("KO", "FR"),
        tags       = listOf("READING", "WRITING", "STUDY_GROUP"),
        isActive   = false, streakDays = 3, isFriend = true,
    ),
    DummyUser(
        id         = "u03",
        name       = "Peter",
        nationCode = "US",
        email      = "spidey@marvel.com",
        age        = 21, gender = "남성",
        bio        = "Your friendly neighborhood Spider-Man! 🕸️",
        myLangs    = mapOf("EN" to 5),
        learnLangs = listOf("KO", "JP"),
        tags       = listOf("PHOTOGRAPHY", "GYM", "GAMING"),
        isActive   = true, streakDays = 5, isFriend = true,
    ),
    DummyUser(
        id         = "u04",
        name       = "Stark",
        nationCode = "US",
        email      = "ironman@stark.com",
        age        = 35, gender = "남성",
        bio        = "사랑해요 연애가 중계.",
        myLangs    = mapOf("EN" to 5),
        learnLangs = listOf("KO", "ZH"),
        tags       = listOf("DIY", "INVESTMENT", "BURGER"),
        isActive   = false, streakDays = 0, isFriend = false,
    ),
    DummyUser(
        id         = "u05",
        name       = "Tony",
        nationCode = "US",
        email      = "tony@stark.com",
        age        = 35, gender = "남성",
        bio        = "I am Iron Man.",
        myLangs    = mapOf("EN" to 5),
        learnLangs = listOf("KO"),
        tags       = listOf("DIY", "INVESTMENT", "BURGER"),
        isActive   = true, streakDays = 2, isFriend = true,
    ),
    DummyUser(
        id         = "u06",
        name       = "Toby",
        nationCode = "US",
        email      = "toby@spidey.com",
        age        = 26, gender = "남성",
        bio        = "Great power comes with responsibility.",
        myLangs    = mapOf("EN" to 5),
        learnLangs = listOf("KO"),
        tags       = listOf("PHOTOGRAPHY", "GYM", "PIZZA"),
        isActive   = false, streakDays = 1, isFriend = true,
    ),
    DummyUser(
        id         = "u07",
        name       = "Thor",
        nationCode = "US",
        email      = "thor@asgard.com",
        age        = 30, gender = "남성",
        bio        = "Bring me Thanos! I love beer.",
        myLangs    = mapOf("EN" to 5),
        learnLangs = listOf("KO"),
        tags       = listOf("WORKOUT", "BEER", "MEAT"),
        isActive   = true, streakDays = 9, isFriend = false,
    ),

    // 2. 한국어 이름 (KR)
    //    검색 검증: 초성 'ㄱ','ㄴ','ㅇ', 외자('오혁', '강혁'), 네글자('황보은준')

    DummyUser(
        id         = "u08",
        name       = "오혁",
        nationCode = "KR",
        email      = "oh@hyuk.com",
        age        = 28, gender = "남성",
        bio        = "밴드 혁오입니다. 예술을 사랑해요.",
        myLangs    = mapOf("KO" to 5),
        learnLangs = listOf("ZH", "EN"),
        tags       = listOf("ART", "FASHION", "MUSIC"),
        isActive   = true, streakDays = 10, isFriend = true,
    ),
    DummyUser(
        id         = "u09",
        name       = "신온유",
        nationCode = "KR",
        email      = "onyu@band.com",
        age        = 25, gender = "여성",
        bio        = "떠오르는 신인류",
        myLangs    = mapOf("KO" to 5),
        learnLangs = listOf("EN"),
        tags       = listOf("INSTRUMENT", "DIY", "MOVIE"),
        isActive   = false, streakDays = 4, isFriend = true,
    ),
    DummyUser(
        id         = "u10",
        name       = "윤지영",
        nationCode = "KR",
        email      = "jiyoung@indie.com",
        age        = 26, gender = "여성",
        bio        = "노래하는 윤지영입니다. 산책을 좋아해요.",
        myLangs    = mapOf("KO" to 5),
        learnLangs = listOf("EN", "FR"),
        tags       = listOf("WALKING", "DOG", "CAFE_TOUR"),
        isActive   = true, streakDays = 6, isFriend = false,
    ),
    DummyUser(
        id         = "u11",
        name       = "김한주",
        nationCode = "KR",
        email      = "silicagel@indie.com",
        age        = 26, gender = "남성",
        bio        = "No pain No gain 아닙니다.",
        myLangs    = mapOf("KO" to 5),
        learnLangs = listOf("EN"),
        tags       = listOf("SINGING", "INSTRUMENT", "MUSIC"),
        isActive   = true, streakDays = 14, isFriend = true,
    ),
    DummyUser(
        id         = "u12",
        name       = "노상현",
        nationCode = "KR",
        email      = "sh@band.com",
        age        = 28, gender = "남성",
        bio        = "밴드 너드커넥션 멤버입니다. 같이 언어 공부해요!",
        myLangs    = mapOf("KO" to 5),
        learnLangs = listOf("EN", "JP"),
        tags       = listOf("GUITAR", "READING", "CAFE_TOUR"),
        isActive   = true, streakDays = 3, isFriend = true,
    ),
    DummyUser(
        id         = "u13",
        name       = "나상현",
        nationCode = "KR",
        email      = "sh.na@band.com",
        age        = 29, gender = "남성",
        bio        = "나상현씨밴드의나상현입니다.",
        myLangs    = mapOf("KO" to 5),
        learnLangs = listOf("EN"),
        tags       = listOf("MUSIC", "CAFE_TOUR", "TRAVEL"),
        isActive   = false, streakDays = 1, isFriend = true,
    ),
    DummyUser(
        id         = "u14",
        name       = "조휴일",
        nationCode = "KR",
        email      = "holiday@band.com",
        age        = 32, gender = "남성",
        bio        = "검정치마입니다. 가끔 가사를 쓰며 시간을 보내요.",
        myLangs    = mapOf("KO" to 5),
        learnLangs = listOf("EN"),
        tags       = listOf("WRITING", "MOVIE", "WINE"),
        isActive   = true, streakDays = 8, isFriend = false,
    ),
    DummyUser(
        id         = "u15",
        name       = "강 혁",                    // 외자 이름 (공백 포함 — 검색 검증용)
        nationCode = "KR",
        email      = "hyuk@band.com",
        age        = 25, gender = "남성",
        bio        = "밴드 혁오의 오혁...이 아닌 강혁입니다.",
        myLangs    = mapOf("KO" to 5),
        learnLangs = listOf("ZH"),
        tags       = listOf("FASHION", "ART", "HIPHOP"),
        isActive   = false, streakDays = 0, isFriend = false,
    ),
    DummyUser(
        id         = "u16",
        name       = "황보은준",                 // 네 글자 이름 — 검색 검증용
        nationCode = "KR",
        email      = "ej@band.com",
        age        = 25, gender = "남성",
        bio        = "풀스택",
        myLangs    = mapOf("KO" to 5),
        learnLangs = listOf("EN"),
        tags       = listOf("GAMING", "WEBTOON", "DESSERT"),
        isActive   = true, streakDays = 5, isFriend = false,
    ),

    // 3. 중국어 이름 (CN)
    //    영어발음 + 한자 + 중국어 한줄소개
    DummyUser(
        id         = "u17",
        name       = "Winwin",
        nationCode = "CN",
        email      = "winwin@nct.com",
        age        = 24, gender = "남성",
        bio        = "My name is Winwin! 我喜欢跳舞",
        myLangs    = mapOf("ZH" to 5),
        learnLangs = listOf("KO"),
        tags       = listOf("DANCE", "FOOD_TOUR", "TRAVEL"),
        isActive   = true, streakDays = 6, isFriend = true,
    ),
    DummyUser(
        id         = "u18",
        name       = "Karina",
        nationCode = "CN",
        email      = "karina@cos.com",
        age        = 23, gender = "여성",
        bio        = "Cosplayer from China. Let's be friends!",
        myLangs    = mapOf("ZH" to 5, "EN" to 4),
        learnLangs = listOf("KO", "JP"),
        tags       = listOf("FASHION", "ANIME", "GAMING"),
        isActive   = false, streakDays = 2, isFriend = false,
    ),
    DummyUser(
        id         = "u19",
        name       = "宁宁",
        nationCode = "CN",
        email      = "ningning@idol.com",
        age        = 22, gender = "여성",
        bio        = "Hello~ woshi NingNing. Let's chat!",
        myLangs    = mapOf("ZH" to 5),
        learnLangs = listOf("KO", "EN"),
        tags       = listOf("FASHION", "SINGING", "COOKING"),
        isActive   = true, streakDays = 11, isFriend = true,
    ),
    DummyUser(
        id         = "u20",
        name       = "雨琦",
        nationCode = "CN",
        email      = "yuqi@idol.com",
        age        = 24, gender = "여성",
        bio        = "I love Gidle! 我想学韩语",
        myLangs    = mapOf("ZH" to 5),
        learnLangs = listOf("KO", "EN"),
        tags       = listOf("GYM", "DANCE", "FOOD_TOUR"),
        isActive   = false, streakDays = 0, isFriend = false,
    ),

    // 4. 일본어 이름 (JP)
    //    가타카나/히라가나/영어발음 + 일본어 한줄소개
    DummyUser(
        id         = "u21",
        name       = "Aki",
        nationCode = "JP",
        email      = "gun@csm.com",
        age        = 24, gender = "남성",
        bio        = "Kon.",
        myLangs    = mapOf("JA" to 5),
        learnLangs = listOf("KO", "EN"),
        tags       = listOf("COFFEE", "COOKING", "READING"),
        isActive   = true, streakDays = 4, isFriend = true,
    ),
    DummyUser(
        id         = "u22",
        name       = "デンジ",
        nationCode = "JP",
        email      = "denji@csm.com",
        age        = 19, gender = "남성",
        bio        = "食パン Tabetai!",
        myLangs    = mapOf("JA" to 5),
        learnLangs = listOf("KO"),
        tags       = listOf("BREAD", "SOLO_MEAL", "DOG"),
        isActive   = true, streakDays = 2, isFriend = false,
    ),
    DummyUser(
        id         = "u23",
        name       = "パワー",
        nationCode = "JP",
        email      = "power@csm.com",
        age        = 20, gender = "여성",
        bio        = "Kono washi ga saikyo!",
        myLangs    = mapOf("JA" to 5),
        learnLangs = listOf("KO"),
        tags       = listOf("CAT", "MEAT", "SPICY"),
        isActive   = false, streakDays = 0, isFriend = false,
    ),
    DummyUser(
        id         = "u24",
        name       = "マキマ",
        nationCode = "JP",
        email      = "makima@csm.com",
        age        = 25, gender = "여성",
        bio        = "ワン :)",
        myLangs    = mapOf("JA" to 5),
        learnLangs = listOf("KO", "EN"),
        tags       = listOf("DOG", "MOVIE", "COFFEE"),
        isActive   = true, streakDays = 7, isFriend = true,
    ),

    // 5. 기타 국적 (FR / ES / DE / RU / VN / IN)
    DummyUser(
        id         = "u25",
        name       = "Leo",
        nationCode = "FR",
        email      = "leo@france.com",
        age        = 22, gender = "남성",
        bio        = "Bonjour! Love French wine and bread.",
        myLangs    = mapOf("FR" to 5, "EN" to 3),
        learnLangs = listOf("KO"),
        tags       = listOf("WINE", "BREAD", "TRAVEL"),
        isActive   = true, streakDays = 3, isFriend = false,
    ),
    DummyUser(
        id         = "u26",
        name       = "Elena",
        nationCode = "ES",
        email      = "elena@spain.com",
        age        = 25, gender = "여성",
        bio        = "Hola! I want to learn Korean culture.",
        myLangs    = mapOf("ES" to 5, "EN" to 4),
        learnLangs = listOf("KO"),
        tags       = listOf("DANCE", "FOOD_TOUR", "MUSIC"),
        isActive   = false, streakDays = 1, isFriend = false,
    ),
    DummyUser(
        id         = "u27",
        name       = "Hans",
        nationCode = "DE",
        email      = "hans@germany.com",
        age        = 29, gender = "남성",
        bio        = "Guten Tag. I love cars and beer.",
        myLangs    = mapOf("DE" to 5),
        learnLangs = listOf("KO", "EN"),
        tags       = listOf("CAR", "BEER", "CAMPING"),
        isActive   = true, streakDays = 5, isFriend = false,
    ),
    DummyUser(
        id         = "u28",
        name       = "Svetlana",
        nationCode = "RU",
        email      = "svet@russia.com",
        age        = 23, gender = "여성",
        bio        = "Privet! Studying Korean literature.",
        myLangs    = mapOf("RU" to 5),
        learnLangs = listOf("KO", "EN"),
        tags       = listOf("READING", "WRITING", "MOVIE"),
        isActive   = false, streakDays = 0, isFriend = false,
    ),
    DummyUser(
        id         = "u29",
        name       = "Minh",
        nationCode = "VN",
        email      = "minh@viet.com",
        age        = 21, gender = "남성",
        bio        = "Xin chào! Love Pho and K-pop.",
        myLangs    = mapOf("VI" to 5),
        learnLangs = listOf("KO", "EN"),
        tags       = listOf("MUSIC", "COOKING", "STUDY_GROUP"),
        isActive   = true, streakDays = 8, isFriend = true,
    ),
    DummyUser(
        id         = "u30",
        name       = "Raj",
        nationCode = "IN",
        email      = "raj@india.com",
        age        = 27, gender = "남성",
        bio        = "Namaste. Software engineer here.",
        myLangs    = mapOf("HI" to 5, "EN" to 5),
        learnLangs = listOf("KO"),
        tags       = listOf("GAMING", "GYM", "SELF_DEV"),
        isActive   = false, streakDays = 0, isFriend = false,
    ),
)

// 편의 필터 프로퍼티
/** 탐색 탭 전체 30명 */
val exploreUsers: List<DummyUser>
    get() = allDummyUsers

/**
 * 친구 탭 — isFriend = true (15명)
 * u01,u02,u03,u05,u06,u08,u09,u11,u12,u13,u17,u19,u21,u24,u29
 */
val friendUsers: List<DummyUser>
    get() = allDummyUsers.filter { it.isFriend }

/** 채팅 생성 — 친구만 채팅 가능 */
val chattableFriends: List<DummyUser>
    get() = friendUsers

/** 현재 활동중 */
val activeUsers: List<DummyUser>
    get() = allDummyUsers.filter { it.isActive }


// 국기 이모지 변환
fun DummyUser.flagEmoji(): String = nationFlagEmoji(nationCode)

fun nationFlagEmoji(code: String): String = when (code.uppercase()) {
    "KR" -> "🇰🇷"
    "US" -> "🇺🇸"
    "GB" -> "🇬🇧"
    "JP" -> "🇯🇵"
    "CN" -> "🇨🇳"
    "TW" -> "🇹🇼"
    "AU" -> "🇦🇺"
    "CA" -> "🇨🇦"
    "FR" -> "🇫🇷"
    "DE" -> "🇩🇪"
    "ES" -> "🇪🇸"
    "RU" -> "🇷🇺"
    "VN" -> "🇻🇳"   // 베트남 (Minh)
    "IN" -> "🇮🇳"   // 인도 (Raj)
    else -> "🌐"
}

/** 학습 언어 표시용 (e.g. "EN  JP") */
fun DummyUser.learnLangsDisplay(): String = learnLangs.joinToString("  ")

/** 상위 태그 3개 */
fun DummyUser.topTags(): List<String> = tags.take(3)
