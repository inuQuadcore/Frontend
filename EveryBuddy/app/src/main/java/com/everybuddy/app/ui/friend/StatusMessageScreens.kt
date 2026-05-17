package com.everybuddy.app.ui.friend

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.everybuddy.app.data.dto.FriendStatusMessageDto
import com.everybuddy.app.ui.theme.PretendardFamily
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val C = FriendColors

@Composable
fun StatusWriteScreen(viewModel: StatusMessageViewModel) {
    val state by viewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BackHandler { viewModel.closeWriteScreen() }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White).statusBarsPadding()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    IconButton(
                        onClick  = { viewModel.closeWriteScreen() },
                        modifier = Modifier.align(Alignment.CenterStart).size(40.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = C.TextPri)
                    }
                    Row(
                        modifier              = Modifier.align(Alignment.Center),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_pencil), null, Modifier.size(16.dp), tint = C.TextPri)
                        Text(
                            if (state.isEditMode) "상태메시지 수정" else "상태메시지 작성",
                            style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                        )
                    }
                }
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Surface(
                shape    = RoundedCornerShape(14.dp),
                color    = Color.White,
                border   = androidx.compose.foundation.BorderStroke(0.8.dp, C.Border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFCCCCCC)))
                        Text(
                            "나의 상태 메시지",
                            style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = C.TextPri),
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = C.Border, thickness = 0.5.dp)
                    Spacer(Modifier.height(12.dp))

                    BasicTextField(
                        value         = state.draftText,
                        onValueChange = viewModel::updateDraftText,
                        modifier      = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 80.dp)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            fontSize   = 14.sp,
                            fontFamily = PretendardFamily,
                            color      = if (state.draftText.length >= 150) C.TextRed else C.TextPri,
                            lineHeight = 22.sp,
                        ),
                        decorationBox = { inner ->
                            if (state.draftText.isEmpty()) {
                                Text("작성하기...", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextSec))
                            }
                            inner()
                        },
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${state.draftText.length}/150",
                        style    = TextStyle(fontSize = 11.sp, color = if (state.draftText.length >= 150) C.TextRed else C.TextSec),
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SquareCancelButton(label = "취소", onClick = { viewModel.closeWriteScreen() })
                SquareConfirmButton(
                    label   = if (state.isEditMode) "수정 완료" else "작성 완료",
                    enabled = state.draftText.isNotBlank(),
                    onClick = { viewModel.submitStatus() },
                )
            }
        }
    }
}

@Composable
fun MyStatusCard(viewModel: StatusMessageViewModel) {
    val state    by viewModel.state.collectAsState()
    val myStatus  = state.myStatus
    val myProfile = state.myProfileImageUrl

    Surface(
        modifier = Modifier
            .width(140.dp)
            .clickable {
                if (myStatus == null) viewModel.openWriteNew()
                else viewModel.openMyStatusMenu()
            },
        shape  = RoundedCornerShape(12.dp),
        color  = Color.White,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, C.Border),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top,
            ) {
                if (myProfile != null) {
                    AsyncImage(
                        model              = myProfile,
                        contentDescription = "내 프로필",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFD0D0D0)),
                    )
                } else {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFD0D0D0)))
                }
                if (myStatus != null) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_more_vertical),
                        contentDescription = "더보기",
                        modifier           = Modifier.size(16.dp),
                        tint               = C.TextSec,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text     = "나",
                style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = C.TextPri),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            if (myStatus == null) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_pencil), null, Modifier.size(12.dp), tint = C.TextSec)
                    Text("나도 작성하기", style = TextStyle(fontSize = 11.sp, fontFamily = PretendardFamily, color = C.TextSec))
                }
            } else {
                val preview = myStatus.content.let { if (it.length > 80) it.take(80) + "…" else it }
                Text(
                    text     = preview,
                    style    = TextStyle(fontSize = 11.sp, fontFamily = PretendardFamily, color = C.TextPri),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (myStatus != null) {
                Spacer(Modifier.height(6.dp))
                Text(myStatus.updatedAt.toRelativeTime(), style = TextStyle(fontSize = 10.sp, color = C.TextSec))
            }
        }
    }
}

@Composable
fun MyStatusMenuSheet(viewModel: StatusMessageViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable { viewModel.closeMyStatusMenu() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier        = Modifier
                .fillMaxWidth()
                .clickable {},
            shape           = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color           = Color.White,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
            ) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFDDDDDD))
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openEditMode() }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_pencil), "수정", Modifier.size(20.dp), tint = C.TextPri)
                    Text("수정하기", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, color = C.TextPri))
                }

                HorizontalDivider(color = C.Border, thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openDeleteConfirm() }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_trash), "삭제", Modifier.size(20.dp), tint = C.TextRed)
                    Text("삭제하기", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, color = C.TextRed))
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(viewModel: StatusMessageViewModel) {
    AlertDialog(
        onDismissRequest  = { viewModel.closeDeleteConfirm() },
        title             = { Text("상태메시지 삭제", style = TextStyle(fontFamily = PretendardFamily, fontWeight = FontWeight(700))) },
        text              = { Text("상태메시지를 삭제할까요?", style = TextStyle(fontFamily = PretendardFamily, color = C.TextSec)) },
        confirmButton     = {
            TextButton(onClick = { viewModel.deleteStatus() }) {
                Text("삭제", style = TextStyle(fontFamily = PretendardFamily, color = C.TextRed, fontWeight = FontWeight(600)))
            }
        },
        dismissButton     = {
            TextButton(onClick = { viewModel.closeDeleteConfirm() }) {
                Text("취소", style = TextStyle(fontFamily = PretendardFamily, color = C.TextSec))
            }
        },
        containerColor    = Color.White,
        shape             = RoundedCornerShape(16.dp),
    )
}

@Composable
fun FriendStatusDetailPopup(
    sm           : FriendStatusMessageDto,
    viewModel    : StatusMessageViewModel,
    onReplySent  : (friendId: String, friendName: String) -> Unit = { _, _ -> },
) {
    val state      by viewModel.state.collectAsState()
    val friendId   = remember { sm.userId.toString() }
    val friendName = remember { sm.userName }

    LaunchedEffect(state.replySent) {
        if (state.replySent) {
            delay(1200)
            onReplySent(friendId, friendName)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable { viewModel.closeFriendStatus() },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier        = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable {},
            shape           = RoundedCornerShape(16.dp),
            color           = Color.White,
            shadowElevation = 12.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD0D0D0)),
                    ) {
                        AsyncImage(
                            model              = sm.profileImageUrl,
                            contentDescription = sm.userName,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        sm.userName,
                        style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text  = sm.content,
                    style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextPri, lineHeight = 22.sp),
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(sm.timeAgo, style = TextStyle(fontSize = 12.sp, color = C.TextSec))

                    if (!state.isReplying && !state.replySent) {
                        Button(
                            onClick        = { viewModel.openReply() },
                            shape          = RoundedCornerShape(20.dp),
                            colors         = ButtonDefaults.buttonColors(containerColor = C.Accent),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text("답장하기", style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = Color.White))
                            Spacer(Modifier.width(4.dp))
                            Icon(painterResource(R.drawable.ic_send), "전송", Modifier.size(14.dp), tint = Color.White)
                        }
                    } else if (state.isReplying) {
                        Text(
                            "취소",
                            style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = C.TextRed),
                            modifier = Modifier.clickable { viewModel.cancelReply() },
                        )
                    }
                }

                if (state.isReplying && !state.replySent) {
                    Spacer(Modifier.height(10.dp))
                    val canSend = state.replyText.isNotBlank() && !state.isSending
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFF0F0F0))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value         = state.replyText,
                            onValueChange = viewModel::updateReplyText,
                            modifier      = Modifier.weight(1f),
                            singleLine    = true,
                            enabled       = !state.isSending,
                            textStyle     = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextPri),
                            decorationBox = { inner ->
                                if (state.replyText.isEmpty()) {
                                    Text("답장하기", style = TextStyle(fontSize = 14.sp, color = C.TextSec))
                                }
                                inner()
                            },
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (canSend) C.Accent else Color(0xFFCCCCCC))
                                .clickable(enabled = canSend) { viewModel.sendReply() },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.isSending) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(16.dp),
                                    color       = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(painterResource(R.drawable.ic_send), "전송", Modifier.size(16.dp), tint = Color.White)
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = state.replySent,
                    enter   = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.85f, animationSpec = tween(200)),
                    exit    = fadeOut(animationSpec = tween(150)),
                ) {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(top = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF444444))
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            Text("전송 완료!", style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = Color.White))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendStatusPreviewCard(
    sm      : FriendStatusMessageDto,
    onClick : (FriendStatusMessageDto) -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick(sm) },
        shape  = RoundedCornerShape(12.dp),
        color  = Color.White,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, C.Border),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD0D0D0)),
                ) {
                    AsyncImage(
                        model              = sm.profileImageUrl,
                        contentDescription = sm.userName,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    text     = sm.userName,
                    style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = C.TextPri),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text     = if (sm.content.length > 15) sm.content.take(15) + "…" else sm.content,
                style    = TextStyle(fontSize = 11.sp, fontFamily = PretendardFamily, color = C.TextPri),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(sm.timeAgo, style = TextStyle(fontSize = 10.sp, color = C.TextSec))
        }
    }
}

@Composable
fun FriendStatusGridCard(
    sm      : FriendStatusMessageDto,
    onClick : (FriendStatusMessageDto) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick(sm) },
        shape  = RoundedCornerShape(12.dp),
        color  = Color.White,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, C.Border),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD0D0D0)),
                ) {
                    AsyncImage(
                        model              = sm.profileImageUrl,
                        contentDescription = sm.userName,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    text     = sm.userName,
                    style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = C.TextPri),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            val content = if (sm.content.length > 15) sm.content.take(15) + "…" else sm.content
            Text(
                text     = content,
                style    = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = C.TextPri, lineHeight = 18.sp),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(sm.timeAgo, style = TextStyle(fontSize = 10.sp, color = C.TextSec))
        }
    }
}

@Composable
fun FriendStatusFullItem(
    sm      : FriendStatusMessageDto,
    onClick : (FriendStatusMessageDto) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(sm) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD0D0D0)),
            ) {
                AsyncImage(
                    model              = sm.profileImageUrl,
                    contentDescription = sm.userName,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = sm.userName,
                    style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = C.TextPri),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text  = sm.content,
                    style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = C.TextPri, lineHeight = 20.sp),
                )
                Spacer(Modifier.height(4.dp))
                Text(sm.timeAgo, style = TextStyle(fontSize = 11.sp, color = C.TextSec))
            }
        }
    }
    HorizontalDivider(color = C.Border, thickness = 0.5.dp)
}

// 서버 ISO 8601 LocalDateTime(KST 가정) → "방금 / N분 전 / N시간 전 / N일 전".
private fun String.toRelativeTime(): String {
    val past = try {
        LocalDateTime.parse(this).atZone(ZoneId.systemDefault()).toInstant()
    } catch (_: Exception) {
        return this
    }
    val diff = Duration.between(past, Instant.now())
    val minutes = diff.toMinutes()
    val hours = diff.toHours()
    return when {
        minutes < 1 -> "방금"
        hours < 1   -> "${minutes}분 전"
        hours < 24  -> "${hours}시간 전"
        else        -> "${diff.toDays()}일 전"
    }
}
