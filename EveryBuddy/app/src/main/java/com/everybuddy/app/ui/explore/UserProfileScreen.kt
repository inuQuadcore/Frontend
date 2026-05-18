package com.everybuddy.app.ui.explore

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.everybuddy.app.R
import com.everybuddy.app.ui.theme.PretendardFamily

private val C = AppColors

@Composable
fun UserProfileScreen(
    user                 : DiscoverUser,
    detail               : UserDetail? = null,
    isFriend             : Boolean = false,
    myTags               : List<UserTag> = ExploreDemo.myProfile.tags,
    translatedBio        : String? = null,
    isBioTranslating     : Boolean = false,
    onTranslateBio       : () -> Unit = {},
    onClearBioTranslation: () -> Unit = {},
    onBack               : () -> Unit,
    onChat               : (DiscoverUser) -> Unit = {},
    onAddFriend          : (DiscoverUser) -> Unit = {},
    onRemoveFriend       : (DiscoverUser) -> Unit = {},
    onBlock              : (DiscoverUser) -> Unit = {},
) {
    BackHandler(onBack = onBack)

    var isSideBarOpen      by remember { mutableStateOf(false) }
    var pendingAction       by remember { mutableStateOf<String?>(null) }
    var isFriendLocal       by remember(isFriend) { mutableStateOf(isFriend) }
    var showRemoveDialog    by remember { mutableStateOf(false) }

    val myTagKeys     = myTags.map { it.tag }.toSet()
    val matchingCount = user.tags.count { it.tag in myTagKeys }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White).statusBarsPadding()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                ) {
                    IconButton(
                        onClick  = onBack,
                        modifier = Modifier.size(40.dp).align(Alignment.CenterStart),
                    ) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = C.TextPri)
                    }
                    Text(
                        text     = user.name,
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 17.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                    )
                    IconButton(
                        onClick  = { isSideBarOpen = true },
                        modifier = Modifier.size(40.dp).align(Alignment.CenterEnd),
                    ) {
                        Icon(painterResource(R.drawable.ic_sidemenu), "더보기", Modifier.size(22.dp), tint = C.TextPri)
                    }
                }
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
            }
        },
        contentWindowInsets = WindowInsets(0),
        containerColor = Color.White,
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {

            // TODO padding: 헤더 horizontal 16dp, top 20dp
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 프로필 이미지 + 국기 뱃지
                Box(modifier = Modifier.size(72.dp)) {
                    Box(
                        modifier         = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFD0D0D0)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!user.profileImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model              = user.profileImageUrl,
                                contentDescription = user.name,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(painterResource(R.drawable.ic_nav_my), null, Modifier.size(40.dp), tint = Color(0xFF555555))
                        }
                    }
                    Text(
                        user.countryFlag(), fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    // 이름 + 현활 뱃지
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(user.name, style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri))
                        if (user.isOnline) {
                            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFF3E0)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(C.Online))
                                    Text("현재활동중", style = TextStyle(fontSize = 10.sp, color = Color(0xFFE65100), fontFamily = PretendardFamily))
                                }
                            }
                        }
                    }
                    if (detail != null) {
                        val genderLabel = detail.genderLabel()
                        val ageText     = "${detail.age}세"
                        val combined    = if (genderLabel.isNotEmpty()) "$ageText · $genderLabel" else ageText
                        Text(combined, style = TextStyle(fontSize = 13.sp, color = C.TextSec, fontFamily = PretendardFamily))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 자기소개
            Text(
                translatedBio ?: user.bio,
                style    = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextPri, lineHeight = 22.sp),
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(10.dp))

            // 국적 / 연속출석 / 번역버튼 정보 행
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(painterResource(R.drawable.ic_location), "국적", Modifier.size(14.dp), tint = C.TextSec)
                    Text(user.countryName(), style = TextStyle(fontSize = 12.sp, color = C.TextSec, fontFamily = PretendardFamily))
                }
                if (detail != null && detail.consecutiveDays > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(painterResource(R.drawable.ic_calendar), "출석", Modifier.size(14.dp), tint = C.TextSec)
                        Text("연속 ${detail.consecutiveDays}일 출석", style = TextStyle(fontSize = 12.sp, color = C.TextSec, fontFamily = PretendardFamily))
                    }
                }
                if (user.bio.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable(enabled = !isBioTranslating) {
                            if (translatedBio != null) onClearBioTranslation() else onTranslateBio()
                        },
                    ) {
                        Icon(painterResource(R.drawable.ic_translate), "번역", Modifier.size(14.dp), tint = C.Accent)
                        Text(
                            text  = if (isBioTranslating) "번역 중..." else if (translatedBio != null) "원문보기" else "번역하기",
                            style = TextStyle(fontSize = 12.sp, color = C.Accent, fontFamily = PretendardFamily),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isFriendLocal) {
                    // [나의친구] — ic_follow 아이콘, 누르면 삭제 다이얼로그
                    OutlinedButton(
                        onClick  = { showRemoveDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape    = RoundedCornerShape(10.dp),
                        border   = BorderStroke(1.5.dp, C.Accent),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(painterResource(R.drawable.ic_follow), "나의친구", Modifier.size(18.dp), tint = C.Accent)
                            Text("나의친구", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.Accent, fontWeight = FontWeight(600)))
                        }
                    }
                } else {
                    // [친구추가] — ic_friend_add 파란 filled 버튼
                    Button(
                        onClick  = { isFriendLocal = true; onAddFriend(user) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = C.Accent),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(painterResource(R.drawable.ic_friend_add), "친구추가", Modifier.size(18.dp), tint = Color.White)
                            Text("친구추가", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = Color.White, fontWeight = FontWeight(600)))
                        }
                    }
                }

                // [채팅하기] — 항상 outline
                OutlinedButton(
                    onClick  = { onChat(user) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.5.dp, C.Border),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(painterResource(R.drawable.ic_nav_chat), "채팅", Modifier.size(18.dp), tint = C.TextPri)
                        Text("채팅하기", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextPri, fontWeight = FontWeight(600)))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = C.Border, thickness = 8.dp)

            Spacer(Modifier.height(20.dp))
            Text("소통 언어", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri), modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))
            user.languages.forEach { lang ->
                LanguageLevelRow(lang = lang, onTap = {}, showChevron = false)
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = C.Border, thickness = 8.dp)

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("친구의 태그", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri))
                if (matchingCount > 0) {
                    Text(
                        "나와 ${matchingCount}개 일치해요!",
                        style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = C.Accent, fontWeight = FontWeight(600)),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // 태그 wrap layout (FlowRow 대신 3열 그리드)
            TagWrapLayout(tags = user.tags, myTagKeys = myTagKeys)

            Spacer(Modifier.height(32.dp))
        }
    }

        // 딤 배경
        AnimatedVisibility(
            visible = isSideBarOpen,
            enter   = fadeIn(),
            exit    = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { isSideBarOpen = false },
            )
        }

        // 우측 사이드바
        AnimatedVisibility(
            visible  = isSideBarOpen,
            enter    = slideInHorizontally(initialOffsetX = { it }),
            exit     = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            ProfileSideBar(
                onDismiss = { isSideBarOpen = false },
                onAction  = { action ->
                    pendingAction  = action
                    isSideBarOpen  = false
                },
            )
        }

        // 확인 다이얼로그 (차단/삭제/신고)
        pendingAction?.let { action ->
            ProfileActionDialog(
                action    = action,
                userName  = user.name,
                onConfirm = {
                    when (action) {
                        "차단" -> { onBlock(user); onBack() }
                        "삭제" -> { isFriendLocal = false; onRemoveFriend(user) }
                        "신고" -> { /* TODO: API - POST /api/v1/users/{userId}/report */ }
                    }
                    pendingAction = null
                },
                onDismiss = { pendingAction = null },
            )
        }

        // 친구 삭제 확인 다이얼로그
        if (showRemoveDialog) {
            AlertDialog(
                onDismissRequest = { showRemoveDialog = false },
                containerColor   = Color.White,
                shape            = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        "나의 친구에서 삭제하시겠습니까?",
                        style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = C.TextPri),
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveDialog = false }) {
                        Text("취소", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextSec))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        isFriendLocal = false
                        showRemoveDialog = false
                        onRemoveFriend(user)
                    }) {
                        Text("삭제", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color(0xFFFF3333)))
                    }
                },
                text = null,
            )
        }
    }
}

@Composable
private fun ProfileSideBar(
    onDismiss : () -> Unit,
    onAction  : (String) -> Unit,
) {
    val menuItems = listOf("차단", "삭제", "신고")
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(220.dp)
            .background(Color.White)
            .systemBarsPadding()
            .padding(top = 56.dp),
    ) {
        menuItems.forEach { action ->
            Text(
                text     = action,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(action) }
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                style = TextStyle(
                    fontSize   = 16.sp,
                    fontFamily = PretendardFamily,
                    color      = if (action == "삭제") Color(0xFFFF3333) else Color(0xFF1B1B1B),
                ),
            )
            HorizontalDivider(color = C.Border, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun ProfileActionDialog(
    action    : String,
    userName  : String,
    onConfirm : () -> Unit,
    onDismiss : () -> Unit,
) {
    val description = when (action) {
        "차단" -> "${userName}님을 차단하면 서로의 메시지를 받을 수 없습니다."
        "삭제" -> "${userName}님을 친구 목록에서 삭제합니다."
        "신고" -> "${userName}님을 신고합니다. 검토 후 조치됩니다."
        else   -> ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "${userName}님을 ${action}하시겠습니까?",
                style = TextStyle(
                    fontSize   = 16.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = Color(0xFF1B1B1B),
                ),
            )
        },
        text = {
            if (description.isNotEmpty()) {
                Text(
                    description,
                    style = TextStyle(
                        fontSize   = 14.sp,
                        fontFamily = PretendardFamily,
                        color      = Color(0xFF8F9399),
                        lineHeight  = 20.sp,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "확인",
                    style = TextStyle(
                        fontSize   = 15.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(600),
                        color      = if (action == "삭제") Color(0xFFFF3333) else C.Accent,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "취소",
                    style = TextStyle(
                        fontSize   = 15.sp,
                        fontFamily = PretendardFamily,
                        color      = Color(0xFF8F9399),
                    ),
                )
            }
        },
        containerColor = Color.White,
        shape          = RoundedCornerShape(16.dp),
    )
}

// 언어 레벨 행
@Composable
fun LanguageLevelRow(lang: UserLanguage, onTap: () -> Unit, showChevron: Boolean = true) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable(onClick = onTap),
        shape    = RoundedCornerShape(12.dp),
        color    = Color.White,
        border   = BorderStroke(0.8.dp, C.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(lang.flagEmoji(), fontSize = 22.sp)
                Text(
                    when (lang.language.uppercase()) {
                        "ENGLISH" -> "영어"; "JAPANESE" -> "일본어"; "KOREAN" -> "한국어"
                        "CHINESE" -> "중국어"; "FRENCH" -> "프랑스어"; else -> lang.language
                    },
                    style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = C.TextPri),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(5) { idx ->
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape)
                                .background(if (idx < lang.level) C.Accent else C.Border),
                        )
                    }
                }
            }
            if (showChevron) {
                Icon(painterResource(R.drawable.ic_chevron_right), "상세", Modifier.size(16.dp), tint = C.TextSec)
            }
        }
    }
}

// 태그 Wrap 레이아웃
@Composable
fun TagWrapLayout(tags: List<UserTag>, myTagKeys: Set<String> = emptySet()) {
    // 간단한 가로 LazyRow → 실제 wrap은 TODO: accompanist FlowRow
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        tags.chunked(4).forEach { rowTags ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                rowTags.forEach { tag ->
                    val isMatch = tag.tag in myTagKeys
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isMatch) C.TagBg else C.TagBgGray)
                            .border(if (isMatch) 1.dp else 0.dp, if (isMatch) C.Accent else Color.Transparent, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text("${tag.emoji} ${tag.displayName}", style = TextStyle(fontSize = 12.sp, color = if (isMatch) C.Accent else C.TagTextGray, fontFamily = PretendardFamily))
                    }
                }
            }
        }
    }
}

// BorderStroke 편의
private fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)
