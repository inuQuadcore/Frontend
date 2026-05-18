package com.everybuddy.app.ui.my

// MyPageScreen — 마이페이지

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    viewModel       : MyViewModel,
    onNotification  : () -> Unit = {},
    hasNotification : Boolean    = false,
    onLogout        : () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val logoutComplete by viewModel.logoutComplete.collectAsState()

    LaunchedEffect(uiState.toastMessage) {
        if (uiState.toastMessage != null) { delay(2000); viewModel.consumeToast() }
    }
    LaunchedEffect(logoutComplete) {
        if (logoutComplete) onLogout()
    }

    when {
        uiState.isEditMode           -> ProfileEditScreen(viewModel = viewModel)
        uiState.isTagEditOpen        -> TagEditScreen(viewModel = viewModel)
        uiState.openLanguageCode != null -> LanguageEditSubPage(
            language = uiState.openLanguageCode!!,
            currentLevel = uiState.profile.learningLanguages.find {
                it.language.equals(uiState.openLanguageCode, ignoreCase = true)
            }?.level ?: 1,
            isSaving = uiState.isSaving,
            onSave   = { level -> viewModel.saveLanguageLevel(uiState.openLanguageCode!!, level) },
            onBack   = viewModel::closeLanguage,
        )
        uiState.openSubMenu != null  -> SubMenuScreen(key = uiState.openSubMenu!!, viewModel = viewModel, onBack = viewModel::closeSubMenu)
        else -> MyPageContent(viewModel = viewModel, onNotification = onNotification, hasNotification = hasNotification)
    }
}

// 마이페이지 메인 콘텐츠
@Composable
private fun MyPageContent(
    viewModel       : MyViewModel,
    onNotification  : () -> Unit = {},
    hasNotification : Boolean    = false,
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 16.dp),
                ) {
                    Text(
                        "마이",
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                    )
                    Row(
                        modifier              = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(onClick = { viewModel.openSubMenu("settings") }, modifier = Modifier.size(40.dp)) {
                            Icon(painterResource(R.drawable.ic_settings), "설정", Modifier.size(24.dp), tint = C.TextPri)
                        }
                        Box {
                            IconButton(onClick = onNotification, modifier = Modifier.size(40.dp)) {
                                Icon(painterResource(R.drawable.ic_alarm), "알림", Modifier.size(24.dp), tint = C.TextPri)
                            }
                            if (hasNotification) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(C.Accent)
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-6).dp, y = 6.dp),
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
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
                        Text("${profile.age}세 · ${profile.gender}", style = TextStyle(fontSize = 12.sp, color = C.TextSec, fontFamily = PretendardFamily))
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(profile.bio, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextPri, lineHeight = 22.sp), modifier = Modifier.padding(horizontal = 16.dp))

                if (profile.consecutiveDays > 0) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_calendar), "출석", Modifier.size(14.dp), tint = C.TextSec)
                        Text(
                            "연속 ${profile.consecutiveDays}일 출석",
                            style = TextStyle(fontSize = 12.sp, color = C.TextSec, fontFamily = PretendardFamily),
                        )
                    }
                }

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
                    "차단한 친구"     to "blocked",
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

                Text(
                    "로그아웃",
                    style    = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = Color(0xFFE53935)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutDialog = true }
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = C.Border, thickness = 0.5.dp)

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

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor   = Color.White,
            title = {
                Text(
                    "로그아웃",
                    style = TextStyle(fontSize = 17.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                )
            },
            text = {
                Text(
                    "로그아웃하시겠습니까?",
                    style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextSec),
                )
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextSec))
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; viewModel.logout() }) {
                    Text("확인", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = Color(0xFFE53935), fontWeight = FontWeight(600)))
                }
            },
        )
    }
}

// 프로필 수정 화면
@Composable
private fun ProfileEditScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    BackHandler { viewModel.closeEdit() }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.setPendingImageUri(uri.toString())
    }

    var editSubPage by remember { mutableStateOf<String?>(null) }

    when (editSubPage) {
        "name"     -> NameEditSubPage(
            current  = uiState.editName,
            onSave   = { viewModel.updateEditName(it); editSubPage = null },
            onBack   = { editSubPage = null },
        )
        "birthday" -> BirthdayPickerSubPage(
            current = uiState.editBirthday,
            onSave  = { viewModel.updateEditBirthday(it); editSubPage = null },
            onBack  = { editSubPage = null },
        )
        "gender"   -> GenderSelectSubPage(
            current = uiState.editGender,
            onSave  = { viewModel.updateEditGender(it); editSubPage = null },
            onBack  = { editSubPage = null },
        )
        "country"  -> CountrySelectSubPage(
            current = uiState.editCountry,
            onSave  = { viewModel.updateEditCountry(it); editSubPage = null },
            onBack  = { editSubPage = null },
        )
        else -> {
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
                            enabled  = !uiState.isSaving,
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

                    Box(modifier = Modifier.size(100.dp).clickable { imageLauncher.launch("image/*") }) {
                        Box(Modifier.size(100.dp).clip(CircleShape).background(Color(0xFFD0D0D0))) {
                            AsyncImage(
                                model              = uiState.pendingImageUri ?: uiState.profile.profileImageUrl,
                                contentDescription = uiState.profile.name,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize(),
                            )
                        }
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF888888).copy(alpha = 0.85f)).align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_media_camera),
                                contentDescription = null,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    Text("기본 정보", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    Spacer(Modifier.height(12.dp))

                    listOf(
                        Triple("이름", uiState.editName,              { editSubPage = "name" }),
                        Triple("생일", uiState.editBirthday,          { editSubPage = "birthday" }),
                        Triple("성별", uiState.editGender,            { editSubPage = "gender" }),
                        Triple(
                            "국적",
                            "${countryFlag(uiState.editCountry)} ${countryName(uiState.editCountry)}",
                            { editSubPage = "country" },
                        ),
                    ).forEach { (label, value, onTap) ->
                        EditInfoRow(label = label, value = value, onTap = onTap)
                    }

                    Spacer(Modifier.height(28.dp))

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
            Text(value, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
            Icon(painterResource(R.drawable.ic_chevron_right), "수정", Modifier.size(16.dp), tint = C.TextSec)
        }
    }
}

// 이름 수정 서브페이지
@Composable
private fun NameEditSubPage(current: String, onSave: (String) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var name by remember { mutableStateOf(current) }
    Scaffold(
        topBar = { SubScreenTopBar(title = "이름 수정", onBack = onBack) },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Spacer(Modifier.height(12.dp))
            Text("이름", style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = C.TextSec))
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8F8F8), border = BorderStroke(0.5.dp, C.Border), modifier = Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = name, onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                    textStyle = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri),
                    singleLine = true,
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onSave(name) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = C.Accent),
            ) {
                Text("저장", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = Color.White))
            }
        }
    }
}

// 생년월일 선택 서브페이지 (년/월/일 분리)
@Composable
private fun BirthdayPickerSubPage(current: String, onSave: (String) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    val parsed = current.split(".").let {
        Triple(
            it.getOrNull(0)?.toIntOrNull() ?: 2000,
            it.getOrNull(1)?.toIntOrNull() ?: 1,
            it.getOrNull(2)?.toIntOrNull() ?: 1,
        )
    }

    var selectedYear  by remember { mutableStateOf(parsed.first) }
    var selectedMonth by remember { mutableStateOf(parsed.second) }
    var selectedDay   by remember { mutableStateOf(parsed.third) }

    val years  = (1950..2015).toList()
    val months = (1..12).toList()
    val days   = (1..31).toList()

    Scaffold(
        topBar = { SubScreenTopBar(title = "생년월일", onBack = onBack) },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DrumRollPicker(
                    label    = "년",
                    items    = years.map { "${it}년" },
                    selected = years.indexOf(selectedYear).coerceAtLeast(0),
                    onSelect = { selectedYear = years[it] },
                    modifier = Modifier.weight(2f),
                )
                DrumRollPicker(
                    label    = "월",
                    items    = months.map { "${it}월" },
                    selected = months.indexOf(selectedMonth).coerceAtLeast(0),
                    onSelect = { selectedMonth = months[it] },
                    modifier = Modifier.weight(1f),
                )
                DrumRollPicker(
                    label    = "일",
                    items    = days.map { "${it}일" },
                    selected = days.indexOf(selectedDay).coerceAtLeast(0),
                    onSelect = { selectedDay = days[it] },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick  = { onSave("%04d.%02d.%02d".format(selectedYear, selectedMonth, selectedDay)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = C.Accent),
            ) {
                Text("저장", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = Color.White))
            }
        }
    }
}

@Composable
private fun DrumRollPicker(
    label    : String,
    items    : List<String>,
    selected : Int,
    onSelect : (Int) -> Unit,
    modifier : Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = C.TextSec))
        Spacer(Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8F8F8), border = BorderStroke(0.5.dp, C.Border), modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.height(180.dp),
                state    = androidx.compose.foundation.lazy.rememberLazyListState(
                    initialFirstVisibleItemIndex = (selected - 1).coerceAtLeast(0),
                ),
                contentPadding = PaddingValues(vertical = 72.dp),
            ) {
                items.forEachIndexed { idx, item ->
                    item(key = idx) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(idx) }
                                .background(if (idx == selected) C.Accent.copy(alpha = 0.08f) else Color.Transparent)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                item,
                                style = TextStyle(
                                    fontSize   = 15.sp,
                                    fontFamily = PretendardFamily,
                                    color      = if (idx == selected) C.Accent else C.TextPri,
                                    fontWeight = if (idx == selected) FontWeight(600) else FontWeight(400),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

// 성별 선택 서브페이지
@Composable
private fun GenderSelectSubPage(current: String, onSave: (String) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var selected by remember { mutableStateOf(current) }
    val options = listOf("남성", "여성")
    Scaffold(
        topBar = { SubScreenTopBar(title = "성별", onBack = onBack) },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Spacer(Modifier.height(12.dp))
            options.forEach { opt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = opt }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(opt, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
                    if (selected == opt) {
                        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(18.dp), tint = C.Accent)
                    }
                }
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onSave(selected) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = C.Accent),
            ) {
                Text("저장", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = Color.White))
            }
        }
    }
}

// 국적 선택 서브페이지
@Composable
private fun CountrySelectSubPage(current: String, onSave: (String) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var selected by remember { mutableStateOf(current) }
    val options = listOf(
        "KOREA"  to "한국",
        "USA"    to "미국",
        "JAPAN"  to "일본",
        "CHINA"  to "중국",
        "FRANCE" to "프랑스",
    )
    Scaffold(
        topBar = { SubScreenTopBar(title = "국적", onBack = onBack) },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Spacer(Modifier.height(12.dp))
            options.forEach { (code, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = code }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(name, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
                    if (selected == code) {
                        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(18.dp), tint = C.Accent)
                    }
                }
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onSave(selected) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = C.Accent),
            ) {
                Text("저장", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = Color.White))
            }
        }
    }
}

// 언어 레벨 수정 하위 페이지
@Composable
private fun LanguageEditSubPage(
    language     : String,
    currentLevel : Int,
    isSaving     : Boolean,
    onSave       : (Int) -> Unit,
    onBack       : () -> Unit,
) {
    BackHandler(onBack = onBack)
    var selectedLevel by remember { mutableIntStateOf(currentLevel) }

    val langName = when (language.uppercase()) {
        "ENGLISH"  -> "영어"; "JAPANESE" -> "일본어"; "KOREAN"  -> "한국어"
        "CHINESE"  -> "중국어"; "FRENCH" -> "프랑스어"; else     -> language
    }
    val levelLabels = listOf("입문", "초급", "중급", "고급", "원어민")

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = C.TextPri)
                    }
                    Text(
                        langName,
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                    Box(Modifier.size(48.dp))
                }
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                langName,
                style = TextStyle(fontSize = 22.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                levelLabels.getOrElse(selectedLevel - 1) { "" },
                style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.Accent, fontWeight = FontWeight(600)),
            )

            Spacer(Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(5) { idx ->
                    val isSelected = idx < selectedLevel
                    Box(
                        modifier = Modifier
                            .size(if (idx == selectedLevel - 1) 22.dp else 16.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) C.Accent else C.Border)
                            .clickable { selectedLevel = idx + 1 },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                levelLabels.forEachIndexed { idx, label ->
                    Text(
                        label,
                        style    = TextStyle(
                            fontSize   = 11.sp,
                            fontFamily = PretendardFamily,
                            color      = if (idx < selectedLevel) C.Accent else C.TextSec,
                            fontWeight = if (idx == selectedLevel - 1) FontWeight(700) else FontWeight(400),
                        ),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick  = onBack,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, C.Border),
                ) {
                    Text("취소", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, color = C.TextPri))
                }
                Button(
                    onClick  = { onSave(selectedLevel) },
                    enabled  = !isSaving,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = C.Accent),
                ) {
                    Text("수정 완료", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = Color.White))
                }
            }
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
                        style    = TextStyle(
                            fontSize   = 15.sp,
                            fontFamily = PretendardFamily,
                            color      = if (uiState.isSaving) C.TextSec else C.Accent,
                            fontWeight = FontWeight(600),
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp).clickable(enabled = !uiState.isSaving) { viewModel.saveTagEdit() },
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
private fun SubMenuScreen(key: String, viewModel: MyViewModel, onBack: () -> Unit) {
    when (key) {
        "settings" -> SettingsScreen(viewModel = viewModel, onBack = onBack)
        "version"  -> VersionScreen(onBack = onBack)
        "guide"    -> GuideScreen(onBack = onBack)
        "notice"   -> NoticeScreen(onBack = onBack)
        "blocked"  -> {
            val uiState by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { viewModel.loadBlockedUsers() }
            FriendManageScreen(
                title        = "차단한 친구",
                actionLabel  = "차단 해제",
                dialogText   = "차단을 해제하시겠습니까?",
                friends      = uiState.blockedFriends.map {
                    FriendListItem(it.userId, it.name, it.nationality, it.languages)
                },
                isLoading    = uiState.isLoadingBlocked,
                errorMessage = uiState.blockedError,
                onRetry      = { viewModel.loadBlockedUsers() },
                onAction     = { userId -> viewModel.unblockFriend(userId) },
                onBack       = onBack,
            )
        }
        else       -> EmptySubScreen(title = key, onBack = onBack)
    }
}

private data class FriendListItem(
    val userId      : Long,
    val name        : String,
    val nationality : String,
    val languages   : List<String>,
)

@Composable
private fun FriendManageScreen(
    title        : String,
    actionLabel  : String,
    dialogText   : String,
    friends      : List<FriendListItem>,
    isLoading    : Boolean  = false,
    errorMessage : String?  = null,
    onRetry      : () -> Unit = {},
    onAction     : (Long) -> Unit = {},
    onBack       : () -> Unit,
) {
    BackHandler(onBack = onBack)
    var confirmTarget by remember { mutableStateOf<FriendListItem?>(null) }

    Scaffold(
        topBar = { SubScreenTopBar(title = title, onBack = onBack) },
        containerColor = Color.White,
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = C.Accent)
            }
        } else if (errorMessage != null) {
            Column(
                modifier            = Modifier.padding(innerPadding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(errorMessage, style = TextStyle(fontSize = 14.sp, color = C.TextSec, fontFamily = PretendardFamily))
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onRetry) {
                    Text("다시 시도", style = TextStyle(fontSize = 14.sp, color = C.Accent, fontFamily = PretendardFamily))
                }
            }
        } else if (friends.isEmpty()) {
            Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("목록이 없습니다.", style = TextStyle(fontSize = 14.sp, color = C.TextSec, fontFamily = PretendardFamily))
            }
        } else {
            LazyColumn(
                modifier       = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(friends) { friend ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    friend.name,
                                    style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                                )
                                friend.languages.forEach { lang ->
                                    Text(
                                        lang,
                                        style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = C.Accent),
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                friend.nationality,
                                style = TextStyle(fontSize = 11.sp, fontFamily = PretendardFamily, color = C.TextSec),
                            )
                        }
                        OutlinedButton(
                            onClick  = { confirmTarget = friend },
                            shape    = RoundedCornerShape(8.dp),
                            border   = BorderStroke(1.dp, C.Border),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(actionLabel, style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = C.TextPri))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = C.Border, thickness = 0.5.dp)
                }
            }
        }
    }

    confirmTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmTarget = null },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(16.dp),
            text = {
                Text(
                    dialogText,
                    style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAction(target.userId)
                    confirmTarget = null
                }) {
                    Text("예", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.Accent, fontWeight = FontWeight(600)))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmTarget = null }) {
                    Text("아니오", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextSec))
                }
            },
        )
    }
}

// 설정 화면
@Composable
private fun SettingsScreen(viewModel: MyViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val uiState by viewModel.uiState.collectAsState()
    var soundEnabled     by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var settingsSubPage  by remember { mutableStateOf<String?>(null) }

    when (settingsSubPage) {
        "blocked" -> {
            LaunchedEffect(Unit) { viewModel.loadBlockedUsers() }
            FriendManageScreen(
                title        = "차단한 친구",
                actionLabel  = "차단 해제",
                dialogText   = "차단을 해제하시겠습니까?",
                friends      = uiState.blockedFriends.map {
                    FriendListItem(it.userId, it.name, it.nationality, it.languages)
                },
                isLoading    = uiState.isLoadingBlocked,
                errorMessage = uiState.blockedError,
                onRetry      = { viewModel.loadBlockedUsers() },
                onAction     = { userId -> viewModel.unblockFriend(userId) },
                onBack       = { settingsSubPage = null },
            )
        }
        else -> {

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

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("소리", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
                Switch(
                    checked         = soundEnabled,
                    onCheckedChange = { soundEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = C.Accent),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = C.Border, thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("진동", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
                Switch(
                    checked         = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = C.Accent),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = C.Border, thickness = 0.5.dp)

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = C.Border, thickness = 8.dp)

            listOf("차단한 친구" to "blocked").forEach { (label, key) ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { settingsSubPage = key }.padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
                    Icon(painterResource(R.drawable.ic_chevron_right), "이동", Modifier.size(16.dp), tint = C.TextSec)
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = C.Border, thickness = 0.5.dp)
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = C.Border, thickness = 8.dp)
            Text(
                "회원 탈퇴",
                style    = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = Color(0xFFE53935)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !uiState.isSaving) { showDeleteDialog = true }
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = C.Border, thickness = 0.5.dp)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isSaving) showDeleteDialog = false },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(16.dp),
            title = {
                Text(
                    "회원 탈퇴",
                    style = TextStyle(fontSize = 17.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                )
            },
            text = {
                Text(
                    "탈퇴하시겠습니까?\n계정과 데이터는 복구할 수 없습니다.",
                    style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextSec, lineHeight = 20.sp),
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !uiState.isSaving,
                ) {
                    Text("취소", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextSec))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteMyAccount() },
                    enabled = !uiState.isSaving,
                ) {
                    Text("탈퇴", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color(0xFFE53935)))
                }
            },
        )
    }

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
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(C.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = "앱 아이콘",
                    modifier = Modifier.size(96.dp),
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

// 에브리버디 가이드 화면
@Composable
private fun GuideScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = { SubScreenTopBar(title = "에브리버디 가이드", onBack = onBack) },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text("EveryBuddy란?", style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri))
            Spacer(Modifier.height(10.dp))
            Text(
                "EveryBuddy는 서로 다른 언어를 배우고 싶은 사람들을 연결해주는 언어 교환 앱이에요. 원어민과 자유롭게 대화하며 자연스럽게 언어를 익혀보세요.",
                style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextPri, lineHeight = 22.sp),
            )

            Spacer(Modifier.height(28.dp))
            GuideSection(
                title = "01. 친구 찾기",
                body  = "탐색 탭에서 관심사와 학습 언어를 기준으로 나와 잘 맞는 친구를 발견해보세요. 필터를 활용하면 원하는 조건의 친구를 더 빠르게 찾을 수 있어요.",
            )
            Spacer(Modifier.height(20.dp))
            GuideSection(
                title = "02. 대화하기",
                body  = "친구와 1:1 채팅 또는 그룹 채팅을 시작할 수 있어요. 채팅 중 마음에 드는 표현은 스크립트로 저장해두면 나중에 다시 복습할 수 있어요.",
            )
            Spacer(Modifier.height(20.dp))
            GuideSection(
                title = "03. 스크립트 저장",
                body  = "채팅 중 배운 문장, 단어를 스크립트 탭에 저장하고 폴더로 정리해보세요. 원어민 발음을 음성으로 들으며 반복 학습할 수 있어요.",
            )
            Spacer(Modifier.height(20.dp))
            GuideSection(
                title = "04. 상태메시지",
                body  = "친구 탭에서 나만의 상태메시지를 작성하고 친구들의 메시지에 가볍게 답장해보세요. 짧은 문장으로도 언어 교환이 시작돼요.",
            )
            Spacer(Modifier.height(20.dp))
            GuideSection(
                title = "05. 연속 출석",
                body  = "매일 앱을 방문하면 연속 출석 일수가 쌓여요. 꾸준한 학습 습관을 만들고 프로필에 출석 뱃지를 자랑해보세요.",
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GuideSection(title: String, body: String) {
    Column {
        Text(title, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.Accent))
        Spacer(Modifier.height(6.dp))
        Text(body, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = C.TextPri, lineHeight = 22.sp))
    }
}

// 공지사항 화면 — 빈 페이지
@Composable
private fun NoticeScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = { SubScreenTopBar(title = "공지사항", onBack = onBack) },
        containerColor = Color.White,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize())
    }
}

// 공통 빈 서브페이지
@Composable
private fun EmptySubScreen(title: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = { SubScreenTopBar(title = title, onBack = onBack) },
        containerColor = Color.White,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize())
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
