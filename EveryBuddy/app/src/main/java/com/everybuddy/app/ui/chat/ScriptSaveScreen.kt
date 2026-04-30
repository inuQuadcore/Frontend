package com.everybuddy.app.ui.chat

// =============================================================================
// ScriptSaveScreen.kt  (최종 디자인 반영 v2)
// =============================================================================

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.everybuddy.app.R
import com.everybuddy.app.data.chat.ChatMessage
import com.everybuddy.app.data.chat.MessageType
import com.everybuddy.app.ui.theme.PretendardFamily
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// 색상
// ─────────────────────────────────────────────────────────────────────────────
private val SsAccent     = Color(0xFF0167FF)
private val SsTextPri    = Color(0xFF000000)
private val SsTextSec    = Color(0xFF797979)
private val SsTextRed    = Color(0xFFFF3333)
private val SsBorder     = Color(0xFFE5E5E5)
private val SsBubbleGray = Color(0xFFE5E6E8)
private val SsBubbleBlue = Color(0xFFDEF0FF)
private val SsSelectBg   = Color(0x0A0167FF)

// ─────────────────────────────────────────────────────────────────────────────
// 모델
// ─────────────────────────────────────────────────────────────────────────────
data class ScriptFolder(
    val id         : String,
    val name       : String,
    val count      : Int    = 0,
    val coverImage : String = "",  // TODO: URL or local path
)

val dummyScriptFolders = listOf(
    ScriptFolder("f01", "영어",  15),
    ScriptFolder("f02", "일본어", 27),
)

data class ScriptSaveItem(
    val messageId      : String,
    val originalText   : String,
    val translatedText : String  = "",
    val memo           : String  = "",
    val selectedFolder : String? = null,
)

enum class CaptureOption { COMBINED, SEPARATE }

// ─────────────────────────────────────────────────────────────────────────────
// MessageContextMenu
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MessageContextMenu(
    onDismiss    : () -> Unit,
    onCopy       : () -> Unit,
    onSelectCopy : () -> Unit,
    onReply      : () -> Unit,
    onSaveScript : () -> Unit,
) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier        = Modifier.widthIn(min = 160.dp).clickable {},
            shape           = RoundedCornerShape(12.dp),
            color           = Color.White,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                listOf(
                    "복사"              to onCopy,
                    "선택 복사"         to onSelectCopy,
                    "답장"              to onReply,
                    "대화 스크립트 저장" to onSaveScript,
                ).forEach { (label, action) ->
                    Text(
                        text     = label,
                        style    = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SsTextPri),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { action(); onDismiss() }
                            // TODO padding: 메뉴 항목 horizontal 20dp / vertical 12dp
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    if (label != "대화 스크립트 저장") HorizontalDivider(color = SsBorder, thickness = 0.5.dp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ScriptSaveSheet — 스크립트 저장 바텀시트
//
// 1/2: [다음] 파란 버튼 full-width + [취소하기] 빨간 텍스트
// 2/2: [저장] 파란 버튼 full-width + [이전으로] 검정 텍스트
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptSaveSheet(
    message     : ChatMessage,
    step        : Int,
    folders     : List<ScriptFolder> = dummyScriptFolders,
    onNext      : () -> Unit,
    onBack      : () -> Unit,
    onSave      : (ScriptSaveItem) -> Unit,
    onDismiss   : () -> Unit,
    onAddFolder : () -> Unit = {},
) {
    var editedText     by remember { mutableStateOf(message.text) }
    var memo           by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            // dragHandle 람다는 BoxScope — align 사용 가능
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFDDDDDD)),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                // TODO padding: 바텀시트 본문 horizontal 패딩 (현재 16dp)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            // 타이틀 — 숫자+괄호만 파란색
            // TODO padding: 타이틀 상단 16dp / 하단 20dp
            Spacer(Modifier.height(16.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = SsTextPri)) { append("스크립트 저장 ") }
                    withStyle(SpanStyle(color = SsAccent))  { append("($step/2)") }
                },
                style     = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600)),
                modifier  = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            // 원문 카드
            OriginalTextCard(text = editedText, onChange = { editedText = it })

            // 뜻 섹션
            // TODO padding: 뜻 섹션 상단 패딩 (현재 20dp)
            Spacer(Modifier.height(20.dp))
            SsLabel("뜻")
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8F8F8))
                    .border(0.5.dp, SsBorder, RoundedCornerShape(10.dp))
                    // TODO padding: 뜻 카드 내부 패딩 (현재 12dp)
                    .padding(12.dp),
            ) {
                Column {
                    Text(
                        text  = message.translatedText.ifEmpty { "(번역 없음)" },
                        style = TextStyle(
                            fontSize   = 14.sp,
                            fontFamily = PretendardFamily,
                            color      = if (message.translatedText.isEmpty()) SsTextSec else SsTextPri,
                            lineHeight  = 21.sp,
                        ),
                    )
                    // TODO padding: 글자수 카운터 상단 패딩 (현재 6dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text     = "${message.translatedText.length}자",
                        style    = TextStyle(fontSize = 11.sp, color = SsTextSec),
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }

            // 메모 섹션
            // TODO padding: 메모 섹션 상단 패딩 (현재 20dp)
            Spacer(Modifier.height(20.dp))
            SsLabel("메모")
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8F8F8))
                    .border(0.5.dp, SsBorder, RoundedCornerShape(10.dp))
                    // TODO padding: 메모 카드 내부 패딩 (현재 12dp)
                    .padding(12.dp),
            ) {
                Column {
                    BasicTextField(
                        value         = memo,
                        onValueChange = { if (it.length <= 150) memo = it },
                        modifier      = Modifier
                            .fillMaxWidth()
                            // TODO padding: 메모 입력 최소 높이 (현재 60dp)
                            .defaultMinSize(minHeight = 60.dp),
                        textStyle     = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = SsTextPri),
                        decorationBox = { inner ->
                            if (memo.isEmpty()) Text("언어를 배울 때 처음 쓰기 좋은 인삿말", style = TextStyle(fontSize = 14.sp, color = SsTextSec))
                            inner()
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${memo.length}/150", style = TextStyle(fontSize = 11.sp, color = SsTextSec), modifier = Modifier.align(Alignment.End))
                }
            }

            // 폴더 선택
            // TODO padding: 폴더 선택 섹션 상단 패딩 (현재 20dp)
            Spacer(Modifier.height(20.dp))
            SsLabel("폴더 선택")
            // TODO padding: 폴더 목록 상단 패딩 (현재 10dp)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(folders) { folder ->
                    ScriptFolderThumbnail(
                        folder     = folder,
                        isSelected = selectedFolder == folder.id,
                        onClick    = { selectedFolder = folder.id },
                    )
                }
                item {
                    // + 폴더추가 버튼
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier            = Modifier
                            // TODO size: 폴더추가 버튼 전체 크기 (현재 72x90dp)
                            .size(width = 72.dp, height = 90.dp)
                            .clickable(onClick = onAddFolder),
                    ) {
                        Box(
                            modifier         = Modifier
                                // TODO size: + 원 크기 (현재 44dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.dp, SsBorder, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("+", style = TextStyle(fontSize = 22.sp, color = SsTextSec))
                        }
                        // TODO padding: "폴더추가" 텍스트 상단 패딩 (현재 4dp)
                        Spacer(Modifier.height(4.dp))
                        Text("폴더추가", style = TextStyle(fontSize = 11.sp, color = SsTextSec))
                    }
                }
            }

            // 하단 버튼
            // TODO padding: 하단 버튼 영역 상단 패딩 (현재 28dp)
            Spacer(Modifier.height(28.dp))
            Button(
                onClick  = if (step == 1) onNext else {
                    { onSave(ScriptSaveItem(message.id, editedText, message.translatedText, memo, selectedFolder)) }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SsAccent),
            ) {
                Text(
                    if (step == 1) "다음" else "저장",
                    style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color.White),
                )
            }
            // TODO padding: 보조 버튼 상단 패딩 (현재 12dp)
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick  = if (step == 1) onDismiss else onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (step == 1) "취소하기" else "이전으로",
                    style = TextStyle(
                        fontSize   = 15.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(500),
                        color      = if (step == 1) SsTextRed else SsTextPri,
                    ),
                )
            }
            // TODO padding: 바텀시트 하단 여백 (현재 8dp)
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SsLabel — 섹션 제목
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SsLabel(text: String) {
    Text(
        text  = text,
        style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = SsTextPri),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// OriginalTextCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun OriginalTextCard(text: String, onChange: (String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, SsBorder, RoundedCornerShape(10.dp))
            // TODO padding: 원문 카드 내부 패딩 (현재 16dp)
            .padding(16.dp),
    ) {
        Column {
            if (isEditing) {
                BasicTextField(
                    value         = text,
                    onValueChange = onChange,
                    modifier      = Modifier.fillMaxWidth(),
                    textStyle     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SsTextPri, lineHeight = 23.sp),
                )
            } else {
                Text(text = text, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SsTextPri, lineHeight = 23.sp))
            }
            // TODO padding: 수정하기 버튼 행 상단 패딩 (현재 10dp)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text("버튼을 눌러 원하는 대로 수정해보세요.", style = TextStyle(fontSize = 12.sp, color = SsTextSec))
                OutlinedButton(
                    onClick        = { isEditing = !isEditing },
                    shape          = RoundedCornerShape(20.dp),
                    border         = BorderStroke(0.5.dp, SsBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        if (isEditing) "✓ 완료하기" else "✏ 수정하기",
                        style = TextStyle(fontSize = 12.sp, color = SsTextSec),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ScriptFolderThumbnail
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ScriptFolderThumbnail(folder: ScriptFolder, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            // TODO size: 폴더 썸네일 크기 (현재 120x90dp)
            .size(width = 120.dp, height = 90.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFD0D0D0))   // TODO: coil AsyncImage
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) SsAccent else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                // TODO padding: 폴더 오버레이 패딩 (현재 horizontal 8dp, vertical 5dp)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(folder.name, style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(500), color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${folder.count}", style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f)))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ConversationSelectScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ConversationSelectScreen(
    messages : List<ChatMessage>,
    roomName : String,
    onBack   : () -> Unit,
    onSave   : (List<ChatMessage>, CaptureOption) -> Unit,
) {
    var selectedRange     by remember { mutableStateOf<IntRange?>(null) }
    var captureOptionOpen by remember { mutableStateOf(false) }
    var captureOption     by remember { mutableStateOf(CaptureOption.COMBINED) }

    BackHandler(onBack = onBack)

    val selectedMessages = remember(selectedRange, messages) {
        selectedRange?.let { r -> messages.filterIndexed { i, _ -> i in r } } ?: emptyList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color.White)
                            // TODO padding: 앱바 horizontal 패딩 (현재 4dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(R.drawable.ic_back), "뒤로", modifier = Modifier.size(24.dp), tint = SsTextPri)
                        }
                        Text(
                            "대화 선택",
                            style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = SsTextPri),
                        )
                        Box(modifier = Modifier.size(48.dp))  // 균형용
                    }
                    HorizontalDivider(color = SsBorder, thickness = 0.5.dp)
                }
            },
            bottomBar = {
                Column {
                    HorizontalDivider(color = SsBorder, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            // TODO padding: 하단바 horizontal 20dp / vertical 12dp
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        // 저장옵션 버튼
                        Row(
                            modifier              = Modifier.clickable { captureOptionOpen = true },
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            // TODO icon: ic_list_option.xml ("큰=" 아이콘) — 현재 ic_sidemenu로 임시 대체
                            Icon(painterResource(R.drawable.ic_sidemenu), "저장옵션", modifier = Modifier.size(20.dp), tint = SsTextPri)
                            Text("저장옵션", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SsTextPri))
                        }

                        Button(
                            onClick  = { if (selectedMessages.isNotEmpty()) onSave(selectedMessages, captureOption) },
                            enabled  = selectedMessages.isNotEmpty(),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = SsAccent,
                                disabledContainerColor = Color(0xFFCCCCCC),
                            ),
                            // TODO size: 저장하기 버튼 (현재 width 160dp, height 46dp)
                            modifier = Modifier.width(160.dp).height(46.dp),
                        ) {
                            Text("저장하기", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color.White))
                        }
                    }
                }
            },
            containerColor = Color.Transparent,
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                // 배경 전체 회색
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFAAAAAA)))

                // 안내문구
                Text(
                    "저장하고 싶은 대화를 선택해주세요.",
                    style     = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = SsTextSec),
                    modifier  = Modifier
                        .fillMaxWidth()
                        // TODO padding: 안내문구 상하 패딩 (현재 top 10dp)
                        .padding(top = 10.dp),
                    textAlign = TextAlign.Center,
                )

                // 메시지 목록
                SelectableMessageList(
                    messages      = messages,
                    selectedRange = selectedRange,
                    onRangeSelect = { selectedRange = it },
                    modifier      = Modifier.fillMaxSize(),
                )
            }
        }

        if (captureOptionOpen) {
            CaptureOptionDialog(
                selected  = captureOption,
                onSelect  = { captureOption = it },
                onDismiss = { captureOptionOpen = false },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SelectableMessageList
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SelectableMessageList(
    messages      : List<ChatMessage>,
    selectedRange : IntRange?,
    onRangeSelect : (IntRange?) -> Unit,
    modifier      : Modifier = Modifier,
) {
    LazyColumn(
        modifier       = modifier,
        // TODO padding: 메시지 목록 상하 패딩 (현재 top 36dp — 안내문구 공간 확보)
        contentPadding = PaddingValues(top = 36.dp, bottom = 8.dp),
    ) {
        items(messages.indices.toList()) { index ->
            val isInRange = selectedRange != null && index in selectedRange
            val isFirst   = selectedRange?.first == index
            val isLast    = selectedRange?.last  == index

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val cur = selectedRange
                        onRangeSelect(
                            if (cur == null) index..index
                            else minOf(cur.first, index)..maxOf(cur.last, index)
                        )
                    }
                    .then(
                        if (isInRange) {
                            Modifier
                                .background(Color.White)
                                .drawBehind {
                                    val sw   = 2.dp.toPx()
                                    val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                                    // 상단선: 첫 항목만
                                    if (isFirst) drawLine(SsAccent, Offset(0f, sw / 2), Offset(size.width, sw / 2), sw, pathEffect = dash)
                                    // 하단선: 마지막 항목만
                                    if (isLast)  drawLine(SsAccent, Offset(0f, size.height - sw / 2), Offset(size.width, size.height - sw / 2), sw, pathEffect = dash)
                                    // 좌측선
                                    drawLine(SsAccent, Offset(sw / 2, 0f), Offset(sw / 2, size.height), sw, pathEffect = dash)
                                    // 우측선
                                    drawLine(SsAccent, Offset(size.width - sw / 2, 0f), Offset(size.width - sw / 2, size.height), sw, pathEffect = dash)
                                }
                        } else Modifier
                    ),
            ) {
                SelectableMessageItem(message = messages[index], isMe = messages[index].senderId == "me")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SelectableMessageItem
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SelectableMessageItem(message: ChatMessage, isMe: Boolean) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("a hh:mm") }

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            // TODO padding: 메시지 아이템 horizontal 16dp / vertical 4dp
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
    ) {
        Row(
            verticalAlignment     = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier              = Modifier.fillMaxWidth(),
        ) {
            if (!isMe) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFCCCCCC)))  // TODO: coil
                // TODO padding: 아바타-말풍선 간격 (현재 8dp)
                Spacer(Modifier.width(8.dp))
            }
            Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                if (!isMe) {
                    Text(message.senderName, style = TextStyle(fontSize = 12.sp, color = SsTextSec), modifier = Modifier.padding(bottom = 3.dp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = if (isMe) 14.dp else 4.dp, topEnd = if (isMe) 4.dp else 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                        .background(if (isMe) SsBubbleBlue else SsBubbleGray)
                        // TODO padding: 말풍선 내부 패딩 (현재 horizontal 12dp, vertical 8dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .widthIn(max = 250.dp),
                ) {
                    when (message.type) {
                        MessageType.TEXT ->
                            Text(message.text, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = SsTextPri, lineHeight = 21.sp))
                        MessageType.VOICE ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("▶", style = TextStyle(fontSize = 11.sp, color = SsAccent))
                                Text("음성 메시지", style = TextStyle(fontSize = 13.sp, color = SsTextSec))
                            }
                        else -> {}
                    }
                }
                if (message.translatedText.isNotEmpty()) {
                    // TODO padding: 번역문 상단 패딩 (현재 3dp)
                    Spacer(Modifier.height(3.dp))
                    Text(message.translatedText, style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = SsTextSec, lineHeight = 18.sp))
                }
            }
            // TODO padding: 시간-말풍선 간격 (현재 4dp)
            Spacer(Modifier.width(4.dp))
            Text(message.timestamp.format(timeFormatter), style = TextStyle(fontSize = 10.sp, color = SsTextSec), modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CaptureOptionDialog — 저장옵션
//
// 디자인:
//   헤더: "저장옵션" + 부제목 + X 닫기
//   선택 항목: 테두리 박스 (선택=파란 테두리+연파랑bg+파란텍스트+파란체크원 / 미선택=회색)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CaptureOptionDialog(
    selected  : CaptureOption,
    onSelect  : (CaptureOption) -> Unit,
    onDismiss : () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column(
                // TODO padding: 다이얼로그 내부 패딩 (현재 24dp)
                modifier = Modifier.padding(24.dp),
            ) {
                // 헤더
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "저장옵션",
                            style = TextStyle(fontSize = 17.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = SsTextPri),
                        )
                        // TODO padding: 부제목 상단 패딩 (현재 4dp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "항목을 통합 또는 개별 저장할 수 있습니다.",
                            style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = SsTextSec),
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        // TODO icon: ic_close.xml
                        Text("✕", style = TextStyle(fontSize = 16.sp, color = SsTextSec))
                    }
                }

                // TODO padding: 옵션 목록 상단 패딩 (현재 16dp)
                Spacer(Modifier.height(16.dp))

                listOf(CaptureOption.COMBINED to "통합 저장", CaptureOption.SEPARATE to "따로 저장")
                    .forEachIndexed { idx, (option, label) ->
                        val isSel = selected == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSel) 1.5.dp else 1.dp,
                                    color = if (isSel) SsAccent else SsBorder,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .background(if (isSel) SsSelectBg else Color.Transparent)
                                .clickable { onSelect(option) }
                                // TODO padding: 옵션 항목 내부 패딩 (현재 horizontal 16dp, vertical 14dp)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically,
                        ) {
                            Text(
                                label,
                                style = TextStyle(
                                    fontSize   = 15.sp,
                                    fontFamily = PretendardFamily,
                                    fontWeight = if (isSel) FontWeight(500) else FontWeight(400),
                                    color      = if (isSel) SsAccent else SsTextPri,
                                ),
                            )
                            // 체크 원형
                            Box(
                                modifier         = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) SsAccent else Color.Transparent)
                                    .border(1.5.dp, if (isSel) SsAccent else SsBorder, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSel) Text("✓", style = TextStyle(fontSize = 13.sp, color = Color.White, fontWeight = FontWeight(700)))
                            }
                        }
                        // TODO padding: 옵션 항목 간 간격 (현재 10dp)
                        if (idx == 0) Spacer(Modifier.height(10.dp))
                    }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SavedToast
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SavedToast(message: String = "저장되었습니다.") {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF444444))
            // TODO padding: 토스트 패딩 (현재 horizontal 20dp, vertical 10dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(message, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = Color.White))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NewFolderScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun NewFolderScreen(
    roomName : String,
    onBack   : () -> Unit,
    onSave   : (name: String, imageUri: String?) -> Unit,
) {
    var folderName by remember { mutableStateOf("") }
    var imageUri   by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.White)
                        // TODO padding: 앱바 horizontal 패딩 (현재 4dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", modifier = Modifier.size(24.dp), tint = SsTextPri)
                    }
                    Text(roomName, style = TextStyle(fontSize = 20.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = SsTextPri), modifier = Modifier.weight(1f))
                    IconButton(onClick = {}) { Icon(painterResource(R.drawable.ic_search),   "검색", modifier = Modifier.size(24.dp), tint = SsTextPri) }
                    IconButton(onClick = {}) { Icon(painterResource(R.drawable.ic_sidemenu), "메뉴",  modifier = Modifier.size(24.dp), tint = SsTextPri) }
                }
                HorizontalDivider(color = SsBorder, thickness = 0.5.dp)
                // 탭 인디케이터 + 새 폴더 제목
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.width(60.dp).height(2.5.dp).background(SsTextPri))
                    Text("새 폴더", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(500), color = SsTextPri),
                        // TODO padding: "새 폴더" 텍스트 vertical 패딩 (현재 10dp)
                        modifier = Modifier.padding(vertical = 10.dp))
                    HorizontalDivider(color = SsBorder, thickness = 0.5.dp)
                }
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                // TODO padding: 본문 horizontal 패딩 (현재 16dp)
                .padding(horizontal = 16.dp),
        ) {
            // 폴더명
            // TODO padding: 폴더명 섹션 상단 패딩 (현재 24dp)
            Spacer(Modifier.height(24.dp))
            Text("폴더명", style = TextStyle(fontSize = 14.sp, color = SsTextPri), modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, SsBorder, RoundedCornerShape(8.dp))
                    // TODO padding: 폴더명 입력 필드 내부 패딩 (현재 horizontal 12dp, vertical 14dp)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value         = folderName,
                    onValueChange = { if (it.length <= 10) folderName = it },
                    modifier      = Modifier.weight(1f),
                    textStyle     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SsTextPri),
                )
                Text("${folderName.length}/10", style = TextStyle(fontSize = 12.sp, color = SsTextSec))
            }

            // 폴더 대표 이미지
            // TODO padding: 이미지 섹션 상단 패딩 (현재 24dp)
            Spacer(Modifier.height(24.dp))
            Text("폴더 대표 이미지", style = TextStyle(fontSize = 14.sp, color = SsTextPri), modifier = Modifier.padding(bottom = 10.dp))
            Box(
                modifier = Modifier
                    // TODO size: 폴더 대표 이미지 크기 (현재 150x120dp)
                    .size(width = 150.dp, height = 120.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F0F0))
                    .clickable { /* TODO: 갤러리 런처 */ },
                contentAlignment = Alignment.Center,
            ) {
                if (imageUri != null) {
                    // TODO: coil AsyncImage
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("+", style = TextStyle(fontSize = 28.sp, color = Color(0xFFAAAAAA)))
                        Text("새 폴더", style = TextStyle(fontSize = 12.sp, color = SsTextSec))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 하단 취소 / 저장
            HorizontalDivider(color = SsBorder, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // TODO padding: 하단 버튼 패딩 (현재 horizontal 20dp, vertical 14dp)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onBack) {
                    Text("취소", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SsTextRed))
                }
                TextButton(onClick = { if (folderName.isNotEmpty()) onSave(folderName, imageUri) }, enabled = folderName.isNotEmpty()) {
                    Text("저장", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = if (folderName.isNotEmpty()) SsTextPri else SsTextSec))
                }
            }
        }
    }
}
