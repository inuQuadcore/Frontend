package com.everybuddy.app.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

private val TextPri = Color(0xFF1B1B1B)
private val TextSec = Color(0xFF8F9399)
private val Border  = Color(0xFFE5E5E5)
private val Accent  = Color(0xFF0167FF)

data class AppNotification(
    val type    : String,
    val title   : String,
    val message : String,
    val time    : String,
    val isRead  : Boolean = false,
)

@Composable
fun NotificationScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    // TODO: API - GET /api/v1/notifications 연동 후 실제 알림 목록으로 교체
    val items = remember { emptyList<AppNotification>() }

    Scaffold(
        topBar = {
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
        },
        containerColor = Color.White,
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier         = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = "새 알림이 없습니다.",
                    style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = TextSec),
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                items(items) { notif ->
                    NotificationRow(notif)
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 16.dp),
                        color     = Border,
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notif: AppNotification) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notif.isRead) Color.White else Color(0xFFF0F6FF))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when (notif.type) {
                        "chat"   -> Color(0xFFDEEFFF)
                        "friend" -> Color(0xFFE8F5E9)
                        else     -> Color(0xFFF3E5F5)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    when (notif.type) {
                        "chat"   -> R.drawable.ic_nav_chat
                        "friend" -> R.drawable.ic_nav_friend
                        else     -> R.drawable.ic_alarm
                    }
                ),
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
                    text  = notif.title,
                    style = TextStyle(
                        fontSize   = 14.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(600),
                        color      = TextPri,
                    ),
                )
                Text(
                    text  = notif.time,
                    style = TextStyle(fontSize = 11.sp, color = TextSec),
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text  = notif.message,
                style = TextStyle(
                    fontSize   = 13.sp,
                    fontFamily = PretendardFamily,
                    color      = TextSec,
                    lineHeight  = 18.sp,
                ),
            )
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
