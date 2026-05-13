package com.everybuddy.app.ui.onboarding

import com.everybuddy.app.R

// 총 7단계 + 앱소개
const val ONBOARDING_TOTAL_STEPS = 7
// 바이오 최대 150자 제한
const val BIO_MAX_LENGTH         = 150
// TODO:
// 1. 태그 슬라이드 카테고리
// 2. 태그들 드래그, 카테고리 별로 24개 정도

// 관심사 태그 최대 선택 개수 = 15개
const val TAG_MAX_COUNT          = 15

// Enums 집합
/* 성별 선택 Enum
* label; ui 표시 텍스트 "남성"/"여성"
* symbol: ic_male.xml, ic_female.xml
* apiValue: API 전송 시 사용하는 값 "MALE"/"FEMALE"
 */

enum class Gender(
    val label: String,    // UI 표시용 (예: "남성")
    val iconRes: Int,     // 아이콘 리소스 (예: R.drawable.ic_male)
    val apiValue: String  // 서버 송수신용 (예: "MALE")
) {
    MALE("남성", R.drawable.ic_male, "MALE"),
    FEMALE("여성", R.drawable.ic_female, "FEMALE");
}

/*
* 관심사 태그 카테고리 Enum
* step5 태크 탭에서 카테고리 필터로 사용
*/
enum class InterestCategory(val label: String) {
    HOBBY("취미/관심사"),
    MBTI("성격/MBTI"),
    FOOD("음식"),
    ENTERTAINMENT("엔터테인먼트"),
}

// Data 클래스 정의
data class CountryItem(
    val flag    : String, // 국기 이모지
    val nameKo  : String, // 한국어 국가명
    val code    : String,   // API 전송값 e.g. "KOREA"
)

data class LanguageItem(
    val flag    : String, // 국기 이모지
    val nameKo  : String, // 한국어 언어명
    val code    : String,   // ISO 언어 코드 e.g. "KO"
    val apiValue: String,   // API 전송값 e.g. "KOREAN"
    val level   : Int = 0,  // 언어 숙련도 1~5단계, 기본값 0
)

data class InterestTag(
    val emoji    : String, // 태그 대표 이모지
    val label    : String, // UI 표시 텍스트
    val apiValue : String, // API 전송 값
    val category : InterestCategory, // 소속 카테고리 (InterestCategory)
)

// UI State
// 온보딩 전 단계의 입력 상태를 단일 객체로 관리
// ViewModel이 StateFlow로 노출될거임. Screen이 UI 갱신.

data class OnboardingUiState(
    val currentStep        : Int                 = 1,

    // Step 1: 기본 정보
    // 사용자 이름(최대 8자)
    val name               : String              = "",
    // 생년월일(DataPicker)
    val birthYear          : Int?                = null,
    val birthMonth         : Int?                = null,
    val birthDay           : Int?                = null,
    // 성별 선택
    val gender             : Gender?             = null,
    // DataPicker 다이얼로그 표시 여부
    val showDatePicker     : Boolean             = false,

    // Step 2: 거주 국가
    // 국가 검색 입력값 (실시간 필터링)
    val countryQuery       : String              = "",
    // 최종 선택 국가
    val selectedCountry    : CountryItem?        = null,

    // Step 3: 사용 언어 (내 언어)
    // 언어 검색 입력값 (실시간 필터링)
    val myLangQuery        : String              = "",
    // 선택된 사용 언어 (단일 선택)
    val selectedMyLang     : LanguageItem?       = null,
    // 레벨 설정 바텀시트 표시 여부
    val showLevelSheet     : Boolean             = false,

    // Step 4: 학습 언어
    // 학습 언어 검색 입력값 (실시간 필터링)
    val learnQuery         : String              = "",
    // 선택된 학습 언어 목록
    val selectedLearnLangs : List<LanguageItem>  = emptyList(),

    // Step 5: 관심사 태그, 태그 목록은 Interest tags dataset.md파일
    val selectedTab        : InterestCategory    = InterestCategory.HOBBY,
    // 선택된 관심사 태그 목록 (최대 TAG_MAX_COUNT = 15개)
    val selectedTags       : List<InterestTag>   = emptyList(),

    // Step 6: 한줄 소개
    // BIO_MAX_LENGTH = 150자
    val bio                : String              = "",

    // Step 7: 권한 (UI only)
    // 별도 상태 없음, 순수 안내 화면

    // 공통
    val isLoading          : Boolean             = false,
    val errorMessage       : String?             = null,
    // 회원가입 API 성공 시 true → OnboardingScreen의 LaunchedEffect가 onFinish() 호출
    val registerSuccess    : Boolean             = false,
    // 구글 가입은 응답에 토큰이 포함되어 바로 로그인 상태가 됨 → true면 MAIN, false면 LOGIN
    val autoLoggedIn       : Boolean             = false,
)

// 샘플 데이터(현재는 하드 코딩, 추후 API 연동 시 교체)

// Step 2 국가 목록 샘플
val sampleCountries = listOf(
    CountryItem("KR", "한국",    "KOREA"),
    CountryItem("US", "미국",    "USA"),
    CountryItem("GB", "영국",    "UK"),
    CountryItem("IT", "이탈리아", "ITALY"),
    CountryItem("CN", "중국",    "CHINA"),
    CountryItem("FR", "프랑스",  "FRANCE"),
    CountryItem("DE", "독일",    "GERMANY"),
    CountryItem("ES", "스페인",  "SPAIN"),
    CountryItem("RU", "러시아",  "RUSSIA"),
    CountryItem("JP", "일본",    "JAPAN"),
    CountryItem("BR", "브라질",  "BRAZIL"),
    CountryItem("AU", "호주",    "AUSTRALIA"),
)

// Step 3,4 언어 목록 샘플
val sampleLanguages = listOf(
    LanguageItem("🇰🇷", "한국어",    "KO", "KOREAN"),
    LanguageItem("🇺🇸", "영어",     "EN", "ENGLISH"),
    LanguageItem("🇯🇵", "일본어",   "JA", "JAPANESE"),
    LanguageItem("🇨🇳", "중국어",   "ZH", "CHINESE"),
    LanguageItem("🇫🇷", "프랑스어", "FR", "FRENCH"),
    LanguageItem("🇩🇪", "독일어",   "DE", "GERMAN"),
    LanguageItem("🇪🇸", "스페인어", "ES", "SPANISH"),
    LanguageItem("🇷🇺", "러시아어", "RU", "RUSSIAN"),
)

// Step 5 관심사 태그 전체 목록
/* 카테고리별로 분류되어 있으며, StepInterests에서 selectedTab에 따라 필터링됨
 * apiValue는 RegisterRequest.tags에 담겨 서버로 전송됨
 */
val sampleTags: List<InterestTag> = listOf(
    // 취미/관심사
    InterestTag("☕", "카페탐방",    "CAFE_TOUR",    InterestCategory.HOBBY),
    InterestTag("✈️", "여행",        "TRAVEL",        InterestCategory.HOBBY),
    InterestTag("📷", "사진찍기",    "PHOTOGRAPHY",   InterestCategory.HOBBY),
    InterestTag("🏋️", "운동하기",   "WORKOUT",       InterestCategory.HOBBY),
    InterestTag("🏃", "러닝",        "RUNNING",       InterestCategory.HOBBY),
    InterestTag("💪", "헬스",        "GYM",           InterestCategory.HOBBY),
    InterestTag("🧘", "요가/필라테스","YOGA",          InterestCategory.HOBBY),
    InterestTag("⛰️", "등산",        "HIKING",        InterestCategory.HOBBY),
    InterestTag("⛺", "캠핑",        "CAMPING",       InterestCategory.HOBBY),
    InterestTag("📚", "독서",        "READING",       InterestCategory.HOBBY),
    InterestTag("✍️", "글쓰기",      "WRITING",       InterestCategory.HOBBY),
    InterestTag("🎨", "그림그리기",  "ART",           InterestCategory.HOBBY),
    InterestTag("🎸", "악기연주",    "INSTRUMENT",    InterestCategory.HOBBY),
    InterestTag("🎤", "노래부르기",  "SINGING",       InterestCategory.HOBBY),
    InterestTag("🎮", "게임",        "GAMING",        InterestCategory.HOBBY),
    InterestTag("♟️", "보드게임",    "BOARD_GAME",    InterestCategory.HOBBY),
    InterestTag("🛠️", "DIY/공예",   "DIY",           InterestCategory.HOBBY),
    InterestTag("🍳", "요리",        "COOKING",       InterestCategory.HOBBY),
    InterestTag("🥐", "베이킹",      "BAKING",        InterestCategory.HOBBY),
    InterestTag("🌿", "식물키우기",  "PLANT",         InterestCategory.HOBBY),
    InterestTag("🐾", "반려동물",    "PET",           InterestCategory.HOBBY),
    InterestTag("📈", "재테크",      "INVESTMENT",    InterestCategory.HOBBY),
    InterestTag("🧠", "자기계발",    "SELF_DEV",      InterestCategory.HOBBY),
    InterestTag("📖", "공부모임",    "STUDY_GROUP",   InterestCategory.HOBBY),

    // 성격/MBTI
    // MBTI 칩형 (16개)
    InterestTag("📋", "ISTJ",  "ISTJ", InterestCategory.MBTI),
    InterestTag("🤝", "ISFJ",  "ISFJ", InterestCategory.MBTI),
    InterestTag("🌌", "INFJ",  "INFJ", InterestCategory.MBTI),
    InterestTag("🔭", "INTJ",  "INTJ", InterestCategory.MBTI),
    InterestTag("🔧", "ISTP",  "ISTP", InterestCategory.MBTI),
    InterestTag("🎨", "ISFP",  "ISFP", InterestCategory.MBTI),
    InterestTag("🌙", "INFP",  "INFP", InterestCategory.MBTI),
    InterestTag("🤔", "INTP",  "INTP", InterestCategory.MBTI),
    InterestTag("⚡", "ESTP",  "ESTP", InterestCategory.MBTI),
    InterestTag("🎉", "ESFP",  "ESFP", InterestCategory.MBTI),
    InterestTag("😊", "ENFP",  "ENFP", InterestCategory.MBTI),
    InterestTag("💡", "ENTP",  "ENTP", InterestCategory.MBTI),
    InterestTag("📊", "ESTJ",  "ESTJ", InterestCategory.MBTI),
    InterestTag("🌸", "ESFJ",  "ESFJ", InterestCategory.MBTI),
    InterestTag("🌟", "ENFJ",  "ENFJ", InterestCategory.MBTI),
    InterestTag("👑", "ENTJ",  "ENTJ", InterestCategory.MBTI),
    // 성향 보조 태그
    InterestTag("🗣️", "외향적인",   "EXTROVERT",   InterestCategory.MBTI),
    InterestTag("🔇", "내향적인",   "INTROVERT",   InterestCategory.MBTI),
    InterestTag("💞", "감성적인",   "EMOTIONAL",   InterestCategory.MBTI),
    InterestTag("🧮", "이성적인",   "RATIONAL",    InterestCategory.MBTI),
    InterestTag("📅", "계획적인",   "PLANNER",     InterestCategory.MBTI),
    InterestTag("🎲", "즉흥적인",   "SPONTANEOUS", InterestCategory.MBTI),
    InterestTag("🏠", "혼자도 좋아","SOLO",        InterestCategory.MBTI),
    InterestTag("👥", "함께가 좋아","TOGETHER",    InterestCategory.MBTI),
    InterestTag("🤫", "조용한 편",  "QUIET",       InterestCategory.MBTI),
    InterestTag("🔥", "활발한 편",  "ACTIVE",      InterestCategory.MBTI),

    // 음식
    InterestTag("🍰", "카페/디저트", "DESSERT",    InterestCategory.FOOD),
    InterestTag("🥐", "빵순이·빵돌이","BREAD",     InterestCategory.FOOD),
    InterestTag("☕", "커피러버",    "COFFEE",     InterestCategory.FOOD),
    InterestTag("🍵", "차(티)",      "TEA",        InterestCategory.FOOD),
    InterestTag("🍚", "한식",        "KOREAN_FOOD",InterestCategory.FOOD),
    InterestTag("🥢", "중식",        "CHINESE_FOOD",InterestCategory.FOOD),
    InterestTag("🍣", "일식",        "JAPANESE_FOOD",InterestCategory.FOOD),
    InterestTag("🍝", "양식",        "WESTERN_FOOD",InterestCategory.FOOD),
    InterestTag("🥚", "분식",        "KOREAN_SNACK",InterestCategory.FOOD),
    InterestTag("🍗", "치킨",        "CHICKEN",    InterestCategory.FOOD),
    InterestTag("🍕", "피자",        "PIZZA",      InterestCategory.FOOD),
    InterestTag("🍔", "햄버거",      "BURGER",     InterestCategory.FOOD),
    InterestTag("🥩", "고기러버",    "MEAT",       InterestCategory.FOOD),
    InterestTag("🦞", "해산물",      "SEAFOOD",    InterestCategory.FOOD),
    InterestTag("🌶️", "매운음식",   "SPICY",      InterestCategory.FOOD),
    InterestTag("🥗", "비건/채식",   "VEGAN",      InterestCategory.FOOD),
    InterestTag("🥙", "다이어트식",  "DIET",       InterestCategory.FOOD),
    InterestTag("🍱", "혼밥",        "SOLO_MEAL",  InterestCategory.FOOD),
    InterestTag("📍", "맛집탐방",    "FOOD_TOUR",  InterestCategory.FOOD),
    InterestTag("🍺", "술한잔",      "DRINK",      InterestCategory.FOOD),
    InterestTag("🍷", "와인",        "WINE",       InterestCategory.FOOD),
    InterestTag("🍻", "맥주",        "BEER",       InterestCategory.FOOD),

    // 엔터테인먼트
    InterestTag("🎬", "영화보기",    "MOVIE",      InterestCategory.ENTERTAINMENT),
    InterestTag("📺", "드라마",      "DRAMA",      InterestCategory.ENTERTAINMENT),
    InterestTag("😂", "예능",        "VARIETY",    InterestCategory.ENTERTAINMENT),
    InterestTag("🎥", "다큐멘터리",  "DOCUMENTARY",InterestCategory.ENTERTAINMENT),
    InterestTag("📹", "유튜브",      "YOUTUBE",    InterestCategory.ENTERTAINMENT),
    InterestTag("🎞️", "넷플릭스",   "NETFLIX",    InterestCategory.ENTERTAINMENT),
    InterestTag("📱", "OTT 정주행",  "OTT",        InterestCategory.ENTERTAINMENT),
    InterestTag("🎵", "음악감상",    "MUSIC",      InterestCategory.ENTERTAINMENT),
    InterestTag("🎪", "콘서트",      "CONCERT",    InterestCategory.ENTERTAINMENT),
    InterestTag("🎭", "뮤지컬",      "MUSICAL",    InterestCategory.ENTERTAINMENT),
    InterestTag("🎭", "연극",        "PLAY",       InterestCategory.ENTERTAINMENT),
    InterestTag("🖼️", "전시회",     "EXHIBITION", InterestCategory.ENTERTAINMENT),
    InterestTag("📰", "웹툰",        "WEBTOON",    InterestCategory.ENTERTAINMENT),
    InterestTag("📗", "웹소설",      "WEB_NOVEL",  InterestCategory.ENTERTAINMENT),
    InterestTag("📚", "책읽기",      "BOOK",       InterestCategory.ENTERTAINMENT),
    InterestTag("📡", "게임방송",    "GAME_STREAM",InterestCategory.ENTERTAINMENT),
    InterestTag("🏆", "e스포츠",     "ESPORTS",    InterestCategory.ENTERTAINMENT),
    InterestTag("💃", "아이돌",      "IDOL",       InterestCategory.ENTERTAINMENT),
    InterestTag("🎤", "힙합/R&B",    "HIPHOP",     InterestCategory.ENTERTAINMENT),
    InterestTag("🎶", "발라드",      "BALLAD",     InterestCategory.ENTERTAINMENT),
)

// 앱 소개 아이템 (Step 8)
data class AppFeatureItem(
    val iconRes : Int,    // R.drawable.ic_nav_*.xml
    val title   : String,
    val desc    : String,
)

val appFeatures = listOf(
    AppFeatureItem(R.drawable.ic_nav_chat,   "채팅",     "+ 버튼을 눌러 새 채팅을 시작해봐요!"),
    AppFeatureItem(R.drawable.ic_nav_friend, "친구",     "친구들과 즐겁게 이야기 나눠요!"),
    AppFeatureItem(R.drawable.ic_nav_find,   "탐색",     "나와 맞는 맞춤형 추천 친구를 찾아봐요!"),
    AppFeatureItem(R.drawable.ic_nav_script,   "스크립트", "대화한 표현을 저장하고 공부할 수 있어요!"),
    AppFeatureItem(R.drawable.ic_nav_my,     "마이",     "설정을 여기에서 수정할 수 있어요!"),
)

// 권한 아이템 (Step 7)
data class PermissionItem(val emoji: String, val title: String, val desc: String)

val permissionItems = listOf(
    PermissionItem("📍", "위치 정보",   "원하는 국적의 친구와 매칭하기"),
    PermissionItem("🎙️", "마이크",     "친구와 통화와 음성채팅하기"),
    PermissionItem("🔔", "알림 (선택)", "친구추가, 친구추천, 메시지 등 알림"),
)
