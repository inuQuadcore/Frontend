package com.everybuddy.app.ui.demo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.everybuddy.app.R
import com.everybuddy.app.ui.chat.*
import com.everybuddy.app.ui.script.ScriptMainScreen
import com.everybuddy.app.ui.script.ScriptViewModel
import com.everybuddy.app.ui.theme.PretendardFamily

private enum class DemoRoute {
    CHAT_ROOM,      // 채팅방 (메인 데모)
    SIDEBAR,        // 채팅방 사이드바
    SCRIPT_MAIN,    // 스크립트 메인
}

// 데모용 ChatMessage 래퍼
data class DemoChatMessage(
    val id             : String,
    val senderId       : String,
    val senderName     : String,
    val text           : String,
    val translatedText : String = "",
    val isMine         : Boolean,
)

private val demoChatMessages = listOf(
    DemoChatMessage("1", "hong", "홍길동", "That's lowkey fire ngl",        "솔직히 은근 멋있다",          isMine = false),
    DemoChatMessage("2", "me",   "나",     "I know right? That tracks.",    "맞지? 그럴 만해.",            isMine = true),
    DemoChatMessage("3", "lee",  "이길동", "I fear you're right.",          "인정하기 싫지만 네 말이 맞아",  isMine = false),
    DemoChatMessage("4", "kim",  "김길동", "He's giving NPC Energy today",  "오늘 걔 존재감 없이 행동하네",  isMine = false),
    DemoChatMessage("5", "me",   "나",     "So delulu lol",                 "완전 망상에 빠졌네 ㅋㅋ",      isMine = true),
    DemoChatMessage("6", "park", "박길동", "Mid. Just mid.",                "그냥 그렇다. 평범.",           isMine = false),
    DemoChatMessage("7", "lee",  "이길동", "Lowkey obsessed with this song","이 노래에 은근히 꽂혔어",       isMine = false),
)

private val AccentBlue  = Color(0xFF0167FF)
private val TextPri     = Color(0xFF000000)
private val TextSec     = Color(0xFF797979)
private val Border      = Color(0xFFE5E5E5)
private val BubbleGray  = Color(0xFFE5E6E8)
private val BubbleBlue  = Color(0xFFDEF0FF)

@Composable
fun ScriptDemoScreen(
    scriptViewModel: ScriptViewModel = viewModel(),
) {
    var currentRoute by remember { mutableStateOf(DemoRoute.CHAT_ROOM) }

    when (currentRoute) {
        DemoRoute.CHAT_ROOM -> DemoChatRoomScreen(
            scriptViewModel    = scriptViewModel,
            onOpenSidebar      = { currentRoute = DemoRoute.SIDEBAR },
            onGoToScriptMain   = { currentRoute = DemoRoute.SCRIPT_MAIN },
        )
        DemoRoute.SIDEBAR -> ChatRoomSidebarScreen(
            roomName = "홍길동,김길동,박길동,이...",
            members  = demoMembers,
            mediaThumbs = demoMediaThumbs,
            onBack   = { currentRoute = DemoRoute.CHAT_ROOM },
        )
        DemoRoute.SCRIPT_MAIN -> ScriptMainScreen(
            viewModel     = scriptViewModel,
            onAddFolder   = { /* TODO: NewFolderScreen 이동 */ },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DemoChatRoomScreen(
    scriptViewModel  : ScriptViewModel,
    onOpenSidebar    : () -> Unit,
    onGoToScriptMain : () -> Unit,
) {
    // 컨텍스트 메뉴 상태
    var contextMenuMessage by remember { mutableStateOf<DemoChatMessage?>(null) }

    var saveSheetMessage by remember { mutableStateOf<DemoChatMessage?>(null) }

    // 저장 완료 토스트
    val uiState by scriptViewModel.uiState.collectAsState()
    LaunchedEffect(uiState.toastMessage) {
        if (uiState.toastMessage != null) {
            kotlinx.coroutines.delay(2000)
            scriptViewModel.consumeToast()
        }
    }

    Scaffold(
        topBar = {
            DemoChatTopBar(
                roomName      = "홍길동,김길동,박길동,이...",
                onBack        = {},
                onOpenSidebar = onOpenSidebar,
            )
        },
        bottomBar = {
            // BottomNavBar — 스크립트 탭 강조
            Column {
                HorizontalDivider(color = Border, thickness = 0.5.dp)
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color.White)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    DemoNavItem("대화",   R.drawable.ic_nav_chat,   isSelected = true,  onClick = {})
                    DemoNavItem("친구",   R.drawable.ic_nav_friend, isSelected = false, onClick = {})
                    DemoNavItem("찾기",   R.drawable.ic_nav_find,   isSelected = false, onClick = {})
                    DemoNavItem("스크립트", R.drawable.ic_nav_script, isSelected = false, onClick = onGoToScriptMain)
                    DemoNavItem("마이",   R.drawable.ic_nav_my,     isSelected = false, onClick = {})
                }
            }
        },
        containerColor = Color(0xFFF5F5F5),
    ) { innerPadding ->

        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                reverseLayout  = false,
            ) {
                // 안내 문구 (데모 힌트)
                item {
                    Text(
                        "💡 메시지를 길게 누르면 스크립트 저장 메뉴가 나타납니다",
                        style     = TextStyle(fontSize = 12.sp, color = TextSec),
                        modifier  = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }

                items(demoChatMessages) { message ->
                    DemoChatBubble(
                        message    = message,
                        onLongPress = { contextMenuMessage = message },
                    )
                }
            }

            contextMenuMessage?.let { msg ->
                MessageContextMenu(
                    onDismiss    = { contextMenuMessage = null },
                    onCopy       = { contextMenuMessage = null },
                    onSelectCopy = { contextMenuMessage = null },
                    onReply      = { contextMenuMessage = null },
                    onSaveScript = {
                        saveSheetMessage   = msg
                        contextMenuMessage = null
                    },
                )
            }

            uiState.toastMessage?.let { msg ->
                Box(
                    modifier         = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp),
                ) {
                    SavedToast(message = msg)
                }
            }
        }
    }

    saveSheetMessage?.let { msg ->
        // ChatMessage 래퍼 변환
        val chatMessage = chatMessageFromDemo(msg)
        ScriptSaveSheet(
            message     = chatMessage,
            folders     = scriptViewModel.uiState.collectAsState().value.folders,
            onSave      = { item ->
                scriptViewModel.saveScriptItem(
                    originalText   = item.originalText,
                    translatedText = item.translatedText,
                    memo1          = item.memo1,
                    memo2          = item.memo2,
                    folderId       = item.selectedFolder,
                )
                saveSheetMessage = null
            },
            onDismiss   = { saveSheetMessage = null },
            onAddFolder = { /* TODO: NewFolderScreen */ },
        )
    }
}

// ChatMessage 변환 헬퍼
private fun chatMessageFromDemo(msg: DemoChatMessage): com.everybuddy.app.data.chat.ChatMessage {
    return com.everybuddy.app.data.chat.ChatMessage(
        id             = msg.id,
        senderId       = msg.senderId,
        senderName     = msg.senderName,
        text           = msg.text,
        translatedText = msg.translatedText,
        type           = com.everybuddy.app.data.chat.MessageType.TEXT,
        timestamp      = java.time.LocalDateTime.now(),
    )
}

@Composable
private fun DemoChatTopBar(
    roomName      : String,
    onBack        : () -> Unit,
    onOpenSidebar : () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.White)
                .padding(horizontal = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                // ic_back.xml
                Icon(
                    painter            = painterResource(R.drawable.ic_back),
                    contentDescription = "뒤로",
                    modifier           = Modifier.size(24.dp),
                    tint               = TextPri,
                )
            }
            Text(
                text  = roomName,
                style = TextStyle(fontSize = 17.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = TextPri),
            )
            IconButton(onClick = onOpenSidebar) {
                // ic_sidemenu.xml
                Icon(
                    painter            = painterResource(R.drawable.ic_sidemenu),
                    contentDescription = "메뉴",
                    modifier           = Modifier.size(22.dp),
                    tint               = TextPri,
                )
            }
        }
        HorizontalDivider(color = Border, thickness = 0.5.dp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DemoChatBubble(
    message    : DemoChatMessage,
    onLongPress: (DemoChatMessage) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick    = {},
                onLongClick = { onLongPress(message) },
            )
            // TODO padding: 말풍선 행 horizontal 16dp, vertical 5dp
            .padding(horizontal = 16.dp, vertical = 5.dp),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment     = Alignment.Bottom,
    ) {
        if (!message.isMine) {
            // 프로필 이미지 (플레이스홀더)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCCCCCC))
                    // TODO padding: 아바타 우측 8dp
                    .padding(end = 0.dp),
            )
            // TODO padding: 아바타-말풍선 8dp
            Spacer(Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start) {
            if (!message.isMine) {
                Text(
                    message.senderName,
                    style    = TextStyle(fontSize = 12.sp, color = TextSec),
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart    = if (message.isMine) 14.dp else 4.dp,
                            topEnd      = if (message.isMine) 4.dp else 14.dp,
                            bottomStart = 14.dp,
                            bottomEnd   = 14.dp,
                        )
                    )
                    .background(if (message.isMine) BubbleBlue else BubbleGray)
                    // TODO padding: 말풍선 내부 horizontal 12dp, vertical 8dp
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(max = 250.dp),
            ) {
                Text(
                    text  = message.text,
                    style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = TextPri, lineHeight = 21.sp),
                )
            }
            if (message.translatedText.isNotEmpty()) {
                // TODO padding: 번역문 상단 3dp
                Spacer(Modifier.height(3.dp))
                Text(
                    text  = message.translatedText,
                    style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = TextSec, lineHeight = 18.sp),
                )
            }
        }
    }
}

@Composable
private fun DemoNavItem(
    label      : String,
    iconRes    : Int,
    isSelected : Boolean,
    onClick    : () -> Unit,
) {
    val tint = if (isSelected) AccentBlue else Color(0xFFAAAAAA)
    Column(
        modifier            = Modifier
            .clickable(onClick = onClick)
            // TODO padding: 네비 아이템 horizontal 12dp, vertical 8dp
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter            = painterResource(iconRes),
            contentDescription = label,
            modifier           = Modifier.size(24.dp),
            tint               = tint,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = TextStyle(
                fontSize   = 10.sp,
                fontFamily = PretendardFamily,
                color      = tint,
                fontWeight = if (isSelected) FontWeight(600) else FontWeight(400),
            ),
        )
    }
}

