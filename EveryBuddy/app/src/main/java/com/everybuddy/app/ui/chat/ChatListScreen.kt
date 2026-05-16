@file:Suppress("DEPRECATION")

package com.everybuddy.app.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.everybuddy.app.data.chat.ChatFolder
import com.everybuddy.app.data.chat.ChatListUiState
import com.everybuddy.app.data.chat.ChatRoomUi
import com.everybuddy.app.data.chat.dummyChatRooms
import com.everybuddy.app.ui.theme.*

private val ClAccent    = Color(0xFF0167FF)
private val ClTextPri   = Color(0xFF1B1B1B)
private val ClTextSec   = Color(0xFF8F9399)
private val ClBorder    = Color(0xFFE5E5E5)
private val ClBadge     = Color(0xFF0167FF)   // unreadCount 배지 색

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onRoomClick        : (ChatRoomUi) -> Unit = {},
    onStartChat        : () -> Unit         = {},
    onNotificationClick: () -> Unit         = {},
    viewModel          : ChatViewModel      = hiltViewModel(),
) {
    val rawState      by viewModel.listState.collectAsState()
    val filteredRooms by viewModel.filteredRooms.collectAsState()

    ChatListContent(
        state               = rawState.copy(rooms = filteredRooms),
        allRooms            = rawState.rooms,
        isRefreshing        = rawState.isRefreshing,
        onRefresh           = viewModel::refresh,
        onRoomClick         = onRoomClick,
        onFilterSelect      = viewModel::onFilterSelect,
        onFolderTabSelect   = viewModel::onFolderTabSelect,
        onFolderPlusClick   = viewModel::openFolderManage,
        onFolderManageClose = viewModel::closeFolderManage,
        onFolderCreateOpen  = viewModel::openFolderCreate,
        onFolderCreateClose = viewModel::closeFolderCreate,
        onFolderSave        = { folder -> viewModel.saveFolder(folder); viewModel.closeFolderCreate() },
        onFolderDelete      = viewModel::deleteFolder,
        onSearchToggle      = viewModel::onSearchToggle,
        onSearchChange      = viewModel::onSearchQueryChange,
        onFabClick          = onStartChat,
        onRetry             = viewModel::loadChatRooms,
        onContextMenu       = viewModel::onContextMenu,
        onDismissMenu       = viewModel::onDismissContextMenu,
        onMenuAction        = viewModel::onMenuAction,
        onDismissRoomInfo   = viewModel::dismissRoomInfo,
        onNotificationClick = onNotificationClick,
        hasNotification     = false,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListContent(
    state               : ChatListUiState,
    allRooms            : List<ChatRoomUi>     = emptyList(),
    isRefreshing        : Boolean              = false,
    onRefresh           : () -> Unit           = {},
    onRoomClick         : (ChatRoomUi) -> Unit,
    onFilterSelect      : (ChatFilter) -> Unit,
    onFolderTabSelect   : (String?) -> Unit    = {},
    onFolderPlusClick   : () -> Unit           = {},
    onFolderManageClose : () -> Unit           = {},
    onFolderCreateOpen  : (ChatFolder?) -> Unit = {},
    onFolderCreateClose : () -> Unit           = {},
    onFolderSave        : (ChatFolder) -> Unit  = {},
    onFolderDelete      : (String) -> Unit     = {},
    onSearchToggle      : () -> Unit,
    onSearchChange      : (String) -> Unit,
    onFabClick          : () -> Unit,
    onRetry             : () -> Unit,
    onContextMenu       : (ChatRoomUi) -> Unit,
    onDismissMenu       : () -> Unit,
    onMenuAction        : (String, ChatRoomUi) -> Unit,
    onDismissRoomInfo   : () -> Unit    = {},
    onNotificationClick : () -> Unit    = {},
    hasNotification     : Boolean       = false,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.White)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text     = "대화",
                    modifier = Modifier.align(Alignment.Center),
                    style    = TextStyle(
                        fontSize   = 18.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(700),
                        color      = Color(0xFF000000),
                    ),
                )
                Row(
                    modifier              = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onSearchToggle, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_search),
                            contentDescription = "검색",
                            modifier           = Modifier.size(24.dp),
                            tint               = Color.Black,
                        )
                    }
                    Box {
                        IconButton(
                            onClick  = onNotificationClick,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                painter            = painterResource(R.drawable.ic_alarm),
                                contentDescription = "알림",
                                modifier           = Modifier.size(24.dp),
                                tint               = Color.Black,
                            )
                        }
                        if (hasNotification) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0167FF))
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-6).dp, y = 6.dp),
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = ClBorder, thickness = 0.5.dp)

            AnimatedVisibility(visible = state.isSearchOpen) {
                BasicTextField(
                    value         = state.searchQuery,
                    onValueChange = onSearchChange,
                    singleLine    = true,
                    textStyle     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = ClTextPri),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF2F2F2))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_search), null, Modifier.size(16.dp).padding(end = 4.dp), tint = ClTextSec)
                            Box(Modifier.weight(1f)) {
                                if (state.searchQuery.isEmpty()) {
                                    Text("채팅방 검색 (초성 가능)", style = TextStyle(fontSize = 15.sp, color = ClTextSec, fontFamily = PretendardFamily))
                                }
                                innerTextField()
                            }
                        }
                    },
                )
            }

            LazyRow(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 25.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(ChatFilter.entries.toList()) { filter ->
                    val isSelected = state.activeFolderId == null && filter == state.activeFilter
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

                items(state.folders, key = { it.id }) { folder ->
                    val isSelected = folder.id == state.activeFolderId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (isSelected) Color(0xFF0167FF) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF0167FF) else Color(0xFFE5E5E5),
                                shape = RoundedCornerShape(99.dp),
                            )
                            .clickable { onFolderTabSelect(if (isSelected) null else folder.id) }
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = folder.name,
                            style = TextStyle(
                                fontSize   = 15.sp,
                                fontFamily = PretendardFamily,
                                fontWeight = FontWeight(400),
                                color      = if (isSelected) Color.White else Color(0xFF111111),
                            ),
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .size(33.dp)
                            .clip(RoundedCornerShape(33.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE5E5E5), RoundedCornerShape(33.dp))
                            .clickable { onFolderPlusClick() },
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

            PullToRefreshBox(
                isRefreshing     = isRefreshing,
                onRefresh        = onRefresh,
                modifier         = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    // 로딩
                    state.isLoading -> {
                        Column(
                            modifier            = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            LoadingIndicator()
                            Text(
                                text      = "채팅방을 불러오는 중...",
                                style     = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = ClTextSec),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    // 에러
                    state.errorMessage != null && state.rooms.isEmpty() -> {
                        Column(
                            modifier            = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text      = "채팅방을 불러오지 못했습니다.",
                                style     = TextStyle(fontSize = 15.sp, color = ClTextSec, fontFamily = PretendardFamily),
                                textAlign = TextAlign.Center,
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
                room         = room,
                onDismiss    = onDismissMenu,
                onMenuAction = { action -> onMenuAction(action, room) },
            )
        }

        state.infoRoom?.let { room ->
            ChatRoomInfoScreen(room = room, onBack = onDismissRoomInfo)
        }

        if (state.isFolderManageOpen) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                FolderManageScreen(
                    folders     = state.folders,
                    onBack      = onFolderManageClose,
                    onCreateNew = { onFolderCreateOpen(null) },
                    onEdit      = { folder -> onFolderCreateOpen(folder) },
                    onDelete    = onFolderDelete,
                )
            }
        }

        if (state.isFolderCreateOpen) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                FolderCreateScreen(
                    editingFolder = state.editingFolder,
                    allRooms      = allRooms,
                    onBack        = onFolderCreateClose,
                    onSave        = onFolderSave,
                )
            }
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 아바타 (이니셜 원형)
        Box(
            modifier         = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFDDDDDD)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = room.name.take(1),
                style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color(0xFF888888)),
            )
        }

        // 이름 + 마지막 메시지
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (room.isPinned) {
                    Icon(painterResource(R.drawable.ic_pin), "고정", Modifier.size(12.dp), tint = ClAccent)
                }
                Text(
                    text     = room.name,
                    style    = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = ClTextPri),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (room.lastMessage.isNotEmpty()) {
                Text(
                    text     = room.lastMessage,
                    style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = ClTextSec),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 시간 + 배지
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (room.isMuted) {
                    Icon(painterResource(R.drawable.ic_alarm_off), "알림 꺼짐", Modifier.size(13.dp), tint = ClTextSec)
                }
                if (room.timestamp.isNotEmpty()) {
                    Text(text = room.timestamp, style = TextStyle(fontSize = 11.sp, fontFamily = PretendardFamily, color = ClTextSec))
                }
            }
            if (room.unreadCount > 0) {
                Box(
                    modifier         = Modifier
                        .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(ClBadge)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = if (room.unreadCount > 99) "99+" else room.unreadCount.toString(),
                        style = TextStyle(fontSize = 10.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color.White),
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
    val muteLabel    = if (room.isMuted) "알림 켜기" else "알림 끄기"
    val pinLabel     = if (room.isPinned) "상단 고정 해제" else "상단 고정"
    var showLeaveConfirm by remember { mutableStateOf(false) }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier        = Modifier.widthIn(min = 220.dp).clickable {},
            shape           = RoundedCornerShape(12.dp),
            color           = Color.White,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                ContextMenuItem("채팅방 정보", false) { onMenuAction("info"); onDismiss() }
                HorizontalDivider(color = ClBorder, thickness = 0.5.dp)
                ContextMenuItem(muteLabel, false)    { onMenuAction("toggle_mute"); onDismiss() }
                HorizontalDivider(color = ClBorder, thickness = 0.5.dp)
                ContextMenuItem(pinLabel, false)     { onMenuAction("toggle_pin"); onDismiss() }
                HorizontalDivider(color = ClBorder, thickness = 0.5.dp)
                ContextMenuItem("채팅방 나가기", true) { showLeaveConfirm = true }
            }
        }
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            containerColor   = Color.White,
            title = {
                Text("채팅방 나가기", style = TextStyle(fontSize = 17.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = ClTextPri))
            },
            text = {
                Text("정말 나가겠어요?", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = ClTextSec))
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text("취소", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = ClTextSec))
                }
            },
            confirmButton = {
                TextButton(onClick = { onMenuAction("leave"); onDismiss() }) {
                    Text("확인", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color(0xFFFF3333)))
                }
            },
        )
    }
}

@Composable
private fun ContextMenuItem(label: String, isDestructive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text  = label,
            style = TextStyle(
                fontSize   = 15.sp,
                fontFamily = PretendardFamily,
                color      = if (isDestructive) Color(0xFFFF3333) else ClTextPri,
            ),
        )
    }
}

@Composable
private fun ChatRoomInfoScreen(room: ChatRoomUi, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 8.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(40.dp)) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = ClTextPri)
                    }
                    Text(
                        "채팅방 정보",
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = ClTextPri),
                    )
                }
                HorizontalDivider(color = ClBorder, thickness = 0.5.dp)
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp)) {
            Text(
                "내가 설정한 사진과 이름은 나에게만 보입니다.",
                style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = ClTextSec),
            )
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
            onMenuAction        = { _, _ -> },
            onDismissRoomInfo   = {},
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
            onMenuAction        = { _, _ -> },
            onDismissRoomInfo   = {},
        )
    }
}
