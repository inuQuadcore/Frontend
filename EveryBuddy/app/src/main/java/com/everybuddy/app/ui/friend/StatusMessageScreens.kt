package com.everybuddy.app.ui.friend

import androidx.activity.compose.BackHandler
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
import com.everybuddy.app.data.dto.FriendStatusMessage
import com.everybuddy.app.ui.theme.PretendardFamily
import kotlinx.coroutines.delay

private val C = FriendColors

@Composable
fun StatusWriteScreen(viewModel: StatusMessageViewModel) {
    val state by viewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BackHandler { viewModel.closeWriteScreen() }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.White)
                        .padding(horizontal = 8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(Modifier.size(40.dp))
                    Text(
                        if (state.isEditMode) "상태메시지 수정" else "상태메시지",
                        style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {}) {
                            Icon(painterResource(R.drawable.ic_search), "검색", Modifier.size(22.dp), tint = C.TextPri)
                        }
                        Box {
                            IconButton(onClick = {}) {
                                Icon(painterResource(R.drawable.ic_alarm), "알림", Modifier.size(22.dp), tint = C.TextPri)
                            }
                            Box(Modifier.size(8.dp).clip(CircleShape).background(C.Accent).align(Alignment.TopEnd).offset(x = (-6).dp, y = 6.dp))
                        }
                    }
                }
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            FriendBottomNavBar(onChat = {}, onFind = {}, onScript = {}, onMy = {})
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
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD0D0D0)),
                )
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
            val preview = myStatus?.content?.let { if (it.length > 80) it.take(80) + "…" else it } ?: "작성하기...○"
            Text(
                text     = preview,
                style    = TextStyle(fontSize = 11.sp, fontFamily = PretendardFamily, color = if (myStatus == null) C.TextSec else C.TextPri),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (myStatus != null) {
                Spacer(Modifier.height(6.dp))
                Text(myStatus.timeAgo, style = TextStyle(fontSize = 10.sp, color = C.TextSec))
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
    sm        : FriendStatusMessage,
    viewModel : StatusMessageViewModel,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.replySent) {
        if (state.replySent) {
            delay(1500)
            viewModel.consumeReplySent()
            viewModel.closeFriendStatus()
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
                                .background(if (state.replyText.isNotBlank()) C.Accent else Color(0xFFCCCCCC))
                                .clickable(enabled = state.replyText.isNotBlank()) { viewModel.sendReply() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.ic_send), "전송", Modifier.size(16.dp), tint = Color.White)
                        }
                    }
                }

                if (state.replySent) {
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
    sm      : FriendStatusMessage,
    onClick : (FriendStatusMessage) -> Unit,
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
            Spacer(Modifier.height(8.dp))
            Text(
                text     = sm.userName,
                style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = C.TextPri),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text     = if (sm.content.length > 25) sm.content.take(25) + "…" else sm.content,
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
fun FriendStatusFullItem(
    sm      : FriendStatusMessage,
    onClick : (FriendStatusMessage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(sm) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
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
