package com.everybuddy.app.ui.explore

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun ExploreScreen(
    onUserClick : (String) -> Unit = {},
    viewModel   : ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    ExploreContent(
        state          = state,
        onFriendToggle = viewModel::onFriendToggle,
        onSearchToggle = viewModel::onSearchToggle,
        onSearchChange = viewModel::onSearchQueryChange,
        onTabSelect    = viewModel::onTabSelect,
        onUserClick    = onUserClick,
    )
}


// Content
@Composable
fun ExploreContent(
    state          : ExploreUiState,
    onFriendToggle : (String) -> Unit,
    onSearchToggle : () -> Unit,
    onSearchChange : (String) -> Unit,
    onTabSelect    : (String) -> Unit,
    onUserClick    : (String) -> Unit,
) {
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
                text     = "탐색",
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

        // TODO UI: 검색 모드 진입 시 상단 검색 바 애니메이션 표시
        //         state.isSearchMode 플래그 추가 후 AnimatedVisibility로 처리

        // TODO UI: 탭 필터 바(추천 친구 | 필터) — ScrollableTabRow 또는 LazyRow 칩 구성
        //         onTabSelect()로 ExploreViewModel.selectedCategory 업데이트

        // TODO UI: 추천 친구 카드 슬라이더 (HorizontalPager or LazyRow)
        //         GET /api/v1/users/recommended → 추천 유저 목록 표시

        // TODO UI: 섹션 헤더 ("오늘의 언어 파트너" 등 카테고리 구분선)

        LazyColumn {
            items(state.users) { user ->
                ExploreUserRow(
                    user           = user,
                    onFriendToggle = onFriendToggle,
                    onUserClick    = onUserClick,
                )
            }
        }
    }
}


// 유저 행
@Composable
fun ExploreUserRow(
    user           : DummyUser,
    onFriendToggle : (String) -> Unit,
    onUserClick    : (String) -> Unit,
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
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                user.topTags().forEach { tag ->
                    ExploreTagChip(label = tag.replace("_", " "))
                }
            }

            Spacer(Modifier.height(8.dp))

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
                                Icon(
                                    painter            = painterResource(
                                        if (i < level) R.drawable.ic_dot_filled
                                        else           R.drawable.ic_dot_empty
                                    ),
                                    contentDescription = null,
                                    modifier           = Modifier
                                        .padding(1.dp)
                                        .size(6.dp),
                                    tint               = if (i < level) Color(0xFF0167FF)
                                                        else           Color(0xFFD9D9D9),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ════════════════════════════════════════════════════════════════
// 공용 서브 컴포넌트
// ════════════════════════════════════════════════════════════════

@Composable
fun ExploreTagChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFEEF4FF))
            .padding(horizontal = 7.dp, vertical = 4.dp),
    ) {
        Text(
            text  = label,
            style = TextStyle(
                fontSize   = 11.sp,
                fontFamily = PretendardFamily,
                fontWeight = FontWeight(600),
                color      = Color(0xFF0167FF),
            ),
        )
    }
}

@Composable
fun ActiveBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF0E0))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter            = painterResource(R.drawable.ic_dot_filled),
                contentDescription = null,
                modifier           = Modifier
                    .padding(1.dp)
                    .size(4.dp),
                tint               = Color(0xFFFA9F62),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                "현재활동중",
                style = TextStyle(
                    fontSize   = 8.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(500),
                    color      = Color(0xFFFA9F62),
                ),
            )
        }
    }
}


// Preview
@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "탐색 탭")
@Composable
private fun PreviewExplore() {
    EveryBuddyTheme {
        ExploreContent(
            state          = ExploreUiState(),
            onFriendToggle = {},
            onSearchToggle = {},
            onSearchChange = {},
            onTabSelect    = {},
            onUserClick    = {},
        )
    }
}
