package com.everybuddy.app.ui.notification

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuddy.app.R
import com.everybuddy.app.data.chat.RelativeTimeFormatter
import com.everybuddy.app.data.dto.Notification
import com.everybuddy.app.data.local.KST
import com.everybuddy.app.ui.theme.PretendardFamily
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDateTime

private val TextPri = Color(0xFF1B1B1B)
private val TextSec = Color(0xFF8F9399)
private val Border  = Color(0xFFE5E5E5)
private val Accent  = Color(0xFF0167FF)
private val UnreadBg = Color(0xFFF0F6FF)

@Composable
fun NotificationScreen(
    onBack         : () -> Unit,
    onFriendClick  : (Long) -> Unit = {},
    viewModel      : NotificationViewModel = hiltViewModel(),
) {
    BackHandler(onBack = onBack)
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // 마지막 항목 근처에 닿으면 다음 페이지 요청
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= state.notifications.size - 3 && state.hasNext && !state.isAppending
        }
    }
    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore }.distinctUntilChanged().collect { if (it) viewModel.loadMore() }
    }

    Scaffold(
        topBar         = { NotificationTopBar(onBack) },
        containerColor = Color.White,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                state.isLoading                  -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
                state.notifications.isEmpty()    -> EmptyText("새 알림이 없습니다.")
                else                             -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(state.notifications, key = { it.notificationId }) { notif ->
                        NotificationRow(
                            notif   = notif,
                            onClick = {
                                viewModel.markRead(notif.notificationId)
                                onFriendClick(notif.fromUserId)
                            },
                        )
                        HorizontalDivider(
                            modifier  = Modifier.padding(horizontal = 16.dp),
                            color     = Border,
                            thickness = 0.5.dp,
                        )
                    }
                    if (state.isAppending) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                                CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationTopBar(onBack: () -> Unit) {
    Column {
        Spacer(Modifier.statusBarsPadding())
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.White)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(
                onClick  = onBack,
                modifier = Modifier.size(40.dp).align(Alignment.CenterStart),
            ) {
                Icon(
                    painter            = painterResource(R.drawable.ic_back),
                    contentDescription = "뒤로",
                    modifier           = Modifier.size(24.dp),
                    tint               = TextPri,
                )
            }
            Text(
                text     = "알림",
                modifier = Modifier.align(Alignment.Center),
                style    = TextStyle(
                    fontSize   = 18.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(700),
                    color      = TextPri,
                ),
            )
        }
        HorizontalDivider(color = Border, thickness = 0.5.dp)
    }
}

@Composable
private fun EmptyText(text: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(
            text  = text,
            style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = TextSec),
        )
    }
}

@Composable
private fun NotificationRow(notif: Notification, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notif.isRead) Color.White else UnreadBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter            = painterResource(R.drawable.ic_nav_friend),
                contentDescription = null,
                modifier           = Modifier.size(20.dp),
                tint               = Accent,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text     = notif.body,
                    modifier = Modifier.weight(1f),
                    style    = TextStyle(
                        fontSize   = 14.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(500),
                        color      = TextPri,
                        lineHeight = 20.sp,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = formatRelative(notif.createdAt),
                    style = TextStyle(fontSize = 11.sp, color = TextSec),
                )
            }
        }
    }
}

@Composable
fun NotificationBadge() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Accent),
    )
}

/** ISO "yyyy-MM-ddTHH:mm:ss" (KST) → "방금/3분 전/..." */
private fun formatRelative(iso: String): String {
    val epochMs = try {
        LocalDateTime.parse(iso).atZone(KST).toInstant().toEpochMilli()
    } catch (_: Exception) { 0L }
    return RelativeTimeFormatter.format(epochMs)
}
