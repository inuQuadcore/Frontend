package com.everybuddy.app.ui.friend

object FriendDemoData {

    const val MY_USER_ID = 0L
    const val MY_NAME    = "나"

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
            authorId     = 7L,
            authorName   = "홍길동",
            profileImageUrl = "demo_hongkildong",
            content      = "배고파 양식을 먹을까 일식을 먹을까 중식을 먹을까 한식을 먹을까 한식, 양식, 중식, 일식 매운 거 안 매운 거~ 빵이나 밥이나 면이나 떡 뜨거운 거 차...",
            createdAt    = System.currentTimeMillis() - 2 * 3_600_000L,
            isMyMessage  = false,
        ),
        StatusMessage(
            id           = "sm_2",
            authorId     = 8L,
            authorName   = "はるか",
            profileImageUrl = "demo_haruka",
            content      = "まるでこの世界で二人だけみたいだね なんて少しだけ夢をみてしまったよ つま先に月明かり 花束の香り 指に触れる指 さよな…",
            createdAt    = System.currentTimeMillis() - 3 * 3_600_000L,
            isMyMessage  = false,
        ),
        StatusMessage(
            id           = "sm_3",
            authorId     = 5L,  // 우원재
            authorName   = "우원재",
            profileImageUrl = "demo_woowonzae",
            content      = "밤새 모니터에 튀긴 침이 마르기도 전에 강의실로 아 참, 교수님이 분신 때엔 날 일고 오랜 나 시작도 전에 눈을 감았지 날 한심하게 볼 게 뻔하니 이게...",
            createdAt    = System.currentTimeMillis() - 21 * 3_600_000L,
            isMyMessage  = false,
        ),
        StatusMessage(
            id           = "sm_4",
            authorId     = 9L,  // 김감전
            authorName   = "김감전",
            profileImageUrl = "demo_kimgamjeon",
            content      = "나는 다채로운 랩핑과 라이밍 혹은 랩스킬로 혼을 쏙 빼놓는 대한민국 최고의 r.a.p 뱉는자 손심바 하지만 데릴리 5명의 감상자 oh man 하지만 난 déja v...",
            createdAt    = System.currentTimeMillis() - 21 * 3_600_000L,
            isMyMessage  = false,
        ),
        StatusMessage(
            id           = "sm_5",
            authorId     = 101L,
            authorName   = "D8",
            profileImageUrl = "demo_d8",
            content      = "海城 静卧在星空 不规律波动 不经意随风 似梦 是梦？ 沉睡得太熟 南漂流太久 好像没有尽头 当冬天来临的那一天 被围巾遮掩...",
            createdAt    = System.currentTimeMillis() - 22 * 3_600_000L,
            isMyMessage  = false,
        ),
        StatusMessage(
            id           = "sm_6",
            authorId     = 102L,
            authorName   = "황정민",
            profileImageUrl = "demo_hwang",
            content      = "아이 씹브라더~",
            createdAt    = System.currentTimeMillis() - 22 * 3_600_000L,
            isMyMessage  = false,
        ),
        StatusMessage(
            id           = "sm_7",
            authorId     = 6L,  // 이예지
            authorName   = "이예지",
            profileImageUrl = "demo_leeyeji",
            content      = "주현아너요즘교회왜안나오니아다시나온다고?갑자기모범생...",
            createdAt    = System.currentTimeMillis() - 22 * 3_600_000L,
            isMyMessage  = false,
        ),
    )

}
