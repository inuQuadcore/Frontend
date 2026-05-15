package com.everybuddy.app.ui.friend

import com.everybuddy.app.data.dto.FriendStatusMessageDto
import java.util.UUID

object FriendDemoData {

    const val MY_USER_ID = 0L
    const val MY_NAME    = "나"

    val friends: List<FriendProfile> = listOf(
        FriendProfile(id = 5L,   name = "우원재",  nationality = "KR", nativeLanguages = listOf("KR"), learningLanguages = listOf("EN"), interests = listOf("음악", "랩"), bio = "랩 하는 사람", isOnline = true),
        FriendProfile(id = 6L,   name = "이예지",  nationality = "KR", nativeLanguages = listOf("KR"), learningLanguages = listOf("JP"), interests = listOf("교회", "독서"), bio = "모범생ㅋ"),
        FriendProfile(id = 7L,   name = "홍길동",  nationality = "KR", nativeLanguages = listOf("KR"), learningLanguages = listOf("EN", "JP"), interests = listOf("먹방", "여행"), bio = "배고파", isOnline = true),
        FriendProfile(id = 8L,   name = "はるか", nationality = "JP", nativeLanguages = listOf("JP"), learningLanguages = listOf("KR"), interests = listOf("음악", "시"), bio = "月明かりが好き"),
        FriendProfile(id = 9L,   name = "김감전",  nationality = "KR", nativeLanguages = listOf("KR"), learningLanguages = listOf("EN"), interests = listOf("랩", "힙합"), bio = "대한민국 최고의 래퍼"),
        FriendProfile(id = 101L, name = "D8",     nationality = "CN", nativeLanguages = listOf("CN"), learningLanguages = listOf("KR"), interests = listOf("시", "음악"), bio = "海城 静卧在星空"),
        FriendProfile(id = 102L, name = "황정민",  nationality = "KR", nativeLanguages = listOf("KR"), learningLanguages = listOf("EN"), interests = listOf("영화"), bio = "아이 씹브라더~"),
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

    val demoFriendStatusDtos: List<FriendStatusMessageDto> = listOf(
        FriendStatusMessageDto(
            statusMessageId = 4L,
            userId          = 9L,
            userName        = "김감전",
            profileImageUrl = null,
            content         = "나는 다채로운 랩핑과 라이밍 혹은 랩스킬로 혼을 쏙 빼놓는 대한민국 최고의 r.a.p 뱉는자 손심바 하지만 데일리 5명의 감상자 oh man 하지만 난 déjà vu 소속 래퍼들이 전부갓마백 군대 갔다 온 비와이한테 대접해 손심바로 끓인 보신탕 정신차려 심바 넌 31살",
            timeAgo         = "21시간 전",
        ),
        FriendStatusMessageDto(
            statusMessageId = 1L,
            userId          = 7L,
            userName        = "홍길동",
            profileImageUrl = null,
            content         = "배고파 양식을 먹을까 일식을 먹을까 중식을 먹을까 한식을 먹을까 한식, 양식, 중식, 일식 매운 거 안 매운 거~ 빵이냐 밥이냐 면이냐 떡 뜨거운 거 차가운 거 아무거나 다 좋아",
            timeAgo         = "2시간 전",
        ),
        FriendStatusMessageDto(
            statusMessageId = 2L,
            userId          = 8L,
            userName        = "はるか",
            profileImageUrl = null,
            content         = "まるでこの世界で二人だけみたいだね なんて少しだけ夢をみてしまったよ つま先に月明かり 花束の香り 指に触れる指 さよなら",
            timeAgo         = "3시간 전",
        ),
        FriendStatusMessageDto(
            statusMessageId = 3L,
            userId          = 5L,
            userName        = "우원재",
            profileImageUrl = null,
            content         = "밤새 모니터에 튀긴 침이 마르기도 전에 강의실로 아 참, 교수님이 문신 때문에 날 팔고 오래 나 시작도 전에 눈을 감았지 날 한심하게 볼 게 뻔하니 이게",
            timeAgo         = "21시간 전",
        ),
        FriendStatusMessageDto(
            statusMessageId = 5L,
            userId          = 99L,
            userName        = "D8",
            profileImageUrl = null,
            content         = "海城 静卧在星空 不规律波动 不经意随风 似梦 是梦？ 沉睡得太熟 而漂流太久 好像没有尽头 当冬天来临的那一天 被围巾遮掩",
            timeAgo         = "22시간 전",
        ),
        FriendStatusMessageDto(
            statusMessageId = 6L,
            userId          = 98L,
            userName        = "황정민",
            profileImageUrl = null,
            content         = "아이 씹브라더~",
            timeAgo         = "22시간 전",
        ),
        FriendStatusMessageDto(
            statusMessageId = 7L,
            userId          = 6L,
            userName        = "이예지",
            profileImageUrl = null,
            content         = "주현아 너 요즘 교회 왜 안 나오니 아 다시 나온다고? 갑자기 모범생ㅋㅋ",
            timeAgo         = "22시간 전",
        ),
    )

    data class DemoChatRoom(
        val id         : String,
        val friendId   : String,
        val friendName : String,
        val messages   : MutableList<DemoChatMsg> = mutableListOf(),
    )

    data class DemoChatMsg(
        val id      : String = UUID.randomUUID().toString(),
        val text    : String,
        val isMine  : Boolean,
        val isStatusReply: Boolean = false,  // 상태메시지 답장 여부
        val originalStatusPreview: String = "",  // 채팅방 내 상단 인용 텍스트
    )

    val chatRooms: MutableList<DemoChatRoom> = mutableListOf()
}

private fun UUID.randomUUID() = java.util.UUID.randomUUID()