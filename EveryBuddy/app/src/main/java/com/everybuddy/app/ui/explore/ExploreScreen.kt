@file:Suppress("DEPRECATION")

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
import androidx.compose.ui.graphics.TransformOrigin
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
    viewModel      : ExploreViewModel       = hiltViewModel(),
    onOpenProfile  : (DiscoverUser) -> Unit = {},
    onStartChat    : (DiscoverUser) -> Unit = {},
    onNotification : () -> Unit             = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    // 프로필 화면 분기
    if (uiState.isProfileOpen && uiState.selectedUser != null) {
        UserProfileScreen(
            user           = uiState.selectedUser!!,
            detail         = uiState.selectedUserDetail,
            isFriend       = uiState.selectedUserDetail?.isFriend ?: false,
            onBack         = viewModel::closeProfile,
            onChat         = { onStartChat(it) },
            onAddFriend    = { viewModel.addFriend(it.userId) },
            onRemoveFriend = { viewModel.removeFriend(it.userId) },
        )
        return
    }

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
                onNotification = onNotification,
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
            UserListItem(
                user    = user,
                onClick = { onOpenProfile(user) },
            )
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
            UserListItem(
                user    = user,
                onClick = { onOpenProfile(user) },
            )
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
                UserListItem(
                    user    = user,
                    onClick = { onOpenProfile(user) },
                )
            }
            if (uiState.filterHasNext) {
                item {
                    LaunchedEffect(uiState.filterNextCursor) {
                        viewModel.loadMoreFilterResults()
                    }
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
    if (cardSet.isEmpty()) return

    val scope    = rememberCoroutineScope()
    val offsetX  = remember { Animatable(0f) }
    val offsetY  = remember { Animatable(0f) }
    val swipeDir = remember { mutableIntStateOf(0) }

    LaunchedEffect(currentIndex) {
        val dir = swipeDir.intValue
        offsetY.snapTo(0f)
        if (dir != 0) {
            offsetX.snapTo(-dir * 450f)
            offsetX.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
        } else {
            offsetX.snapTo(0f)
        }
    }

    val cardUser = cardSet.getOrNull(currentIndex) ?: return

    val cardRotations = remember { floatArrayOf(+6f, -3f, +2f, -5f) }

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            modifier         = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Back card (visualIndex = 2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(3f / 4f)
                    .graphicsLayer {
                        translationX = 24.dp.toPx()
                        translationY = (-12).dp.toPx()
                        scaleX       = 0.92f
                        scaleY       = 0.92f
                        rotationZ    = cardRotations[(currentIndex + 2) % cardRotations.size]
                        alpha        = 0.75f
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFB8C8FF)),
            )
            // Middle card (visualIndex = 1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(3f / 4f)
                    .graphicsLayer {
                        translationX = 12.dp.toPx()
                        translationY = (-6).dp.toPx()
                        scaleX       = 0.96f
                        scaleY       = 0.96f
                        rotationZ    = cardRotations[(currentIndex + 1) % cardRotations.size]
                        alpha        = 0.9f
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFC8D5FF)),
            )
            // Front card (visualIndex = 0) — draggable
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .pointerInput(currentIndex) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val dx = offsetX.value
                                if (abs(dx) > 80f) {
                                    val dir = if (dx < 0) -1 else 1
                                    swipeDir.intValue = dir
                                    scope.launch {
                                        val jX = launch { offsetX.animateTo(-900f, tween(320)) }
                                        val jY = launch { offsetY.animateTo(700f, tween(320)) }
                                        jX.join(); jY.join()
                                        if (dir < 0) onNext() else onPrev()
                                    }
                                } else {
                                    scope.launch {
                                        launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                                        launch { offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                                    }
                                }
                            },
                            onHorizontalDrag = { _, delta ->
                                scope.launch {
                                    offsetX.snapTo((offsetX.value + delta).coerceIn(-220f, 220f))
                                }
                            },
                        )
                    }
                    .graphicsLayer {
                        translationX = offsetX.value
                        translationY = offsetY.value
                        rotationZ    = 0f
                        alpha        = (1f - offsetY.value / 700f).coerceAtLeast(0f)
                    },
            ) {
                ProfileCard(user = cardUser, onClick = { onCardClick(cardUser) })
            }
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
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF888888))
            .clickable(onClick = onClick),
    ) {
        if (user.profileImageUrl != null) {
            AsyncImage(
                model              = user.profileImageUrl,
                contentDescription = user.name,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier         = Modifier.fillMaxSize().background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter            = painterResource(R.drawable.ic_nav_my),
                    contentDescription = user.name,
                    modifier           = Modifier.size(60.dp),
                    tint               = Color(0xFFCCCCCC),
                )
            }
        }

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
            .height(88.dp)
            .clip(RoundedCornerShape(0.dp))
            .background(C.BannerBg),
    ) {
        // 반원: 위쪽 절반, 왼쪽 조금 잘린채로
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = (-20).dp, y = (40).dp)
                .background(Color(0xFFCCDDFF), CircleShape),
        )

        Row(
            modifier              = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter            = painterResource(R.drawable.ic_filter),
                contentDescription = null,
                contentScale       = ContentScale.FillHeight,
                modifier           = Modifier.requiredHeight(72.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(
                    "나에게 맞는 친구만",
                    style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                )
                Text(
                    subtitle,
                    style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextSec),
                )
            }
            Button(
                onClick        = onClick,
                shape          = RoundedCornerShape(50.dp),
                colors         = ButtonDefaults.buttonColors(containerColor = C.Accent),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("필터 설정", style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color.White))
            }
        }
    }
}

@Composable
fun UserListItem(
    user    : DiscoverUser,
    onClick : () -> Unit,
) {
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
                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFF0F0F0)),
            ) {
                if (user.profileImageUrl != null) {
                    AsyncImage(
                        model              = user.profileImageUrl,
                        contentDescription = user.name,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_nav_my),
                            contentDescription = user.name,
                            modifier           = Modifier.size(36.dp),
                            tint               = Color(0xFFCCCCCC),
                        )
                    }
                }
            }
            Text(
                user.countryFlag(),
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp),
            )
        }
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
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
private fun ExploreTopBar(
    onSearch       : () -> Unit,
    onNotification : () -> Unit,
    hasNotification: Boolean = false,
) {
    Column {
        Box(
            modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 16.dp),
        ) {
            Text(
                "탐색",
                modifier = Modifier.align(Alignment.Center),
                style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
            )
            Row(
                modifier              = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onSearch, modifier = Modifier.size(40.dp)) {
                    Icon(painterResource(R.drawable.ic_search), "검색", Modifier.size(24.dp), tint = C.TextPri)
                }
                Box {
                    IconButton(onClick = onNotification, modifier = Modifier.size(40.dp)) {
                        Icon(painterResource(R.drawable.ic_alarm), "알림", Modifier.size(24.dp), tint = C.TextPri)
                    }
                    if (hasNotification) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(C.Accent).align(Alignment.TopEnd).offset(x = (-6).dp, y = 6.dp))
                    }
                }
            }
        }
        HorizontalDivider(color = C.Border, thickness = 0.5.dp)
    }
}



