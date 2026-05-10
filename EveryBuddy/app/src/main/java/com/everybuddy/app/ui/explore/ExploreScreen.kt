package com.everybuddy.app.ui.explore

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.everybuddy.app.R
import com.everybuddy.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

private val C = AppColors

// ExploreScreen
@Composable
fun ExploreScreen(
    viewModel           : ExploreViewModel    = hiltViewModel(),
    onNavigateToChat    : () -> Unit          = {},
    onNavigateToFriend  : () -> Unit          = {},
    onNavigateToScript  : () -> Unit          = {},
    onNavigateToMy      : () -> Unit          = {},
    onOpenProfile       : (DiscoverUser) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    // 필터 화면 분기
    if (uiState.isFilterScreenOpen) {
        FilterSettingScreen(
            current  = uiState.filterSettings,
            onApply  = viewModel::applyFilter,
            onBack   = viewModel::closeFilterScreen,
        )
        return
    }

    Scaffold(
        topBar = {
            ExploreTopBar(
                onSearch       = { /* TODO: 검색 */ },
                onNotification = { /* TODO: 알림 */ },
            )
        },
        bottomBar = {
            ExploreBottomNavBar(
                onChat   = onNavigateToChat,
                onFriend = onNavigateToFriend,
                onScript = onNavigateToScript,
                onMy     = onNavigateToMy,
            )
        },
        containerColor = Color.White,
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor   = Color.White,
                contentColor     = C.Accent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                        color    = C.Accent,
                    )
                },
            ) {
                listOf("추천친구", "필터").forEachIndexed { idx, title ->
                    Tab(
                        selected = uiState.selectedTab == idx,
                        onClick  = { viewModel.selectTab(idx) },
                        text     = {
                            Text(
                                title,
                                style = TextStyle(
                                    fontSize   = 15.sp,
                                    fontFamily = PretendardFamily,
                                    fontWeight = if (uiState.selectedTab == idx) FontWeight(700) else FontWeight(400),
                                    color      = if (uiState.selectedTab == idx) C.Accent else C.TextSec,
                                ),
                            )
                        },
                    )
                }
            }

            when (uiState.selectedTab) {
                0 -> RecommendTab(
                    uiState        = uiState,
                    viewModel      = viewModel,
                    onOpenProfile  = { viewModel.openProfile(it); onOpenProfile(it) },
                )
                1 -> FilterTab(
                    uiState       = uiState,
                    viewModel     = viewModel,
                    onOpenProfile = { viewModel.openProfile(it); onOpenProfile(it) },
                )
            }
        }
    }
}

// 추천친구 탭
@Composable
private fun RecommendTab(
    uiState       : ExploreUiState,
    viewModel     : ExploreViewModel,
    onOpenProfile : (DiscoverUser) -> Unit,
) {
    // 스와이프 새로고침 (위/아래 땡기면 전체 새로고침)
    // TODO: SwipeRefresh 라이브러리 연동 후 실제 API 호출
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {

        item {
            Spacer(Modifier.height(16.dp))
            RandomCardSection(
                cardSet      = uiState.cardSet,
                currentIndex = uiState.currentCardIndex,
                onNext       = viewModel::nextCard,
                onPrev       = viewModel::prevCard,
                onCardClick  = onOpenProfile,
            )
            Spacer(Modifier.height(24.dp))
        }

        item {
            val topTag = ExploreDemo.myProfile.tags.firstOrNull()?.displayName ?: "관심사"
            SectionHeader(
                title    = "나와 같은 '$topTag' 관심사를 가진 추천 친구",
                subtitle = "관심사가 같으면 대화도 쉬워져요",
                onMore   = { /* TODO: 더보기 → 무한 스크롤 화면 */ },
            )
        }
        items(uiState.tagMatchUsers.take(3)) { user ->
            UserListItem(user = user, onClick = { onOpenProfile(user) })
        }

        item {
            Spacer(Modifier.height(16.dp))
            FilterBanner(onClick = { viewModel.openFilterScreen() })
            Spacer(Modifier.height(16.dp))
        }

        item {
            val lang = ExploreDemo.myProfile.learningLanguages.firstOrNull()
                ?.let { if (it.language == "ENGLISH") "영어" else it.language } ?: "한국어"
            SectionHeader(
                title    = "${lang}를 배우고 싶어해요",
                subtitle = "편하게 대화 나눠주실래요?",
                onMore   = { /* TODO: 더보기 */ },
            )
        }
        items(uiState.learningLangUsers.take(3)) { user ->
            UserListItem(user = user, onClick = { onOpenProfile(user) })
        }
    }
}

// 필터 탭
@Composable
private fun FilterTab(
    uiState       : ExploreUiState,
    viewModel     : ExploreViewModel,
    onOpenProfile : (DiscoverUser) -> Unit,
) {
    if (!uiState.isFilterApplied) {
        // 필터 미설정 — 배너만
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column {
                Spacer(Modifier.height(40.dp))
                FilterBanner(
                    subtitle = "필터를 설정해보세요!",
                    onClick  = { viewModel.openFilterScreen() },
                )
            }
        }
    } else {
        // 필터 설정 후 — 결과 목록
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                // 필터 초기화 버튼
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${uiState.filterResults.size}명", style = TextStyle(fontSize = 14.sp, color = C.TextSec, fontFamily = PretendardFamily))
                    Text(
                        "필터 초기화",
                        style    = TextStyle(fontSize = 13.sp, color = C.TextRed, fontFamily = PretendardFamily),
                        modifier = Modifier.clickable { viewModel.resetFilter() },
                    )
                }
            }
            items(uiState.filterResults) { user ->
                UserListItem(user = user, onClick = { onOpenProfile(user) })
            }
            if (uiState.filterHasNext) {
                item {
                    // TODO: 다음 페이지 로드 (nextCursor 기반)
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

// 랜덤 카드 섹션
@Composable
private fun RandomCardSection(
    cardSet      : List<DiscoverUser>,
    currentIndex : Int,
    onNext       : () -> Unit,
    onPrev       : () -> Unit,
    onCardClick  : (DiscoverUser) -> Unit,
) {
    val scope = rememberCoroutineScope()

    // 수평 드래그로 카드 전환
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animOffsetX by animateFloatAsState(targetValue = offsetX, label = "card_offset")

    val cardUser = cardSet.getOrNull(currentIndex) ?: return

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 카드 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .pointerInput(currentIndex) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (abs(offsetX) > 80f) {
                                if (offsetX < 0) onNext() else onPrev()
                            }
                            scope.launch { offsetX = 0f }
                        },
                        onHorizontalDrag = { _, delta ->
                            offsetX = (offsetX + delta).coerceIn(-150f, 150f)
                        },
                    )
                }
                .graphicsLayer {
                    // 대각선 전환 인터렉션: 수평 드래그 시 살짝 기울기
                    translationX = animOffsetX * 0.3f
                    rotationZ    = animOffsetX * 0.05f
                },
        ) {
            ProfileCard(user = cardUser, onClick = { onCardClick(cardUser) })
        }

        // 카드 순서 점 인디케이터
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(cardSet.size) { idx ->
                Box(
                    modifier = Modifier
                        .size(if (idx == currentIndex) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (idx == currentIndex) C.Accent else C.Border),
                )
            }
        }
    }
}

// 프로필 카드
@Composable
private fun ProfileCard(user: DiscoverUser, onClick: () -> Unit) {
    val (top2Tags, restCount) = user.top2TagsAndRest()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF888888))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model              = user.profileImageUrl,
            contentDescription = user.name,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize(),
        )

        // 하단 그라데이션 오버레이
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                    )
                ),
        )

        // 정보 영역 (좌하단)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            // 이름 + 국기 + 언어 코드
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(user.countryFlag(), fontSize = 20.sp)
                Text(
                    user.name,
                    style = TextStyle(fontSize = 20.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = Color.White),
                )
                Text("·", style = TextStyle(fontSize = 16.sp, color = Color.White.copy(0.8f)))
                user.languageLabels().take(2).forEach { code ->
                    Text(code, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = Color.White.copy(0.9f)))
                }
            }

            Spacer(Modifier.height(6.dp))

            // 태그 2개 + 나머지
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                top2Tags.forEach { tag ->
                    CardTag(label = "${tag.emoji} ${tag.displayName}")
                }
                if (restCount > 0) {
                    CardTag(label = "+$restCount")
                }
            }

            Spacer(Modifier.height(8.dp))

            // 한줄소개
            if (user.bio.isNotEmpty()) {
                Text(
                    user.bioPreview15(),
                    style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = Color.White),
                )
            }
        }
    }
}

@Composable
private fun CardTag(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.25f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = Color.White))
    }
}

// 섹션 헤더
@Composable
private fun SectionHeader(title: String, subtitle: String, onMore: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style    = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                modifier = Modifier.weight(1f),
            )
            // ic_chevron_right.xml
            Icon(
                painter            = painterResource(R.drawable.ic_chevron_right),
                contentDescription = "더보기",
                modifier           = Modifier.size(18.dp).clickable(onClick = onMore),
                tint               = C.TextSec,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = C.TextSec))
    }
}

// 필터 배너
@Composable
fun FilterBanner(subtitle: String = "조건에 맞추어 추천해요", onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(C.BannerBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // TODO: 일러스트 이미지 (아바타들)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCCDDFF)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "나에게 맞는 친구만",
                    style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                )
                Text(
                    subtitle,
                    style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = C.TextSec),
                )
            }
            Button(
                onClick        = onClick,
                shape          = RoundedCornerShape(20.dp),
                colors         = ButtonDefaults.buttonColors(containerColor = C.Accent),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("필터설정", style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color.White))
            }
        }
    }
}

// 유저 리스트 아이템
@Composable
fun UserListItem(user: DiscoverUser, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 프로필 이미지 + 국기 뱃지
        Box(modifier = Modifier.size(60.dp)) {
            Box(
                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFD0D0D0)),
            ) {
                AsyncImage(
                    model              = user.profileImageUrl,
                    contentDescription = user.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            }
            Text(
                user.countryFlag(),
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp),
            )
        }
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // 태그 3개 (온보딩 순서)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                user.top3Tags().forEach { tag ->
                    ExploreTag(label = "${tag.emoji} ${tag.displayName}")
                }
            }
            Spacer(Modifier.height(3.dp))

            // 이름 + 현활뱃지
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(user.name, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri))
                if (user.isOnline) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFF3E0)).padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(C.Online))
                            Text("현재활동중", style = TextStyle(fontSize = 9.sp, color = Color(0xFFE65100), fontFamily = PretendardFamily))
                        }
                    }
                }
            }

            // bio 15자 제한
            if (user.bio.isNotEmpty()) {
                Text(
                    user.bioPreview15(),
                    style    = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = C.TextSec),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 언어 코드
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                user.languageLabels().take(4).forEach { code ->
                    Text(code, style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = C.TextSec, fontWeight = FontWeight(500)))
                }
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = C.Border, thickness = 0.5.dp)
}

@Composable
private fun ExploreTag(label: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(C.TagBg).padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, style = TextStyle(fontSize = 10.sp, fontFamily = PretendardFamily, color = C.TagText))
    }
}

@Composable
fun ExploreTagChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.TagBg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, style = TextStyle(fontSize = 10.sp, fontFamily = PretendardFamily, color = AppColors.TagText))
    }
}

@Composable
fun ActiveBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFFF3E0))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Row(
            verticalAlignment        = Alignment.CenterVertically,
            horizontalArrangement   = Arrangement.spacedBy(3.dp),
        ) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(AppColors.Online))
            Text("현재활동중", style = TextStyle(fontSize = 9.sp, color = Color(0xFFE65100), fontFamily = PretendardFamily))
        }
    }
}

// TopBar + BottomNavBar
@Composable
private fun ExploreTopBar(onSearch: () -> Unit, onNotification: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("탐색", style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onSearch, modifier = Modifier.size(40.dp)) {
                    Icon(painterResource(R.drawable.ic_search), "검색", Modifier.size(22.dp), tint = C.TextPri)
                }
                Box {
                    IconButton(onClick = onNotification, modifier = Modifier.size(40.dp)) {
                        Icon(painterResource(R.drawable.ic_alarm), "알림", Modifier.size(22.dp), tint = C.TextPri)
                    }
                    Box(Modifier.size(8.dp).clip(CircleShape).background(C.Accent).align(Alignment.TopEnd).offset(x = (-6).dp, y = 6.dp))
                }
            }
        }
        HorizontalDivider(color = C.Border, thickness = 0.5.dp)
    }
}

@Composable
private fun ExploreBottomNavBar(onChat: () -> Unit, onFriend: () -> Unit, onScript: () -> Unit, onMy: () -> Unit) {
    Column {
        HorizontalDivider(color = C.Border, thickness = 0.5.dp)
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).background(Color.White).navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically,
        ) {
            ExpNavItem("대화",    R.drawable.ic_nav_chat,   false, onChat)
            ExpNavItem("친구",    R.drawable.ic_nav_friend, false, onFriend)
            ExpNavItem("탐색",    R.drawable.ic_nav_find,   true,  {})
            ExpNavItem("스크립트", R.drawable.ic_nav_script, false, onScript)
            ExpNavItem("마이",    R.drawable.ic_nav_my,     false, onMy)
        }
    }
}

@Composable
private fun ExpNavItem(label: String, iconRes: Int, isSelected: Boolean, onClick: () -> Unit) {
    val tint = if (isSelected) C.Accent else Color(0xFFAAAAAA)
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(iconRes), label, Modifier.size(24.dp), tint = tint)
        Spacer(Modifier.height(2.dp))
        Text(label, style = TextStyle(fontSize = 10.sp, fontFamily = PretendardFamily, color = tint, fontWeight = if (isSelected) FontWeight(600) else FontWeight(400)))
    }
}

