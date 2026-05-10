package com.everybuddy.app.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.everybuddy.app.R
import com.everybuddy.app.ui.chat.ChatListScreen
import com.everybuddy.app.ui.chat.ChatRoomScreen
import com.everybuddy.app.ui.chat.ConversationSelectScreen
import com.everybuddy.app.ui.chat.NewFolderScreen
import com.everybuddy.app.ui.chat.StartChatScreen
import com.everybuddy.app.ui.explore.ExploreScreen
import com.everybuddy.app.ui.friend.FriendMainScreen
import com.everybuddy.app.ui.friend.FriendViewModel
import com.everybuddy.app.ui.my.MyPageScreen
import com.everybuddy.app.ui.explore.MyViewModel
import com.everybuddy.app.ui.script.ScriptMainScreen
import com.everybuddy.app.ui.script.ScriptViewModel
import com.everybuddy.app.ui.theme.PretendardFamily
import com.everybuddy.app.data.chat.dummyChatRooms
import com.everybuddy.app.data.chat.dummyMessages
import androidx.compose.foundation.background

// BottomTab 정의
sealed class BottomTab(
    val route   : String,
    val label   : String,
    val iconRes : Int,
) {
    data object Chat    : BottomTab("tab_chat",    "대화",    R.drawable.ic_nav_chat)
    data object Friend  : BottomTab("tab_friend",  "친구",    R.drawable.ic_nav_friend)
    data object Explore : BottomTab("tab_explore", "찾기",    R.drawable.ic_nav_find)
    data object Script  : BottomTab("tab_script",  "스크립트", R.drawable.ic_nav_script)
    data object My      : BottomTab("tab_my",      "마이",    R.drawable.ic_nav_my)
}

val bottomTabs = listOf(
    BottomTab.Chat,
    BottomTab.Friend,
    BottomTab.Explore,
    BottomTab.Script,
    BottomTab.My,
)

// 탭 화면은 자체 BottomNavBar를 가지므로 MainScreen EBBottomBar 숨김
// Chat 탭(ChatListScreen)은 자체 BottomNavBar 없으므로 EBBottomBar 사용
private val hideBottomBarRoutes = setOf(
    "tab_friend",
    "tab_explore",
    "tab_script",
    "tab_my",
    "start_chat",
    "chat_room/{roomId}",
    "conversation_select/{roomId}",
    "script_save/{roomId}",
    "conversation_script_save/{roomId}",
    "new_folder/{roomId}",
)

// MainScreen
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route

    val showBottomBar = currentRoute !in hideBottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                EBBottomBar(
                    tabs        = bottomTabs,
                    currentDest = navBackStack?.destination,
                    onTabClick  = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                )
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = BottomTab.Chat.route,
            modifier         = Modifier.padding(innerPadding),
        ) {

            // 채팅 탭
            composable(BottomTab.Chat.route) {
                ChatListScreen(
                    onRoomClick = { room ->
                        navController.navigate("chat_room/${room.id}")
                    },
                    onStartChat = { navController.navigate("start_chat") },
                )
            }

            // 채팅 시작 화면
            composable("start_chat") {
                StartChatScreen(
                    onBack    = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() },
                )
            }

            // 채팅방 화면
            composable("chat_room/{roomId}") { backStack ->
                val roomId = backStack.arguments?.getString("roomId") ?: return@composable
                ChatRoomScreen(
                    roomId                   = roomId,
                    onBack                   = { navController.popBackStack() },
                    onNavigateToScriptSave   = { messageId ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("pending_message_id", messageId)
                        navController.navigate("script_save/$roomId")
                    },
                    onNavigateToConversation = {
                        navController.navigate("conversation_select/$roomId")
                    },
                )
            }

            // 대화 선택 화면 (다중 스크립트 저장)
            composable("conversation_select/{roomId}") { backStack ->
                val roomId   = backStack.arguments?.getString("roomId") ?: return@composable
                val messages = dummyMessages[roomId] ?: emptyList()
                val room     = dummyChatRooms.find { it.id == roomId }
                ConversationSelectScreen(
                    messages  = messages,
                    roomName  = room?.name ?: "",
                    onBack    = { navController.popBackStack() },
                    onSave    = { selectedMessages, captureOption ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("selected_message_ids", selectedMessages.map { it.id })
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("capture_option", captureOption.name)
                        navController.navigate("conversation_script_save/$roomId")
                    },
                )
            }

            // 단일 메시지 스크립트 저장
            composable("script_save/{roomId}") { backStack ->
                val roomId    = backStack.arguments?.getString("roomId") ?: return@composable
                val messageId = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("pending_message_id")
                val message   = dummyMessages[roomId]?.find { it.id == messageId }
                    ?: dummyMessages[roomId]?.firstOrNull()
                if (message == null) {
                    navController.popBackStack()
                    return@composable
                }
                // TODO: ScriptSaveSheet 또는 ScriptSaveScreen 연동 추가요망
                // 현재 저장은 ChatRoomScreen 내 BottomSheet에서 처리됨
                navController.popBackStack()
            }

            // 다중 선택 스크립트 저장
            composable("conversation_script_save/{roomId}") { backStack ->
                val roomId     = backStack.arguments?.getString("roomId") ?: return@composable
                val allMessages = dummyMessages[roomId] ?: emptyList()
                val selectedIds = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<List<String>>("selected_message_ids")
                    ?: emptyList()
                val messages = allMessages.filter { it.id in selectedIds }
                    .ifEmpty { allMessages.take(1) }
                // TODO: ScriptSaveSheet 또는 ScriptSaveScreen 연동 추가요망
                navController.popBackStack()
            }

            // 폴더 추가 화면
            composable("new_folder/{roomId}") { backStack ->
                val roomId = backStack.arguments?.getString("roomId") ?: return@composable
                val room   = dummyChatRooms.find { it.id == roomId }
                NewFolderScreen(
                    roomName = room?.name ?: "",
                    onBack   = { navController.popBackStack() },
                    onSave   = { _, _ ->
                        // TODO: ScriptViewModel.addFolder 연동 추가요망
                        navController.popBackStack()
                    },
                )
            }

            // 친구 탭
            composable(BottomTab.Friend.route) {
                val friendVm: FriendViewModel = hiltViewModel()
                FriendMainScreen(
                    viewModel         = friendVm,
                    onNavigateToChat  = { navController.navigate(BottomTab.Chat.route) { launchSingleTop = true; restoreState = true } },
                    onNavigateToFind  = { navController.navigate(BottomTab.Explore.route) { launchSingleTop = true; restoreState = true } },
                    onNavigateToScript= { navController.navigate(BottomTab.Script.route) { launchSingleTop = true; restoreState = true } },
                    onNavigateToMy    = { navController.navigate(BottomTab.My.route) { launchSingleTop = true; restoreState = true } },
                    onFriendClick     = { /* TODO: 친구 프로필 화면 이동 추가요망 */ },
                    onAddFriend       = { /* TODO: 친구 추가 화면 이동 추가요망 */ },
                )
            }

            // 탐색 탭
            composable(BottomTab.Explore.route) {
                ExploreScreen(
                    onNavigateToChat   = { navController.navigate(BottomTab.Chat.route) { launchSingleTop = true; restoreState = true } },
                    onNavigateToFriend = { navController.navigate(BottomTab.Friend.route) { launchSingleTop = true; restoreState = true } },
                    onNavigateToScript = { navController.navigate(BottomTab.Script.route) { launchSingleTop = true; restoreState = true } },
                    onNavigateToMy     = { navController.navigate(BottomTab.My.route) { launchSingleTop = true; restoreState = true } },
                )
            }

            // 스크립트 탭
            composable(BottomTab.Script.route) {
                val scriptVm: ScriptViewModel = hiltViewModel()
                ScriptMainScreen(
                    viewModel          = scriptVm,
                    onNavigateToChat   = { navController.navigate(BottomTab.Chat.route) { launchSingleTop = true; restoreState = true } },
                    onNavigateToFriend = { navController.navigate(BottomTab.Friend.route) { launchSingleTop = true; restoreState = true } },
                    onNavigateToFind   = { navController.navigate(BottomTab.Explore.route) { launchSingleTop = true; restoreState = true } },
                    onNavigateToMy     = { navController.navigate(BottomTab.My.route) { launchSingleTop = true; restoreState = true } },
                    onAddFolder        = {
                        // TODO: 폴더 추가 전용 화면 이동 추가요망 (현재 폴더 추가 화면은 채팅방 컨텍스트에 묶여있음)
                    },
                    onFolderClick      = { /* TODO: 폴더 상세 화면 추가요망 */ },
                    onItemClick        = { /* TODO: 스크립트 상세 화면 추가요망 */ },
                    onItemAudio        = { /* TODO: TTS 재생 추가요망 */ },
                )
            }

            // 마이 탭
            composable(BottomTab.My.route) {
                val myVm: MyViewModel = hiltViewModel()
                MyPageScreen(
                    viewModel          = myVm,
                    onNavigateToChat   = { navController.navigate(BottomTab.Chat.route) { launchSingleTop = true; restoreState = true } },
                    onNavigateToFriend = { navController.navigate(BottomTab.Friend.route) { launchSingleTop = true; restoreState = true } },
                    onNavigateToFind   = { navController.navigate(BottomTab.Explore.route) { launchSingleTop = true; restoreState = true } },
                    onNavigateToScript = { navController.navigate(BottomTab.Script.route) { launchSingleTop = true; restoreState = true } },
                )
            }
        }
    }
}

// EBBottomBar — 채팅 탭 전용
@Composable
fun EBBottomBar(
    tabs        : List<BottomTab>,
    currentDest : NavDestination?,
    onTabClick  : (BottomTab) -> Unit,
) {
    val bottomBarShape = RoundedCornerShape(topStart = 37.9.dp, topEnd = 37.9.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(102.dp)
            .shadow(elevation = 10.83.dp, spotColor = Color(0x0A000000), ambientColor = Color(0x0A000000), shape = bottomBarShape)
            .border(width = 1.dp, color = Color(0xFFE8E9EB), shape = bottomBarShape)
            .background(color = Color.White, shape = bottomBarShape)
            .padding(top = 1.dp),
        color          = Color.White,
        shape          = bottomBarShape,
        tonalElevation = 0.dp,
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier       = Modifier.fillMaxSize(),
        ) {
            tabs.forEach { tab ->
                val isSelected = currentDest?.hierarchy?.any { it.route == tab.route } == true
                val iconTint  = if (isSelected) Color(0xFF000000) else Color(0xFF9A9A9A)
                val textColor = if (isSelected) Color(0xFF181818) else Color(0xFFBFBFBF)

                NavigationBarItem(
                    selected = isSelected,
                    onClick  = { onTabClick(tab) },
                    icon     = {
                        Icon(painterResource(tab.iconRes), tab.label, Modifier.size(30.dp), tint = iconTint)
                    },
                    label    = {
                        Text(tab.label, style = TextStyle(
                            fontSize   = 13.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = textColor,
                        ))
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor      = Color.Transparent,
                        selectedIconColor   = Color(0xFF000000),
                        unselectedIconColor = Color(0xFF9A9A9A),
                        selectedTextColor   = Color(0xFF181818),
                        unselectedTextColor = Color(0xFFBFBFBF),
                    ),
                    alwaysShowLabel = true,
                )
            }
        }
    }
}

// 미구현 탭 플레이스홀더
@Composable
fun PlaceholderScreen(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text  = "$label\n(준비 중)",
            style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, color = Color(0xFF8F9399)),
            textAlign = TextAlign.Center,
        )
    }
}
