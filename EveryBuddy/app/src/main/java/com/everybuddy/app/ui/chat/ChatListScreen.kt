package com.everybuddy.app.ui.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.everybuddy.app.R
import com.everybuddy.app.data.chat.ChatFilter
import com.everybuddy.app.data.chat.ChatListUiState
import com.everybuddy.app.data.chat.ChatRoom
import com.everybuddy.app.ui.theme.*

// 진입점
@Composable
fun ChatListScreen(
    onRoomClick : (ChatRoom) -> Unit = {},
    onStartChat : () -> Unit         = {},
    viewModel   : ChatViewModel      = hiltViewModel(),
) {
    val state by viewModel.listState.collectAsState()

    ChatListContent(
        state          = state,
        onRoomClick    = onRoomClick,
        onFilterSelect = viewModel::onFilterSelect,
        onSearchToggle = viewModel::onSearchToggle,
        onSearchChange = viewModel::onSearchQueryChange,
        onFabClick     = onStartChat,
    )
}

// Content
@Composable
fun ChatListContent(
    state          : ChatListUiState,
    onRoomClick    : (ChatRoom) -> Unit,
    onFilterSelect : (ChatFilter) -> Unit,
    onSearchToggle : () -> Unit,
    onSearchChange : (String) -> Unit,
    onFabClick     : () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 상단 앱바
            // "채팅" 타이틀 / ic_search.xml / ic_alarm.xml + 파란 뱃지 점
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
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

                // TODO UI: 검색 모드 진입 시 AnimatedVisibility로 SearchBar 표시
                //           state.isSearchMode → OutlinedTextField + 취소 버튼
                IconButton(onClick = onSearchToggle, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_search),
                        contentDescription = "검색",
                        modifier           = Modifier.size(24.dp),
                        tint               = Color.Black,
                    )
                }

                // TODO 기능: 알림 화면 이동 — NotificationScreen 구현 후 onClick 연결
                Box(
                    modifier         = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_alarm),
                        contentDescription = "알림",
                        modifier           = Modifier.size(24.dp),
                        tint               = Color.Black,
                    )
                    // 파란 뱃지 점
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                            .background(Color(0xFF0167FF), CircleShape),
                    )
                }
            }

            // 필터 탭
            // 선택=검정 배경 흰글씨 / 미선택=흰배경 회색 테두리
            LazyRow(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 25.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(ChatFilter.entries.toList()) { filter ->
                    val isSelected = filter == state.activeFilter
                    Box(
                        modifier         = Modifier
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

                // "+" 폴더 버튼 — ic_folder_plus.xml (TODO: 채팅방 그룹화 기능)
                item {
                    Box(
                        modifier         = Modifier
                            .size(33.dp)
                            .clip(RoundedCornerShape(33.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE5E5E5), RoundedCornerShape(33.dp))
                            .clickable { /* TODO: 채팅방 그룹화(폴더) 기능 */ },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_folder_plus),
                            contentDescription = "폴더 추가",
                            modifier           = Modifier.size(13.dp),
                            tint               = Color(0xFF555555),
                        )
                    }
                }
            }

            // 채팅방 목록 / 빈 상태
            Box(
                modifier         = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (state.rooms.isEmpty()) {
                    Column(
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
                } else {
                    // TODO UI: 채팅방 리스트 LazyColumn 표시
                    //           ChatRoomItem 컴포저블 구현 (마지막 메시지, 시간, 읽지 않은 수 배지)
                    //           onRoomClick → AppNavGraph에서 chat_room/{roomId}로 이동
                    //           롱클릭 → onContextMenu(room) → 컨텍스트 메뉴(알림 끄기/고정/나가기)
                }
            }

        }

        // FAB "+" 버튼
        // ic_fab_plus.xml / 우하단 end 18dp, bottom 126dp
        FloatingActionButton(
            onClick        = onFabClick,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
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

    }
}


// Preview
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
        )
    }
}
