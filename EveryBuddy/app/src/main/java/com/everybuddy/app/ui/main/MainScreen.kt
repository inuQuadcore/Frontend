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
//import com.everybuddy.app.ui.chat.ScriptSaveScreen: TODO
import com.everybuddy.app.ui.chat.StartChatScreen
import com.everybuddy.app.ui.explore.ExploreScreen
import com.everybuddy.app.ui.explore.FriendScreen
import com.everybuddy.app.ui.theme.PretendardFamily
import com.everybuddy.app.data.chat.dummyChatRooms
import com.everybuddy.app.data.chat.dummyMessages
import androidx.compose.foundation.background

// BottomTab 정의
/**
 * 피그마 수치:
 * BottomBar 전체: 436×102dp
 * Border: 1dp 상단 / elevation: 10.83dp
 * 내부 패딩: 상 1dp
 * 배경: 흰색
 *
 * 아이콘:
 * ic_nav_chat.xml   — 대화
 * ic_nav_friend.xml — 친구
 * ic_nav_find.xml   — 찾기
 * ic_nav_keep.xml   — 스크립트
 * ic_nav_my.xml     — 마이
 */
sealed class BottomTab(
    val route   : String,
    val label   : String,
    val iconRes : Int,
) {
    data object Chat    : BottomTab("tab_chat",    "대화",    R.drawable.ic_nav_chat)
    data object Friend  : BottomTab("tab_friend",  "친구",    R.drawable.ic_nav_friend)
    data object Explore : BottomTab("tab_explore", "찾기",    R.drawable.ic_nav_find)
    data object Script  : BottomTab("tab_script",  "스크립트", R.drawable.ic_nav_keep)
    data object My      : BottomTab("tab_my",      "마이",    R.drawable.ic_nav_my)
}

val bottomTabs = listOf(
    BottomTab.Chat,
    BottomTab.Friend,
    BottomTab.Explore,
    BottomTab.Script,
    BottomTab.My,
)

// BottomBar 숨김 라우트 — 하위 페이지 진입 시
private val hideBottomBarRoutes = setOf(
    "start_chat",
    "chat_room/{roomId}",
    "conversation_select/{roomId}",
    "script_save/{roomId}",         // 단일 메시지 스크립트 저장
    "conversation_script_save/{roomId}", // 다중 선택 스크립트 저장
    "new_folder/{roomId}",
    // TODO: "user_profile/{userId}" — 유저 프로필 화면 구현 후 추가
    // TODO: "friend_request"        — 친구 요청 화면 구현 후 추가
)


// MainScreen
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route

    // 하위 페이지 진입 시 BottomBar 숨김
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

            // 채팅 시작 화면 (BottomBar 숨김)
            composable("start_chat") {
                StartChatScreen(
                    onBack    = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() },
                )
            }

            // ── 채팅방 화면 ──────────────────────────────────────────
            composable("chat_room/{roomId}") { backStack ->
                val roomId = backStack.arguments?.getString("roomId") ?: return@composable
                ChatRoomScreen(
                    roomId                   = roomId,
                    onBack                   = { navController.popBackStack() },
                    onNavigateToScriptSave   = { messageId ->
                        // 단일 메시지 꾹 눌러 저장 → messageId를 SavedStateHandle로 전달
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

            // ── 대화 선택 화면 ────────────────────────────────────────
            // 미디어패널 "대화저장하기" → 다중 메시지 선택 후 스크립트 저장
            composable("conversation_select/{roomId}") { backStack ->
                val roomId   = backStack.arguments?.getString("roomId") ?: return@composable
                val messages = dummyMessages[roomId] ?: emptyList()
                val room     = dummyChatRooms.find { it.id == roomId }
                ConversationSelectScreen(
                    messages  = messages,
                    roomName  = room?.name ?: "",
                    onBack    = { navController.popBackStack() },
                    onSave    = { selectedMessages, captureOption ->
                        // 선택된 메시지 목록을 SavedStateHandle에 저장
                        // TODO: SharedViewModel 또는 SavedStateHandle로 교체 권장
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

            // ── 스크립트 저장 (단일 메시지) ──────────────────────────
            // 말풍선 꾹 눌러 → "대화 스크립트 저장" 선택 시 진입
            composable("script_save/{roomId}") { backStack ->
                val roomId    = backStack.arguments?.getString("roomId") ?: return@composable
                val room      = dummyChatRooms.find { it.id == roomId }
                val messageId = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("pending_message_id")
                val message   = dummyMessages[roomId]?.find { it.id == messageId }
                    ?: dummyMessages[roomId]?.firstOrNull()
                    ?: return@composable

//                ScriptSaveScreen(
//                    messages    = listOf(message),
//                    roomName    = room?.name ?: "",
//                    onBack      = { navController.popBackStack() },
//                    onSave      = { _ ->
//                        // TODO: ScriptRepository.save(items)
//                        navController.popBackStack()
//                    },
//                    onAddFolder = {
//                        navController.navigate("new_folder/$roomId")
//                    },
//                )
            }

            // ── 스크립트 저장 (다중 메시지 — 대화 선택 화면에서 진입) ─
            composable("conversation_script_save/{roomId}") { backStack ->
                val roomId     = backStack.arguments?.getString("roomId") ?: return@composable
                val room       = dummyChatRooms.find { it.id == roomId }
                val allMessages = dummyMessages[roomId] ?: emptyList()
                val selectedIds = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<List<String>>("selected_message_ids")
                    ?: emptyList()
                val messages = allMessages.filter { it.id in selectedIds }
                    .ifEmpty { allMessages.take(1) }   // 폴백: 빈 경우 첫 메시지

//                ScriptSaveScreen(
//                    messages    = messages,
//                    roomName    = room?.name ?: "",
//                    onBack      = { navController.popBackStack() },
//                    onSave      = { _ ->
//                        // TODO: ScriptRepository.save(items)
//                        // 저장 완료 후 채팅방으로 돌아가기 (선택 화면까지 pop)
//                        navController.popBackStack(
//                            route         = "chat_room/$roomId",
//                            inclusive     = false,
//                        )
//                    },
//                    onAddFolder = {
//                        navController.navigate("new_folder/$roomId")
//                    },
//                )
            }

            // ── 새 폴더 만들기 ────────────────────────────────────────
            // ScriptSaveScreen 내 "폴더추가" 버튼 클릭 시 진입
            composable("new_folder/{roomId}") { backStack ->
                val roomId = backStack.arguments?.getString("roomId") ?: return@composable
                val room   = dummyChatRooms.find { it.id == roomId }
                NewFolderScreen(
                    roomName = room?.name ?: "",
                    onBack   = { navController.popBackStack() },
                    onSave   = { name, imageUri ->
                        // TODO: ScriptRepository.createFolder(name, imageUri)
                        //       성공 후 ScriptSaveScreen 폴더 목록 갱신 필요
                        //       → SharedViewModel 또는 SavedStateHandle 활용
                        navController.popBackStack()
                    },
                )
            }

            // 친구 탭
            composable(BottomTab.Friend.route) {
                FriendScreen(
                    onUserClick = { /* TODO: 친구 리스트 화면 이동 */ },
                )
            }

            // 탐색(찾기) 탭
            composable(BottomTab.Explore.route) {
                ExploreScreen(
                    onUserClick = { /* TODO: 유저 리스트 화면 이동 */ },
                )
            }

            // 스크립트 탭
            composable(BottomTab.Script.route) {
                // TODO: ScriptScreen 구현
                PlaceholderScreen(label = "스크립트")
            }

            // 마이 탭
            composable(BottomTab.My.route) {
                // TODO: MyScreen 구현
                PlaceholderScreen(label = "마이")
            }
        }
    }
}


// BottomBar
/**
 * EBBottomBar
 *
 * 피그마 수치:
 * - 전체 높이: 102dp
 * - 상단 Border: 1dp #E5E5E5
 * - elevation: 10.83dp (상단 그림자 효과)
 * - 내부 상단 패딩: 1dp
 * - 배경: 흰색
 *
 * 탭 아이콘:
 * - 크기: 24dp
 * - 선택: #000000 (검정)
 * - 미선택: #9A9A9A (회색)
 *
 * 탭 라벨:
 * - 폰트: 11sp
 * - 선택: Bold #181818
 * - 미선택: Regular #BFBFBF
 *
 * 선택 인디케이터: 없음 (색상으로만 구분)
 */
@Composable
fun EBBottomBar(
    tabs        : List<BottomTab>,
    currentDest : NavDestination?,
    onTabClick  : (BottomTab) -> Unit,
) {
    // 피그마: 상단 모서리만 37.9dp 곡률 적용
    val bottomBarShape = RoundedCornerShape(
        topStart = 37.9.dp,
        topEnd = 37.9.dp,
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(102.dp) // 피그마 전체 높이 반영
            .shadow(
                elevation = 10.83.dp,
                spotColor = Color(0x0A000000), // 피그마 spotColor 반영
                ambientColor = Color(0x0A000000), // 피그마 ambientColor 반영
                shape = bottomBarShape
            )
            .border(
                width = 1.dp,
                color = Color(0xFFE8E9EB),
                shape = bottomBarShape
            )
            .background(color = Color.White, shape = bottomBarShape)
            .padding(top = 1.dp), // 내부 상단 패딩
        color = Color.White,
        shape = bottomBarShape,
        tonalElevation = 0.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent, // Surface의 배경색을 사용하기 위해 투명 설정
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            tabs.forEach { tab ->
                val isSelected = currentDest?.hierarchy?.any { it.route == tab.route } == true

                // 피그마: 선택 여부에 따른 아이콘 및 텍스트 색상
                val iconTint = if (isSelected) Color(0xFF000000) else Color(0xFF9A9A9A)
                val textColor = if (isSelected) Color(0xFF181818) else Color(0xFFBFBFBF)

                NavigationBarItem(
                    selected = isSelected,
                    onClick  = { onTabClick(tab) },
                    icon     = {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = tab.label,
                            modifier = Modifier.size(30.dp),
                            tint = iconTint
                        )
                    },
                    label    = {
                        Text(
                            text  = tab.label,
                            style = TextStyle(
                                fontSize   = 13.sp,
                                fontFamily = PretendardFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color      = textColor
                            ),
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent, // 선택 인디케이터 없음
                        selectedIconColor = Color(0xFF000000),
                        unselectedIconColor = Color(0xFF9A9A9A),
                        selectedTextColor = Color(0xFF181818),
                        unselectedTextColor = Color(0xFFBFBFBF)
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
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text      = "$label\n(준비 중)",
            style     = TextStyle(
                fontSize   = 16.sp,
                fontFamily = PretendardFamily,
                color      = Color(0xFF8F9399),
            ),
            textAlign = TextAlign.Center,
        )
    }
}
