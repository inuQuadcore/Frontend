package com.everybuddy.app.ui.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuddy.app.R
import com.everybuddy.app.data.chat.ChatFilter
import com.everybuddy.app.data.chat.ChatListUiState
import com.everybuddy.app.data.chat.ChatRoomUi
import com.everybuddy.app.data.chat.dummyChatRooms
import com.everybuddy.app.ui.theme.*

private val ClAccent    = Color(0xFF0167FF)
private val ClTextPri   = Color(0xFF1B1B1B)
private val ClTextSec   = Color(0xFF8F9399)
private val ClBorder    = Color(0xFFE5E5E5)
private val ClBadge     = Color(0xFF0167FF)   // unreadCount 배지 색

@Composable
fun ChatListScreen(
    onRoomClick : (ChatRoomUi) -> Unit = {},
    onStartChat : () -> Unit         = {},
    viewModel   : ChatViewModel      = hiltViewModel(),
) {
    val state         by viewModel.listState.collectAsState()
    val filteredRooms by viewModel.filteredRooms.collectAsState()

    ChatListContent(
        state          = state.copy(rooms = filteredRooms),
        onRoomClick    = onRoomClick,
        onFilterSelect = viewModel::onFilterSelect,
        onSearchToggle = viewModel::onSearchToggle,
        onSearchChange = viewModel::onSearchQueryChange,
        onFabClick     = onStartChat,
        onRetry        = viewModel::loadChatRooms,
        onContextMenu  = viewModel::onContextMenu,
        onDismissMenu  = viewModel::onDismissContextMenu,
        onMenuAction   = viewModel::onMenuAction,
    )
}

@Composable
fun ChatListContent(
    state          : ChatListUiState,
    onRoomClick    : (ChatRoomUi) -> Unit,
    onFilterSelect : (ChatFilter) -> Unit,
    onSearchToggle : () -> Unit,
    onSearchChange : (String) -> Unit,
    onFabClick     : () -> Unit,
    onRetry        : () -> Unit,
    onContextMenu  : (ChatRoomUi) -> Unit,
    onDismissMenu  : () -> Unit,
    onMenuAction   : (String, ChatRoomUi) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // TODO padding: 앱바 높이 (현재 65dp) / horizontal (현재 20dp)
                    .height(65.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text     = "채팅",
                    style    = TextStyle(
                        fontSize   = 20.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(600),
                        color      = Color(0xFF000000),
                    ),
                    modifier = Modifier.weight(1f),
                )

                // 검색 버튼
                // TODO UI: 클릭 시 AnimatedVisibility SearchBar 표시
                IconButton(onClick = onSearchToggle, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_search),
                        contentDescription = "검색",
                        modifier           = Modifier.size(24.dp),
                        tint               = Color.Black,
                    )
                }

                // 알림 버튼 + 파란 뱃지 점
                // TODO 기능: NotificationScreen 구현 후 onClick 연결
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_alarm),
                        contentDescription = "알림",
                        modifier           = Modifier.size(24.dp),
                        tint               = Color.Black,
                    )
                    // TODO padding: 뱃지 점 위치 (현재 offset x -8dp, y 8dp)
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                            .background(Color(0xFF0167FF), CircleShape),
                    )
                }
            }

            LazyRow(
                modifier              = Modifier
                    .fillMaxWidth()
                    // TODO padding: 필터 탭 horizontal 14dp / vertical 25dp
                    .padding(horizontal = 14.dp, vertical = 25.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(ChatFilter.entries.toList()) { filter ->
                    val isSelected = filter == state.activeFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (isSelected) Color(0xFF111111) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF111111) else Color(0xFFE5E5E5),
                                shape = RoundedCornerShape(99.dp),
                            )
                            .clickable { onFilterSelect(filter) }
                            // TODO padding: 필터 탭 내부 패딩 (현재 horizontal 18dp, vertical 9dp)
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = filter.label,
                            style = TextStyle(
                                fontSize   = 15.sp,
                                fontFamily = PretendardFamily,
                                fontWeight = FontWeight(400),
                                color      = if (isSelected) Color.White else Color(0xFF111111),
                            ),
                        )
                    }
                }

                // 폴더(그룹) 추가 버튼
                item {
                    Box(
                        modifier = Modifier
                            .size(33.dp)
                            .clip(RoundedCornerShape(33.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE5E5E5), RoundedCornerShape(33.dp))
                            .clickable { /* TODO: 채팅방 그룹화 */ },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_fab_plus),
                            contentDescription = "폴더 추가",
                            modifier           = Modifier.size(13.dp),
                            tint               = Color(0xFF555555),
                        )
                    }
                }
            }

            Box(
                modifier         = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    // 로딩
                    state.isLoading -> {
                        LoadingIndicator()
                    }

                    // 에러
                    state.errorMessage != null && state.rooms.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text  = "채팅방을 불러오지 못했습니다.",
                                style = TextStyle(fontSize = 15.sp, color = ClTextSec),
                            )
                            TextButton(onClick = onRetry) {
                                Text("다시 시도", style = TextStyle(color = ClAccent, fontSize = 14.sp))
                            }
                        }
                    }

                    // 빈 상태
                    state.rooms.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(text = "💬", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text      = "아직 채팅방이 없어요",
                                style     = TextStyle(
                                    fontSize   = 18.sp,
                                    fontFamily = PretendardFamily,
                                    fontWeight = FontWeight(700),
                                    color      = Color(0xFF1B1B1B),
                                ),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text      = "+ 버튼을 눌러 새 채팅을 시작해봐요!",
                                style     = TextStyle(
                                    fontSize   = 14.sp,
                                    fontFamily = PretendardFamily,
                                    color      = Color(0xFF8F9399),
                                ),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    // 채팅방 목록
                    else -> {
                        LazyColumn(
                            modifier       = Modifier.fillMaxSize(),
                            // TODO padding: 채팅방 목록 상하 패딩 (현재 0dp)
                            contentPadding = PaddingValues(bottom = 80.dp),
                        ) {
                            items(state.rooms, key = { it.id }) { room ->
                                ChatRoomItem(
                                    room         = room,
                                    onClick      = { onRoomClick(room) },
                                    onLongClick  = { onContextMenu(room) },
                                )
                                HorizontalDivider(
                                    // TODO padding: 구분선 horizontal 패딩 (현재 16dp)
                                    modifier  = Modifier.padding(horizontal = 16.dp),
                                    color     = ClBorder,
                                    thickness = 0.5.dp,
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick        = onFabClick,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                // TODO padding: FAB 위치 end 18dp / bottom 126dp
                .padding(end = 18.dp, bottom = 126.dp),
            shape          = CircleShape,
            containerColor = Color(0xFF0167FF),
            contentColor   = Color.White,
            elevation      = FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp,
            ),
        ) {
            Icon(
                painter            = painterResource(R.drawable.ic_fab_plus),
                contentDescription = "새 채팅",
                modifier           = Modifier.size(24.dp),
                tint               = Color.White,
            )
        }

        state.contextMenuRoom?.let { room ->
            ChatRoomContextMenu(
                room        = room,
                onDismiss   = onDismissMenu,
                onMenuAction = { action -> onMenuAction(action, room) },
            )
        }
    }
}

@Composable
fun ChatRoomItem(
    room        : ChatRoomUi,
    onClick     : () -> Unit,
    onLongClick : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick     = onClick,
                onLongClick = onLongClick,
            )
            // TODO padding: 채팅방 아이템 horizontal 16dp / vertical 12dp
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 아바타
        // TODO: coil AsyncImage(model = avatarUrl)
        Box(
            modifier = Modifier
                // TODO size: 아바타 크기 (현재 48dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFDDDDDD)),
            contentAlignment = Alignment.Center,
        ) {
            // TODO: 아바타 이미지 없으면 이름 첫 글자 표시
            Text(
                text  = room.name.take(1),
                style = TextStyle(
                    fontSize   = 18.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = Color(0xFF888888),
                ),
            )
        }

        // 이름 + 마지막 메시지
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text  = room.name,
                style = TextStyle(
                    fontSize   = 15.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = ClTextPri,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (room.lastMessage.isNotEmpty()) {
                Text(
                    text  = room.lastMessage,
                    style = TextStyle(
                        fontSize   = 13.sp,
                        fontFamily = PretendardFamily,
                        color      = ClTextSec,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 시간 + 읽지 않은 수 배지
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (room.timestamp.isNotEmpty()) {
                Text(
                    text  = room.timestamp,
                    style = TextStyle(
                        fontSize   = 11.sp,
                        fontFamily = PretendardFamily,
                        color      = ClTextSec,
                    ),
                )
            }

            // unreadCount 배지
            if (room.unreadCount > 0) {
                Box(
                    modifier         = Modifier
                        // TODO size: 배지 최소 크기 (현재 18dp)
                        .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(ClBadge)
                        // TODO padding: 배지 내부 패딩 (현재 horizontal 5dp)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = if (room.unreadCount > 99) "99+" else room.unreadCount.toString(),
                        style = TextStyle(
                            fontSize   = 10.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight(600),
                            color      = Color.White,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatRoomContextMenu(
    room         : ChatRoomUi,
    onDismiss    : () -> Unit,
    onMenuAction : (String) -> Unit,
) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier        = Modifier
                .widthIn(min = 180.dp)
                .clickable {},
            shape           = RoundedCornerShape(12.dp),
            color           = Color.White,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                listOf("채팅방 정보", "알림 끄기", "상단 고정", "채팅방 나가기").forEach { label ->
                    Text(
                        text     = label,
                        style    = TextStyle(
                            fontSize   = 15.sp,
                            fontFamily = PretendardFamily,
                            color      = if (label == "채팅방 나가기") Color(0xFFFF3333) else ClTextPri,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMenuAction(label); onDismiss() }
                            // TODO padding: 메뉴 항목 패딩 (현재 horizontal 20dp, vertical 12dp)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    if (label != "채팅방 나가기") {
                        HorizontalDivider(color = ClBorder, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "채팅 탭 — 목록")
@Composable
private fun PreviewChatListWithRooms() {
    EveryBuddyTheme {
        ChatListContent(
            state          = ChatListUiState(rooms = dummyChatRooms),
            onRoomClick    = {},
            onFilterSelect = {},
            onSearchToggle = {},
            onSearchChange = {},
            onFabClick     = {},
            onRetry        = {},
            onContextMenu  = {},
            onDismissMenu  = {},
            onMenuAction   = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "채팅 탭 — 빈 상태")
@Composable
private fun PreviewChatListEmpty() {
    EveryBuddyTheme {
        ChatListContent(
            state          = ChatListUiState(rooms = emptyList()),
            onRoomClick    = {},
            onFilterSelect = {},
            onSearchToggle = {},
            onSearchChange = {},
            onFabClick     = {},
            onRetry        = {},
            onContextMenu  = {},
            onDismissMenu  = {},
            onMenuAction   = { _, _ -> },
        )
    }
}
