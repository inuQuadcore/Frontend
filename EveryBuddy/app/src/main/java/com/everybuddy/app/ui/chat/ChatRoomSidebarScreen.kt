package com.everybuddy.app.ui.chat

// ChatRoomSidebarScreen — 채팅방 우측 슬라이드 사이드바 메뉴

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.everybuddy.app.R
import com.everybuddy.app.ui.theme.PretendardFamily

private val SbAccent    = Color(0xFF0167FF)
private val SbTextPri   = Color(0xFF000000)
private val SbTextSec   = Color(0xFF797979)
private val SbTextRed   = Color(0xFFFF3333)
private val SbBorder    = Color(0xFFE5E5E5)
private val SbToggleOn  = Color(0xFF0167FF)
private val SbBg        = Color(0xFFFFFFFF)
private val SbSectionBg = Color(0xFFF8F8F8)  // 구분선 사이 섹션 배경

data class ChatMember(
    val id             : String,
    val name           : String,
    val profileImageUrl: String? = null,  // TODO: 실제 이미지 URL
    val isMe           : Boolean = false,
)

data class MediaThumb(
    val id           : String,
    val imageUrl     : String,  // TODO: 실제 이미지 URL
)

@Composable
fun ChatRoomSidebarScreen(
    roomName              : String           = "",
    isGroup               : Boolean          = true,   // 1:1방이면 false — 멤버 초대 차단
    members               : List<ChatMember> = emptyList(),
    mediaThumbs           : List<MediaThumb> = emptyList(),
    isAutoTranslate       : Boolean          = true,
    isMuted               : Boolean          = false,
    isStarred             : Boolean          = false,
    onBack                : () -> Unit       = {},
    onPhotoVideo          : () -> Unit       = {},
    onLink                : () -> Unit       = {},
    onFile                : () -> Unit       = {},
    onInviteMember        : () -> Unit       = {},
    onToggleAutoTranslate : () -> Unit       = {},
    onToggleMute          : () -> Unit       = {},
    onToggleStar          : () -> Unit       = {},
    onDataDelete          : () -> Unit       = {},
    onLeaveRoom           : () -> Unit       = {},
) {
    BackHandler(onBack = onBack)

    var localRoomName by remember(roomName) { mutableStateOf(roomName) }
    var isEditingName  by remember { mutableStateOf(false) }

    val otherMembers = members.filter { !it.isMe }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SbBg)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter            = painterResource(R.drawable.ic_back),
                    contentDescription = "뒤로",
                    modifier           = Modifier.size(24.dp),
                    tint               = SbTextPri,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleStar) {
                Image(
                    painter            = painterResource(if (isStarred) R.drawable.ic_star_filled else R.drawable.ic_star),
                    contentDescription = "즐겨찾기",
                    modifier           = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        if (otherMembers.size == 1) {
            ProfileCircle(
                member   = otherMembers[0],
                size     = 80.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else {
            GroupProfileMosaic(
                members  = members,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        Spacer(Modifier.height(12.dp))
        if (isEditingName) {
            Row(
                modifier          = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BasicTextField(
                    value         = localRoomName,
                    onValueChange = { localRoomName = it },
                    singleLine    = true,
                    textStyle     = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = SbTextPri),
                )
                Icon(
                    painter            = painterResource(R.drawable.ic_pencil),
                    contentDescription = "완료",
                    modifier           = Modifier.size(18.dp).clickable { isEditingName = false },
                    tint               = SbAccent,
                )
            }
        } else {
            Row(
                modifier          = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text  = localRoomName,
                    style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = SbTextPri),
                )
                Icon(
                    painter            = painterResource(R.drawable.ic_pencil),
                    contentDescription = "방 이름 수정",
                    modifier           = Modifier.size(18.dp).clickable { isEditingName = true },
                    tint               = SbTextSec,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = SbBorder, thickness = 8.dp)

        SidebarToggleRow(
            label      = "실시간 번역",
            checked    = isAutoTranslate,
            onChecked  = { onToggleAutoTranslate() },
        )

        HorizontalDivider(color = SbBorder, thickness = 8.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            "미디어",
            style    = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = SbTextPri),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        // TODO padding: 타이틀-첫 항목 간격 12dp
        Spacer(Modifier.height(12.dp))

        // 사진 및 동영상 →
        SidebarMediaRow(
            label   = "사진 및 동영상",
            iconRes = R.drawable.ic_option_photo,
            onClick = onPhotoVideo,
        )

        // 썸네일 가로 스크롤 (4개)
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(mediaThumbs) { thumb ->
                MediaThumbItem(thumb = thumb)
            }
        }
        Spacer(Modifier.height(10.dp))

        HorizontalDivider(color = SbBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

        // 링크 →
        SidebarMediaRow(
            label    = "링크",
            iconRes  = R.drawable.ic_option_link,
            useImage = true,
            onClick  = onLink,
        )
        HorizontalDivider(color = SbBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

        // 파일 →
        SidebarMediaRow(
            label   = "파일",
            iconRes = R.drawable.ic_option_file,
            onClick = onFile,
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = SbBorder, thickness = 8.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            "멤버",
            style    = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = SbTextPri),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(12.dp))

        members.forEach { member ->
            MemberRow(member = member)
        }

        // + 멤버 초대하기 — 1:1방에서는 백엔드가 CANNOT_INVITE_TO_DIRECT로 거부하므로 진입 자체 차단
        if (isGroup) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onInviteMember)
                    // TODO padding: 멤버 초대 행 horizontal 16dp, vertical 12dp
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // + 원형 버튼
                Box(
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF0F4FF)),
                    contentAlignment = Alignment.Center,
                ) {
                    // ic_plus.xml
                    Icon(
                        painter            = painterResource(R.drawable.ic_fab_plus),
                        contentDescription = "멤버 초대",
                        modifier           = Modifier.size(18.dp),
                        tint               = SbAccent,
                    )
                }
                Text(
                    "멤버 초대하기",
                    style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SbAccent, fontWeight = FontWeight(500)),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = SbBorder, thickness = 8.dp)
        Spacer(Modifier.height(8.dp))
        SidebarToggleRow(
            label     = "알림",
            checked   = !isMuted,
            onChecked = { onToggleMute() },
        )
        HorizontalDivider(color = SbBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

        // 데이터 삭제 →
        SidebarArrowRow(
            label      = "데이터 삭제",
            labelColor = SbTextPri,
            onClick    = onDataDelete,
        )
        HorizontalDivider(color = SbBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

        // 채팅방 나가기 (빨간글씨, 화살표 없음)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onLeaveRoom)
                // TODO padding: 채팅방 나가기 행 horizontal 16dp, vertical 16dp
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(
                "채팅방 나가기",
                style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SbTextRed),
            )
        }

        // TODO padding: 하단 여백 32dp (네비게이션 바 공간)
        Spacer(Modifier.height(32.dp))
    }
}

// GroupProfileMosaic — 4인 프로필 그리드
@Composable
private fun GroupProfileMosaic(
    members  : List<ChatMember>,
    modifier : Modifier = Modifier,
) {
    // 최대 4명 표시, 2x2 그리드
    val displayMembers = members.take(4)

    // TODO size: 전체 모자이크 크기 약 80x80dp
    Box(
        modifier = modifier.size(80.dp),
    ) {
        when (displayMembers.size) {
            0 -> {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFCCCCCC)))
            }
            1 -> {
                ProfileCircle(member = displayMembers[0], size = 80.dp, modifier = Modifier.align(Alignment.Center))
            }
            2 -> {
                ProfileCircle(displayMembers[0], 40.dp, Modifier.align(Alignment.TopStart))
                ProfileCircle(displayMembers[1], 40.dp, Modifier.align(Alignment.BottomEnd))
            }
            3 -> {
                ProfileCircle(displayMembers[0], 38.dp, Modifier.align(Alignment.TopStart))
                ProfileCircle(displayMembers[1], 38.dp, Modifier.align(Alignment.TopEnd))
                ProfileCircle(displayMembers[2], 38.dp, Modifier.align(Alignment.BottomCenter))
            }
            else -> {
                // 4인 이상: 2x2 그리드 (이미지 속 레이아웃)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        ProfileCircle(displayMembers[0], 38.dp)
                        ProfileCircle(displayMembers[1], 38.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        ProfileCircle(displayMembers[2], 38.dp)
                        // 마지막 자리에는 실제 프로필 또는 플레이스홀더
                        if (displayMembers.size >= 4) {
                            ProfileCircle(displayMembers[3], 38.dp)
                        } else {
                            Box(Modifier.size(38.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCircle(
    member   : ChatMember,
    size     : androidx.compose.ui.unit.Dp,
    modifier : Modifier = Modifier,
) {
    if (member.profileImageUrl != null) {
        AsyncImage(
            model              = member.profileImageUrl,
            contentDescription = member.name,
            contentScale       = ContentScale.Crop,
            modifier           = modifier.size(size).clip(CircleShape).background(Color(0xFFCCCCCC)),
        )
    } else {
        Box(modifier = modifier.size(size).clip(CircleShape).background(Color(0xFFCCCCCC)))
    }
}

@Composable
private fun SidebarToggleRow(
    label     : String,
    checked   : Boolean,
    onChecked : (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // TODO padding: 토글 행 horizontal 16dp, vertical 16dp
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text  = label,
            style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SbTextPri),
        )
        Switch(
            checked         = checked,
            onCheckedChange = onChecked,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = SbToggleOn,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCCCCCC),
            ),
        )
    }
}

@Composable
private fun SidebarMediaRow(
    label    : String,
    iconRes  : Int,
    useImage : Boolean = false,
    onClick  : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (useImage) {
                Image(
                    painter            = painterResource(iconRes),
                    contentDescription = label,
                    modifier           = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    painter            = painterResource(iconRes),
                    contentDescription = label,
                    modifier           = Modifier.size(20.dp),
                    tint               = SbAccent,
                )
            }
            Text(
                text  = label,
                style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SbTextPri),
            )
        }
        Icon(
            painter            = painterResource(R.drawable.ic_chevron_right),
            contentDescription = "이동",
            modifier           = Modifier.size(16.dp),
            tint               = SbTextSec,
        )
    }
}

@Composable
private fun SidebarArrowRow(
    label      : String,
    labelColor : Color,
    onClick    : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // TODO padding: 행 horizontal 16dp, vertical 16dp
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text  = label,
            style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = labelColor),
        )
        // ic_chevron_right.xml
        Icon(
            painter            = painterResource(R.drawable.ic_chevron_right),
            contentDescription = "이동",
            modifier           = Modifier.size(16.dp),
            tint               = SbTextSec,
        )
    }
}

@Composable
private fun MemberRow(member: ChatMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // TODO padding: 멤버 행 horizontal 16dp, vertical 10dp
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 프로필 이미지
        if (member.profileImageUrl != null) {
            AsyncImage(
                model              = member.profileImageUrl,
                contentDescription = member.name,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFCCCCCC)),
            )
        } else {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFCCCCCC)))
        }
        Text(
            text  = member.name,
            style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SbTextPri),
        )
    }
}

@Composable
private fun MediaThumbItem(thumb: MediaThumb) {
    AsyncImage(
        model              = thumb.imageUrl,
        contentDescription = null,
        contentScale       = ContentScale.Crop,
        modifier           = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFD0D0D0)),
    )
}
