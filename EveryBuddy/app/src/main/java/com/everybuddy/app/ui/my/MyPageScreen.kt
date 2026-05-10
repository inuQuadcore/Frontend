package com.everybuddy.app.ui.my

// MyPageScreen — 마이페이지

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.everybuddy.app.R
import com.everybuddy.app.ui.explore.*
import com.everybuddy.app.ui.theme.PretendardFamily
import kotlinx.coroutines.delay

private val C = AppColors

// MyPageScreen 진입점
@Composable
fun MyPageScreen(
    viewModel          : MyViewModel,
    onNavigateToChat   : () -> Unit = {},
    onNavigateToFriend : () -> Unit = {},
    onNavigateToFind   : () -> Unit = {},
    onNavigateToScript : () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.toastMessage) {
        if (uiState.toastMessage != null) { delay(2000); viewModel.consumeToast() }
    }

    when {
        uiState.isEditMode    -> ProfileEditScreen(viewModel = viewModel)
        uiState.isTagEditOpen -> TagEditScreen(viewModel = viewModel)
        uiState.openSubMenu != null -> SubMenuScreen(key = uiState.openSubMenu!!, onBack = viewModel::closeSubMenu)
        else -> MyPageContent(
            viewModel          = viewModel,
            onNavigateToChat   = onNavigateToChat,
            onNavigateToFriend = onNavigateToFriend,
            onNavigateToFind   = onNavigateToFind,
            onNavigateToScript = onNavigateToScript,
        )
    }
}

// 마이페이지 메인 콘텐츠
@Composable
private fun MyPageContent(
    viewModel          : MyViewModel,
    onNavigateToChat   : () -> Unit,
    onNavigateToFriend : () -> Unit,
    onNavigateToFind   : () -> Unit,
    onNavigateToScript : () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("마이페이지", style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { viewModel.openSubMenu("settings") }, modifier = Modifier.size(40.dp)) {
                            // ic_settings.xml
                            Icon(painterResource(R.drawable.ic_settings), "설정", Modifier.size(22.dp), tint = C.TextPri)
                        }
                        Box {
                            IconButton(onClick = { /* TODO: 알림 */ }, modifier = Modifier.size(40.dp)) {
                                Icon(painterResource(R.drawable.ic_alarm), "알림", Modifier.size(22.dp), tint = C.TextPri)
                            }
                            Box(Modifier.size(8.dp).clip(CircleShape).background(C.Accent).align(Alignment.TopEnd).offset(x = (-6).dp, y = 6.dp))
                        }
                    }
                }
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().height(60.dp).background(Color.White).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically,
                ) {
                    MyNavItem("대화",    R.drawable.ic_nav_chat,   false, onNavigateToChat)
                    MyNavItem("친구",    R.drawable.ic_nav_friend, false, onNavigateToFriend)
                    MyNavItem("탐색",    R.drawable.ic_nav_find,   false, onNavigateToFind)
                    MyNavItem("스크립트", R.drawable.ic_nav_script, false, onNavigateToScript)
                    MyNavItem("마이",    R.drawable.ic_nav_my,     true,  {})
                }
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->

        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // 프로필 이미지 + 국기
                    Box(modifier = Modifier.size(64.dp)) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFD0D0D0))) {
                            AsyncImage(
                                model              = profile.profileImageUrl,
                                contentDescription = profile.name,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize(),
                            )
                        }
                        Text(profile.countryFlag(), fontSize = 16.sp, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp))
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(profile.name, style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri))
                            Icon(painterResource(R.drawable.ic_location), "국적", Modifier.size(14.dp), tint = C.TextSec)
                            Text(profile.countryName(), style = TextStyle(fontSize = 13.sp, color = C.TextSec, fontFamily = PretendardFamily))
                        }
                        Text(profile.email, style = TextStyle(fontSize = 12.sp, color = C.TextSec, fontFamily = PretendardFamily))
                        Text("${profile.age}세 · ${profile.gender}", style = TextStyle(fontSize = 12.sp, color = C.TextSec, fontFamily = PretendardFamily))
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(profile.bio, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextPri, lineHeight = 22.sp), modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(Modifier.height(12.dp))
                // [프로필 수정] 버튼
                OutlinedButton(
                    onClick  = { viewModel.openEdit() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(44.dp),
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, C.Border),
                ) {
                    Text("프로필 수정", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextPri))
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = C.Border, thickness = 8.dp)

                Spacer(Modifier.height(20.dp))
                Text("사용 언어", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri), modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(12.dp))
                profile.learningLanguages.forEach { lang ->
                    LanguageLevelRow(lang = lang, onTap = { viewModel.openLanguage(lang.language) })
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = C.Border, thickness = 8.dp)

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("나의 태그", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri))
                    Text(
                        "편집하기",
                        style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = C.Accent),
                        modifier = Modifier.clickable { viewModel.openTagEdit() },
                    )
                }
                Spacer(Modifier.height(12.dp))
                TagWrapLayout(tags = profile.tags)

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = C.Border, thickness = 8.dp)

                val menuItems = listOf(
                    "에브리버디 가이드" to "guide",
                    "공지사항"        to "notice",
                    "설정"            to "settings",
                    "버전정보"        to "version",
                )
                menuItems.forEach { (label, key) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.openSubMenu(key) }.padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
                        Icon(painterResource(R.drawable.ic_chevron_right), "이동", Modifier.size(16.dp), tint = C.TextSec)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = C.Border, thickness = 0.5.dp)
                }

                Spacer(Modifier.height(32.dp))
            }

            // 토스트
            uiState.toastMessage?.let { msg ->
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF444444)).padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text(msg, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = Color.White))
                    }
                }
            }
        }
    }
}

// 프로필 수정 화면
@Composable
private fun ProfileEditScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    BackHandler { viewModel.closeEdit() }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { viewModel.closeEdit() }) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = C.TextPri)
                    }
                    Text(
                        "프로필 수정",
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                    Box(Modifier.size(48.dp))
                }
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
                Button(
                    onClick  = { viewModel.saveEdit() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding().height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = C.Accent),
                ) {
                    Text("완료", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = Color.White))
                }
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            // 프로필 이미지
            Box(modifier = Modifier.size(100.dp)) {
                Box(Modifier.size(100.dp).clip(CircleShape).background(Color(0xFFD0D0D0))) {
                    AsyncImage(
                        model              = uiState.profile.profileImageUrl,
                        contentDescription = uiState.profile.name,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF888888).copy(alpha = 0.85f)).align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    // ic_camera.xml
                    Icon(painterResource(R.drawable.ic_media_camera), "카메라", Modifier.size(18.dp), tint = Color.White)
                }
            }
            // TODO: 갤러리 연동 - rememberLauncherForActivityResult 추가요망

            Spacer(Modifier.height(28.dp))

            // 기본 정보
            Text("기본 정보", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))

            listOf(
                Triple("이름",  uiState.editName,     { /* TODO: 이름 수정 시트 */ }),
                Triple("생일",  uiState.editBirthday, { /* TODO: 날짜 피커 */ }),
                Triple("성별",  uiState.editGender,   { /* TODO: 성별 선택 시트 */ }),
                Triple("국적",  uiState.profile.countryName(), { /* TODO: 국적 선택 시트 */ }),
            ).forEach { (label, value, onTap) ->
                EditInfoRow(label = label, value = value, onTap = onTap)
            }

            Spacer(Modifier.height(28.dp))

            // 자기소개
            Text("자기소개", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape    = RoundedCornerShape(12.dp),
                color    = Color(0xFFF8F8F8),
                border   = BorderStroke(0.5.dp, C.Border),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    BasicTextField(
                        value         = uiState.editBio,
                        onValueChange = viewModel::updateEditBio,
                        modifier      = Modifier.fillMaxWidth().defaultMinSize(minHeight = 80.dp),
                        textStyle     = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextPri, lineHeight = 22.sp),
                        decorationBox = { inner ->
                            if (uiState.editBio.isEmpty()) Text("자기소개를 입력해주세요", style = TextStyle(fontSize = 14.sp, color = C.TextSec))
                            inner()
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${uiState.editBio.length}/150",
                        style    = TextStyle(fontSize = 11.sp, color = if (uiState.editBio.length >= 150) C.TextRed else C.TextSec),
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// 편집 정보 행
@Composable
private fun EditInfoRow(label: String, value: String, onTap: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable(onClick = onTap),
        shape    = RoundedCornerShape(12.dp),
        color    = Color.White,
        border   = BorderStroke(0.8.dp, C.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            // 국적 행에는 국기 이모지
            val display = if (label == "국적") "🇺🇸 $value" else value
            Text(display, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
            Icon(painterResource(R.drawable.ic_chevron_right), "수정", Modifier.size(16.dp), tint = C.TextSec)
        }
    }
}

// 태그 편집 화면
@Composable
private fun TagEditScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    BackHandler { viewModel.closeTagEdit() }
    var categoryIdx by remember { mutableIntStateOf(0) }
    val categories = listOf("취미/관심사", "성격/MBTI", "음식", "엔터테인먼트")

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { viewModel.closeTagEdit() }) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = C.TextPri)
                    }
                    Text("태그 편집", style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(
                        "완료",
                        style    = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.Accent, fontWeight = FontWeight(600)),
                        modifier = Modifier.padding(horizontal = 12.dp).clickable { viewModel.saveTagEdit() },
                    )
                }
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Text("${uiState.editingTags.size}/15", style = TextStyle(fontSize = 13.sp, color = C.TextSec), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            ScrollableTabRow(selectedTabIndex = categoryIdx, containerColor = Color.White, contentColor = C.Accent, edgePadding = 0.dp) {
                categories.forEachIndexed { idx, cat ->
                    Tab(selected = categoryIdx == idx, onClick = { categoryIdx = idx }, text = { Text(cat, style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily)) })
                }
            }
            Spacer(Modifier.height(12.dp))
            val filtered = ExploreDemo.allTags.filter { it.category == listOf("HOBBY","PERSONALITY","FOOD","ENTERTAINMENT")[categoryIdx] }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                filtered.chunked(4).forEach { rowTags ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        rowTags.forEach { tag ->
                            val isSel = uiState.editingTags.any { it.tag == tag.tag }
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(if (isSel) C.TagBg else C.TagBgGray)
                                    .border(if (isSel) 1.dp else 0.dp, if (isSel) C.Accent else Color.Transparent, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .clickable { viewModel.toggleTag(tag) },
                            ) {
                                Text("${tag.emoji} ${tag.displayName}", style = TextStyle(fontSize = 12.sp, color = if (isSel) C.Accent else C.TagTextGray, fontFamily = PretendardFamily))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 서브 메뉴 화면 분기
@Composable
private fun SubMenuScreen(key: String, onBack: () -> Unit) {
    when (key) {
        "settings" -> SettingsScreen(onBack = onBack)
        "version"  -> VersionScreen(onBack = onBack)
        else       -> EmptySubScreen(
            title = when (key) {
                "guide"  -> "에브리버디 가이드"
                "notice" -> "공지사항"
                else     -> key
            },
            onBack = onBack,
        )
    }
}

// 설정 화면
@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var soundEnabled    by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            SubScreenTopBar(title = "설정", onBack = onBack)
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            Text("알림", style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = C.TextSec), modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))

            // 소리 알림
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("소리", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
                Switch(
                    checked         = soundEnabled,
                    onCheckedChange = { soundEnabled = it },
                    // TODO: DataStore에 소리 설정 저장
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = C.Accent),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = C.Border, thickness = 0.5.dp)

            // 진동 알림
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("진동", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
                Switch(
                    checked         = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it },
                    // TODO: DataStore에 진동 설정 저장
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = C.Accent),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = C.Border, thickness = 0.5.dp)

            // TODO: 추가 설정 항목 추가요망 (언어 설정, 개인정보 처리방침 등)
        }
    }
}

// 버전정보 화면
@Composable
private fun VersionScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var showOptimizeScreen by remember { mutableStateOf(false) }

    if (showOptimizeScreen) {
        EmptySubScreen(title = "앱 최적화", onBack = { showOptimizeScreen = false })
        return
    }

    Scaffold(
        topBar = { SubScreenTopBar(title = "버전정보", onBack = onBack) },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))

            // 앱 아이콘
            Box(
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(20.dp)).background(C.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_nav_chat),
                    contentDescription = "앱 아이콘",
                    modifier = Modifier.size(56.dp),
                    tint = Color.White,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("EveryBuddy", style = TextStyle(fontSize = 20.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri))
            Spacer(Modifier.height(6.dp))
            Text("버전 1.0.0", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextSec))

            Spacer(Modifier.height(40.dp))
            HorizontalDivider(color = C.Border, thickness = 8.dp)

            // 앱 최적화
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showOptimizeScreen = true }.padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("앱 최적화", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
                Icon(painterResource(R.drawable.ic_chevron_right), "이동", Modifier.size(16.dp), tint = C.TextSec)
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = C.Border, thickness = 0.5.dp)
        }
    }
}

// 공통 빈 서브페이지 (가이드, 공지사항 등)
@Composable
private fun EmptySubScreen(title: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = { SubScreenTopBar(title = title, onBack = onBack) },
        containerColor = Color.White,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("TODO: $title 내용 추가요망", style = TextStyle(fontSize = 14.sp, color = C.TextSec))
        }
    }
}

// 서브 화면 공통 TopBar
@Composable
private fun SubScreenTopBar(title: String, onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = C.TextPri)
            }
            Text(title, style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Box(Modifier.size(48.dp))
        }
        HorizontalDivider(color = C.Border, thickness = 0.5.dp)
    }
}

// 네비 아이템
@Composable
private fun MyNavItem(label: String, iconRes: Int, isSelected: Boolean, onClick: () -> Unit) {
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

// 편의 함수
private fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)
