@file:Suppress("DEPRECATION")

package com.everybuddy.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.everybuddy.app.R
import com.everybuddy.app.ui.chat.ChatListScreen
import com.everybuddy.app.ui.chat.ChatRoomScreen
import com.everybuddy.app.ui.chat.ChatViewModel
import com.everybuddy.app.ui.chat.DirectChatScreen
import com.everybuddy.app.ui.chat.GroupChatScreen
import com.everybuddy.app.ui.chat.NewFolderScreen
import com.everybuddy.app.ui.chat.ScriptFolder
import com.everybuddy.app.ui.chat.ScriptItem
import com.everybuddy.app.ui.chat.ScriptSaveItem
import com.everybuddy.app.ui.explore.ExploreScreen
import com.everybuddy.app.ui.explore.MyViewModel
import com.everybuddy.app.ui.friend.FriendMainScreen
import com.everybuddy.app.ui.friend.FriendViewModel
import com.everybuddy.app.ui.my.MyPageScreen
import com.everybuddy.app.ui.notification.NotificationScreen
import com.everybuddy.app.ui.script.ScriptDetailScreen
import com.everybuddy.app.ui.script.ScriptFolderScreen
import com.everybuddy.app.ui.script.ScriptMainScreen
import com.everybuddy.app.ui.script.ScriptViewModel
import com.everybuddy.app.ui.theme.PretendardFamily

private object MainRoute {
    const val CHAT_LIST = "chat_list"
    const val CHAT_ROOM = "chat_room/{roomId}"
    fun chatRoom(roomId: String) = "chat_room/$roomId"
}

private enum class MainTab(val label: String, val iconRes: Int) {
    CHAT    ("대화",    R.drawable.ic_nav_chat),
    FRIEND  ("친구",    R.drawable.ic_nav_friend),
    EXPLORE ("탐색",    R.drawable.ic_nav_find),
    SCRIPT  ("스크립트", R.drawable.ic_nav_script),
    MY      ("마이",    R.drawable.ic_nav_my),
}

private val AccentBlue  = Color(0xFF0167FF)
private val NavInactive = Color(0xFFAAAAAA)
private val BorderGray  = Color(0xFFE5E5E5)

@Composable
fun MainScreen(onLogout: () -> Unit = {}) {
    var selectedTab       by remember { mutableStateOf(MainTab.CHAT) }
    var isChatRoomOpen    by remember { mutableStateOf(false) }
    var showNotification  by remember { mutableStateOf(false) }
    var showDirectChat    by remember { mutableStateOf(false) }
    var showGroupChat     by remember { mutableStateOf(false) }

    val chatNavController = rememberNavController()
    val scriptViewModel   : ScriptViewModel     = hiltViewModel()
    val chatVm            : ChatViewModel       = hiltViewModel()
    val attendanceVm      : AttendanceViewModel = hiltViewModel()

    LaunchedEffect(Unit) { attendanceVm.markAttendanceIfNeeded() }

    var selectedScriptItem    by remember { mutableStateOf<ScriptItem?>(null) }
    var selectedScriptFolder  by remember { mutableStateOf<ScriptFolder?>(null) }
    var showNewFolderInScript by remember { mutableStateOf(false) }

    if (showNotification) {
        NotificationScreen(onBack = { showNotification = false })
        return
    }

    Scaffold(
        bottomBar = {
            if (!isChatRoomOpen) {
                Box(
                    modifier = Modifier
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Color.White)
                        .navigationBarsPadding(),
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().height(64.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        MainTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val tint       = if (isSelected) AccentBlue else NavInactive
                            Column(
                                modifier            = Modifier.clickable { selectedTab = tab }.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(painterResource(tab.iconRes), tab.label, Modifier.size(28.dp), tint = tint)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    tab.label,
                                    style = TextStyle(
                                        fontSize   = 10.sp,
                                        fontFamily = PretendardFamily,
                                        color      = tint,
                                        fontWeight = if (isSelected) FontWeight(600) else FontWeight(400),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedTab) {

                MainTab.CHAT -> {
                    NavHost(navController = chatNavController, startDestination = MainRoute.CHAT_LIST) {
                        composable(MainRoute.CHAT_LIST) {
                            isChatRoomOpen = false
                            ChatListScreen(
                                viewModel           = chatVm,
                                onRoomClick         = { room ->
                                    chatVm.markRoomAsRead(room.id)
                                    chatNavController.navigate(MainRoute.chatRoom(room.id))
                                },
                                onStartChat         = {},
                                onStartDirectChat   = { showDirectChat = true },
                                onStartGroupChat    = { showGroupChat = true },
                                onNotificationClick = { showNotification = true },
                                onNavigateToExplore = { selectedTab = MainTab.EXPLORE },
                            )
                        }
                        composable(
                            route     = MainRoute.CHAT_ROOM,
                            arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
                        ) { backStack ->
                            val roomId = backStack.arguments?.getString("roomId") ?: return@composable
                            val chatListState by chatVm.listState.collectAsState()
                            val room = chatListState.rooms.find { it.id == roomId }
                            val roomName = room?.name ?: ""
                            val isGroup  = room?.isGroup ?: false
                            isChatRoomOpen = true
                            ChatRoomScreen(
                                roomId                = roomId,
                                roomName              = roomName,
                                isGroup               = isGroup,
                                onBack                = { isChatRoomOpen = false; chatNavController.popBackStack() },
                                onNavigateToScriptTab = {
                                    isChatRoomOpen = false
                                    chatNavController.popBackStack()
                                    selectedTab = MainTab.SCRIPT
                                },
                                onSaveScriptItem = { item ->
                                    scriptViewModel.saveScriptItem(
                                        originalText   = item.originalText,
                                        translatedText = item.translatedText,
                                        memo1          = item.memo1,
                                        memo2          = item.memo2,
                                        folderId       = item.selectedFolder,
                                    )
                                },
                                onMuteChanged   = { isMuted -> chatVm.updateRoomMute(roomId, isMuted) },
                                onFolderCreated = { folder -> scriptViewModel.addFolder(folder.name, folder.coverImage.ifEmpty { null }, folder.id) },
                                isStarred       = chatListState.rooms.find { it.id == roomId }?.isStarred ?: false,
                                onToggleStar    = { chatVm.toggleStar(roomId) },
                            )
                        }
                    }

                    if (showDirectChat) {
                        DirectChatScreen(
                            onBack        = { showDirectChat = false },
                            onRoomCreated = { room ->
                                showDirectChat = false
                                isChatRoomOpen = true
                                chatVm.markRoomAsRead(room.id)
                                chatNavController.navigate(MainRoute.chatRoom(room.id))
                            },
                            chatVm   = chatVm,
                        )
                    }

                    if (showGroupChat) {
                        GroupChatScreen(
                            onBack        = { showGroupChat = false },
                            onRoomCreated = { room ->
                                showGroupChat = false
                                isChatRoomOpen = true
                                chatVm.markRoomAsRead(room.id)
                                chatNavController.navigate(MainRoute.chatRoom(room.id))
                            },
                            chatVm   = chatVm,
                        )
                    }
                }

                MainTab.FRIEND -> {
                    isChatRoomOpen = false
                    val friendVm: FriendViewModel = hiltViewModel()
                    FriendMainScreen(
                        viewModel       = friendVm,
                        onAddFriend     = { selectedTab = MainTab.EXPLORE },
                        onStartChat     = { user ->
                            chatVm.createChatRoom(
                                roomName       = user.name,
                                isGroup        = false,                       // 친구 화면에서 시작 = 1:1
                                participantIds = listOf(user.userId),
                                onSuccess      = { room ->
                                    isChatRoomOpen = true
                                    chatVm.markRoomAsRead(room.id)
                                    chatNavController.navigate(MainRoute.chatRoom(room.id))
                                    selectedTab = MainTab.CHAT
                                },
                                onError        = {},
                            )
                        },
                        onNotification  = { showNotification = true },
                        // 답장 흐름은 FriendViewModel.sendReply가 직접 chat REST 호출 — 채팅방 자동 이동 X.
                        // 사용자는 토스트 보고 채팅 탭 가서 확인.
                        onReplyToStatus = { _, _ -> },
                    )
                }

                MainTab.EXPLORE -> {
                    isChatRoomOpen = false
                    ExploreScreen(
                        onStartChat    = { selectedTab = MainTab.CHAT },
                        onNotification = { showNotification = true },
                    )
                }

                MainTab.SCRIPT -> {
                    isChatRoomOpen = false
                    when {
                        selectedScriptItem != null -> {
                            val scriptUiStateDetail by scriptViewModel.uiState.collectAsState()
                            ScriptDetailScreen(
                                item     = selectedScriptItem!!,
                                folders  = scriptUiStateDetail.folders,
                                onBack   = { selectedScriptItem = null },
                                onSave   = { updatedItem ->
                                    scriptViewModel.updateItem(updatedItem)
                                    selectedScriptItem = updatedItem
                                },
                                onDelete = { item ->
                                    scriptViewModel.deleteItem(item.id)
                                    selectedScriptItem = null
                                },
                            )
                        }
                        selectedScriptFolder != null -> {
                            val scriptUiState by scriptViewModel.uiState.collectAsState()
                            ScriptFolderScreen(
                                folder      = selectedScriptFolder!!,
                                allItems    = scriptUiState.items,
                                onBack      = { selectedScriptFolder = null },
                                onItemClick = { item -> selectedScriptItem = item },
                            )
                        }
                        else -> {
                            val scriptUiStateMain by scriptViewModel.uiState.collectAsState()
                            ScriptMainScreen(
                                viewModel      = scriptViewModel,
                                isRefreshing   = scriptUiStateMain.isRefreshing,
                                onRefresh      = scriptViewModel::refresh,
                                onFolderClick  = { folder -> selectedScriptFolder = folder },
                                onItemClick    = { item -> selectedScriptItem = item },
                                onAddFolder    = { showNewFolderInScript = true },
                                onNotification = { showNotification = true },
                            )
                            if (showNewFolderInScript) {
                                Dialog(
                                    onDismissRequest = { showNewFolderInScript = false },
                                    properties       = DialogProperties(usePlatformDefaultWidth = false),
                                ) {
                                    NewFolderScreen(
                                        roomName = "스크립트",
                                        onBack   = { showNewFolderInScript = false },
                                        onSave   = { name, coverUri ->
                                            scriptViewModel.addFolder(name, coverUri)
                                            showNewFolderInScript = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                MainTab.MY -> {
                    isChatRoomOpen = false
                    val myVm: MyViewModel = hiltViewModel()
                    MyPageScreen(
                        viewModel      = myVm,
                        onNotification = { showNotification = true },
                        onLogout       = onLogout,
                    )
                }
            }
        }
    }
}
