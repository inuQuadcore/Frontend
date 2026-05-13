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
import com.everybuddy.app.R
import com.everybuddy.app.ui.chat.ChatListScreen
import com.everybuddy.app.ui.chat.ChatRoomScreen
import com.everybuddy.app.ui.chat.ChatViewModel
import com.everybuddy.app.ui.chat.ScriptSaveItem
import com.everybuddy.app.ui.explore.ExploreScreen
import com.everybuddy.app.ui.explore.MyViewModel
import com.everybuddy.app.ui.friend.FriendMainScreen
import com.everybuddy.app.ui.friend.FriendViewModel
import com.everybuddy.app.ui.my.MyPageScreen
import com.everybuddy.app.ui.notification.NotificationScreen
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
    EXPLORE ("찾기",    R.drawable.ic_nav_find),
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

    val chatNavController = rememberNavController()
    val scriptViewModel   : ScriptViewModel = hiltViewModel()

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
                    val chatVm: ChatViewModel = hiltViewModel()
                    NavHost(navController = chatNavController, startDestination = MainRoute.CHAT_LIST) {
                        composable(MainRoute.CHAT_LIST) {
                            isChatRoomOpen = false
                            ChatListScreen(
                                viewModel           = chatVm,
                                onRoomClick         = { room ->
                                    chatVm.markRoomAsRead(room.id)
                                    chatNavController.navigate(MainRoute.chatRoom(room.id))
                                },
                                onStartChat         = { /* TODO: 채팅 시작 화면 */ },
                                onNotificationClick = { showNotification = true },
                            )
                        }
                        composable(
                            route     = MainRoute.CHAT_ROOM,
                            arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
                        ) { backStack ->
                            val roomId = backStack.arguments?.getString("roomId") ?: return@composable
                            isChatRoomOpen = true
                            ChatRoomScreen(
                                roomId                = roomId,
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
                                onMuteChanged = { isMuted -> chatVm.updateRoomMute(roomId, isMuted) },
                            )
                        }
                    }
                }

                MainTab.FRIEND -> {
                    isChatRoomOpen = false
                    val friendVm: FriendViewModel = hiltViewModel()
                    FriendMainScreen(
                        viewModel       = friendVm,
                        onAddFriend     = { selectedTab = MainTab.EXPLORE },
                        onStartChat     = { user -> chatNavController.navigate(MainRoute.chatRoom("chat_${user.userId}")); selectedTab = MainTab.CHAT },
                        onNotification  = { showNotification = true },
                        onReplyToStatus = { friendId, _ ->
                            isChatRoomOpen = true
                            chatNavController.navigate(MainRoute.chatRoom("reply_$friendId"))
                            selectedTab = MainTab.CHAT
                        },
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
                    ScriptMainScreen(
                        viewModel      = scriptViewModel,
                        onNotification = { showNotification = true },
                    )
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
