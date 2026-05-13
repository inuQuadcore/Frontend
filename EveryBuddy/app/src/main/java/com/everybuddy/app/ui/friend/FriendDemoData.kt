package com.everybuddy.app.ui.friend

object FriendDemoData {

    const val MY_USER_ID = "me"
    const val MY_NAME    = "나"
    val friends = listOf(
        FriendProfile(
            id               = "user_1",
            name             = "김민준",
            profileImageUrl  = "demo_kiminjun",
            nationality      = "US",
            nativeLanguages  = listOf("EN"),
            learningLanguages= listOf("EN", "JP"),
            interests        = listOf("PHOTOGRAPHY", "RUNNING", "TRAVEL"),
            bio              = "영어 배우고 싶어요",
            isOnline         = true,
            isFriend         = true,
        ),
        FriendProfile(
            id               = "user_2",
            name             = "홍현준",
            profileImageUrl  = "demo_honghyunjun",
            nationality      = "US",
            nativeLanguages  = listOf("EN"),
            learningLanguages= listOf("EN", "JP"),
            interests        = listOf("WORKOUT", "COOKING", "FOOD_TOUR"),
            bio              = "영어 배우고 싶어요",
            isOnline         = false,
            isFriend         = true,
        ),
        FriendProfile(
            id               = "user_3",
            name             = "임채민",
            profileImageUrl  = "demo_imchaemın",
            nationality      = "US",
            nativeLanguages  = listOf("EN"),
            learningLanguages= listOf("EN", "JP"),
            interests        = listOf("HIKING", "RUNNING", "TRAVEL"),
            bio              = "영어 배우고 싶어요",
            isOnline         = false,
            isFriend         = true,
        ),
        FriendProfile(
            id               = "user_4",
            name             = "조윤성",
            profileImageUrl  = "demo_joyoonsung",
            nationality      = "US",
            nativeLanguages  = listOf("EN"),
            learningLanguages= listOf("EN", "JP"),
            interests        = listOf("PHOTOGRAPHY", "RUNNING", "TRAVEL"),
            bio              = "영어 배우고 싶어요",
            isOnline         = false,
            isFriend         = true,
        ),
        FriendProfile(
            id               = "user_5",
            name             = "우원재",
            profileImageUrl  = "demo_woowonzae",
            nationality      = "FR",
            nativeLanguages  = listOf("FR"),
            learningLanguages= listOf("FR", "EN"),
            interests        = listOf("INSTRUMENT", "HIPHOP", "QUIET"),
            bio              = "난 파리의 시간을 사는 중",
            isOnline         = true,
            isFriend         = true,
        ),
        FriendProfile(
            id               = "user_6",
            name             = "이예지",
            profileImageUrl  = "demo_leeyeji",
            nationality      = "KR",
            nativeLanguages  = listOf("KR"),
            learningLanguages= listOf("KR", "CN"),
            interests        = listOf("PET", "COOKING", "FOOD_TOUR"),
            bio              = "해리 키우기",
            isOnline         = false,
            isFriend         = true,
        ),
        FriendProfile(
            id               = "user_7",
            name             = "홍길동",
            profileImageUrl  = "demo_hongkildong",
            nationality      = "US",
            nativeLanguages  = listOf("EN"),
            learningLanguages= listOf("EN"),
            interests        = listOf("WORKOUT", "COOKING"),
            bio              = "",
            isOnline         = false,
            isFriend         = true,
        ),
        FriendProfile(
            id               = "user_8",
            name             = "はるか",
            profileImageUrl  = "demo_haruka",
            nationality      = "JP",
            nativeLanguages  = listOf("JP"),
            learningLanguages= listOf("JP", "EN"),
            interests        = listOf("COOKING", "MUSIC"),
            bio              = "",
            isOnline         = false,
            isFriend         = true,
        ),
        FriendProfile(
            id               = "user_9",
            name             = "김감전",
            profileImageUrl  = "demo_kimgamjeon",
            nationality      = "KR",
            nativeLanguages  = listOf("KR"),
            learningLanguages= listOf("EN"),
            interests        = listOf("HIPHOP", "MUSIC"),
            bio              = "",
            isOnline         = false,
            isFriend         = true,
        ),
    )

    val statusMessages = mutableListOf(
        StatusMessage(
            id           = "sm_my",
            authorId     = MY_USER_ID,
            authorName   = MY_NAME,
            profileImageUrl = null,
            content      = "",               // 비어있으면 "작성하기..." 상태
            createdAt    = System.currentTimeMillis(),
            isMyMessage  = true,
        ),
        StatusMessage(
            id           = "sm_1",
            authorId     = "user_7",
            authorName   = "홍길동",
            profileImageUrl = "demo_hongkildong", // TODO
            content      = "배고파 양식을 먹을까 일식을 먹을까 중식을 먹을까 한식을 먹을까 한식, 양식, 중식, 일식 매운 거 안 매운 거~ 빵이나 밥이나 면이나 떡 뜨거운 거 차...",
            createdAt    = System.currentTimeMillis() - 2 * 3_600_000L,
            isMyMessage  = false,
        ),
        StatusMessage(
            id           = "sm_2",
            authorId     = "user_8",
            authorName   = "はるか",
            profileImageUrl = "demo_haruka",  // TODO
            content      = "まるでこの世界で二人だけみたいだね なんて少しだけ夢をみてしまったよ つま先に月明かり 花束の香り 指に触れる指 さよな…",
            createdAt    = System.currentTimeMillis() - 3 * 3_600_000L,
            isMyMessage  = false,
        ),
        StatusMessage(
            id           = "sm_3",
            authorId     = "user_5",  // 우원재
            authorName   = "우원재",
            profileImageUrl = "demo_woowonzae", // TODO
            content      = "밤새 모니터에 튀긴 침이 마르기도 전에 강의실로 아 참, 교수님이 분신 때엔 날 일고 오랜 나 시작도 전에 눈을 감았지 날 한심하게 볼 게 뻔하니 이게...",
            createdAt    = System.currentTimeMillis() - 21 * 3_600_000L,
            isMyMessage  = false,
        ),
        StatusMessage(
            id           = "sm_4",
            authorId     = "user_9",  // 김감전
            authorName   = "김감전",
            profileImageUrl = "demo_kimgamjeon", // TODO
            content      = "나는 다채로운 랩핑과 라이밍 혹은 랩스킬로 혼을 쏙 빼놓는 대한민국 최고의 r.a.p 뱉는자 손심바 하지만 데릴리 5명의 감상자 oh man 하지만 난 déja v...",
            createdAt    = System.currentTimeMillis() - 21 * 3_600_000L,
            isMyMessage  = false,
        ),
        StatusMessage(
            id           = "sm_5",
            authorId     = "user_extra_1",
            authorName   = "D8",
            profileImageUrl = "demo_d8", // TODO
            content      = "海城 静卧在星空 不规律波动 不经意随风 似梦 是梦？ 沉睡得太熟 南漂流太久 好像没有尽头 当冬天来临的那一天 被围巾遮掩...",
            createdAt    = System.currentTimeMillis() - 22 * 3_600_000L,
            isMyMessage  = false,
        ),
        StatusMessage(
            id           = "sm_6",
            authorId     = "user_extra_2",
            authorName   = "황정민",
            profileImageUrl = "demo_hwang", // TODO
            content      = "아이 씹브라더~",
            createdAt    = System.currentTimeMillis() - 22 * 3_600_000L,
            isMyMessage  = false,
        ),
        StatusMessage(
            id           = "sm_7",
            authorId     = "user_6",  // 이예지
            authorName   = "이예지",
            profileImageUrl = "demo_leeyeji", // TODO
            content      = "주현아너요즘교회왜안나오니아다시나온다고?갑자기모범생...",
            createdAt    = System.currentTimeMillis() - 22 * 3_600_000L,
            isMyMessage  = false,
        ),
    )

}