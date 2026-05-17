@file:Suppress("DEPRECATION")

package com.everybuddy.app.ui.explore

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuddy.app.R
import com.everybuddy.app.data.dummy.*
import com.everybuddy.app.ui.theme.*


// 진입점
@Composable
fun FriendScreen(
    onUserClick : (String) -> Unit = {},
    viewModel   : FriendViewModel  = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    FriendContent(
        state          = state,
        onSearchToggle = viewModel::onSearchToggle,
        onSearchChange = viewModel::onSearchQueryChange,
        onSortChange   = viewModel::onSortChange,
        onUserClick    = onUserClick,
    )
}


// Content
@Composable
fun FriendContent(
    state          : FriendUiState,
    onSearchToggle : () -> Unit,
    onSearchChange : (String) -> Unit,
    onSortChange   : (String) -> Unit,
    onUserClick    : (String) -> Unit,
) {
    val filtered = state.filteredFriends

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = "친구",
                style    = TextStyle(
                    fontSize   = 20.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = Color(0xFF000000),
                ),
                modifier = Modifier.weight(1f),
            )

            // TODO: 검색 기능 — ic_search.xml
            IconButton(onClick = onSearchToggle, modifier = Modifier.size(40.dp)) {
                Image(
                    painter            = painterResource(R.drawable.ic_search),
                    contentDescription = "검색",
                    modifier           = Modifier.size(24.dp),
                )
            }

            // TODO: 알림 화면 만들어야 함.
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clickable(onClick = { /* TODO: 알림 화면 이동 */ }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter            = painterResource(id = R.drawable.ic_alarm),
                    contentDescription = "알림",
                    modifier           = Modifier.size(24.dp),
                    tint               = Color.Black,
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 8.dp)
                        .background(color = Color(0xFF0167FF), shape = CircleShape),
                )
            }
        }

        // TODO UI: 검색 모드 진입 시 AnimatedVisibility로 SearchBar 표시
        //         state.isSearchMode → OutlinedTextField + 취소 버튼

        // TODO UI: 상태메시지 섹션 — 내 프로필 + 상태메시지 한 줄 표시 (피그마 참고)
        //         GET /api/v1/user/me → 내 프로필 정보 표시

        // TODO UI: 정렬 드롭다운 (최신순 / 이름순)
        //         onSortChange() 연동 → FriendViewModel.currentSort 업데이트

        LazyColumn {
            items(filtered) { user ->
                FriendItem(
                    user        = user,
                    onUserClick = onUserClick,
                )
            }
        }
    }
}


// 친구 리스트 아이템
@Composable
fun FriendItem(
    user        : DummyUser,
    onUserClick : (String) -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onUserClick(user.id) }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(67.dp)) {
            Box(
                modifier         = Modifier
                    .size(67.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8EEF7))
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center,
            ) {
                // TODO Coil: AsyncImage(model = user.profileImageUrl, ...) 로 교체
                //           현재는 더미 기본 프로필 이미지 사용
                Image(
                    painter            = painterResource(R.drawable.ic_profile_default),
                    contentDescription = user.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.size(67.dp),
                )
            }
            Box(
                modifier         = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text     = nationFlagEmoji(user.nationCode),
                    fontSize = 21.sp,
                )
            }
        }

        Spacer(Modifier.width(15.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = user.name,
                    style = TextStyle(
                        fontSize   = 16.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(600),
                        color      = Color(0xFF000000),
                    ),
                )
                if (user.isActive) {
                    Spacer(Modifier.width(6.dp))
                    ActiveBadge()
                }
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text     = user.bio.take(15),
                style    = TextStyle(
                    fontSize   = 13.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(400),
                    color      = Color(0xFF434343),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(3.dp))

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                user.myLangs.forEach { (langCode, level) ->
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text  = langCode,
                            style = TextStyle(
                                fontSize   = 13.sp,
                                fontFamily = PretendardFamily,
                                fontWeight = FontWeight(500),
                                color      = Color(0xFF9A9A9A),
                            ),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(5) { i ->
                                Image(
                                    painter            = painterResource(
                                        if (i < level) R.drawable.ic_dot_filled
                                        else           R.drawable.ic_dot_empty
                                    ),
                                    contentDescription = null,
                                    modifier           = Modifier
                                        .padding(1.dp)
                                        .size(6.dp),
                                    colorFilter        = ColorFilter.tint(
                                        if (i < level) Color(0xFF0167FF)
                                        else           Color(0xFFD9D9D9),
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            val topTags = user.topTags()
            if (topTags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    topTags.forEach { tag ->
                        ExploreTagChip(label = tag.replace("_", " "))
                    }
                }
            }
        }
    }
}


// Preview
@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "친구 탭")
@Composable
private fun PreviewFriend() {
    EveryBuddyTheme {
        FriendContent(
            state          = FriendUiState(),
            onSearchToggle = {},
            onSearchChange = {},
            onSortChange   = {},
            onUserClick    = {},
        )
    }
}
