package com.everybuddy.app.ui.friend

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuddy.app.R
import com.everybuddy.app.ui.theme.PretendardFamily

private val C = FriendColors

@Composable
fun FriendSearchScreen(
    viewModel : FriendViewModel,
    statusVm  : StatusMessageViewModel = hiltViewModel(),
) {
    val uiState     by viewModel.uiState.collectAsState()
    val statusState by statusVm.state.collectAsState()
    val friendResults = viewModel.filteredFriends()
    val statusResults = if (uiState.searchQuery.isBlank()) emptyList()
        else statusState.friendStatuses.filter { sm ->
            KoreanChosung.matches(uiState.searchQuery, sm.userName) ||
            KoreanChosung.matches(uiState.searchQuery, sm.content)
        }

    BackHandler { viewModel.setSearchActive(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White).statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = { viewModel.setSearchActive(false) }) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = C.TextPri)
                    }
                    Text(
                        "친구 검색",
                        style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    IconButton(onClick = {}) {
                        Icon(painterResource(R.drawable.ic_search), "검색", Modifier.size(22.dp), tint = C.TextPri)
                    }
                }
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F0F0))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value         = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier      = Modifier.weight(1f),
                    singleLine    = true,
                    textStyle     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri),
                    decorationBox = { inner ->
                        if (uiState.searchQuery.isEmpty()) {
                            Text("검색하기", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextSec))
                        }
                        inner()
                    },
                )
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }, modifier = Modifier.size(20.dp)) {
                        Icon(painterResource(R.drawable.ic_tagselectcancel), "지우기", Modifier.size(16.dp), tint = C.TextSec)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (uiState.searchQuery.isBlank()) return@Column

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (statusResults.isNotEmpty()) {
                    item {
                        Text(
                            "상태메시지",
                            style    = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding        = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(statusResults) { sm ->
                                FriendStatusPreviewCard(sm = sm, onClick = { statusVm.openFriendStatus(sm) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = C.Border, thickness = 8.dp)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (friendResults.isNotEmpty()) {
                    item {
                        Text(
                            "나의 친구",
                            style    = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(friendResults) { friend ->
                        FriendListItem(friend = friend, onClick = {})
                    }
                }

                if (statusResults.isEmpty() && friendResults.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                            Text("검색 결과가 없습니다.", style = TextStyle(fontSize = 14.sp, color = C.TextSec))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusMessageAllScreen(
    viewModel : FriendViewModel,
    statusVm  : StatusMessageViewModel,
) {
    val statusState by statusVm.state.collectAsState()
    val friendStatuses = statusState.friendStatuses

    BackHandler { viewModel.closeStatusPage() }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White).statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { viewModel.closeStatusPage() }) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = C.TextPri)
                    }
                    Text(
                        "상태메시지",
                        style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                            Icon(painterResource(R.drawable.ic_search), "검색", Modifier.size(22.dp), tint = C.TextPri)
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                            Icon(painterResource(R.drawable.ic_alarm), "알림", Modifier.size(22.dp), tint = C.TextPri)
                        }
                    }
                }
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->

        LazyVerticalGrid(
            columns        = GridCells.Fixed(2),
            modifier       = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "나도 작성하기",
                        style    = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            MyStatusCard(viewModel = statusVm)
                        }
                        items(friendStatuses) { sm ->
                            FriendStatusPreviewCard(sm = sm, onClick = { statusVm.openFriendStatus(sm) })
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = C.Border, thickness = 8.dp)
                    Spacer(Modifier.height(8.dp))
                }
            }

            gridItems(friendStatuses) { sm ->
                FriendStatusGridCard(sm = sm, onClick = { statusVm.openFriendStatus(sm) })
            }
        }
    }
}
