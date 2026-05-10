package com.everybuddy.app.ui.chat

// ChatRoomScreen          — Hilt 진입점
// ChatRoomContent         — 상태 기반 전체 레이아웃
// ChatAppBar              — 상단바 (뒤로/이름/검색/메뉴)
// MessageList             — LazyColumn 메시지 목록
// TextMessageBubble       — 텍스트 말풍선 + 번역 버튼/번역문
// VoiceMessageBubble      — 음성 말풍선 (파형 + 재생 + STT)
// InputBar                — 입력바 (+/텍스트/마이크/전송)
// RecordingOverlay        — 음성 녹음 바텀시트
// ScriptSaveSheet         — 스크립트 저장 바텀시트 (1/2, 2/2)
// SaveOptionDialog        — 통합/따로 저장 다이얼로그

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuddy.app.R
import com.everybuddy.app.data.chat.*
import com.everybuddy.app.ui.theme.PretendardFamily
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

// 색상 상수 (디자인 이미지 기준)
private val BgGray         = Color(0xFFF5F7F9)   // 채팅방 배경
private val BubbleWhite    = Color(0xFFE5E6E8)   // 상대 말풍선
private val BubbleBlue     = Color(0xFFDEF0FF)   // 내 텍스트 말풍선
private val BubbleMyVoice  = Color(0xFFDEF0FF)   // 내 음성 말풍선
private val AccentBlue     = Color(0xFF0167FF)   // 주요 파란색
private val TextPrimary    = Color(0xFF000000)
private val TextSecondary  = Color(0xFF797979)
private val TextTranslated = Color(0xFF797979)
private val DividerGray    = Color(0xFFE5E5E5)
private val DateBadgeBg    = Color(0xFFDDDDDD)

// ChatRoomScreen — Hilt 진입점
@Composable
fun ChatRoomScreen(
    roomId                   : String,
    onBack                   : () -> Unit,
    onNavigateToScriptSave   : (String) -> Unit = {},  // messageId 전달
    onNavigateToConversation : () -> Unit       = {},  // 대화 선택 화면
    viewModel                : ChatRoomViewModel = hiltViewModel(),
) {
    LaunchedEffect(roomId) { viewModel.loadRoom(roomId) }
    val state by viewModel.uiState.collectAsState()

    ChatRoomContent(
        state                  = state,
        onBack                 = onBack,
        onInputChange          = viewModel::onInputChange,
        onSendText             = viewModel::onSendText,
        onStartRecording       = viewModel::onStartRecording,
        onStopRecording        = viewModel::onStopRecording,
        onCancelRecording      = viewModel::onCancelRecording,
        onPlayVoice            = viewModel::onPlayVoice,
        onToggleTranslation    = viewModel::onToggleTranslation,
        onToggleAutoTranslate  = viewModel::onToggleAutoTranslate,
        onToggleMediaPanel     = viewModel::onToggleMediaPanel,
        onLongPressMessage     = viewModel::onLongPressMessage,
        onDismissContextMenu   = viewModel::onDismissContextMenu,
        onConversationSelect   = onNavigateToConversation,
        onSaveScriptComplete   = viewModel::onScriptSaved,
        onNavigateToScriptSave = onNavigateToScriptSave,
    )
}

// ChatRoomContent — 전체 레이아웃
@Composable
fun ChatRoomContent(
    state                  : ChatRoomUiState,
    onBack                 : () -> Unit,
    onInputChange          : (String) -> Unit,
    onSendText             : () -> Unit,
    onStartRecording       : () -> Unit,
    onStopRecording        : () -> Unit,
    onCancelRecording      : () -> Unit,
    onPlayVoice            : (String) -> Unit,
    onToggleTranslation    : (String) -> Unit,
    onToggleAutoTranslate  : () -> Unit,
    onToggleMediaPanel     : () -> Unit,
    onLongPressMessage     : (ChatMessage) -> Unit  = {},
    onDismissContextMenu   : () -> Unit             = {},
    onConversationSelect   : () -> Unit             = {},
    onSaveScriptComplete   : () -> Unit             = {},
    onNavigateToScriptSave : (String) -> Unit       = {},
) {
    // 스크립트 저장 바텀시트 상태
    var scriptSaveMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var scriptSaveStep    by remember { mutableStateOf(1) }   // 1 or 2
    var saveOptionOpen    by remember { mutableStateOf(false) }
    var isMenuOpen        by remember { mutableStateOf(false) }

    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()

    // 새 메시지 도착 시 최하단 스크롤
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            ChatAppBar(
                roomName = state.room.name,
                onBack   = onBack,
                onSearch = { /* TODO: 채팅 내 검색 */ },
                onMenu   = { isMenuOpen = true },
            )

            HorizontalDivider(color = DividerGray, thickness = 0.5.dp)

            MessageList(
                messages              = state.messages,
                isAutoTranslate       = state.isAutoTranslate,
                showTranslation       = state.showTranslation,
                translatingMessageIds = state.translatingMessageIds,
                playingMessageId      = state.playingMessageId,
                modifier              = Modifier.weight(1f),
                listState             = listState,
                onPlayVoice           = onPlayVoice,
                onToggleTranslation   = onToggleTranslation,
                onLongPressMessage    = onLongPressMessage,
                onSaveScript          = { msg ->
                    scriptSaveMessage = msg
                    scriptSaveStep    = 1
                },
            )

            AnimatedVisibility(
                visible = state.isMediaPanelOpen && !state.isRecording,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut(),
            ) {
//                MediaPanel(
//                    onPhoto      = { /* TODO: 갤러리 */ },
//                    onCamera     = { /* TODO: 카메라 */ },
//                    onCall       = { /* TODO: 통화 */ },
//                    onFile       = { /* TODO: 파일 */ },
//                    onSaveScript = onConversationSelect,
//                )
            }

            if (!state.isRecording) {
                InputBar(
                    text             = state.inputText,
                    onTextChange     = onInputChange,
                    onSend           = onSendText,
                    onMicClick       = onStartRecording,
                    onPlusClick      = onToggleMediaPanel,
                    isMediaPanelOpen = state.isMediaPanelOpen,
                )
            }
        }

        AnimatedVisibility(
            visible  = state.isRecording,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter    = slideInVertically { it },
            exit     = slideOutVertically { it },
        ) {
            RecordingOverlay(
                seconds    = state.recordingSeconds,
                amplitudes = state.recordingAmplitudes,
                onDelete   = onCancelRecording,
                onStop     = onStopRecording,
                onPause    = { /* TODO */ },
            )
        }

        state.contextMenuMessage?.let { msg ->
            MessageContextMenu(
                onDismiss    = onDismissContextMenu,
                onCopy       = {
                    // TODO: 클립보드 복사
                },
                onSelectCopy = {
                    // TODO: 텍스트 선택 복사 UI
                },
                onReply      = {
                    // TODO: 답장 기능
                },
                onSaveScript = {
                    // 컨텍스트 메뉴 닫고 스크립트 저장 화면으로 이동
                    onDismissContextMenu()
                    onNavigateToScriptSave(msg.id)
                },
            )
        }

        AnimatedVisibility(
            visible  = state.savedToastVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // TODO padding: 토스트 하단 여백 (현재 80dp)
                .padding(bottom = 80.dp),
            enter = fadeIn(),
            exit  = fadeOut(),
        ) {
            SavedToast()
        }
    }   // Box 끝

    scriptSaveMessage?.let { msg ->
        ScriptSaveSheet(
            message   = msg,
            step      = scriptSaveStep,
            onNext    = { scriptSaveStep = 2 },
            onBack    = { scriptSaveStep = 1 },
            onSave    = { _ ->
                // TODO: 스크립트 저장 API 연동
                onSaveScriptComplete()
                scriptSaveMessage = null
                scriptSaveStep    = 1
            },
            onDismiss = {
                scriptSaveMessage = null
                scriptSaveStep    = 1
            },
        )
    }

    if (saveOptionOpen) {
        SaveOptionDialog(onDismiss = { saveOptionOpen = false })
    }

    if (isMenuOpen) {
        ChatSideMenu(
            isAutoTranslate       = state.isAutoTranslate,
            onToggleAutoTranslate = onToggleAutoTranslate,
            onDismiss             = { isMenuOpen = false },
        )
    }
}

// ChatAppBar
@Composable
fun ChatAppBar(
    roomName : String,
    onBack   : () -> Unit,
    onSearch : () -> Unit,
    onMenu   : () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.White)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                painter            = painterResource(R.drawable.ic_back),
                contentDescription = "뒤로가기",
                modifier           = Modifier.size(24.dp),
                tint               = Color(0xFF000000),
            )
        }

        Text(
            text     = roomName,
            style    = TextStyle(
                fontSize   = 20.sp,
                fontFamily = PretendardFamily,
                fontWeight = FontWeight(600),
                color      = Color(0xFF000000),
                textAlign  = TextAlign.Center,
            ),
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onSearch, modifier = Modifier.size(40.dp)) {
            Icon(
                painter            = painterResource(R.drawable.ic_search),
                contentDescription = "검색",
                modifier           = Modifier.size(24.dp),
                tint               = Color(0xFF000000),
            )
        }

        IconButton(onClick = onMenu, modifier = Modifier.size(40.dp)) {
            Icon(
                painter            = painterResource(R.drawable.ic_sidemenu),
                contentDescription = "메뉴",
                modifier           = Modifier.size(24.dp),
                tint               = Color(0xFF000000),
            )
        }
    }
}

// ChatSideMenu — 사이드 메뉴 바텀시트
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSideMenu(
    isAutoTranslate       : Boolean,
    onToggleAutoTranslate : () -> Unit,
    onDismiss             : () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text  = "채팅 설정",
                style = TextStyle(
                    fontSize   = 17.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = TextPrimary,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF8F8F8))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text  = "자동번역",
                        style = TextStyle(
                            fontSize   = 15.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight(500),
                            color      = TextPrimary,
                        ),
                    )
                    Text(
                        text  = if (isAutoTranslate) "상대 메시지를 한국어로 자동 번역 중"
                        else "말풍선 옆 번역 아이콘을 탭하면 번역",
                        style = TextStyle(fontSize = 12.sp, color = TextSecondary),
                    )
                }
                Switch(
                    checked         = isAutoTranslate,
                    onCheckedChange = { onToggleAutoTranslate() },
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = AccentBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCCCCCC),
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// TranslationLoadingDots — 번역 로딩 애니메이션 (ic_transload × 3)
@Composable
fun TranslationLoadingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "transload")

    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue  = 0.25f,
                targetValue   = 1f,
                animationSpec = infiniteRepeatable(
                    animation          = tween(500),
                    repeatMode         = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 166),
                ),
                label = "dot_$index",
            )
            Icon(
                painter            = painterResource(R.drawable.ic_transload),
                contentDescription = null,
                modifier           = Modifier.size(8.dp),
                tint               = AccentBlue.copy(alpha = alpha),
            )
        }
    }
}

// MessageList
@Composable
fun MessageList(
    messages              : List<ChatMessage>,
    isAutoTranslate       : Boolean,
    showTranslation       : Map<String, Boolean>,
    translatingMessageIds : Set<String>,
    playingMessageId      : String?,
    modifier              : Modifier = Modifier,
    listState             : androidx.compose.foundation.lazy.LazyListState,
    onPlayVoice           : (String) -> Unit,
    onToggleTranslation   : (String) -> Unit,
    onLongPressMessage    : (ChatMessage) -> Unit = {},
    onSaveScript          : (ChatMessage) -> Unit,
) {
    // 날짜별 그룹핑
    val grouped = remember(messages) {
        messages.groupBy { it.timestamp.toLocalDate() }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yy. MM. dd (E)") }

    LazyColumn(
        state             = listState,
        modifier          = modifier.fillMaxWidth(),
        contentPadding    = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        grouped.forEach { (date, msgs) ->
            // 날짜 구분선
            item(key = date.toString()) {
                DateDivider(label = date.format(dateFormatter))
            }

            items(msgs, key = { it.id }) { msg ->
                val isMe    = msg.senderId == "me"
                val showTr  = showTranslation[msg.id] ?: isAutoTranslate
                val playing = playingMessageId == msg.id

                when (msg.type) {
                    MessageType.TEXT ->
                        TextMessageBubble(
                            message             = msg,
                            isMe                = isMe,
                            isAutoTranslate     = isAutoTranslate,
                            showTranslation     = showTr,
                            isTranslating       = msg.id in translatingMessageIds,
                            onToggleTranslation = { onToggleTranslation(msg.id) },
                            onSaveScript        = { onSaveScript(msg) },
                            onLongPress         = { onLongPressMessage(msg) },
                        )

                    MessageType.VOICE ->
                        VoiceMessageBubble(
                            message             = msg,
                            isMe                = isMe,
                            isAutoTranslate     = isAutoTranslate,
                            isPlaying           = playing,
                            showTranslation     = showTr,
                            isTranslating       = msg.id in translatingMessageIds,
                            onPlay              = { onPlayVoice(msg.id) },
                            onToggleTranslation = { onToggleTranslation(msg.id) },
                            onLongPress         = { onLongPressMessage(msg) },
                        )

                    MessageType.IMAGE ->
                        // TODO: 이미지 메시지 UI 구현
                        Box(modifier = Modifier.height(0.dp))
                }
            }
        }
    }
}

// DateDivider
@Composable
fun DateDivider(label: String) {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(DateBadgeBg)
                .padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
            Text(
                text  = label,
                style = TextStyle(
                    fontSize   = 12.sp,
                    fontFamily = PretendardFamily,
                    color      = Color(0xFF666666),
                ),
            )
        }
    }
}

// TextMessageBubble — 상대=왼쪽(아바타+이름+흰말풍선) / 나=오른쪽(연파랑말풍선)
@Composable
fun TextMessageBubble(
    message             : ChatMessage,
    isMe                : Boolean,
    isAutoTranslate     : Boolean,
    showTranslation     : Boolean,
    isTranslating       : Boolean,
    onToggleTranslation : () -> Unit,
    onSaveScript        : () -> Unit,
    onLongPress         : () -> Unit = {},
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("a hh:mm") }

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 상대 아바타
            if (!isMe) {
                // TODO: 실제 프로필 이미지 — coil AsyncImage(model = avatarUrl)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCCCCCC)),
                )
                Spacer(Modifier.width(8.dp))
            }

            Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                // 발신자 이름 (상대만)
                if (!isMe) {
                    Text(
                        text  = message.senderName,
                        style = TextStyle(
                            fontSize   = 12.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight(500),
                            color      = TextSecondary,
                        ),
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }

                // 말풍선 + 번역 버튼 행
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 말풍선
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart     = if (isMe) 16.dp else 4.dp,
                                    topEnd       = if (isMe) 4.dp  else 16.dp,
                                    bottomStart  = 16.dp,
                                    bottomEnd    = 16.dp,
                                )
                            )
                            .background(if (isMe) BubbleBlue else BubbleWhite)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .widthIn(max = 260.dp)
                            .combinedClickable(
                                onClick     = { /* 기존 동작 */ },
                                onLongClick = { onLongPress() },
                            ),
                    ) {
                        Text(
                            text  = message.text,
                            style = TextStyle(
                                fontSize   = 15.sp,
                                fontFamily = PretendardFamily,
                                color      = TextPrimary,
                                lineHeight  = 22.sp,
                            ),
                        )
                    }

                    // 번역 버튼: 상대 메시지 & 자동번역 OFF일 때만
                    if (!isMe && !isAutoTranslate) {
                        Spacer(Modifier.width(4.dp))
                        TranslateIconButton(onClick = onToggleTranslation)
                    }
                }
            }

            // 시간 + 스크립트 저장
            if (isMe) {
                Spacer(Modifier.width(4.dp))
            } else {
                Spacer(Modifier.width(4.dp))
            }
        }

        // 시간 표시
        Text(
            text    = message.timestamp.format(timeFormatter),
            style   = TextStyle(fontSize = 11.sp, color = TextSecondary),
            modifier = Modifier.padding(
                start = if (!isMe) 46.dp else 0.dp,
                top   = 2.dp,
            ),
        )

        // 번역 결과 (세로바 + 텍스트 or 로딩 점)
        if (isTranslating || (showTranslation && message.translatedText.isNotEmpty())) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier              = Modifier
                    .padding(start = if (!isMe) 46.dp else 0.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (isMe) BubbleBlue else BubbleWhite),
                )
                if (isTranslating) {
                    TranslationLoadingDots()
                } else {
                    Text(
                        text  = message.translatedText,
                        style = TextStyle(
                            fontSize   = 13.sp,
                            fontFamily = PretendardFamily,
                            color      = TextTranslated,
                            lineHeight  = 20.sp,
                        ),
                    )
                }
            }
        }
    }
}

// 번역 아이콘 버튼
@Composable
fun TranslateIconButton(onClick: () -> Unit) {
    Box(
        modifier         = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color(0xFFEEEEEE))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter            = painterResource(R.drawable.ic_translate),
            contentDescription = "번역",
            modifier           = Modifier.size(18.dp),
            tint               = AccentBlue,
        )
    }
}

// VoiceMessageBubble — 재생버튼(파란원) + 파형(Canvas) + 타이머 / STT 텍스트 하단
@Composable
fun VoiceMessageBubble(
    message             : ChatMessage,
    isMe                : Boolean,
    isAutoTranslate     : Boolean,
    isPlaying           : Boolean,
    showTranslation     : Boolean,
    isTranslating       : Boolean,
    onPlay              : () -> Unit,
    onToggleTranslation : () -> Unit,
    onLongPress         : () -> Unit = {},
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("a hh:mm") }
    val bubbleBg  = if (isMe) BubbleMyVoice else BubbleWhite
    val textColor = if (isMe) Color.White else TextPrimary

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!isMe) {
                // TODO: 실제 프로필 이미지
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCCCCCC)),
                )
                Spacer(Modifier.width(8.dp))
            }

            Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                if (!isMe) {
                    Text(
                        text  = message.senderName,
                        style = TextStyle(
                            fontSize   = 12.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight(500),
                            color      = TextSecondary,
                        ),
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }

                // 번역 버튼 + 말풍선
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart    = if (isMe) 16.dp else 4.dp,
                                    topEnd      = if (isMe) 4.dp  else 16.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd   = 16.dp,
                                )
                            )
                            .background(bubbleBg)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .widthIn(min = 200.dp, max = 260.dp)
                            .combinedClickable(
                                onClick     = { /* 기존 동작 */ },
                                onLongClick = { onLongPress() },
                            ),
                    ) {
                        Column {
                            // 재생 버튼 + 파형
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // 재생/정지 버튼
                                Box(
                                    modifier         = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isMe) Color.White else AccentBlue)
                                        .clickable(onClick = onPlay),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    // TODO icon: ic_play.xml / ic_pause.xml 교체
                                    Text(
                                        text  = if (isPlaying) "⏸" else "▶",
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            color    = if (isMe) AccentBlue else Color.White,
                                        ),
                                    )
                                }

                                // 파형 Canvas
                                WaveformView(
                                    modifier  = Modifier
                                        .weight(1f)
                                        .height(24.dp),
                                    isPlaying = isPlaying,
                                    progress  = 0f, // TODO: VoicePlayer에서 진행률 StateFlow 노출 후 연동
                                    barColor  = if (isMe) Color.White.copy(alpha = 0.8f) else AccentBlue,
                                )

                                // 타이머
                                Text(
                                    text  = formatDuration(message.voiceDurationSec),
                                    style = TextStyle(
                                        fontSize   = 11.sp,
                                        fontFamily = PretendardFamily,
                                        color      = if (isMe) Color.White.copy(0.8f) else TextSecondary,
                                    ),
                                )
                            }

                            // STT 텍스트 (번역 ON이거나 재생 중일 때)
                            if (message.text.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                HorizontalDivider(
                                    color     = if (isMe) Color.White.copy(0.3f) else DividerGray,
                                    thickness = 0.5.dp,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text  = message.text,
                                    style = TextStyle(
                                        fontSize   = 13.sp,
                                        fontFamily = PretendardFamily,
                                        color      = textColor,
                                        lineHeight  = 19.sp,
                                    ),
                                )
                            }

                            // 번역 결과 (세로바 + 텍스트 or 로딩 점)
                            if (isTranslating || (showTranslation && message.translatedText.isNotEmpty())) {
                                Spacer(Modifier.height(6.dp))
                                if (isTranslating) {
                                    TranslationLoadingDots()
                                } else {
                                    Text(
                                        text  = message.translatedText,
                                        style = TextStyle(
                                            fontSize   = 12.sp,
                                            fontFamily = PretendardFamily,
                                            color      = if (isMe) Color.White.copy(0.75f) else TextTranslated,
                                            lineHeight  = 18.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    // 번역 버튼: 상대 메시지 & 자동번역 OFF일 때만
                    if (!isMe && !isAutoTranslate) {
                        Spacer(Modifier.width(4.dp))
                        TranslateIconButton(onClick = onToggleTranslation)
                    }
                }
            }
        }

        Text(
            text     = message.timestamp.format(timeFormatter),
            style    = TextStyle(fontSize = 11.sp, color = TextSecondary),
            modifier = Modifier.padding(
                start = if (!isMe) 46.dp else 0.dp,
                top   = 2.dp,
            ),
        )
    }
}

// WaveformView — 파형 Canvas 컴포저블
@Composable
fun WaveformView(
    modifier  : Modifier = Modifier,
    isPlaying : Boolean,
    progress  : Float,           // 0f~1f
    barColor  : Color,
    barCount  : Int = 30,
) {
    // 더미 파형 (실제: VoicePlayer 진행률 StateFlow 연동 후 교체)
    val heights = remember {
        listOf(0.3f,0.6f,0.8f,0.5f,0.9f,0.7f,0.4f,0.85f,0.6f,0.3f,
            0.7f,0.95f,0.5f,0.6f,0.8f,0.4f,0.7f,0.6f,0.9f,0.5f,
            0.3f,0.75f,0.65f,0.8f,0.4f,0.55f,0.7f,0.45f,0.85f,0.6f)
    }

    Canvas(modifier = modifier) {
        val barWidth = 2.dp.toPx()
        val gap      = (size.width - barCount * barWidth) / (barCount - 1)
        heights.take(barCount).forEachIndexed { i, h ->
            val x       = i * (barWidth + gap)
            val barH    = h * size.height * 0.9f
            val played  = (i.toFloat() / barCount) <= progress
            drawRoundRect(
                color        = if (played) barColor else barColor.copy(alpha = 0.3f),
                topLeft      = Offset(x, (size.height - barH) / 2f),
                size         = Size(barWidth, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
            )
        }
    }
}

// InputBar — + 원형버튼 / 둥근 텍스트필드 / 마이크 아이콘 / 전송 버튼
@Composable
fun InputBar(
    text             : String,
    onTextChange     : (String) -> Unit,
    onSend           : () -> Unit,
    onMicClick       : () -> Unit,
    onPlusClick      : () -> Unit,
    isMediaPanelOpen : Boolean,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(Color.White)
            // TODO padding: 디자인 수치 확정 후 교체 (현재 horizontal 12dp, vertical 8dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // + 버튼
        Box(
            modifier         = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFEEEEEE))
                .clickable(onClick = onPlusClick),
            contentAlignment = Alignment.Center,
        ) {
            // TODO icon: ic_plus.xml
            Text(
                text  = if (isMediaPanelOpen) "×" else "+",
                style = TextStyle(fontSize = 20.sp, color = TextSecondary),
            )
        }

        // 텍스트 필드
        BasicTextField(
            value         = text,
            onValueChange = onTextChange,
            modifier      = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFF2F2F7))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            textStyle     = TextStyle(
                fontSize   = 15.sp,
                fontFamily = PretendardFamily,
                color      = TextPrimary,
            ),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text(
                        text  = "메시지를 입력하세요",
                        style = TextStyle(fontSize = 15.sp, color = TextSecondary),
                    )
                }
                inner()
            },
        )

        // 마이크 or 전송 버튼
        if (text.isEmpty()) {
            // 마이크
            Box(
                modifier         = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEEEEE))
                    .clickable(onClick = onMicClick),
                contentAlignment = Alignment.Center,
            ) {
                // TODO icon: ic_mic.xml
                Icon(
                    painter            = painterResource(R.drawable.ic_mic),
                    contentDescription = "녹음",
                    modifier           = Modifier.size(20.dp),
                    tint               = TextSecondary,
                )
            }
        } else {
            // 전송
            Box(
                modifier         = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AccentBlue)
                    .clickable(onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                // TODO icon: ic_send.xml
                Icon(
                    painter            = painterResource(R.drawable.ic_send),
                    contentDescription = "전송",
                    modifier           = Modifier.size(18.dp),
                    tint               = Color.White,
                )
            }
        }
    }
}

//@Composable
//fun MediaPanel(
//    onPhoto      : () -> Unit,
//    onCamera     : () -> Unit,
//    onCall       : () -> Unit,
//    onFile       : () -> Unit,
//    onSaveScript : () -> Unit,
//) {
//    val items = listOf(
//        Triple("사진",   R.drawable.ic_media_photo,    onPhoto),
//        Triple("카메라", R.drawable.ic_media_camera,   onCamera),
//        Triple("통화",   R.drawable.ic_media_call,     onCall),
//        Triple("파일",   R.drawable.ic_media_file,     onFile),
//        Triple("대화저장", R.drawable.ic_media_script, onSaveScript),
//    )
//
//    Row(
//        modifier              = Modifier
//            .fillMaxWidth()
//            .background(Color.White)
//            .padding(horizontal = 16.dp, vertical = 16.dp),
//        horizontalArrangement = Arrangement.SpaceAround,
//    ) {
//        items.forEach { (label, iconRes, action) ->
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.spacedBy(6.dp),
//                modifier            = Modifier.clickable(onClick = action),
//            ) {
//                Box(
//                    modifier         = Modifier
//                        .size(52.dp)
//                        .clip(CircleShape)
//                        .background(Color(0xFFEEF4FF)),
//                    contentAlignment = Alignment.Center,
//                ) {
//                    Icon(
//                        painter            = painterResource(iconRes),
//                        contentDescription = label,
//                        modifier           = Modifier.size(26.dp),
//                        tint               = AccentBlue,
//                    )
//                }
//                Text(
//                    text  = label,
//                    style = TextStyle(
//                        fontSize   = 11.sp,
//                        fontFamily = PretendardFamily,
//                        color      = TextSecondary,
//                    ),
//                )
//            }
//        }
//    }
//}

// RecordingOverlay — 흰 바텀시트, 마이크(파란 원) + 파동 애니메이션, 타이머, 삭제/일시정지/정지 버튼
@Composable
fun RecordingOverlay(
    seconds    : Int,
    amplitudes : List<Float>,
    onDelete   : () -> Unit,
    onPause    : () -> Unit,
    onStop     : () -> Unit,
) {
    // 파동 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue   = 0.85f,
        targetValue    = 1.15f,
        animationSpec  = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape         = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color         = Color.White,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 드래그 핸들
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFDDDDDD)),
            )

            // 마이크 버튼 + 파동
            Box(
                modifier         = Modifier.size(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                // 파동 원
                Box(
                    modifier = Modifier
                        .size((80 * pulseScale).dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.15f)),
                )
                // 마이크 파란 원
                Box(
                    modifier         = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    // TODO icon: ic_mic_white.xml
                    Icon(
                        painter            = painterResource(R.drawable.ic_mic),
                        contentDescription = "녹음 중",
                        modifier           = Modifier.size(28.dp),
                        tint               = Color.White,
                    )
                }
            }

            // 타이머
            Text(
                text  = formatTimer(seconds),
                style = TextStyle(
                    fontSize   = 20.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(500),
                    color      = TextPrimary,
                ),
            )

            // 파형 (실시간 amplitudes)
            if (amplitudes.isNotEmpty()) {
                RealtimeWaveform(
                    amplitudes = amplitudes,
                    modifier   = Modifier
                        .fillMaxWidth(0.8f)
                        .height(32.dp),
                )
            }

            // 하단 버튼 3개: 삭제 / 일시정지 / 정지
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // 삭제
                RecordActionButton(
                    label    = "삭제",
                    iconRes  = R.drawable.ic_delete,
                    tint     = TextSecondary,
                    onClick  = onDelete,
                )

                // 일시정지
                RecordActionButton(
                    label    = "일시정지",
                    iconRes  = R.drawable.ic_pause,
                    tint     = Color(0xFFFF6B35),   // 오렌지-레드 (디자인 이미지 기준)
                    onClick  = onPause,
                )

                // 정지 (완료)
                RecordActionButton(
                    label    = "완료",
                    iconRes  = R.drawable.ic_stop,
                    tint     = TextSecondary,
                    onClick  = onStop,
                )
            }
        }
    }
}

@Composable
private fun RecordActionButton(
    label   : String,
    iconRes : Int,
    tint    : Color,
    onClick : () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier            = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2F2F7)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter            = painterResource(iconRes),
                contentDescription = label,
                modifier           = Modifier.size(22.dp),
                tint               = tint,
            )
        }
        // TODO: 라벨 표시 여부 — 디자인 이미지에는 없으나 접근성 용도로 보관
    }
}

// RealtimeWaveform — 녹음 중 실시간 파형
@Composable
fun RealtimeWaveform(
    amplitudes : List<Float>,
    modifier   : Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val barW = 3.dp.toPx()
        val gap  = 2.dp.toPx()
        val total = amplitudes.size
        amplitudes.forEachIndexed { i, amp ->
            val x    = i * (barW + gap)
            val barH = (amp.coerceIn(0.05f, 1f)) * size.height * 0.85f
            drawRoundRect(
                color        = AccentBlue,
                topLeft      = Offset(x, (size.height - barH) / 2f),
                size         = Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptSaveSheet(
    message   : ChatMessage,
    step      : Int,
    onNext    : () -> Unit,
    onBack    : () -> Unit,
    onSave    : () -> Unit,
    onDismiss : () -> Unit,
) {
    var memo         by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    // TODO: 실제 폴더 목록 — ScriptViewModel 또는 Repository에서 로드

    val dummyFolders = listOf("영어" to 15, "일본어" to 27)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            // 타이틀
            Text(
                text  = "스크립트 저장 ($step/2)",
                style = TextStyle(
                    fontSize   = 18.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = AccentBlue.copy(alpha = 0.85f), // 숫자 파란색
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                textAlign = TextAlign.Center,
            )

            // 원문 카드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8F8F8))
                    .border(0.5.dp, DividerGray, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Column {
                    Text(
                        text  = message.text,
                        style = TextStyle(
                            fontSize   = 15.sp,
                            fontFamily = PretendardFamily,
                            color      = TextPrimary,
                            lineHeight  = 22.sp,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            text  = "버튼을 눌러 원하는 대로 수정해보세요.",
                            style = TextStyle(fontSize = 12.sp, color = TextSecondary),
                        )
                        // TODO icon: ic_edit.xml
                        OutlinedButton(
                            onClick      = { /* TODO: 원문 편집 */ },
                            shape        = RoundedCornerShape(20.dp),
                            border       = BorderStroke(0.5.dp, DividerGray),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("✏ 수정하기", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 뜻 섹션
            Text(
                text  = "뜻",
                style = TextStyle(
                    fontSize   = 15.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = TextPrimary,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8F8F8))
                    .border(0.5.dp, DividerGray, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Column {
                    Text(
                        text  = message.translatedText.ifEmpty { "(번역 없음)" },
                        style = TextStyle(
                            fontSize   = 14.sp,
                            fontFamily = PretendardFamily,
                            color      = TextPrimary,
                            lineHeight  = 21.sp,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = "${message.translatedText.length}자",
                        style    = TextStyle(fontSize = 11.sp, color = TextSecondary),
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 메모 섹션
            Text(
                text  = "메모",
                style = TextStyle(
                    fontSize   = 15.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = TextPrimary,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8F8F8))
                    .border(0.5.dp, DividerGray, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Column {
                    BasicTextField(
                        value         = memo,
                        onValueChange = { if (it.length <= 150) memo = it },
                        modifier      = Modifier.fillMaxWidth(),
                        textStyle     = TextStyle(
                            fontSize   = 14.sp,
                            fontFamily = PretendardFamily,
                            color      = TextPrimary,
                        ),
                        decorationBox = { inner ->
                            if (memo.isEmpty()) {
                                Text(
                                    text  = "메모를 입력해보세요",
                                    style = TextStyle(fontSize = 14.sp, color = TextSecondary),
                                )
                            }
                            inner()
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = "${memo.length}/150",
                        style    = TextStyle(fontSize = 11.sp, color = TextSecondary),
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 폴더 선택 섹션
            Text(
                text  = "폴더 선택",
                style = TextStyle(
                    fontSize   = 15.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = TextPrimary,
                ),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                dummyFolders.forEach { (name, count) ->
                    val isSelected = selectedFolder == name
                    Box(
                        modifier = Modifier
                            .size(width = 110.dp, height = 80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFCCCCCC))   // TODO: 실제 폴더 커버 이미지
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) AccentBlue else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { selectedFolder = name },
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(name, fontSize = 12.sp, color = Color.White)
                            Text("$count", fontSize = 12.sp, color = Color.White.copy(0.7f))
                        }
                    }
                }

                // + 폴더추가
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(width = 70.dp, height = 80.dp)
                        .clickable { /* TODO: 폴더 추가 화면 이동 */ },
                ) {
                    Box(
                        modifier         = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, DividerGray, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+", fontSize = 20.sp, color = TextSecondary)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("폴더추가", fontSize = 11.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 다음 or 저장 버튼
            Button(
                onClick      = if (step == 1) onNext else onSave,
                modifier     = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape        = RoundedCornerShape(12.dp),
                colors       = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            ) {
                Text(
                    text  = if (step == 1) "다음" else "저장",
                    style = TextStyle(
                        fontSize   = 16.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(600),
                        color      = Color.White,
                    ),
                )
            }

            Spacer(Modifier.height(12.dp))

            // 취소하기 or 이전으로
            TextButton(
                onClick  = if (step == 1) onDismiss else onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text  = if (step == 1) "취소하기" else "이전으로",
                    style = TextStyle(
                        fontSize   = 15.sp,
                        fontFamily = PretendardFamily,
                        color      = if (step == 1) Color(0xFFFF4444) else TextPrimary,
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// SaveOptionDialog — 통합/따로 저장 선택
@Composable
fun SaveOptionDialog(onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(0) }   // 0=통합, 1=따로

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape         = RoundedCornerShape(20.dp),
            color         = Color.White,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text  = "저장옵션",
                            style = TextStyle(
                                fontSize   = 17.sp,
                                fontFamily = PretendardFamily,
                                fontWeight = FontWeight(600),
                                color      = TextPrimary,
                            ),
                        )
                        Text(
                            text  = "항목을 통합 또는 개별 저장할 수 있습니다.",
                            style = TextStyle(fontSize = 12.sp, color = TextSecondary),
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        // TODO icon: ic_close.xml
                        Text("×", fontSize = 20.sp, color = TextSecondary)
                    }
                }

                Spacer(Modifier.height(16.dp))

                listOf("통합 저장", "따로 저장").forEachIndexed { idx, label ->
                    val isSel = selected == idx
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSel) 1.5.dp else 0.5.dp,
                                color = if (isSel) AccentBlue else DividerGray,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .background(if (isSel) AccentBlue.copy(0.05f) else Color.Transparent)
                            .clickable { selected = idx }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            text  = label,
                            style = TextStyle(
                                fontSize   = 15.sp,
                                fontFamily = PretendardFamily,
                                color      = if (isSel) AccentBlue else TextPrimary,
                            ),
                        )
                        Box(
                            modifier         = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSel) AccentBlue else Color.Transparent)
                                .border(1.dp, if (isSel) AccentBlue else DividerGray, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSel) {
                                Text("✓", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                    if (idx == 0) Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

private fun formatTimer(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "우원재 채팅방")
@Composable
private fun PreviewWoowonjai() {
    val room     = dummyChatRooms.find { it.id == "r_woowonjai" }!!
    val messages = dummyMessages["r_woowonjai"] ?: emptyList()
    ChatRoomContent(
        state                 = ChatRoomUiState(room = room, messages = messages),
        onBack                = {},
        onInputChange         = {},
        onSendText            = {},
        onStartRecording      = {},
        onStopRecording       = {},
        onCancelRecording     = {},
        onPlayVoice           = {},
        onToggleTranslation   = {},
        onToggleAutoTranslate = {},
        onToggleMediaPanel    = {},
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "우원재 채팅방 자동번역 ON")
@Composable
private fun PreviewWoowonajaiAutoTranslate() {
    val room     = dummyChatRooms.find { it.id == "r_woowonjai" }!!
    val messages = dummyMessages["r_woowonjai"] ?: emptyList()
    ChatRoomContent(
        state                 = ChatRoomUiState(room = room, messages = messages, isAutoTranslate = true),
        onBack                = {},
        onInputChange         = {},
        onSendText            = {},
        onStartRecording      = {},
        onStopRecording       = {},
        onCancelRecording     = {},
        onPlayVoice           = {},
        onToggleTranslation   = {},
        onToggleAutoTranslate = {},
        onToggleMediaPanel    = {},
    )
}
