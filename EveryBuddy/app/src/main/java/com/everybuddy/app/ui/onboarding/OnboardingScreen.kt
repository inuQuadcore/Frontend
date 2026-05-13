package com.everybuddy.app.ui.onboarding

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.everybuddy.app.R
import com.everybuddy.app.ui.theme.*

// 아이콘 파일 목록 (res/drawable/*.xml)
// ic_back.xml             — 뒤로가기 화살표
// ic_search.xml           — 검색 돋보기 아이콘
// ic_check.xml            — 선택 완료 체크 아이콘
// ic_check_empty.xml      — 미선택 체크 아이콘
// ic_datapicker.xml       — 생일 드롭다운 화살표 (V자)
// ic_male.xml             — 남성
// ic_female.xml           — 여성
// ic_dot_filled.xml       — 레벨 점 채워진 상태 (파란 원)
// ic_dot_empty.xml        — 레벨 점 비어있는 상태 (회색 원)
// ic_tagselectcancel.xml — 선택 태그 제거 X 버튼

// 스크린 시작
/**
 * OnboardingScreen
 *
 * Hilt ViewModel을 주입받는 최상위 진입점.
 * NavGraph에서 이 함수를 직접 호출함.
 * uiState를 구독하고 이벤트 콜백을 ViewModel에 연결한 뒤
 * 실제 UI 구성은 OnboardingHost에 위임.
 */
@Composable
fun OnboardingScreen(
    onFinish  : (autoLoggedIn: Boolean) -> Unit,
    viewModel : OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // 회원가입 API 성공 시 onFinish 호출 (autoLoggedIn 여부에 따라 NavGraph가 MAIN/LOGIN 분기)
    LaunchedEffect(uiState.registerSuccess) {
        if (uiState.registerSuccess) onFinish(uiState.autoLoggedIn)
    }

    OnboardingHost(
        uiState       = uiState,
        onNext        = viewModel::onNext,
        onBack        = viewModel::onBack,
        onStateChange = viewModel::onStateChange,
        onRegister    = viewModel::register,
    )
}

// HOST - STEP ROUTER
/**
 * OnboardingHost
 *
 * currentStep 값에 따라 해당 Step Composable을 렌더링하는 라우터.
 * Step8(앱 소개)에서는 상단 TopBar(뒤로가기 + 인디케이터)를 숨김.
 *
 * Step 구성:
 *  1 → 기본 정보 (이름, 생일, 성별)
 *  2 → 거주 국가 선택
 *  3 → 사용 언어 선택 + 레벨 설정
 *  4 → 학습 언어 선택
 *  5 → 관심사 태그 선택
 *  6 → 한줄 소개 입력
 *  7 → 권한 안내
 *  8 → 앱 기능 소개 (온보딩 완료)
 */
// 시발 가보자

@Composable
fun OnboardingHost(
    uiState       : OnboardingUiState,
    onNext        : () -> Unit,
    onBack        : () -> Unit,
    onStateChange : (OnboardingUiState) -> Unit,
    onRegister    : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        // Step 8(앱 소개)는 뒤로가기/인디케이터 없음
        if (uiState.currentStep < 8) {
            OnboardingTopBar(
                currentStep = uiState.currentStep,
                onBack      = onBack,
            )
        }

        when (uiState.currentStep) {
            1 -> StepBasicInfo(uiState, onStateChange, onNext)
            2 -> StepCountry(uiState, onStateChange, onNext)
            3 -> StepMyLanguage(uiState, onStateChange, onNext)
            4 -> StepLearnLanguage(uiState, onStateChange, onNext)
            5 -> StepInterests(uiState, onStateChange, onNext)
            6 -> StepBio(uiState, onStateChange, onNext)
            7 -> StepPermissions(onNext)
            8 -> StepIntro(
                isLoading    = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onRegister   = onRegister,
            )
        }
    }
}
// 공통 컴포넌트
/**
 * OnboardingTopBar
 *
 * 뒤로가기 버튼 + 단계 인디케이터 바를 표시하는 공통 상단 영역.
 * index < currentStep인 바는 파란색(완료), 나머지는 회색(미완료).
 */

@Composable
fun OnboardingTopBar(currentStep: Int, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 28.dp, top = 24.dp),
    ) {
        // 뒤로가기 버튼
        // 아이콘: res/drawable/ic_back.xml (< 모양 화살표)
        IconButton(
            onClick  = onBack,
            modifier = Modifier.size(24.dp), // 버튼 터치 영역은 좀 크게
        ) {
            Image(
                painter            = painterResource(R.drawable.ic_back),
                contentDescription = "뒤로가기",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.size(14.8.dp),
            )
        }

        Spacer(Modifier.height(30.dp))

        // 스텝 인디케이터(총 ONBOARDING_TOTAL_STEPS = 7개)
        // 파란색: 완료 단계 / 회색: 미완료 단계
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            repeat(ONBOARDING_TOTAL_STEPS) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (index < currentStep) Color(0xFF0167FF)
                            else Color(0xFFD9D9D9),
                        ),
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

// 타이틀(질문 & 안내)
/**
 * StepTitle
 *
 * 각 Step 화면 상단에 표시되는 타이틀(질문)과 서브타이틀(안내) 텍스트.
 */
@Composable
fun StepTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text  = title,
            style = TextStyle(
                fontSize   = 34.sp,
                lineHeight = 40.sp,
                fontFamily = PretendardFamily,
                fontWeight = FontWeight(600),
                color = Color(0xFF000000),
            ),
        )
        Spacer(Modifier.height(12.dp))

        Text(
            text  = subtitle,
            style = TextStyle(
                fontSize   = 17.sp,
                fontFamily = PretendardFamily,
                fontWeight = FontWeight(600),
                color = Color(0xFF8F9399),
            ),
        )
    }
}

// 다음 버튼
/**
 * OnboardingNextButton
 *
 * 온보딩 전 단계에서 공통으로 사용하는 "다음" 버튼.
 * enabled = false일 때 회색으로 비활성화됨.
 */
@Composable
fun OnboardingNextButton(
    text    : String  = "다음",
    enabled : Boolean = true,
    onClick : () -> Unit,
) {
    Button(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape    = RoundedCornerShape(15.dp),
        enabled  = enabled,
        colors   = ButtonDefaults.buttonColors(
            containerColor         = Color(0xFF0167FF),
            contentColor           = Color.White,
            disabledContainerColor = Color(0xFFD9D9D9),
            disabledContentColor   = Color.White,
        ),
    ) {
        Text(
            text  = text,
            style = TextStyle(
                fontSize   = 20.sp,
                fontFamily = PretendardFamily,
                fontWeight = FontWeight(600),
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center,
            ),
        )
    }
}

// STEP 2,3,4 검색 필드
/**
 * OnboardingSearchField
 *
 * Step2(국가), Step3(사용 언어), Step4(학습 언어)에서 공통으로 사용하는 검색 필드.
 * 입력값이 바뀔 때마다 onChange 콜백을 통해 UiState를 업데이트하고,
 * 상위 Composable에서 remember(query)로 필터링 결과를 재계산함.
 */
@Composable
fun OnboardingSearchField(
    query    : String,
    hint     : String,
    onChange : (String) -> Unit,
) {
    OutlinedTextField(
        value         = query,
        onValueChange = onChange,
        placeholder   = {
            Text(
                hint,
                style = TextStyle(
                    fontSize   = 16.sp,
                    fontFamily = PretendardFamily,
                    color      = Color(0xFFA9A9A9),
                ),
            )
        },
        leadingIcon   = {
            // 아이콘: res/drawable/ic_search.xml (돋보기 모양)
            Image(
                painter            = painterResource(R.drawable.ic_search),
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.size(16.dp),
            )
        },
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(10.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color(0xFFF5F5F5),
            focusedContainerColor   = Color(0xFFDEF0FF),
            unfocusedBorderColor    = Color.Transparent,
            focusedBorderColor      = Color(0xFF0167FF),
            cursorColor             = Color(0xFF0167FF),
        ),
        textStyle = TextStyle(
            fontSize   = 16.sp,
            fontFamily = PretendardFamily,
            color      = Color(0xFF1B1B1B),
        ),
    )
}

// 선택 가능한 행 (국가/언어 공용)
// 아이콘: res/drawable/ic_check.xml       — 선택된 상태 (파란 원 + 흰 체크)
// 아이콘: res/drawable/ic_check_empty.xml — 미선택 상태 (회색 원 테두리)
@Composable
fun SelectableRow(
    flag     : String,
    label    : String,
    selected : Boolean,
    onClick  : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFDEF0FF) else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) Color(0xFF0167FF) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(flag, fontSize = 16.sp)
            Spacer(Modifier.width(36.dp))
            Text(
                text  = label,
                style = TextStyle(
                    fontSize   = 16.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = if (selected) FontWeight(600) else FontWeight(500),
                    color      = if (selected) Color(0xFF0167FF) else Color(0xFF1B1B1B),
                ),
            )
        }

        // 선택 아이콘
        // 선택됨: res/drawable/ic_check.xml (파란 원 + 흰 체크)
        // 미선택: res/drawable/ic_check_empty.xml (회색 원 테두리)
        Image(
            painter            = painterResource(
                if (selected) R.drawable.ic_check
                else          R.drawable.ic_check_empty
            ),
            contentDescription = if (selected) "선택됨" else "미선택",
            contentScale       = ContentScale.Fit,
            modifier           = Modifier.size(20.dp),
        )
    }
}

// step.1 이름, 생일, 성별
@Composable
fun StepBasicInfo(
    uiState       : OnboardingUiState,
    onStateChange : (OnboardingUiState) -> Unit,
    onNext        : () -> Unit,
) {
    val canProceed = uiState.name.isNotBlank() && uiState.gender != null &&
            uiState.birthYear != null && uiState.birthMonth != null && uiState.birthDay != null

    // DatePicker 다이얼로그 (showDatePicker = true일 때만 표시)
    if (uiState.showDatePicker) {
        DatePickerModal(
            onConfirm = { y, m, d ->
                onStateChange(
                    uiState.copy(
                        birthYear      = y,
                        birthMonth     = m,
                        birthDay       = d,
                        showDatePicker = false,
                    )
                )
            },
            onDismiss = { onStateChange(uiState.copy(showDatePicker = false)) },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
    ) {
        StepTitle(
            title    = "사용자의\n정보를 입력해주세요",
            subtitle = "서비스 이용을 위한 기본 정보예요.",
        )

        Spacer(Modifier.height(60.dp))

        // 이름
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = "이름",
                style    = TextStyle(
                    fontSize   = 17.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = Color(0xFF8F9399),
                ),
                modifier = Modifier.width(30.dp),
            )
            // 이름 라벨 | 입력 필드 간격
            Spacer(Modifier.width(35.dp))
            OutlinedTextField(
                value         = uiState.name,
                onValueChange = {
                    if (it.length <= 8) onStateChange(uiState.copy(name = it))
                },
                placeholder   = {
                    Text(
                        "이름 (최대 8자)",
                        style = TextStyle(
                            fontSize   = 18.sp,
                            fontFamily = PretendardFamily,
                            color      = Color(0xFFE1E1E1),
                        ),
                    )
                },
                singleLine    = true,
                modifier      = Modifier
                    .width(287.dp)
                    .height(50.dp),
                shape         = RoundedCornerShape(6.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFFFFFFF),
                    focusedContainerColor   = Color(0xFFFFFFFF),
                    unfocusedBorderColor    = Color(0xFFDCDCDC),
                    focusedBorderColor      = Color(0xFF0167FF),
                    cursorColor             = Color(0xFF0167FF),
                ),
                textStyle = TextStyle(
                    fontSize   = 18.sp,
                    fontFamily = PretendardFamily,
                    color      = Color(0xFF1B1B1B),
                ),
            )
        }

        // 이름 라벨 | 생일 라벨 간격
        Spacer(Modifier.height(40.dp))

        // 생일
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = "생일",
                style    = TextStyle(
                    fontSize   = 17.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = Color(0xFF8F9399),
                ),
                modifier = Modifier.width(30.dp),
            )
            Spacer(Modifier.width(35.dp))
            // 버튼 클릭 시 showDatePicker = true로 설정 → DatePickerModal 표시
            OutlinedButton(
                onClick        = { onStateChange(uiState.copy(showDatePicker = true)) },
                modifier       = Modifier
                    .width(287.dp)
                    .height(50.dp),
                shape          = RoundedCornerShape(6.dp),
                border         = BorderStroke(1.dp, Color.Transparent),
                colors         = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFFFFFFF),
                    contentColor   = Color(0xFFE1E1E1),
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text  = if (uiState.birthYear != null)
                            "${uiState.birthYear}년 ${uiState.birthMonth}월 ${uiState.birthDay}일"
                        else "선택",
                        style = TextStyle(
                            fontSize   = 18.sp,
                            fontFamily = PretendardFamily,
                            color      = if (uiState.birthYear != null) Color(0xFF1B1B1B)
                            else Color(0xFFE1E1E1),
                        ),
                    )
                    // 드롭다운 화살표 아이콘: res/drawable/ic_datapicker.xml (V자 화살표)
                    Image(
                        painter            = painterResource(R.drawable.ic_datapicker),
                        contentDescription = null,
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.size(13.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        // 성별
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Gender.entries.forEach { g ->
                val selected = uiState.gender == g
                OutlinedButton(
                    onClick        = { onStateChange(uiState.copy(gender = g)) },
                    modifier       = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape          = RoundedCornerShape(6.dp),
                    border         = BorderStroke(
                        1.dp,
                        color = if (selected) Color(0xFF0167FF) else Color(0xFFDCDCDC),
                    ),
                    colors         = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) Color(0xFFDEF0FF) else Color(0xFFFFFFFF),
                        contentColor   = if (selected) Color(0xFF0167FF) else Color(0xFF8F9399),
                    ),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text  = g.label,
                            style = TextStyle(
                                fontSize   = 18.sp,
                                fontFamily = PretendardFamily,
                                fontWeight = FontWeight(600),
                                color      = if (selected) Color(0xFF0167FF) else Color(0xFF8F9399),
                            ),
                        )
                        Spacer(Modifier.width(4.dp))
                        // 성별 아이콘 (ic_male.xml / ic_female.xml)
                        Icon(
                            painter            = painterResource(id = g.iconRes),
                            contentDescription = g.label,
                            modifier           = Modifier.size(16.dp),
                            // Image의 colorFilter 대신 Icon은 바로 tint를 지원합니다.
                            tint               = if (selected) Color(0xFF0167FF) else Color.Unspecified
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        OnboardingNextButton(enabled = canProceed, onClick = onNext)
        Spacer(Modifier.height(45.dp))
    }
}

// 생일 고르는 Dialog -  DataPickerModal
/**
 * DatePickerModal
 *
 * Material3 DatePickerDialog를 래핑한 생일 선택 다이얼로그.
 * 선택 완료 시 Calendar에서 연/월/일을 추출해 onConfirm으로 전달.
 * 취소 또는 날짜 미선택 상태에서 확인 클릭 시 onDismiss 호출.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onConfirm : (Int, Int, Int) -> Unit,
    onDismiss : () -> Unit,
) {
    // 2000-01-01 UTC 기준으로 시작 — 생일 선택에 적합한 초기 위치
    val defaultMillis = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        set(2000, 0, 1, 0, 0, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    val state = rememberDatePickerState(
        initialSelectedDateMillis   = defaultMillis,
        initialDisplayedMonthMillis = defaultMillis,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { ms ->
                    // UTC 기준으로 파싱해야 타임존에 관계없이 선택한 날짜 그대로 추출됨
                    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                        .apply { timeInMillis = ms }
                    onConfirm(
                        cal.get(java.util.Calendar.YEAR),
                        cal.get(java.util.Calendar.MONTH) + 1,
                        cal.get(java.util.Calendar.DAY_OF_MONTH),
                    )
                } ?: onDismiss()
            }) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    ) {
        DatePicker(state = state)
    }
}

// step.2 국가 선택
@Composable
fun StepCountry(
    uiState       : OnboardingUiState,
    onStateChange : (OnboardingUiState) -> Unit,
    onNext        : () -> Unit,
) {
    // 검색어로 실시간 필터링 (피그마 예시: "독" 입력 → 독일만 표시)
    val filtered = remember(uiState.countryQuery) {
        if (uiState.countryQuery.isEmpty()) sampleCountries
        else sampleCountries.filter {
            it.nameKo.contains(uiState.countryQuery) ||
                    it.code.contains(uiState.countryQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
    ) {
        StepTitle(
            title    = "어느 나라에\n거주하고 계신가요?",
            subtitle = "현재 거주 중인 국가를 선택해 주세요.",
        )
        Spacer(Modifier.height(25.dp))

        // 검색 필드 (ic_search.xml)
        OnboardingSearchField(
            query    = uiState.countryQuery,
            hint     = "국가를 검색해보세요",
            onChange = { onStateChange(uiState.copy(countryQuery = it)) },
        )
        Spacer(Modifier.height(20.dp))

        // 필터링된 국가 리스트
        // 선택됨: ic_check.xml / 미선택: ic_check_empty.xml
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered, key = { it.code }) { country ->
                SelectableRow(
                    flag     = country.flag,
                    label    = country.nameKo,
                    selected = uiState.selectedCountry?.code == country.code,
                    onClick  = { onStateChange(uiState.copy(selectedCountry = country)) },
                )
            }
        }

        OnboardingNextButton(
            enabled = uiState.selectedCountry != null,
            onClick = onNext,
        )
        Spacer(Modifier.height(45.dp))
    }
}

//step.3 사용 언어 (단일 선택)
@Composable
fun StepMyLanguage(
    uiState       : OnboardingUiState,
    onStateChange : (OnboardingUiState) -> Unit,
    onNext        : () -> Unit,
) {
    val filtered = remember(uiState.myLangQuery) {
        if (uiState.myLangQuery.isEmpty()) sampleLanguages
        else sampleLanguages.filter {
            it.nameKo.contains(uiState.myLangQuery) ||
                    it.code.contains(uiState.myLangQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
    ) {
        StepTitle(
            title    = "어떤 언어를\n사용하고 계신가요?",
            subtitle = "사용 가능한 언어 하나를 선택해 주세요.",
        )
        Spacer(Modifier.height(25.dp))

        OnboardingSearchField(
            query    = uiState.myLangQuery,
            hint     = "언어를 검색해보세요",
            onChange = { onStateChange(uiState.copy(myLangQuery = it)) },
        )
        Spacer(Modifier.height(20.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered, key = { it.code }) { lang ->
                val selected = uiState.selectedMyLang?.code == lang.code
                SelectableRow(
                    flag     = lang.flag,
                    label    = "${lang.nameKo} / ${lang.code}",
                    selected = selected,
                    onClick  = {
                        onStateChange(uiState.copy(selectedMyLang = if (selected) null else lang))
                    },
                )
            }
        }

        OnboardingNextButton(
            enabled = uiState.selectedMyLang != null,
            onClick = onNext,
        )
        Spacer(Modifier.height(45.dp))
    }
}

// step.4 학습 언어 + 레벨 바텀시트
@Composable
fun StepLearnLanguage(
    uiState       : OnboardingUiState,
    onStateChange : (OnboardingUiState) -> Unit,
    onNext        : () -> Unit,
) {
    val filtered = remember(uiState.learnQuery) {
        if (uiState.learnQuery.isEmpty()) sampleLanguages
        else sampleLanguages.filter {
            it.nameKo.contains(uiState.learnQuery) ||
                    it.code.contains(uiState.learnQuery, ignoreCase = true)
        }
    }

    if (uiState.showLevelSheet && uiState.selectedLearnLangs.isNotEmpty()) {
        LevelBottomSheet(
            languages     = uiState.selectedLearnLangs,
            onLevelChange = { lang, level ->
                val updated = uiState.selectedLearnLangs.map {
                    if (it.code == lang.code) it.copy(level = level) else it
                }
                onStateChange(uiState.copy(selectedLearnLangs = updated))
            },
            onConfirm = {
                onStateChange(uiState.copy(showLevelSheet = false))
                onNext()
            },
            onDismiss = { onStateChange(uiState.copy(showLevelSheet = false)) },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
    ) {
        StepTitle(
            title    = "학습하고 싶은\n언어는 무엇인가요?",
            subtitle = "관심 있는 언어를 골라 학습을 시작해요.",
        )
        Spacer(Modifier.height(25.dp))

        OnboardingSearchField(
            query    = uiState.learnQuery,
            hint     = "언어를 검색해보세요",
            onChange = { onStateChange(uiState.copy(learnQuery = it)) },
        )
        Spacer(Modifier.height(20.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered, key = { it.code }) { lang ->
                val selected = uiState.selectedLearnLangs.any { it.code == lang.code }
                SelectableRow(
                    flag     = lang.flag,
                    label    = "${lang.nameKo} / ${lang.code}",
                    selected = selected,
                    onClick  = {
                        val updated = if (selected)
                            uiState.selectedLearnLangs.filter { it.code != lang.code }
                        else
                            uiState.selectedLearnLangs + lang
                        onStateChange(uiState.copy(selectedLearnLangs = updated))
                    },
                )
            }
        }

        OnboardingNextButton(
            enabled = uiState.selectedLearnLangs.isNotEmpty(),
            onClick = { onStateChange(uiState.copy(showLevelSheet = true)) },
        )
        Spacer(Modifier.height(45.dp))
    }
}

// step.5 관심사 태그
/**
 * StepInterests
 *
 * 관심사 태그를 카테고리 탭으로 분류하여 선택하는 Step.
 * - 탭: InterestCategory enum 순서대로 표시
 * - 태그 그리드: 선택된 탭의 태그를 3열 그리드로 표시
 * - 선택된 태그: 상단 LazyRow에 가로 스크롤 칩으로 표시
 * - 최대 선택 개수: TAG_MAX_COUNT = 15개 (초과 시 추가 불가)
 * - 첫 3개 태그가 다른 유저에게 노출됨 (안내 문구로 표시)
 * - 다음 버튼 활성화 조건: selectedTags.isNotEmpty()
 *
 * */
@Composable
fun StepInterests(
    uiState       : OnboardingUiState,
    onStateChange : (OnboardingUiState) -> Unit,
    onNext        : () -> Unit,
) {
    val tabTags = sampleTags.filter { it.category == uiState.selectedTab }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
    ) {
        StepTitle(
            title    = "평소 관심있는 것은\n무엇인가요?",
            subtitle = "선택한 관심사는 나중에 수정할 수 있어요.",
        )
        Spacer(Modifier.height(25.dp))

        // 선택된 태그 헤더
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text = "내가 선택한 태그",
                style = TextStyle(
                    fontSize   = 19.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = Color(0xFF000000),
                ),
            )
            // 카운트
            Text(
                "${uiState.selectedTags.size}/$TAG_MAX_COUNT",
                style = TextStyle(
                    fontSize   = 16.sp,
                    fontFamily = PretendardFamily,
                    color      = Color(0xFF8F9399),
                ),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            "처음 세 개의 태그가 다른 친구들에게 노출되어요!",
            style = TextStyle(
                fontSize   = 12.sp,
                fontFamily = PretendardFamily,
                color      = Color(0xFF8F9399),
            ),
        )
        Spacer(Modifier.height(25.dp))

        // 선택된 태그 가로 스크롤 칩
        if (uiState.selectedTags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier              = Modifier.fillMaxWidth(),
            ) {
                items(uiState.selectedTags, key = { it.label }) { tag ->
                    SelectedTagChip(
                        tag      = tag,
                        onRemove = {
                            onStateChange(
                                uiState.copy(selectedTags = uiState.selectedTags - tag)
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(35.dp))
        }

        // 카테고리 탭 (취미/관심사 | 성격/MBTI | 음식 | 엔터테인먼트)
        PrimaryScrollableTabRow(
            selectedTabIndex = InterestCategory.entries.indexOf(uiState.selectedTab),
            containerColor   = Color.Transparent,
            contentColor     = Color(0xFF000000),
            edgePadding      = 0.dp,
        ) {
            InterestCategory.entries.forEach { cat ->
                Tab(
                    selected = uiState.selectedTab == cat,
                    onClick  = { onStateChange(uiState.copy(selectedTab = cat)) },
                    text     = {
                        Text(
                            text  = cat.label,
                            style = TextStyle(
                                fontSize   = 19.sp,
                                fontFamily = PretendardFamily,
                                fontWeight = FontWeight(600),
                                color      = if (uiState.selectedTab == cat) Color(0xFF000000)  // 선택됨
                                else Color(0xFFACACAC),                             // 미선택
                            ),
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(25.dp))

        // 태그 — 3열 고정, 세로 스크롤 (기본), 가로 스크롤 (태그가 화면 폭 초과 시)
        // 각 태그는 wrapContentWidth → 글자 길이에 맞게 폭 결정, 짤림 없음
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
        ) {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tabTags.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { tag ->
                            TagChip(
                                tag      = tag,
                                selected = uiState.selectedTags.contains(tag),
                                onClick  = {
                                    val cur = uiState.selectedTags
                                    if (cur.contains(tag)) {
                                        onStateChange(uiState.copy(selectedTags = cur - tag))
                                    } else if (cur.size < TAG_MAX_COUNT) {
                                        onStateChange(uiState.copy(selectedTags = cur + tag))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        OnboardingNextButton(
            enabled = uiState.selectedTags.isNotEmpty(),
            onClick = onNext,
        )
        Spacer(Modifier.height(7.dp))
    }
}

// 태그 칩 — 텍스트 길이에 맞게 폭이 결정됨
@Composable
private fun TagChip(
    tag      : InterestTag,
    selected : Boolean,
    onClick  : () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .wrapContentWidth()
            .clip(RoundedCornerShape(19.dp))
            .background(if (selected) Color(0xFFDEF0FF) else Color(0xFFF5F5F5))
            .border(
                width = 1.7.dp,
                color = if (selected) Color(0xFF0167FF) else Color(0xFFE4E6EC),
                shape = RoundedCornerShape(19.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = "${tag.emoji} ${tag.label}",
            style = TextStyle(
                fontSize   = 15.sp,
                fontFamily = PretendardFamily,
                fontWeight = if (selected) FontWeight(700) else FontWeight(600),
                color      = if (selected) Color(0xFF0167FF) else Color(0xFF797979),
            ),
            maxLines = 1,
        )
    }
}

// 선택된 태그 가로 칩 (X 버튼 포함)
@Composable
private fun SelectedTagChip(tag: InterestTag, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(color = Color(0xFFDEF0FF), shape = RoundedCornerShape(21.dp))
            .border(
                width = 1.7.dp,
                color = Color(0xFF0167FF),
                shape = RoundedCornerShape(21.dp),
            )
            .padding(start = 13.dp, top = 10.dp, end = 16.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.Start),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text  = "${tag.emoji} ${tag.label}",
            style = TextStyle(
                fontSize   = 14.sp,
                fontFamily = PretendardFamily,
                fontWeight = FontWeight(700),
                color      = Color(0xFF0167FF),
            ),
            maxLines = 1,
        )
        // X 버튼 (ic_tagselectcancel.xml)
        Icon(
            painter            = painterResource(id = R.drawable.ic_tagselectcancel),
            contentDescription = "삭제",
            tint               = Color(0xFF0167FF),
            modifier           = Modifier
                .size(16.dp)
                .clickable { onRemove() },
        )
    }
}

//step.6 한줄 소개(150자 제한)
/**
 * StepBio
 *
 * - BIO_MAX_LENGTH(150자) 초과 시: 텍스트 빨간색 + 글자 수 카운터 빨간색 + 다음 버튼 비활성화
 * - 입력 박스 외부 클릭 시 키보드 닫기 (focusManager.clearFocus())
 * - 포커스 전, 텍스트 비어있을 때 예시 플레이스홀더 표시
 * - 다음 버튼 활성화 조건: bio.isNotEmpty() && !isOverLimit
 *
 */
@Composable
fun StepBio(
    uiState       : OnboardingUiState,
    onStateChange : (OnboardingUiState) -> Unit,
    onNext        : () -> Unit,
) {
    // 150자 초과 여부 확인
    val isOverLimit = uiState.bio.length > BIO_MAX_LENGTH
    // 포커스 상태를 관리하기 위한 FocusRequester & InteractionSource
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp) // 좌우 여백
            .clickable( // 화면 여백 클릭 시 포커스 해제하여 예시문 재등장 유도, 아캔낫포커스아매니띵
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        StepTitle(
            title    = "한줄로\n나를 소개해보아요!",
            subtitle = "한 줄 소개는 나중에 수정할 수 있어요.",
        )
        Spacer(Modifier.height(50.dp))

        // 소개 입력 박스
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(217.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF2F4F5))
                .border(
                    1.dp,
                    if (isOverLimit) Color(0xFFEE696A) else Color(0xFFDCDCDC),  // 초과 시 빨간 테두리
                    RoundedCornerShape(6.dp),
                ),
            ) {
            BasicTextField(
                value         = uiState.bio,
                onValueChange = {
                    // 사용자 입력은 글자 수 제한 없이 받되, 상태만 업데이트하여 이후 처리
                    onStateChange(uiState.copy(bio = it))
                },
                interactionSource = interactionSource, // 포커스 감지용
                textStyle     = TextStyle(
                    fontSize   = 16.sp,
                    fontFamily = PretendardFamily,
                    // 실제 작성 시 기본 검은색, 150자 초과 시 초과 글자부터 빨간색.
                    color      = if (isOverLimit) Color(0xFFEE696A) else Color(0xFF000000),
                    lineHeight = 21.6.sp,
                    fontWeight = FontWeight(500)
                ),
                modifier      = Modifier
                    .fillMaxSize()
                    .padding(start = 18.dp, top = 15.dp, end = 18.dp, bottom = 26.dp),
                decorationBox = { inner ->
                    Box {
                        /* 플레이스 홀더 로직 설명
                           1. 값(텍스트)가 비어있어야 함 (uiState.bio.isEmpty())
                           2. 작성 박스를 클릭해서 포커스가 잡히지 않은 상태여야 함 (!isFocused)
                           이 두 조건이 충족될 때만 예시문(Placeholder) 표시.
                        */
                        if (uiState.bio.isEmpty() && !isFocused) {
                            Text(
                                text  = "예) 안녕하세요.\n에브리버디에서 함께 언어를 배우면서\n자연스럽게 대화해 보고 싶어요.\n편하게 이야기 나눠요!",
                                style = TextStyle(
                                    fontSize   = 16.sp,
                                    lineHeight = 21.6.sp,
                                    fontFamily = PretendardFamily,
                                    fontWeight = FontWeight(600),
                                    color      = Color(0xFFAEAEAE), // 피그마 예시문 색상
                                ),
                            )
                        }

                        // 살제 텍스트 입력 영역(커서 포함)
                        inner()

                        // 글자 수 카운터
                        Text(
                            text  = "${uiState.bio.length}/$BIO_MAX_LENGTH",
                            style = TextStyle(
                                fontSize   = 13.sp,
                                fontFamily = PretendardFamily,
                                color      = if (isOverLimit) Color(0xFFEE696A) else Color(0xFFAEAEAE),
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 13.dp, bottom = 5.dp),
                        )
                    }
                },
            )
        }

        Spacer(Modifier.weight(1f))
        // 다음 버튼 로직: 글자가 없거나 150자를 초과하면 비활성화
        OnboardingNextButton(
            enabled = uiState.bio.isNotEmpty() && !isOverLimit,
            onClick = onNext
        )
        Spacer(Modifier.height(30.dp)) // 하단 여백
    }
}

// 권한 안내(근데 권한은 사용자 핸드폰에서 접근하는 거 아닌가?)
// >> 실제 권한 요청은 이 화면이 아닌 시스템 팝업을 통해 이루어짐.
// 일단 피그마 디자인 반영 UI 코딩만 함. 별도 동작 OnboardingNextButton만.

@Composable
fun StepPermissions(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
    ) {
        StepTitle(
            title    = "서비스 이용을 위해\n권한 허용이 필요해요.",
            subtitle = "휴대폰에서 권한 허용을 해주세요.",
        )
        Spacer(Modifier.height(50.dp))

        permissionItems.forEach { item ->
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 권한 아이콘 박스(이모지)
                Box(
                    modifier         = Modifier
                        .size(66.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFF2F4F5)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.emoji, fontSize = 30.sp)
                }
                Spacer(Modifier.width(25.dp))
                Column {
                    Text(
                        item.title,
                        style = TextStyle(
                            fontSize   =18.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight(700),
                            color      = Color(0xFF404040),
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.desc,
                        style = TextStyle(
                            fontSize   = 17.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight(500),
                            color      = Color(0xFF7A7A7A),
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        OnboardingNextButton(text = "확인", onClick = onNext)
        Spacer(Modifier.height(45.dp))
    }
}

// step.8 앱 소개(에브리버디를 소개해 드릴게요!)
/**
 * StepIntro
 *
 * 온보딩의 마지막 화면으로, 앱의 5가지 주요 기능을 소개함.
 * TopBar(뒤로가기/인디케이터) 없음.
 * "에브리버디 시작하기" 버튼 클릭 시 onFinish() 호출 → NavGraph에서 메인으로 이동.
 */
@Composable
fun StepIntro(
    isLoading    : Boolean,
    errorMessage : String?,
    onRegister   : () -> Unit,
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text = "에브리버디를\n소개해 드릴게요!",
            style = TextStyle(
                fontSize   = 34.sp,
                lineHeight = 40.sp,
                fontFamily = PretendardFamily,
                fontWeight = FontWeight(600),
                color      = Color(0xFF000000),
                textAlign  = TextAlign.Center,
            )
        )

        Spacer(Modifier.height(40.dp))

        appFeatures.forEach { feature ->
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 19.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier         = Modifier
                        .size(66.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFF2F4F5)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter            = painterResource(feature.iconRes),
                        contentDescription = feature.title,
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.width(25.dp))
                Column {
                    Text(
                        feature.title,
                        style = TextStyle(
                            fontSize   = 19.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight(700),
                            color      = Color(0xFF404040),
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        feature.desc,
                        style = TextStyle(
                            fontSize   = 17.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight(500),
                            color      = Color(0xFF7A7A7A),
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        errorMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text  = msg,
                    style = TextStyle(
                        fontSize   = 11.sp,
                        fontFamily = PretendardFamily,
                        color      = Color(0xFFFF2B01),
                        textAlign  = TextAlign.Center,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // 회원가입 완료 버튼 — 클릭 시 모든 데이터를 서버로 전송
        Button(
            onClick  = onRegister,
            enabled  = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape    = RoundedCornerShape(15.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = Color(0xFF0167FF),
                contentColor           = Color.White,
                disabledContainerColor = Color(0xFFD9D9D9),
                disabledContentColor   = Color.White,
            ),
        ) {
            if (isLoading) {
                LoadingIndicator(modifier = Modifier.size(24.dp), tint = Color.White)
            } else {
                Text(
                    "회원가입 완료",
                    style = TextStyle(
                        fontSize   = 20.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(600),
                        color      = Color(0xFFFFFFFF),
                        textAlign  = TextAlign.Center,
                    ),
                )
            }
        }
        Spacer(Modifier.height(45.dp))
    }
}

// step.3 소통 레벨 바텀시트
/**
 * LevelBottomSheet
 *
 * Step3(사용 언어)에서 선택한 언어들의 숙련도(1~5)를 설정하는 바텀시트.
 * - 점 5개(ic_dot_filled / ic_dot_empty)와 Slider가 연동됨<<중요중요
 * - Slider steps = 3 → 0, 1, 2, 3, 4, 5 총 5단계 (0과 5 사이 4 스텝)
 * - 점 클릭 또는 슬라이더 조작 시 onLevelChange 호출 → UiState 업데이트
 * - "선택 완료" 버튼 클릭 시 onConfirm() → showLevelSheet = false + onNext()
 *
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelBottomSheet(
    languages     : List<LanguageItem>,
    onLevelChange : (LanguageItem, Int) -> Unit,
    onConfirm     : () -> Unit,
    onDismiss     : () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 30.dp)
                .padding(bottom = 40.dp), // 하단 여백 확보
        ) {
            Text(
                "이 언어로 소통이\n얼만큼 가능하신가요?",
                style = TextStyle(
                    fontSize   = 25.sp,
                    lineHeight = 31.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = Color(0xFF000000),
                ),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "레벨 1~5 중 선택해주세요.",
                style = TextStyle(
                    fontSize   = 17.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(500),
                    color = Color(0xFF8A8A8A),
                ),
            )
            Spacer(Modifier.height(35.dp))

            languages.forEach { lang ->
                // 언어 + 레벨 점 행
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .border(1.dp, Color(0xFFDCDCDC), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 국기는 이모지 처리(Text)
                    Text(text = lang.flag, fontSize = 24.sp)

                    Spacer(Modifier.width(16.dp))
                    Text(
                        "${lang.nameKo} / ${lang.code}",
                        style    = TextStyle(
                            fontSize   = 16.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight(600),
                            color      = Color(0xFF8F9399),
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    // 레벨 점 5개
                    // 채워진 점: res/drawable/ic_dot_filled.xml (파란 원)
                    // 빈 점:     res/drawable/ic_dot_empty.xml  (회색 원)
                    // 벡터 이미지 교체 방식
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(5) { i ->
                            Image(
                                painter            = painterResource(
                                    // i는 0부터 시작
                                    // 예) level이 3이면 i가 0,1,2일 때 filled
                                    if (i < lang.level) R.drawable.ic_dot_filled
                                    else                R.drawable.ic_dot_empty
                                ),
                                contentDescription = "레벨 ${i+1}",
                                modifier = Modifier
                                    .size(20.dp) // 시각적 크기
                                    .padding(2.dp) // 터치 영역 조절용 패딩
                                    // 점을 누를 수 있게 하려고요
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null // 점 클릭 시 물결 효과 제거
                                        /*
                                        * 무슨 리플효과 같은게 생겨서 즉각 반응을 위해 제거
                                        * gpt 참고*/
                                    ) {
                                        onLevelChange(lang, i + 1)
                                    },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 레벨 슬라이더(1~5단계 점과 실시간 연동)
                Slider(
                    value         = lang.level.toFloat().coerceAtLeast(0f),
                    onValueChange = { onLevelChange(lang, it.toInt().coerceIn(0, 5)) },
                    valueRange    = 0f..5f, // 최소 0단계부터 시작
                    steps         = 4,      // 0과 5 사이의 4개 지점(1,2, 3, 4) 생성
                    colors        = SliderDefaults.colors(
                        activeTrackColor   = Color(0xFF0167FF),
                        inactiveTrackColor = Color(0xFFE5E4EB),
                    ),
                    // 슬라이드 바 thumb custom
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(25.dp) // 조절 버튼 크기
                                .background(color = Color.White, shape = CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFE1E1E1),
                                    shape = CircleShape
                                )
                        )
                    },
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                Spacer(Modifier.height(30.dp)) // 언어 항목 간 간격
            }

            OnboardingNextButton(
                text    = "선택 완료",
                onClick = onConfirm
            )
        }
    }
}

// Previews
@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "Step1 기본정보")
@Composable
private fun PreviewStep1() {
    EveryBuddyTheme { OnboardingHost(OnboardingUiState(currentStep = 1), {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "Step1 남성선택")
@Composable
private fun PreviewStep1Male() {
    EveryBuddyTheme { OnboardingHost(OnboardingUiState(currentStep = 1, gender = Gender.MALE), {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "Step2 국가")
@Composable
private fun PreviewStep2() {
    EveryBuddyTheme { OnboardingHost(OnboardingUiState(currentStep = 2), {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "Step3 언어선택")
@Composable
private fun PreviewStep3() {
    EveryBuddyTheme {
        OnboardingHost(
            OnboardingUiState(currentStep = 3, selectedMyLang = sampleLanguages[0]),
            {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "Step4 학습언어")
@Composable
private fun PreviewStep4() {
    EveryBuddyTheme {
        OnboardingHost(
            OnboardingUiState(currentStep = 4, selectedLearnLangs = listOf(sampleLanguages[1])),
            {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "Step5 관심사")
@Composable
private fun PreviewStep5() {
    EveryBuddyTheme {
        OnboardingHost(
            OnboardingUiState(currentStep = 5, selectedTags = listOf(sampleTags[0])),
            {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "Step6 한줄소개")
@Composable
private fun PreviewStep6() {
    EveryBuddyTheme { OnboardingHost(OnboardingUiState(currentStep = 6), {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "Step7 권한")
@Composable
private fun PreviewStep7() {
    EveryBuddyTheme { OnboardingHost(OnboardingUiState(currentStep = 7), {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "Step8 앱소개")
@Composable
private fun PreviewStep8() {
    EveryBuddyTheme { OnboardingHost(OnboardingUiState(currentStep = 8), {}, {}, {}, {}) }
}
