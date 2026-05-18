package com.everybuddy.app.ui.explore

// =============================================================================
// FilterSettingScreen.kt
// 필터 설정 화면
//
// 섹션: 성별 | 나이(스무딩 슬라이더) | 국적 | 언어 | 활동빈도 | 사용자태그
// 하단: [초기화] [적용하기]
// =============================================================================

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import com.everybuddy.app.R
import com.everybuddy.app.ui.theme.PretendardFamily

private val C = AppColors

private const val AGE_MIN = FilterSettings.AGE_MIN
private const val AGE_MAX = FilterSettings.AGE_MAX

// 탭 카테고리
private val tagCategories = listOf("취미/관심사", "성격/MBTI", "음식", "엔터테인먼트")

// 국적 BottomSheet 옵션 (서버 enum 코드)
private val countryOptions = listOf("KOREA", "USA", "JAPAN", "CHINA", "FRANCE", "CZECH")

@Composable
fun FilterSettingScreen(
    current : FilterSettings,
    onApply : (FilterSettings) -> Unit,
    onBack  : () -> Unit,
) {
    BackHandler(onBack = onBack)

    // 로컬 편집 상태
    var gender          by remember { mutableStateOf(current.gender) }
    var minAge          by remember { mutableFloatStateOf(current.minAge.toFloat()) }
    var maxAge          by remember { mutableFloatStateOf(current.maxAge.toFloat()) }
    var country         by remember { mutableStateOf(current.country) }
    var languages       by remember { mutableStateOf(current.languages) }
    var activityFilters by remember { mutableStateOf(current.activityFilters) }
    var selectedTags    by remember { mutableStateOf(current.selectedTags) }
    var tagCategoryIdx  by remember { mutableIntStateOf(0) }
    var countrySheetOpen by remember { mutableStateOf(false) }

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
                        "필터",
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Box(Modifier.size(48.dp))
                }
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = C.Border, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 초기화 버튼
                    OutlinedButton(
                        onClick        = {
                            gender = GenderFilter.ALL; minAge = AGE_MIN.toFloat()
                            maxAge = AGE_MAX.toFloat(); country = null; languages = emptyList()
                            activityFilters = emptySet(); selectedTags = emptyList()
                        },
                        modifier       = Modifier.weight(1f).height(52.dp),
                        shape          = RoundedCornerShape(12.dp),
                        border         = BorderStroke(1.dp, C.Border),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(painterResource(R.drawable.ic_loading), "초기화", Modifier.size(16.dp), tint = C.TextPri)
                            Text("초기화", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
                        }
                    }
                    // 적용하기
                    Button(
                        onClick = {
                            onApply(FilterSettings(
                                gender          = gender,
                                minAge          = minAge.toInt(),
                                maxAge          = maxAge.toInt(),
                                country         = country,
                                languages       = languages,
                                activityFilters = activityFilters,
                                selectedTags    = selectedTags,
                                isOnline        = activityFilters.contains(ActivityFilter.ONLINE),
                                recentlyActive  = activityFilters.contains(ActivityFilter.RECENT),
                            ))
                        },
                        modifier = Modifier.weight(2f).height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = C.Accent),
                    ) {
                        Text("적용하기", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = Color.White))
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0),
        containerColor = Color.White,
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {

            // ── 성별 ──────────────────────────────────────────────────────
            FilterSection(title = "성별", desc = "소통하고 싶은 친구의 성별을 선택해주세요") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GenderFilter.entries.forEach { g ->
                        FilterChip(
                            selected = gender == g,
                            onClick  = { gender = g },
                            label    = { Text(g.label, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily)) },
                            shape    = RoundedCornerShape(20.dp),
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = C.Accent,
                                selectedLabelColor     = Color.White,
                            ),
                        )
                    }
                }
            }

            HorizontalDivider(color = C.Border.copy(alpha = 0.5f), thickness = 6.dp)

            // ── 나이 ──────────────────────────────────────────────────────
            FilterSection(
                title = "나이",
                desc  = "소통하고 싶은 친구의 나이를 선택해주세요",
                badge = "${minAge.toInt()}~${maxAge.toInt()}세",
            ) {
                AgeRangeSlider(
                    minAge        = minAge,
                    maxAge        = maxAge,
                    onRangeChange = { newMin, newMax -> minAge = newMin; maxAge = newMax },
                )
            }

            HorizontalDivider(color = C.Border.copy(alpha = 0.5f), thickness = 6.dp)

            // ── 국적 ──────────────────────────────────────────────────────
            FilterSection(title = "국적", desc = "소통하고 싶은 친구의 국적을 선택해주세요") {
                SelectionRow(
                    label = country?.let { "${countryFlag(it)} ${countryName(it)}" } ?: "모든 국적",
                    onTap = { countrySheetOpen = true },
                )
            }

            HorizontalDivider(color = C.Border.copy(alpha = 0.5f), thickness = 6.dp)

            // ── 언어 ──────────────────────────────────────────────────────
            FilterSection(title = "언어", desc = "내가 학습하고 싶은 언어를 사용하는 친구를 찾아봐요") {
                LanguageMultiSelect(
                    selected = languages,
                    onToggle = { code ->
                        languages = if (code in languages) languages - code else languages + code
                    },
                )
            }

            HorizontalDivider(color = C.Border.copy(alpha = 0.5f), thickness = 6.dp)

            // ── 활동빈도 ──────────────────────────────────────────────────
            FilterSection(title = "활동빈도", desc = "활동이 활발한 친구를 선택할 수 있어요") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActivityFilter.entries.forEach { af ->
                        val isSelected = activityFilters.contains(af)
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                activityFilters = if (isSelected) activityFilters - af else activityFilters + af
                            },
                            shape  = RoundedCornerShape(12.dp),
                            color  = if (isSelected) C.TagBg else Color.White,
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) C.Accent else C.Border),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(
                                    painter            = painterResource(
                                        when (af) {
                                            ActivityFilter.ONLINE -> R.drawable.markbtn1
                                            ActivityFilter.RECENT -> R.drawable.markbtn3
                                        }
                                    ),
                                    contentDescription = null,
                                    contentScale       = ContentScale.Fit,
                                    modifier           = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(af.label, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = if (isSelected) C.Accent else C.TextPri))
                                    Text(af.desc,  style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = C.TextSec))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = C.Border.copy(alpha = 0.5f), thickness = 6.dp)

            // ── 사용자 태그 ──────────────────────────────────────────────
            FilterSection(
                title = "사용자 태그",
                desc  = "관심사나 성격이 맞는 친구를 골라볼 수 있어요.",
            ) {
                // 선택된 태그 표시
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("선택한 태그", style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = C.TextSec))
                    Text("${selectedTags.size}/15", style = TextStyle(fontSize = 12.sp, color = C.TextSec))
                }
                Spacer(Modifier.height(8.dp))
                if (selectedTags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(selectedTags) { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(1.5.dp, C.Accent, RoundedCornerShape(20.dp))
                                    .background(C.TagBg)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .clickable { selectedTags = selectedTags - tag },
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${tag.emoji} ${tag.displayName}", style = TextStyle(fontSize = 12.sp, color = C.Accent, fontFamily = PretendardFamily))
                                    Text("×", style = TextStyle(fontSize = 12.sp, color = C.Accent))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // 카테고리 탭
                ScrollableTabRow(
                    selectedTabIndex = tagCategoryIdx,
                    containerColor   = Color.White,
                    contentColor     = C.Accent,
                    edgePadding      = 0.dp,
                ) {
                    tagCategories.forEachIndexed { idx, cat ->
                        Tab(
                            selected = tagCategoryIdx == idx,
                            onClick  = { tagCategoryIdx = idx },
                            text     = { Text(cat, style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily)) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                // 태그 그리드 (온보딩 동일 목록)
                val filteredTags = ExploreDemo.allTags.filter { tag ->
                    when (tagCategoryIdx) {
                        0 -> tag.category == "HOBBY"
                        1 -> tag.category == "PERSONALITY"
                        2 -> tag.category == "FOOD"
                        3 -> tag.category == "ENTERTAINMENT"
                        else -> true
                    }
                }
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val rowCount = 3
                    repeat(rowCount) { rowIdx ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredTags.filterIndexed { idx, _ -> idx % rowCount == rowIdx }.forEach { tag ->
                                val isSel = selectedTags.any { it.tag == tag.tag }
                                Box(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(19.dp))
                                        .background(if (isSel) Color(0xFFDEF0FF) else Color(0xFFF5F5F5))
                                        .border(1.7.dp, if (isSel) C.Accent else Color.Transparent, RoundedCornerShape(19.dp))
                                        .clickable {
                                            selectedTags = if (isSel) selectedTags - tag
                                            else if (selectedTags.size < 15) selectedTags + tag else selectedTags
                                        }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${tag.emoji} ${tag.displayName}",
                                        style = TextStyle(
                                            fontSize   = 15.sp,
                                            color      = if (isSel) C.Accent else C.TagTextGray,
                                            fontFamily = PretendardFamily,
                                            fontWeight = if (isSel) FontWeight(700) else FontWeight(600),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (countrySheetOpen) {
        CountryPickerSheet(
            selected = country,
            onSelect = { code ->
                country = code
                countrySheetOpen = false
            },
            onDismiss = { countrySheetOpen = false },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 국적 BottomSheet
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryPickerSheet(
    selected  : String?,
    onSelect  : (String?) -> Unit,
    onDismiss : () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        ) {
            Text(
                "국적 선택",
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style    = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri),
            )
            HorizontalDivider(color = C.Border, thickness = 0.5.dp)

            CountryPickerRow(
                label    = "모든 국적",
                isSelected = selected == null,
                onClick  = { onSelect(null) },
            )
            countryOptions.forEach { code ->
                CountryPickerRow(
                    label    = "${countryFlag(code)} ${countryName(code)}",
                    isSelected = selected == code,
                    onClick  = { onSelect(code) },
                )
            }
        }
    }
}

@Composable
private fun CountryPickerRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = TextStyle(
                fontSize   = 15.sp,
                fontFamily = PretendardFamily,
                fontWeight = if (isSelected) FontWeight(700) else FontWeight(500),
                color      = if (isSelected) C.Accent else C.TextPri,
            ),
        )
        if (isSelected) {
            Icon(
                painter            = painterResource(R.drawable.ic_check),
                contentDescription = "선택됨",
                modifier           = Modifier.size(18.dp),
                tint               = C.Accent,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 필터 섹션 래퍼
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FilterSection(title: String, desc: String, badge: String? = null, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            // TODO padding: 섹션 horizontal 16dp, vertical 20dp
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = C.TextPri))
            badge?.let { Text(it, style = TextStyle(fontSize = 13.sp, color = C.TextSec, fontFamily = PretendardFamily)) }
        }
        Spacer(Modifier.height(4.dp))
        Text(desc, style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = C.TextSec))
        Spacer(Modifier.height(14.dp))
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 선택 행 (국적/언어 → 하위 페이지)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SelectionRow(label: String, onTap: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        shape    = RoundedCornerShape(12.dp),
        color    = Color.White,
        border   = BorderStroke(1.dp, C.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(C.Border), contentAlignment = Alignment.Center) {
                    Text("ALL", style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight(700), color = C.TextSec))
                }
                Text(label, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = C.TextPri))
            }
            Icon(painterResource(R.drawable.ic_chevron_right), "선택", Modifier.size(16.dp), tint = C.TextSec)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgeRangeSlider(
    minAge        : Float,
    maxAge        : Float,
    onRangeChange : (Float, Float) -> Unit,
) {
    RangeSlider(
        value         = minAge..maxAge,
        onValueChange = { r ->
            onRangeChange(
                r.start.coerceIn(AGE_MIN.toFloat(), maxAge - 1),
                r.endInclusive.coerceIn(minAge + 1, AGE_MAX.toFloat()),
            )
        },
        valueRange    = AGE_MIN.toFloat()..AGE_MAX.toFloat(),
        steps         = 0,
        track         = { rs ->
            SliderDefaults.Track(
                rangeSliderState   = rs,
                modifier           = Modifier.height(2.dp),
                colors             = SliderDefaults.colors(
                    activeTrackColor   = C.Accent,
                    inactiveTrackColor = C.Border,
                ),
            )
        },
        startThumb    = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, C.Accent, CircleShape),
            )
        },
        endThumb      = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, C.Accent, CircleShape),
            )
        },
        modifier      = Modifier.fillMaxWidth(),
    )
}

// 언어 다중 선택 — 서버 enum 코드 기준
@Composable
private fun LanguageMultiSelect(
    selected : List<String>,
    onToggle : (String) -> Unit,
) {
    val options = listOf(
        "영어"     to "ENGLISH",
        "일본어"   to "JAPANESE",
        "한국어"   to "KOREAN",
        "중국어"   to "CHINESE",
        "프랑스어" to "FRENCH",
    )
    Column(
        modifier            = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (label, code) ->
                val isSel = code in selected
                FilterChip(
                    selected = isSel,
                    onClick  = { onToggle(code) },
                    label    = { Text(label, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily)) },
                    shape    = RoundedCornerShape(20.dp),
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = C.Accent,
                        selectedLabelColor     = Color.White,
                    ),
                )
            }
        }
    }
}

// 편의 함수
private fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)
