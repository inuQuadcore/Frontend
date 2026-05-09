package com.everybuddy.app.ui.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import com.everybuddy.app.ui.theme.PretendardFamily
import com.everybuddy.app.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource

private val C = FriendColors

@Composable
fun FriendMainScreen(
    viewModel         : FriendViewModel,
    statusVm          : StatusMessageViewModel = hiltViewModel(),
    onNavigateToChat  : () -> Unit = {},
    onNavigateToFind  : () -> Unit = {},
    onNavigateToScript: () -> Unit = {},
    onNavigateToMy    : () -> Unit = {},
    onFriendClick     : (FriendProfile) -> Unit = {},
    onAddFriend       : () -> Unit = {},
) {
    val uiState     by viewModel.uiState.collectAsState()
    val statusState by statusVm.state.collectAsState()

    LaunchedEffect(uiState.toastMessage) {
        if (uiState.toastMessage != null) {
            delay(2000)
            viewModel.consumeToast()
        }
    }

    LaunchedEffect(statusState.toastMessage) {
        if (statusState.toastMessage != null) {
            delay(2000)
            statusVm.consumeToast()
        }
    }

    when {
        uiState.isSearchActive         -> FriendSearchScreen(viewModel = viewModel)
        uiState.isStatusPageOpen       -> StatusMessageAllScreen(viewModel = viewModel, statusVm = statusVm)
        statusState.isWriteScreenOpen  -> StatusWriteScreen(viewModel = statusVm)
        else                           -> FriendMainContent(
            viewModel          = viewModel,
            statusVm           = statusVm,
            onNavigateToChat   = onNavigateToChat,
            onNavigateToFind   = onNavigateToFind,
            onNavigateToScript = onNavigateToScript,
            onNavigateToMy     = onNavigateToMy,
            onFriendClick      = onFriendClick,
            onAddFriend        = onAddFriend,
        )
    }

    if (statusState.isMyStatusMenuOpen) {
        MyStatusMenuSheet(viewModel = statusVm)
    }

    if (statusState.isDeleteConfirmOpen) {
        DeleteConfirmDialog(viewModel = statusVm)
    }

    statusState.expandedStatus?.let { sm ->
        FriendStatusDetailPopup(sm = sm, viewModel = statusVm)
    }
}

@Composable
private fun FriendMainContent(
    viewModel          : FriendViewModel,
    statusVm           : StatusMessageViewModel,
    onNavigateToChat   : () -> Unit,
    onNavigateToFind   : () -> Unit,
    onNavigateToScript : () -> Unit,
    onNavigateToMy     : () -> Unit,
    onFriendClick      : (FriendProfile) -> Unit,
    onAddFriend        : () -> Unit,
) {
    val uiState     by viewModel.uiState.collectAsState()
    val statusState by statusVm.state.collectAsState()
    val friends      = viewModel.filteredFriends()

    Scaffold(
        topBar = {
            FriendTopBar(
                title          = "친구",
                showAddFriend  = true,
                onSearch       = { viewModel.setSearchActive(true) },
                onNotification = { /* TODO: 알림 화면 */ },
                onAddFriend    = onAddFriend,
            )
        },
        bottomBar = {
            FriendBottomNavBar(
                onChat   = onNavigateToChat,
                onFind   = onNavigateToFind,
                onScript = onNavigateToScript,
                onMy     = onNavigateToMy,
            )
        },
        containerColor = Color.White,
    ) { innerPadding ->

        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {

                item {
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            "상태메시지",
                            style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                        )
                        IconButton(onClick = { viewModel.openStatusPage() }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                painter            = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = "더보기",
                                modifier           = Modifier.size(18.dp),
                                tint               = C.TextSec,
                            )
                        }
                    }

                    Text(
                        "지금 친구에게 가볍게 답장해보세요.",
                        style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = C.TextSec),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(10.dp))

                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            MyStatusCard(viewModel = statusVm)
                        }
                        items(statusState.friendStatuses.take(2)) { sm ->
                            FriendStatusPreviewCard(sm = sm, onClick = { statusVm.openFriendStatus(sm) })
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = C.Border, thickness = 8.dp)
                    Spacer(Modifier.height(12.dp))
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "나의 친구",
                                style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                            )
                            Text(
                                "${friends.size}",
                                style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                            )
                        }
                        SortDropdownButton(
                            current  = uiState.sortOption,
                            onSelect = viewModel::changeSortOption,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                items(friends) { friend ->
                    FriendListItem(friend = friend, onClick = onFriendClick)
                }
            }

            val toastMsg = uiState.toastMessage ?: statusState.toastMessage
            toastMsg?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF444444))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Text(msg, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = Color.White))
                    }
                }
            }
        }
    }
}
