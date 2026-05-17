package com.everybuddy.app.ui.friend

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.everybuddy.app.R
import com.everybuddy.app.ui.explore.ExploreDemo
import com.everybuddy.app.ui.explore.countryFlag
import com.everybuddy.app.ui.theme.PretendardFamily

private val C = FriendColors  // 색상 단축키

@Composable
fun FriendTopBar(
    title           : String,
    showAddFriend   : Boolean = false,
    hasNotification : Boolean = false,
    onSearch        : () -> Unit,
    onNotification  : () -> Unit,
    onAddFriend     : () -> Unit = {},
) {
    Column(modifier = Modifier.background(Color.White)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
        ) {
            if (showAddFriend) {
                IconButton(
                    onClick  = onAddFriend,
                    modifier = Modifier.size(40.dp).align(Alignment.CenterStart),
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_friend_add),
                        contentDescription = "친구 추가",
                        modifier           = Modifier.size(24.dp),
                        tint               = C.TextPri,
                    )
                }
            }

            Text(
                text     = title,
                modifier = Modifier.align(Alignment.Center),
                style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
            )

            Row(
                modifier              = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onSearch, modifier = Modifier.size(40.dp)) {
                    Icon(painterResource(R.drawable.ic_search), "검색", Modifier.size(24.dp), tint = C.TextPri)
                }
                Box {
                    IconButton(onClick = onNotification, modifier = Modifier.size(40.dp)) {
                        Icon(painterResource(R.drawable.ic_alarm), "알림", Modifier.size(24.dp), tint = C.TextPri)
                    }
                    if (hasNotification) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(C.Accent)
                                .align(Alignment.TopEnd)
                                .offset(x = (-6).dp, y = 6.dp),
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = C.Border, thickness = 0.5.dp)
    }
}

@Composable
fun InterestTag(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(C.TagBg)
            // TODO padding: 태그 horizontal 8dp, vertical 3dp
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text  = label,
            style = TextStyle(fontSize = 11.sp, fontFamily = PretendardFamily, color = C.TagText),
        )
    }
}

@Composable
fun FriendListItem(
    friend         : FriendProfile,
    isFollowing    : Boolean            = false,
    onFollowToggle : () -> Unit         = {},
    onClick        : (FriendProfile) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(friend) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            // 메인 프로필 원
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD0D0D0)),
            ) {
                AsyncImage(
                    model              = friend.profileImageUrl,
                    contentDescription = friend.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            }
            // 국기 이모지 뱃지 (우하단) — 국적(nationality) 기준
            Text(
                text     = countryFlag(friend.nationality),
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp),
            )
        }

        // TODO padding: 이미지-텍스트 간격 14dp
        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
            // 관심사 태그 (최대 3개)
            if (friend.interests.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    friend.interests.take(3).forEach { rawTag ->
                        val ut = ExploreDemo.allTags.find { it.tag == rawTag }
                        InterestTag(label = if (ut != null) "${ut.emoji} ${ut.displayName}" else rawTag)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // 이름 + 현재활동중 뱃지
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text  = friend.name,
                    style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                )
                if (friend.isOnline) {
                    OnlineBadge()
                }
            }

            // 자기소개 (1줄)
            if (friend.bio.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = friend.bio,
                    style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = C.TextSec),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 언어 표시 (배우는 언어 — chip 스타일)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                friend.learningLanguages.take(4).forEach { lang ->
                    LanguageChip(code = lang)
                }
            }
        }

    }
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        color     = C.Border,
        thickness = 0.5.dp,
    )
}

@Composable
fun OnlineBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF3E0))
            // TODO padding: 뱃지 horizontal 6dp, vertical 2dp
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(C.Online),
            )
            Text(
                text  = "현재활동중",
                style = TextStyle(fontSize = 10.sp, fontFamily = PretendardFamily, color = Color(0xFFE65100)),
            )
        }
    }
}

@Composable
fun LanguageChip(code: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFF0F0F0))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text  = languageLabel(code),
            style = TextStyle(fontSize = 11.sp, fontFamily = PretendardFamily, color = Color(0xFF555555)),
        )
    }
}

@Composable
fun StatusMessagePreviewCard(
    sm      : StatusMessage,
    isMe    : Boolean = false,
    onClick : (StatusMessage) -> Unit,
) {
    Surface(
        modifier = Modifier
            // TODO size: 상태메시지 카드 width 약 140dp
            .width(140.dp)
            .clickable { onClick(sm) },
        shape  = RoundedCornerShape(12.dp),
        color  = Color.White,
        border = BorderStroke(0.8.dp, C.Border),
    ) {
        Column(
            modifier = Modifier
                // TODO padding: 카드 내부 12dp
                .padding(12.dp),
        ) {
            // 프로필 이미지 (원형)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD0D0D0)),
            ) {
                // TODO: Coil AsyncImage
            }
            Spacer(Modifier.height(8.dp))

            // 이름
            Text(
                text  = if (isMe) "나" else sm.authorName,
                style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = C.TextPri),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))

            // 내용 25자 / 내 것이면 "작성하기..." 또는 내용
            val preview = when {
                isMe && sm.content.isEmpty() -> "💬 작성하기..."
                isMe -> sm.preview80()
                else -> sm.preview15()
            }
            Text(
                text     = preview,
                style    = TextStyle(
                    fontSize   = 11.sp,
                    fontFamily = PretendardFamily,
                    color      = if (isMe && sm.content.isEmpty()) C.TextSec else C.TextPri,
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))

            // 경과 시간
            if (!isMe || sm.content.isNotEmpty()) {
                Text(
                    text  = sm.elapsedText(),
                    style = TextStyle(fontSize = 10.sp, color = C.TextSec),
                )
            }
        }
    }
}

@Composable
fun StatusMessageFullItem(
    sm      : StatusMessage,
    onClick : (StatusMessage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(sm) }
            // TODO padding: 아이템 horizontal 16dp, vertical 12dp
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 프로필 이미지
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD0D0D0)),
            ) {
                // TODO: Coil AsyncImage
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = sm.authorName,
                    style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = C.TextPri),
                )
                Spacer(Modifier.height(3.dp))
                // 전체 내용 표시 (세로 스크롤)
                Text(
                    text  = sm.content,
                    style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = C.TextPri, lineHeight = 20.sp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = sm.elapsedText(),
                    style = TextStyle(fontSize = 11.sp, color = C.TextSec),
                )
            }
        }
    }
    HorizontalDivider(color = C.Border, thickness = 0.5.dp)
}

@Composable
fun FriendBottomNavBar(
    onChat   : () -> Unit,
    onFind   : () -> Unit,
    onScript : () -> Unit,
    onMy     : () -> Unit,
) {
    Column {
        HorizontalDivider(color = C.Border, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.White)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            FriendNavItem("대화",    R.drawable.ic_nav_chat,   false, onChat)
            FriendNavItem("친구",    R.drawable.ic_nav_friend, true,  {})
            FriendNavItem("찾기",    R.drawable.ic_nav_find,   false, onFind)
            FriendNavItem("스크립트", R.drawable.ic_nav_script, false, onScript)
            FriendNavItem("마이",    R.drawable.ic_nav_my,     false, onMy)
        }
    }
}

@Composable
private fun FriendNavItem(label: String, iconRes: Int, isSelected: Boolean, onClick: () -> Unit) {
    val tint = if (isSelected) C.Accent else Color(0xFFAAAAAA)
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            // TODO padding: 네비 아이템 horizontal 12dp, vertical 8dp
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(iconRes), label, Modifier.size(24.dp), tint = tint)
        Spacer(Modifier.height(2.dp))
        Text(label, style = TextStyle(
            fontSize = 10.sp, fontFamily = PretendardFamily, color = tint,
            fontWeight = if (isSelected) FontWeight(600) else FontWeight(400)
        ))
    }
}

@Composable
fun SquareCancelButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick        = onClick,
        shape          = RoundedCornerShape(10.dp),
        border         = BorderStroke(1.dp, C.Border),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier       = Modifier.height(48.dp),
    ) {
        Text(label, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
    }
}

@Composable
fun SquareConfirmButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        shape    = RoundedCornerShape(10.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = C.Accent,
            disabledContainerColor = Color(0xFFCCCCCC),
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = Modifier.height(48.dp),
    ) {
        Text(label, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color.White))
    }
}

@Composable
fun SortDropdownButton(
    current  : FriendSortOption,
    onSelect : (FriendSortOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier          = Modifier.clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(current.label, style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = C.TextSec))
            Icon(painterResource(R.drawable.ic_chevron_down), "정렬", Modifier.size(14.dp), tint = C.TextSec)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Color.White) {
            FriendSortOption.entries.forEach { opt ->
                DropdownMenuItem(
                    text    = { Text(opt.label, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily)) },
                    onClick = { onSelect(opt); expanded = false },
                )
            }
        }
    }
}

// BorderStroke 편의 함수
private fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)
