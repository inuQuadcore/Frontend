@file:Suppress("DEPRECATION")

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

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.media.MediaMetadataRetriever
import coil.compose.AsyncImage
import com.everybuddy.app.data.cache.UserSummary
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuddy.app.R
import com.everybuddy.app.data.chat.*
import com.everybuddy.app.ui.theme.PretendardFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
    roomId                : String,
    roomName              : String                   = "",
    isGroup               : Boolean                  = false,
    onBack                : () -> Unit,
    onNavigateToScriptTab : () -> Unit               = {},
    onSaveScriptItem      : (ScriptSaveItem) -> Unit = {},
    onMuteChanged         : (Boolean) -> Unit        = {},
    onFolderCreated       : (ScriptFolder) -> Unit   = {},
    isStarred             : Boolean                  = false,
    onToggleStar          : () -> Unit               = {},
    viewModel             : ChatRoomViewModel         = hiltViewModel(),
) {
    LaunchedEffect(roomId) { viewModel.loadRoom(roomId, roomName, isGroup) }
    val state by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(state.translationError) {
        state.translationError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeTranslationError()
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.onStartRecording() }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> if (bitmap != null) viewModel.onCameraCapture() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.onFilePicked(uri.toString()) }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) takePictureLauncher.launch(null)
    }

    val multiPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.onPhotosPicked(uris)
    }

    val storagePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) filePickerLauncher.launch("*/*")
    }

    if (state.isConversationSelectOpen) {
        ConversationSelectScreen(
            messages      = state.messages,
            myUserId      = state.myUserId,
            userSummaries = state.userSummaries,
            roomName      = state.room.name,
            onBack   = viewModel::onDismissConversationSelect,
            onSave   = viewModel::onConversationSelected,
        )
        return
    }

    ChatRoomContent(
        state                    = state,
        onBack                   = onBack,
        onInputChange            = viewModel::onInputChange,
        onSendText               = viewModel::onSendText,
        onStartRecording         = { micPermLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onStopRecording          = viewModel::onStopRecording,
        onCancelRecording        = viewModel::onCancelRecording,
        onPauseRecording         = viewModel::onPauseRecording,
        onPlayVoice              = viewModel::onPlayVoice,
        onToggleTranslation      = viewModel::onToggleTranslation,
        onTapImage               = viewModel::onTapImage,
        onTapVideo             = viewModel::onOpenVideoPlayer,
        onCloseVideoPlayer     = viewModel::onCloseVideoPlayer,
        onImageAppeared          = viewModel::onImageAppeared,
        onCloseFullscreenImage   = viewModel::onCloseFullscreenImage,
        onToggleAutoTranslate    = viewModel::onToggleAutoTranslate,
        onToggleMuteRoom         = {
            viewModel.onToggleMuteRoom()
            onMuteChanged(!state.room.isMuted)
        },
        onToggleMediaPanel       = viewModel::onToggleMediaPanel,
        onLongPressMessage       = viewModel::onLongPressMessage,
        onDismissContextMenu     = viewModel::onDismissContextMenu,
        onOpenPhotoPicker        = {
            // 시스템 PhotoPicker는 자체 권한 처리 — 별도 권한 요청 불필요.
            multiPhotoLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                )
            )
        },
        onClosePhotoPicker       = viewModel::onClosePhotoPicker,
        onTogglePhotoSelection   = viewModel::onTogglePhotoSelection,
        onSendSelectedPhotos     = viewModel::onSendSelectedPhotos,
        onOpenConversationSelect = viewModel::onOpenConversationSelect,
        onStartScriptSave         = viewModel::onStartScriptSave,
        onDismissScriptSave       = viewModel::onDismissScriptSave,
        onDismissConversationSave = viewModel::onDismissConversationSave,
        onConversationSaved       = viewModel::onConversationSaved,
        onScriptSaved             = viewModel::onScriptSaved,
        onSaveScriptItem          = onSaveScriptItem,
        onOpenCamera             = { cameraPermLauncher.launch(Manifest.permission.CAMERA) },
        onOpenFile               = {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES
            else
                Manifest.permission.READ_EXTERNAL_STORAGE
            storagePermLauncher.launch(perm)
        },
        onFolderCreated          = onFolderCreated,
        onDeleteMessage          = viewModel::onDeleteMessage,
        onEditMessage            = viewModel::onEditMessage,
        onInviteMembers          = viewModel::inviteMembers,
        onRetryMessage           = viewModel::retryMessage,
        onStartReply             = viewModel::startReply,
        onCancelReply            = viewModel::cancelReply,
        isStarred                = isStarred,
        onToggleStar             = onToggleStar,
    )
}

// ChatRoomContent — 전체 레이아웃
@Composable
fun ChatRoomContent(
    state                     : ChatRoomUiState,
    onBack                    : () -> Unit,
    onInputChange             : (String) -> Unit,
    onSendText                : () -> Unit,
    onStartRecording          : () -> Unit,
    onStopRecording           : () -> Unit,
    onCancelRecording         : () -> Unit,
    onPauseRecording          : () -> Unit            = {},
    onPlayVoice               : (String) -> Unit,
    onToggleTranslation       : (String) -> Unit,
    onTapImage                : (String) -> Unit           = {},
    onTapVideo               : (String) -> Unit           = {},
    onCloseVideoPlayer       : () -> Unit                 = {},
    onImageAppeared           : (String) -> Unit           = {},
    onCloseFullscreenImage    : () -> Unit                 = {},
    onToggleAutoTranslate     : () -> Unit,
    onToggleMuteRoom          : () -> Unit            = {},
    onToggleMediaPanel        : () -> Unit,
    onLongPressMessage        : (ChatMessage) -> Unit  = {},
    onDismissContextMenu      : () -> Unit             = {},
    onOpenPhotoPicker         : () -> Unit             = {},
    onClosePhotoPicker        : () -> Unit             = {},
    onTogglePhotoSelection    : (Int) -> Unit          = {},
    onSendSelectedPhotos      : () -> Unit             = {},
    onOpenConversationSelect  : () -> Unit             = {},
    onStartScriptSave         : (String) -> Unit       = {},
    onDismissScriptSave       : () -> Unit             = {},
    onDismissConversationSave : () -> Unit             = {},
    onConversationSaved       : () -> Unit             = {},
    onScriptSaved             : () -> Unit              = {},
    onSaveScriptItem          : (ScriptSaveItem) -> Unit = {},
    onOpenCamera              : () -> Unit             = {},
    onOpenFile                : () -> Unit             = {},
    onFolderCreated           : (ScriptFolder) -> Unit = {},
    onDeleteMessage           : (String) -> Unit       = {},
    onEditMessage             : (String, String) -> Unit = { _, _ -> },
    onInviteMembers           : (List<Long>) -> Unit   = {},
    onRetryMessage            : (String) -> Unit       = {},
    onStartReply              : (ChatMessage) -> Unit  = {},
    onCancelReply             : () -> Unit             = {},
    isStarred                 : Boolean                = false,
    onToggleStar              : () -> Unit             = {},
) {
    var showNewFolderScreen   by remember { mutableStateOf(false) }
    var newFolderAutoSelectId by remember { mutableStateOf<String?>(null) }
    var separateIdx           by remember { mutableIntStateOf(0) }
    var pendingScriptItems    by remember { mutableStateOf<List<ScriptSaveItem>>(emptyList()) }
    var localFolders          by remember { mutableStateOf(emptyList<ScriptFolder>()) }
    var editingMessageId      by remember { mutableStateOf<String?>(null) }
    var editingText           by remember { mutableStateOf("") }
    var isMenuOpen            by remember { mutableStateOf(false) }
    val inputFocusRequester   = remember { FocusRequester() }
    var isSearchMode          by remember { mutableStateOf(false) }
    var searchQuery           by remember { mutableStateOf("") }
    var currentMatchIdx       by remember { mutableIntStateOf(0) }
    var mediaSubPage          by remember { mutableStateOf<String?>(null) }
    var showInviteScreen      by remember { mutableStateOf(false) }

    val members = remember(state.room.participants, state.userSummaries, state.myUserId) {
        state.room.participants.map { p ->
            val summary = state.userSummaries[p.id]
            ChatMember(
                id              = p.id.toString(),
                name            = summary?.name.orEmpty(),
                profileImageUrl = p.profileImageUrl ?: summary?.profileImageUrl,
                isMe            = p.id == state.myUserId,
            )
        }
    }

    LaunchedEffect(state.conversationSaveMessages) { separateIdx = 0; pendingScriptItems = emptyList() }

    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()

    val searchResults = remember(searchQuery, state.messages) {
        if (searchQuery.isBlank()) emptyList()
        else state.messages.filter { it.text.contains(searchQuery, ignoreCase = true) }
    }

    val highlightedMessageId = if (isSearchMode && searchResults.isNotEmpty())
        searchResults.getOrNull(currentMatchIdx)?.id
    else null

    // 검색어 변경 시 인덱스 초기화
    LaunchedEffect(searchQuery) { currentMatchIdx = 0 }

    // 검색 결과 위치로 스크롤
    LaunchedEffect(currentMatchIdx, highlightedMessageId) {
        val targetId = highlightedMessageId ?: return@LaunchedEffect
        val grouped = state.messages.groupBy { it.timestamp.toLocalDate() }
        var flatIdx = 0
        loop@ for ((_, msgs) in grouped) {
            flatIdx++
            for (msg in msgs) {
                if (msg.id == targetId) {
                    scope.launch { listState.animateScrollToItem((flatIdx - 1).coerceAtLeast(0)) }
                    break@loop
                }
                flatIdx++
            }
        }
    }

    // 새 메시지 도착 시 최하단 스크롤 (검색 중이 아닐 때만)
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty() && !isSearchMode) {
            scope.launch {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }
    }

    LaunchedEffect(state.replyToMessage) {
        if (state.replyToMessage != null) {
            try { inputFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray)
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            ChatAppBar(
                roomName   = state.room.name,
                onBack     = onBack,
                onSearch   = { isSearchMode = !isSearchMode; if (!isSearchMode) searchQuery = "" },
                onMenu     = { isMenuOpen = true },
            )

            AnimatedVisibility(visible = isSearchMode, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BasicTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier      = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF2F2F7))
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        textStyle     = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = TextPrimary),
                        singleLine    = true,
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) Text("메시지 검색", style = TextStyle(fontSize = 14.sp, color = TextSecondary))
                            inner()
                        },
                    )
                    if (searchQuery.isNotBlank()) {
                        if (searchResults.isNotEmpty()) {
                            Text(
                                "${currentMatchIdx + 1}/${searchResults.size}",
                                style = TextStyle(fontSize = 12.sp, color = TextSecondary, fontFamily = PretendardFamily),
                            )
                            IconButton(
                                onClick  = { if (currentMatchIdx > 0) currentMatchIdx-- },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(painterResource(R.drawable.ic_chevron_right), "이전",
                                    Modifier.size(16.dp), tint = if (currentMatchIdx > 0) AccentBlue else TextSecondary)
                            }
                            IconButton(
                                onClick  = { if (currentMatchIdx < searchResults.size - 1) currentMatchIdx++ },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(painterResource(R.drawable.ic_chevron_right), "다음",
                                    Modifier.size(16.dp), tint = if (currentMatchIdx < searchResults.size - 1) AccentBlue else TextSecondary)
                            }
                        } else {
                            Text("결과 없음", style = TextStyle(fontSize = 12.sp, color = TextSecondary, fontFamily = PretendardFamily))
                        }
                    }
                }
            }

            HorizontalDivider(color = DividerGray, thickness = 0.5.dp)

            MessageList(
                messages              = state.messages,
                myUserId              = state.myUserId,
                members               = members,
                userSummaries         = state.userSummaries,
                isAutoTranslate       = state.isAutoTranslate,
                showTranslation       = state.showTranslation,
                translatingMessageIds = state.translatingMessageIds,
                playingMessageId      = state.playingMessageId,
                playPositionMs        = state.playPositionMs,
                modifier              = Modifier.weight(1f),
                listState             = listState,
                onPlayVoice           = onPlayVoice,
                onToggleTranslation   = onToggleTranslation,
                onTapImage            = onTapImage,
                onTapVideo            = onTapVideo,
                onImageAppeared       = onImageAppeared,
                onLongPressMessage    = onLongPressMessage,
                onSaveScript          = { msg -> onStartScriptSave(msg.id) },
                onRetryMessage        = onRetryMessage,
                searchQuery           = if (isSearchMode) searchQuery else "",
                highlightedMessageId  = highlightedMessageId,
            )

            AnimatedVisibility(
                visible = state.isMediaPanelOpen && !state.isRecording,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut(),
            ) {
                MediaPanel(
                    onPhoto      = onOpenPhotoPicker,
                    onCamera     = onOpenCamera,
                    onCall       = { /* TODO: 통화 */ },
                    onFile       = onOpenFile,
                    onSaveScript = onOpenConversationSelect,
                )
            }

            state.replyToMessage?.let { reply ->
                ReplyPreviewBar(
                    previewText = reply.text.ifBlank { "[음성 메시지]" },
                    onCancel    = onCancelReply,
                )
            }

            if (!state.isRecording) {
                InputBar(
                    text              = state.inputText,
                    onTextChange      = onInputChange,
                    onSend            = onSendText,
                    onMicClick        = onStartRecording,
                    onPlusClick       = onToggleMediaPanel,
                    isMediaPanelOpen  = state.isMediaPanelOpen,
                    focusRequester    = inputFocusRequester,
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
                isPaused   = state.isRecordingPaused,
                onDelete   = onCancelRecording,
                onSend     = onStopRecording,
                onPause    = onPauseRecording,
            )
        }

        AnimatedVisibility(
            visible  = state.isPhotoPickerOpen,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
        ) {
            PhotoPickerSheet(
                selectedIndices   = state.selectedPhotoIndices,
                onToggleSelection = onTogglePhotoSelection,
                onSend            = onSendSelectedPhotos,
                onDismiss         = onClosePhotoPicker,
            )
        }

        state.fullscreenImage?.let { img ->
            FullscreenImageViewer(image = img, onClose = onCloseFullscreenImage)
        }
        state.videoPlayerMessageId?.let { msgId ->
            val videoMsg = state.messages.find { it.id == msgId }
            if (videoMsg != null) {
                val senderName = state.userSummaries[videoMsg.senderId.toLongOrNull()]?.name ?: ""
                VideoPlayerScreen(
                    message           = videoMsg,
                    senderName        = senderName,
                    subtitles         = state.videoSubtitles[msgId] ?: emptyList(),
                    isSubtitleLoading = msgId in state.subtitleLoadingIds,
                    subtitleProgress  = state.subtitleProgress[msgId],
                    onClose           = onCloseVideoPlayer,
                )
            }
        }

        state.contextMenuMessage?.let { msg ->
            val isOwn = msg.senderId.toLongOrNull() == state.myUserId
            MessageContextMenu(
                onDismiss    = onDismissContextMenu,
                onCopy       = { /* TODO: 클립보드 복사 */ },
                onSelectCopy = { /* TODO: 텍스트 선택 복사 UI */ },
                onReply      = { onStartReply(msg); onDismissContextMenu() },
                onSaveScript = {
                    onDismissContextMenu()
                    onStartScriptSave(msg.id)
                },
                isOwnMessage = isOwn,
                onEdit       = {
                    editingMessageId = msg.id
                    editingText      = msg.text
                },
                onDelete     = { onDeleteMessage(msg.id) },
            )
        }

        if (editingMessageId != null) {
            Box(
                modifier         = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { editingMessageId = null },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Surface(
                    modifier        = Modifier.fillMaxWidth().clickable {},
                    color           = Color.White,
                    shadowElevation = 8.dp,
                ) {
                    Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("메시지 수정", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = TextPrimary), modifier = Modifier.padding(bottom = 8.dp))
                        BasicTextField(
                            value         = editingText,
                            onValueChange = { editingText = it },
                            modifier      = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF2F2F2)).padding(12.dp),
                            textStyle     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = TextPrimary),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { editingMessageId = null }) {
                                Text("취소", style = TextStyle(fontSize = 14.sp, color = TextSecondary))
                            }
                            TextButton(onClick = {
                                val id = editingMessageId
                                if (id != null && editingText.isNotBlank()) {
                                    onEditMessage(id, editingText)
                                    editingMessageId = null
                                }
                            }) {
                                Text("완료", style = TextStyle(fontSize = 14.sp, color = AccentBlue, fontWeight = FontWeight(600)))
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible  = state.savedToastVisible,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
            enter = fadeIn(),
            exit  = fadeOut(),
        ) {
            SavedToast()
        }

        AnimatedVisibility(
            visible = isMenuOpen,
            enter   = fadeIn(),
            exit    = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { isMenuOpen = false },
            )
        }

        AnimatedVisibility(
            visible  = isMenuOpen,
            modifier = Modifier.align(Alignment.CenterEnd).width(300.dp).fillMaxHeight(),
            enter    = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
            exit     = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }),
        ) {
            ChatRoomSidebarScreen(
                roomName              = state.room.name,
                isGroup               = state.room.isGroup,
                members               = members,
                mediaThumbs           = emptyList(),
                isAutoTranslate       = state.isAutoTranslate,
                isMuted               = state.room.isMuted,
                isStarred             = isStarred,
                onBack                = { isMenuOpen = false },
                onPhotoVideo          = { isMenuOpen = false; mediaSubPage = "photo" },
                onLink                = { isMenuOpen = false; mediaSubPage = "link" },
                onFile                = { isMenuOpen = false; mediaSubPage = "file" },
                onInviteMember        = { isMenuOpen = false; showInviteScreen = true },
                onToggleAutoTranslate = onToggleAutoTranslate,
                onToggleMute          = onToggleMuteRoom,
                onToggleStar          = onToggleStar,
                onDataDelete          = { isMenuOpen = false },
                onLeaveRoom           = { isMenuOpen = false },
            )
        }
        if (mediaSubPage != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                when (mediaSubPage) {
                    "photo" -> MediaPhotoScreen(onBack = { mediaSubPage = null })
                    "link"  -> MediaLinkScreen(onBack  = { mediaSubPage = null })
                    "file"  -> MediaFileScreen(onBack  = { mediaSubPage = null })
                }
            }
        }

        if (showInviteScreen) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                InviteMemberScreen(
                    roomId    = state.room.id,
                    onBack    = { showInviteScreen = false },
                    onInvited = { ids ->
                        onInviteMembers(ids)
                        showInviteScreen = false
                    },
                )
            }
        }
    }   // Box 끝

    state.scriptSaveMessage?.let { msg ->
        ScriptSaveSheet(
            message                    = msg,
            folders                    = localFolders,
            onSave                     = { item ->
                onSaveScriptItem(item)
                onScriptSaved()
            },
            onDismiss                  = onDismissScriptSave,
            onAddFolder                = { showNewFolderScreen = true },
            externalAutoSelectFolderId = newFolderAutoSelectId,
        )
    }

    if (state.conversationSaveMessages.isNotEmpty()) {
        if (state.conversationCaptureOption == CaptureOption.SEPARATE) {
            val msg = state.conversationSaveMessages.getOrNull(separateIdx)
            if (msg != null) {
                val isLast = separateIdx == state.conversationSaveMessages.size - 1
                ScriptSaveSheet(
                    message                    = msg,
                    folders                    = localFolders,
                    onSave                     = { item ->
                        if (!isLast) {
                            pendingScriptItems = pendingScriptItems + item
                            separateIdx++
                        } else {
                            (pendingScriptItems + item).forEach { onSaveScriptItem(it) }
                            pendingScriptItems = emptyList()
                            onConversationSaved()
                        }
                    },
                    onDismiss                  = onDismissConversationSave,
                    onAddFolder                = { showNewFolderScreen = true },
                    externalAutoSelectFolderId = newFolderAutoSelectId,
                    currentIdx                 = separateIdx + 1,
                    totalCount                 = state.conversationSaveMessages.size,
                    isLastStep                 = isLast,
                )
            }
        } else {
            val combinedText = state.conversationSaveMessages.joinToString("\n") { it.text }
            ScriptSaveSheet(
                message                    = ChatMessage(id = "combined_save", text = combinedText),
                folders                    = localFolders,
                externalAutoSelectFolderId = newFolderAutoSelectId,
                onSave                     = { item ->
                    onSaveScriptItem(item)
                    onConversationSaved()
                },
                onDismiss                  = onDismissConversationSave,
                onAddFolder                = { showNewFolderScreen = true },
            )
        }
    }

    if (showNewFolderScreen) {
        Dialog(
            onDismissRequest = { showNewFolderScreen = false },
            properties       = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            NewFolderScreen(
                roomName = state.room.name,
                onBack   = { showNewFolderScreen = false },
                onSave   = { name, coverUri ->
                    val newFolder = ScriptFolder(id = java.util.UUID.randomUUID().toString(), name = name, coverImage = coverUri ?: "")
                    localFolders          = localFolders + newFolder
                    newFolderAutoSelectId = newFolder.id
                    showNewFolderScreen   = false
                    onFolderCreated(newFolder)
                },
            )
        }
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
    Column(modifier = Modifier.background(Color.White).statusBarsPadding()) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
    ) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier.size(40.dp).align(Alignment.CenterStart),
        ) {
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
            modifier = Modifier.align(Alignment.Center),
        )

        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
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
    }
}

// ChatSideMenu — 우측 슬라이드 사이드바
@Composable
fun ChatSideMenu(
    roomName              : String  = "",
    isAutoTranslate       : Boolean,
    isMuted               : Boolean = false,
    onToggleAutoTranslate : () -> Unit,
    onToggleMute          : () -> Unit = {},
    onDismiss             : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color.White)
            .systemBarsPadding()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (roomName.isNotEmpty()) {
            Text(
                text     = roomName,
                style    = TextStyle(
                    fontSize   = 17.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = TextPrimary,
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            HorizontalDivider(color = DividerGray, thickness = 0.5.dp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = if (isAutoTranslate) "자동번역 끄기" else "자동번역 켜기",
                style = TextStyle(
                    fontSize   = 15.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(500),
                    color      = TextPrimary,
                ),
            )
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
        HorizontalDivider(color = DividerGray, thickness = 0.5.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = "알림",
                style = TextStyle(
                    fontSize   = 15.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(500),
                    color      = TextPrimary,
                ),
            )
            Switch(
                checked         = !isMuted,
                onCheckedChange = { onToggleMute() },
                colors          = SwitchDefaults.colors(
                    checkedThumbColor   = Color.White,
                    checkedTrackColor   = AccentBlue,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFCCCCCC),
                ),
            )
        }
        HorizontalDivider(color = DividerGray, thickness = 0.5.dp)

        Text(
            text     = "데이터 삭제",
            style    = TextStyle(
                fontSize   = 15.sp,
                fontFamily = PretendardFamily,
                color      = Color(0xFFFF3333),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDismiss() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
        HorizontalDivider(color = DividerGray, thickness = 0.5.dp)

        Text(
            text     = "채팅방 나가기",
            style    = TextStyle(
                fontSize   = 15.sp,
                fontFamily = PretendardFamily,
                color      = Color(0xFFFF3333),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDismiss() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

// TranslationLoadingDots — 번역 로딩 애니메이션 (원형 점 × 3)
@Composable
fun TranslationLoadingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "transload")
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue  = 0.25f,
                targetValue   = 1f,
                animationSpec = infiniteRepeatable(
                    animation          = tween(480),
                    repeatMode         = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 160),
                ),
                label = "dot_$index",
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(AccentBlue.copy(alpha = alpha)),
            )
        }
    }
}

// MessageList
@Composable
fun MessageList(
    messages              : List<ChatMessage>,
    myUserId              : Long,
    members               : List<ChatMember>,
    userSummaries         : Map<Long, UserSummary>,
    isAutoTranslate       : Boolean,
    showTranslation       : Map<String, Boolean>,
    translatingMessageIds : Set<String>,
    playingMessageId      : String?,
    playPositionMs        : Long     = 0L,
    modifier              : Modifier = Modifier,
    listState             : androidx.compose.foundation.lazy.LazyListState,
    onPlayVoice           : (String) -> Unit,
    onToggleTranslation   : (String) -> Unit,
    onTapImage            : (String) -> Unit       = {},
    onTapVideo            : (String) -> Unit       = {},
    onImageAppeared       : (String) -> Unit       = {},
    onLongPressMessage    : (ChatMessage) -> Unit = {},
    onSaveScript          : (ChatMessage) -> Unit,
    onRetryMessage        : (String) -> Unit = {},
    searchQuery           : String  = "",
    highlightedMessageId  : String? = null,
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
                val isMe    = msg.senderId.toLongOrNull() == myUserId
                val showTr  = showTranslation[msg.id] ?: isAutoTranslate
                val playing = playingMessageId == msg.id
                val summary = msg.senderId.toLongOrNull()?.let { userSummaries[it] }
                val senderName            = summary?.name.orEmpty()
                val senderProfileImageUrl = summary?.profileImageUrl
                // 상태메시지 답장 인용 카드: 내가 보냈으면 상대 프로필, 상대가 보냈으면 내 프로필
                val statusAuthor = if (isMe) members.firstOrNull { !it.isMe } else members.firstOrNull { it.isMe }
                val statusAuthorName       = statusAuthor?.name.orEmpty()
                val statusAuthorProfileUrl = statusAuthor?.profileImageUrl

                when (msg.type) {
                    MessageType.TEXT ->
                        TextMessageBubble(
                            message                = msg,
                            isMe                   = isMe,
                            senderName             = senderName,
                            senderProfileImageUrl  = senderProfileImageUrl,
                            statusAuthorName       = statusAuthorName,
                            statusAuthorProfileUrl = statusAuthorProfileUrl,
                            isAutoTranslate        = isAutoTranslate,
                            showTranslation        = showTr,
                            isTranslating          = msg.id in translatingMessageIds,
                            onToggleTranslation    = { onToggleTranslation(msg.id) },
                            onSaveScript           = { onSaveScript(msg) },
                            onLongPress            = { onLongPressMessage(msg) },
                            onRetry                = { onRetryMessage(msg.id) },
                            searchQuery            = searchQuery,
                            isHighlighted          = msg.id == highlightedMessageId,
                        )

                    MessageType.VOICE ->
                        VoiceMessageBubble(
                            message               = msg,
                            isMe                  = isMe,
                            senderName            = senderName,
                            senderProfileImageUrl = senderProfileImageUrl,
                            isAutoTranslate       = isAutoTranslate,
                            isPlaying             = playing,
                            playPositionMs        = if (playing) playPositionMs else 0L,
                            showTranslation       = showTr,
                            isTranslating         = msg.id in translatingMessageIds,
                            onPlay                = { onPlayVoice(msg.id) },
                            onToggleTranslation   = { onToggleTranslation(msg.id) },
                            onLongPress           = { onLongPressMessage(msg) },
                        )

                    MessageType.IMAGE ->
                        ImageMessageBubble(
                            message               = msg,
                            isMe                  = isMe,
                            senderName            = senderName,
                            senderProfileImageUrl = senderProfileImageUrl,
                            onTap                 = { onTapImage(msg.id) },
                            onAppeared            = { onImageAppeared(msg.id) },
                            onLongPress           = { onLongPressMessage(msg) },
                        )

                    MessageType.VIDEO ->
                        VideoMessageBubble(
                            message               = msg,
                            isMe                  = isMe,
                            senderName            = senderName,
                            senderProfileImageUrl = senderProfileImageUrl,
                            onTap                 = { onTapVideo(msg.id) },
                            onLongPress           = { onLongPressMessage(msg) },
                        )

                    MessageType.FILE ->
                        FileMessageBubble(
                            message               = msg,
                            isMe                  = isMe,
                            senderName            = senderName,
                            senderProfileImageUrl = senderProfileImageUrl,
                            onLongPress           = { onLongPressMessage(msg) },
                        )
                }
            }
        }
    }
}

/** 메시지 옆 송신자 아바타. profileImageUrl 있으면 Coil로 로드, 없으면 ic_nav_my 기본 아바타. */
@Composable
private fun SenderAvatar(
    profileImageUrl    : String?,
    contentDescription : String,
    size               : Dp,
) {
    Box(
        modifier         = Modifier.size(size).clip(CircleShape).background(Color(0xFFD0D0D0)),
        contentAlignment = Alignment.Center,
    ) {
        if (!profileImageUrl.isNullOrBlank()) {
            AsyncImage(
                model              = profileImageUrl,
                contentDescription = contentDescription,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        } else {
            Icon(painterResource(R.drawable.ic_nav_my), null, Modifier.size(size * 0.55f), tint = Color(0xFF555555))
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
    message                  : ChatMessage,
    isMe                     : Boolean,
    senderName               : String  = "",
    senderProfileImageUrl    : String? = null,
    statusAuthorName         : String  = "",
    statusAuthorProfileUrl   : String? = null,
    isAutoTranslate          : Boolean,
    showTranslation          : Boolean,
    isTranslating            : Boolean,
    onToggleTranslation      : () -> Unit,
    onSaveScript             : () -> Unit,
    onLongPress              : () -> Unit = {},
    onRetry                  : () -> Unit = {},
    searchQuery              : String  = "",
    isHighlighted            : Boolean = false,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("a hh:mm") }
    val timeText = remember(message.timestamp, message.editedAt) {
        val base = message.timestamp.format(timeFormatter)
        if (message.editedAt != null) "$base · 수정됨" else base
    }
    val isFailed = message.status == "FAILED"

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            repeat(3) {
                shakeOffset.animateTo(6f, tween(60))
                shakeOffset.animateTo(-6f, tween(60))
            }
            shakeOffset.snapTo(0f)
        }
    }

    val displayText = remember(message.text, searchQuery) {
        buildAnnotatedString {
            if (searchQuery.isNotBlank()) {
                val lower  = message.text.lowercase()
                val qLower = searchQuery.lowercase()
                var start  = 0
                var i      = lower.indexOf(qLower, start)
                while (i >= 0) {
                    append(message.text.substring(start, i))
                    withStyle(SpanStyle(background = Color(0xFFFFF59D))) {
                        append(message.text.substring(i, i + searchQuery.length))
                    }
                    start = i + searchQuery.length
                    i     = lower.indexOf(qLower, start)
                }
                append(message.text.substring(start))
            } else {
                append(message.text)
            }
        }
    }

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
    ) {
        if (isMe) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(start = 46.dp),
                verticalAlignment     = Alignment.Bottom,
                horizontalArrangement = Arrangement.End,
            ) {
                if (isFailed) {
                    Text(
                        text     = "전송 실패 · 재시도",
                        style    = TextStyle(fontSize = 9.sp, color = Color(0xFFE53935), fontFamily = PretendardFamily),
                        modifier = Modifier.clickable { onRetry() }.padding(end = 6.dp),
                    )
                }
                Text(
                    text     = timeText,
                    style    = TextStyle(fontSize = 9.sp, color = TextSecondary),
                    modifier = Modifier.padding(end = 4.dp),
                )
                if (message.statusPreview.isNotEmpty()) {
                    val replyCard: @Composable () -> Unit = if (message.isStatusReply) {
                        { StatusReplyCard(message.statusPreview, statusAuthorName, statusAuthorProfileUrl, Modifier.widthIn(max = 260.dp)) }
                    } else {
                        { ChatReplyCard(message.statusPreview, Modifier.widthIn(max = 260.dp)) }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        replyCard()
                        Box(
                            modifier = Modifier
                                .widthIn(max = 260.dp)
                                .offset(y = (-10).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                .background(if (isHighlighted) BubbleBlue.copy(alpha = 0.7f) else BubbleBlue)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .combinedClickable(onClick = {}, onLongClick = { onLongPress() }),
                        ) {
                            Text(
                                text  = displayText,
                                style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = TextPrimary, lineHeight = 22.sp),
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .offset(x = shakeOffset.value.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                            .background(if (isHighlighted) BubbleBlue.copy(alpha = 0.7f) else BubbleBlue)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .combinedClickable(onClick = {}, onLongClick = { onLongPress() }),
                    ) {
                        Text(
                            text  = displayText,
                            style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = TextPrimary, lineHeight = 22.sp),
                        )
                    }
                }
            }
        } else {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(end = 38.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SenderAvatar(
                    profileImageUrl    = senderProfileImageUrl,
                    contentDescription = senderName,
                    size               = 38.dp,
                )
                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text     = senderName,
                        style    = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(500), color = TextSecondary),
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (message.statusPreview.isNotEmpty()) {
                            val replyCard: @Composable () -> Unit = if (message.isStatusReply) {
                                { StatusReplyCard(message.statusPreview, statusAuthorName, statusAuthorProfileUrl, Modifier.widthIn(max = 210.dp)) }
                            } else {
                                { ChatReplyCard(message.statusPreview, Modifier.widthIn(max = 210.dp)) }
                            }
                            Column(horizontalAlignment = Alignment.Start) {
                                replyCard()
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 210.dp)
                                        .offset(y = (-10).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                        .background(if (isHighlighted) BubbleWhite.copy(alpha = 0.7f) else BubbleWhite)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .combinedClickable(onClick = {}, onLongClick = { onLongPress() }),
                                ) {
                                    Text(
                                        text  = displayText,
                                        style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = TextPrimary, lineHeight = 22.sp),
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 210.dp)
                                    .offset(x = shakeOffset.value.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                    .background(if (isHighlighted) BubbleWhite.copy(alpha = 0.7f) else BubbleWhite)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .combinedClickable(onClick = {}, onLongClick = { onLongPress() }),
                            ) {
                                Text(
                                    text  = displayText,
                                    style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = TextPrimary, lineHeight = 22.sp),
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text  = timeText,
                            style = TextStyle(fontSize = 9.sp, color = TextSecondary),
                        )
                        if (!isAutoTranslate) {
                            Spacer(Modifier.width(4.dp))
                            TranslateIconButton(onClick = onToggleTranslation)
                        }
                    }
                }
            }
        }

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
                        style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = TextTranslated, lineHeight = 20.sp),
                    )
                }
            }
        }
    }
}

// ChatReplyCard — 채팅 답장 인용 카드 (텍스트만, 프로필/이름 없음, 최대 20자)
@Composable
private fun ChatReplyCard(preview: String, modifier: Modifier = Modifier) {
    val truncated = if (preview.length > 20) preview.take(20) + "…" else preview
    Surface(
        shape           = RoundedCornerShape(12.dp),
        color           = Color.White,
        border          = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFDDDDDD)),
        shadowElevation = 2.dp,
        modifier        = modifier,
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = truncated,
                style    = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = Color(0xFF888888)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// StatusReplyCard — 상태메시지 답장 인용 카드 (말풍선 위에 표시, 겹침 디자인)
@Composable
private fun StatusReplyCard(
    preview    : String,
    name       : String   = "",
    profileUrl : String?  = null,
    modifier   : Modifier = Modifier,
) {
    val truncated = if (preview.length > 30) preview.take(30) + "…" else preview
    Surface(
        shape           = RoundedCornerShape(12.dp),
        color           = Color.White,
        border          = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFDDDDDD)),
        shadowElevation = 2.dp,
        modifier        = modifier,
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 10.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier         = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFD0D0D0)),
                contentAlignment = Alignment.Center,
            ) {
                if (!profileUrl.isNullOrBlank()) {
                    AsyncImage(model = profileUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(painterResource(R.drawable.ic_nav_my), null, Modifier.size(18.dp), tint = Color(0xFF555555))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                if (name.isNotBlank()) {
                    Text(
                        text     = name,
                        style    = TextStyle(fontSize = 11.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = TextPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text     = truncated,
                    style    = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = Color(0xFF888888)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

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
        Image(
            painter            = painterResource(R.drawable.ic_translate),
            contentDescription = "번역",
            modifier           = Modifier.size(18.dp),
            colorFilter        = androidx.compose.ui.graphics.ColorFilter.tint(AccentBlue),
        )
    }
}

// VoiceMessageBubble — 재생버튼(파란원) + 파형(Canvas) + 타이머 / STT 텍스트 하단
@Composable
fun VoiceMessageBubble(
    message               : ChatMessage,
    isMe                  : Boolean,
    senderName            : String  = "",
    senderProfileImageUrl : String? = null,
    isAutoTranslate       : Boolean,
    isPlaying             : Boolean,
    playPositionMs        : Long    = 0L,
    showTranslation       : Boolean,
    isTranslating         : Boolean,
    onPlay                : () -> Unit,
    onToggleTranslation   : () -> Unit,
    onLongPress           : () -> Unit = {},
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("a hh:mm") }
    val timeText = remember(message.timestamp, message.editedAt) {
        val base = message.timestamp.format(timeFormatter)
        if (message.editedAt != null) "$base · 수정됨" else base
    }
    val bubbleBg  = if (isMe) BubbleMyVoice else BubbleWhite
    val textColor = if (isMe) Color.White else TextPrimary

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
    ) {
        if (isMe) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(start = 46.dp),
                verticalAlignment     = Alignment.Bottom,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text     = timeText,
                    style    = TextStyle(fontSize = 9.sp, color = TextSecondary),
                    modifier = Modifier.padding(end = 4.dp),
                )
                Box(
                    modifier = Modifier
                        .widthIn(min = 200.dp, max = 260.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(bubbleBg)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .combinedClickable(onClick = {}, onLongClick = { onLongPress() }),
                ) {
                    VoiceBubbleContent(
                        message         = message,
                        isMe            = true,
                        isPlaying       = isPlaying,
                        playPositionMs  = playPositionMs,
                        isTranslating   = isTranslating,
                        showTranslation = showTranslation,
                        textColor       = textColor,
                        onPlay          = onPlay,
                    )
                }
            }
        } else {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(end = 38.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SenderAvatar(
                    profileImageUrl    = senderProfileImageUrl,
                    contentDescription = senderName,
                    size               = 38.dp,
                )
                Spacer(Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(
                        text     = senderName,
                        style    = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(500), color = TextSecondary),
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Box(
                            modifier = Modifier
                                .widthIn(min = 160.dp, max = 200.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                .background(bubbleBg)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .combinedClickable(onClick = {}, onLongClick = { onLongPress() }),
                        ) {
                            VoiceBubbleContent(
                                message         = message,
                                isMe            = false,
                                isPlaying       = isPlaying,
                                playPositionMs  = playPositionMs,
                                isTranslating   = isTranslating,
                                showTranslation = showTranslation,
                                textColor       = textColor,
                                onPlay          = onPlay,
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            if (!isAutoTranslate) {
                                Box(
                                    modifier         = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEEEEEE))
                                        .clickable(onClick = onToggleTranslation),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        painter            = painterResource(R.drawable.ic_translate),
                                        contentDescription = "번역",
                                        modifier           = Modifier.size(18.dp),
                                        colorFilter        = androidx.compose.ui.graphics.ColorFilter.tint(AccentBlue),
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                            }
                            Text(
                                text  = timeText,
                                style = TextStyle(fontSize = 9.sp, color = TextSecondary),
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun VoiceBubbleContent(
    message         : ChatMessage,
    isMe            : Boolean,
    isPlaying       : Boolean,
    playPositionMs  : Long    = 0L,
    isTranslating   : Boolean,
    showTranslation : Boolean,
    textColor       : Color,
    onPlay          : () -> Unit,
) {
    val totalMs      = message.voiceDurationSec * 1000L
    val waveProgress = if (isPlaying && totalMs > 0) (playPositionMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
    val displaySec   = if (isPlaying && totalMs > 0) {
        ((totalMs - playPositionMs) / 1000L).coerceAtLeast(0L).toInt()
    } else {
        message.voiceDurationSec
    }

    Column {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier         = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isMe) Color.White else AccentBlue)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                if (isPlaying) {
                    Image(
                        painter            = painterResource(R.drawable.ic_stop),
                        contentDescription = "정지",
                        modifier           = Modifier.size(14.dp),
                        colorFilter        = ColorFilter.tint(if (isMe) AccentBlue else Color.White),
                    )
                } else {
                    Text(
                        text  = "▶",
                        style = TextStyle(fontSize = 11.sp, color = if (isMe) AccentBlue else Color.White),
                    )
                }
            }
            WaveformView(
                modifier      = Modifier.weight(1f).height(24.dp),
                isPlaying     = isPlaying,
                progress      = waveProgress,
                playedColor   = AccentBlue,
                unplayedColor = if (isMe) Color.White.copy(alpha = 0.6f) else AccentBlue.copy(alpha = 0.3f),
                seed          = message.id.hashCode(),
            )
            Text(
                text  = formatDuration(displaySec),
                style = TextStyle(
                    fontSize   = 11.sp,
                    fontFamily = PretendardFamily,
                    color      = if (isMe) Color.White.copy(0.8f) else TextSecondary,
                ),
            )
        }

        // STT: 번역 응답의 sourceText 우선 (백엔드는 음성 본문 텍스트를 별도로 안 보냄)
        val sttText = message.sourceText.ifEmpty { message.text }
        if (sttText.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = if (isMe) Color.White.copy(0.3f) else DividerGray, thickness = 0.5.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                text  = sttText,
                style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = textColor, lineHeight = 19.sp),
            )
        }

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

// ImageMessageBubble — 이미지 메시지 말풍선 (썸네일 + 탭 시 풀스크린 진입)
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ImageMessageBubble(
    message               : ChatMessage,
    isMe                  : Boolean,
    senderName            : String  = "",
    senderProfileImageUrl : String? = null,
    onTap                 : () -> Unit = {},
    onAppeared            : () -> Unit = {},
    onLongPress           : () -> Unit = {},
) {
    LaunchedEffect(message.id) { onAppeared() }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("a hh:mm") }
    val timeText = remember(message.timestamp) { message.timestamp.format(timeFormatter) }

    // Coil 모델: localFilePath 우선, 없으면 fileUrl(=voiceUrl). 둘 다 없으면 placeholder.
    val model: Any? = message.localFilePath
        ?.takeIf { java.io.File(it).exists() }
        ?.let { java.io.File(it) }
        ?: message.voiceUrl.takeIf { it.isNotBlank() }

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
    ) {
        if (isMe) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(start = 46.dp),
                verticalAlignment     = Alignment.Bottom,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text     = timeText,
                    style    = TextStyle(fontSize = 9.sp, color = TextSecondary),
                    modifier = Modifier.padding(end = 4.dp),
                )
                ImageThumb(model, onTap, onLongPress)
            }
        } else {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(end = 38.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SenderAvatar(
                    profileImageUrl    = senderProfileImageUrl,
                    contentDescription = senderName,
                    size               = 38.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text     = senderName,
                        style    = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(500), color = TextSecondary),
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        ImageThumb(model, onTap, onLongPress)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text  = timeText,
                            style = TextStyle(fontSize = 9.sp, color = TextSecondary),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ImageThumb(
    model       : Any?,
    onTap       : () -> Unit,
    onLongPress : () -> Unit,
) {
    Box(
        modifier = Modifier
            .widthIn(max = 220.dp)
            .heightIn(min = 80.dp, max = 280.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE5E5EA))
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
    ) {
        if (model != null) {
            AsyncImage(
                model              = model,
                contentDescription = "사진 메시지",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxWidth(),
            )
        }
    }
}

// VideoMessageBubble — 영상 메시지 말풍선 (썸네일 플레이스홀더 + 재생 버튼)
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VideoMessageBubble(
    message               : ChatMessage,
    isMe                  : Boolean,
    senderName            : String  = "",
    senderProfileImageUrl : String? = null,
    onTap                 : () -> Unit = {},
    onLongPress           : () -> Unit = {},
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("a hh:mm") }
    val timeText = remember(message.timestamp) { message.timestamp.format(timeFormatter) }

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
    ) {
        if (isMe) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(start = 46.dp),
                verticalAlignment     = Alignment.Bottom,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text     = timeText,
                    style    = TextStyle(fontSize = 9.sp, color = TextSecondary),
                    modifier = Modifier.padding(end = 4.dp),
                )
                VideoThumb(onTap = onTap, onLongPress = onLongPress, localFilePath = message.localFilePath, voiceUrl = message.voiceUrl, durationSec = message.voiceDurationSec)
            }
        } else {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(end = 38.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SenderAvatar(
                    profileImageUrl    = senderProfileImageUrl,
                    contentDescription = senderName,
                    size               = 38.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(
                        text     = senderName,
                        style    = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(500), color = TextSecondary),
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        VideoThumb(onTap = onTap, onLongPress = onLongPress, localFilePath = message.localFilePath, voiceUrl = message.voiceUrl, durationSec = message.voiceDurationSec)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text  = timeText,
                            style = TextStyle(fontSize = 9.sp, color = TextSecondary),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun VideoThumb(
    onTap         : () -> Unit,
    onLongPress   : () -> Unit,
    localFilePath : String?,
    voiceUrl      : String = "",
    durationSec   : Int = 0,
) {
    val thumbnail by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, localFilePath, voiceUrl) {
        value = withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                val localValid = !localFilePath.isNullOrBlank() && File(localFilePath).exists()
                if (localValid) {
                    retriever.setDataSource(localFilePath!!)
                } else if (voiceUrl.isNotBlank()) {
                    retriever.setDataSource(voiceUrl, emptyMap())
                } else {
                    return@withContext null
                }
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.asImageBitmap()
                    .also { retriever.release() }
            } catch (_: Exception) { null }
        }
    }

    Box(
        modifier = Modifier
            .width(200.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnail != null) {
            Image(
                bitmap             = thumbnail!!,
                contentDescription = null,
                modifier           = Modifier.fillMaxSize(),
                contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
        // Play button circle
        Box(
            modifier         = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter            = painterResource(R.drawable.ic_play),
                contentDescription = "재생",
                modifier           = Modifier.size(20.dp),
                colorFilter        = ColorFilter.tint(Color(0xFF1A1A1A)),
            )
        }
        // Duration badge
        if (durationSec > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                val m = durationSec / 60
                val s = durationSec % 60
                Text(
                    text  = "%02d:%02d".format(m, s),
                    style = TextStyle(fontSize = 11.sp, color = Color.White, fontFamily = PretendardFamily),
                )
            }
        }
    }
}

// FileMessageBubble — 파일(문서/압축) 메시지 말풍선
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FileMessageBubble(
    message               : ChatMessage,
    isMe                  : Boolean,
    senderName            : String  = "",
    senderProfileImageUrl : String? = null,
    onLongPress           : () -> Unit = {},
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("a hh:mm") }
    val timeText      = remember(message.timestamp) { message.timestamp.format(timeFormatter) }
    val displayName   = message.fileName.ifBlank { "파일" }
    val displaySize   = formatFileSize(message.fileSize)

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
    ) {
        if (isMe) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(start = 46.dp),
                verticalAlignment     = Alignment.Bottom,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text     = timeText,
                    style    = TextStyle(fontSize = 9.sp, color = TextSecondary),
                    modifier = Modifier.padding(end = 4.dp),
                )
                FileChip(displayName, displaySize, isMe, onLongPress)
            }
        } else {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(end = 38.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SenderAvatar(
                    profileImageUrl    = senderProfileImageUrl,
                    contentDescription = senderName,
                    size               = 38.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text     = senderName,
                        style    = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(500), color = TextSecondary),
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        FileChip(displayName, displaySize, isMe, onLongPress)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text  = timeText,
                            style = TextStyle(fontSize = 9.sp, color = TextSecondary),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FileChip(
    fileName    : String,
    fileSize    : String,
    isMe        : Boolean,
    onLongPress : () -> Unit,
) {
    Row(
        modifier = Modifier
            .widthIn(max = 220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isMe) BubbleBlue else BubbleWhite)
            .combinedClickable(onLongClick = onLongPress, onClick = {})
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter            = painterResource(R.drawable.ic_option_file),
            contentDescription = null,
            modifier           = Modifier.size(32.dp),
            colorFilter        = ColorFilter.tint(AccentBlue),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text     = fileName,
                style    = TextStyle(
                    fontSize   = 13.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = TextPrimary,
                ),
                maxLines  = 2,
                overflow  = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (fileSize.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = fileSize,
                    style = TextStyle(fontSize = 11.sp, fontFamily = PretendardFamily, color = TextSecondary),
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    return when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024     -> "%.1f KB".format(bytes / 1_024.0)
        else               -> "$bytes B"
    }
}

// FullscreenImageViewer — 이미지 메시지 탭 시 풀스크린 뷰어 (X 버튼으로 닫기)
@Composable
fun FullscreenImageViewer(
    image   : String,
    onClose : () -> Unit,
) {
    val model: Any = if (image.startsWith("/")) java.io.File(image) else image
    Dialog(
        onDismissRequest = onClose,
        properties       = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model              = model,
                contentDescription = "사진 원본",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter            = painterResource(R.drawable.ic_cancel),
                    contentDescription = "닫기",
                    modifier           = Modifier.size(20.dp),
                )
            }
        }
    }
}

// WaveformView — 파형 Canvas 컴포저블
@Composable
fun WaveformView(
    modifier      : Modifier = Modifier,
    isPlaying     : Boolean,
    progress      : Float,           // 0f~1f
    playedColor   : Color,
    unplayedColor : Color,
    barCount      : Int = 30,
    seed          : Int = 0,
) {
    val heights = remember(seed, barCount) {
        val rng = java.util.Random(seed.toLong())
        List(barCount) { 0.15f + rng.nextFloat() * 0.85f }
    }

    Canvas(modifier = modifier) {
        val barWidth = 2.dp.toPx()
        val gap      = (size.width - barCount * barWidth) / (barCount - 1)
        heights.take(barCount).forEachIndexed { i, h ->
            val x      = i * (barWidth + gap)
            val barH   = h * size.height * 0.9f
            val played = progress > 0f && (i.toFloat() / barCount) < progress
            drawRoundRect(
                color        = if (played) playedColor else unplayedColor,
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
    focusRequester   : FocusRequester? = null,
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
        val fieldModifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFF2F2F7))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
        BasicTextField(
            value         = text,
            onValueChange = onTextChange,
            modifier      = fieldModifier,
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

@Composable
fun MediaPanel(
    onPhoto      : () -> Unit,
    onCamera     : () -> Unit,
    onCall       : () -> Unit,
    onFile       : () -> Unit,
    onSaveScript : () -> Unit,
) {
    val items = listOf(
        Triple("사진/동영상", R.drawable.ic_option_photo, onPhoto),
        Triple("카메라",  R.drawable.ic_option_camera, onCamera),
        Triple("파일",    R.drawable.ic_option_file,   onFile),
        Triple("대화저장", R.drawable.ic_option_script, onSaveScript),
    )

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        items.forEach { (label, iconRes, action) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier            = Modifier.clickable(onClick = action),
            ) {
                Box(
                    modifier         = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEF4FF)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (iconRes == R.drawable.ic_option_camera) {
                        Image(
                            painter            = painterResource(iconRes),
                            contentDescription = label,
                            modifier           = Modifier.size(26.dp),
                        )
                    } else {
                        Icon(
                            painter            = painterResource(iconRes),
                            contentDescription = label,
                            modifier           = Modifier.size(26.dp),
                            tint               = AccentBlue,
                        )
                    }
                }
                Text(
                    text  = label,
                    style = TextStyle(
                        fontSize   = 11.sp,
                        fontFamily = PretendardFamily,
                        color      = TextSecondary,
                    ),
                )
            }
        }
    }
}

@Composable
fun PhotoPickerSheet(
    selectedIndices   : Set<Int>,
    onToggleSelection : (Int) -> Unit,
    onSend            : () -> Unit,
    onDismiss         : () -> Unit,
) {
    Surface(
        modifier        = Modifier.fillMaxWidth().wrapContentHeight(),
        shape           = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color           = Color.White,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
            ) {
                Text(
                    text  = "사진/동영상",
                    style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = TextPrimary),
                    modifier = Modifier.align(Alignment.Center),
                )
                Text(
                    text     = "닫기",
                    style    = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = TextSecondary),
                    modifier = Modifier.align(Alignment.CenterEnd).clickable(onClick = onDismiss),
                )
            }

            LazyVerticalGrid(
                columns        = GridCells.Fixed(3),
                modifier       = Modifier.height(280.dp).padding(horizontal = 2.dp),
                contentPadding = PaddingValues(2.dp),
            ) {
                itemsIndexed(List(18) { it }) { index, _ ->
                    val isSelected = selectedIndices.contains(index)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFDDDDDD))
                            .clickable { onToggleSelection(index) },
                    ) {
                        if (isSelected) {
                            val order = selectedIndices.sorted().indexOf(index) + 1
                            Box(
                                modifier         = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(AccentBlue),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text  = "$order",
                                    style = TextStyle(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight(700)),
                                )
                            }
                        }
                    }
                }
            }

            if (selectedIndices.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Button(
                        onClick = onSend,
                        shape   = RoundedCornerShape(50.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    ) {
                        Text("전송 (${selectedIndices.size})", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = Color.White, fontWeight = FontWeight(600)))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationSaveSheet(
    messages                   : List<ChatMessage>,
    captureOption              : CaptureOption,
    folders                    : List<ScriptFolder>,
    onSave                     : (String?) -> Unit,
    onDismiss                  : () -> Unit,
    onAddFolder                : () -> Unit = {},
    externalAutoSelectFolderId : String?    = null,
) {
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(externalAutoSelectFolderId) {
        if (externalAutoSelectFolderId != null) selectedFolder = externalAutoSelectFolderId
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFDDDDDD)))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                text  = "대화 저장 (${messages.size}개)",
                style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = TextPrimary),
            )

            Text(
                text  = "저장할 폴더를 선택하세요",
                style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = TextSecondary),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                folders.forEach { folder ->
                    val isSel = selectedFolder == folder.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, if (isSel) AccentBlue else DividerGray, RoundedCornerShape(10.dp))
                            .background(if (isSel) AccentBlue.copy(0.08f) else Color.Transparent)
                            .clickable { selectedFolder = folder.id }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text  = folder.name,
                            style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = if (isSel) AccentBlue else TextPrimary),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, DividerGray, RoundedCornerShape(10.dp))
                        .clickable(onClick = onAddFolder)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("+ 새 폴더", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = TextSecondary))
                }
            }

            Button(
                onClick  = { onSave(selectedFolder) },
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("저장하기", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color.White))
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ReplyPreviewBar(previewText: String, onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFEEEEEE))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = previewText,
                style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = TextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Image(
                painter          = painterResource(R.drawable.ic_cancel),
                contentDescription = "취소",
                modifier         = Modifier.size(18.dp).clickable(onClick = onCancel),
            )
        }
    }
}

// RecordingOverlay — 흰 바텀시트, 마이크(파란 원) + 파동 애니메이션, 타이머, 삭제/일시정지/정지 버튼
@Composable
fun RecordingOverlay(
    seconds    : Int,
    amplitudes : List<Float>,
    isPaused   : Boolean = false,
    onDelete   : () -> Unit,
    onPause    : () -> Unit,
    onSend     : () -> Unit,
) {
    var elapsed by remember { mutableIntStateOf(seconds) }
    LaunchedEffect(isPaused) {
        if (!isPaused) {
            while (true) {
                kotlinx.coroutines.delay(1_000L)
                elapsed++
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val rawPulseScale by infiniteTransition.animateFloat(
        initialValue   = 0.85f,
        targetValue    = 1.15f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val pulseScale = if (isPaused) 1f else rawPulseScale

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
                text  = formatTimer(elapsed),
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

                // 전송
                RecordActionButton(
                    label    = "전송",
                    iconRes  = R.drawable.ic_send,
                    tint     = AccentBlue,
                    onClick  = onSend,
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

