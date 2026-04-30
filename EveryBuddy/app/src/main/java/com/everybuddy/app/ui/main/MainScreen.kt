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
import com.everybuddy.app.ui.chat.StartChatScreen
import com.everybuddy.app.ui.explore.ExploreScreen
import com.everybuddy.app.ui.explore.FriendScreen
import com.everybuddy.app.ui.theme.PretendardFamily
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
// TODO: 채팅방, 유저 프로필 등 세부 화면 추가 시 여기에 라우트 등록
private val hideBottomBarRoutes = setOf(
    "start_chat",
    // TODO: "chat_room/{roomId}"    — 채팅방 화면 구현 후 추가
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
                    // onRoomClick = {/* TODO: 채팅방 리스트 화면 이동 */},
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
