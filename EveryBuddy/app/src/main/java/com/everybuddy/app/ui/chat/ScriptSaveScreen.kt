package com.everybuddy.app.ui.chat

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.everybuddy.app.R
import com.everybuddy.app.data.chat.ChatMessage
import com.everybuddy.app.data.chat.MessageType
import com.everybuddy.app.ui.theme.EveryBuddyTheme
import com.everybuddy.app.ui.theme.PretendardFamily
import java.time.format.DateTimeFormatter

private val SsAccent     = Color(0xFF0167FF)
private val SsTextPri    = Color(0xFF000000)
private val SsTextSec    = Color(0xFF797979)
private val SsTextRed    = Color(0xFFFF3333)
private val SsBorder     = Color(0xFFE5E5E5)
private val SsBubbleGray = Color(0xFFE5E6E8)
private val SsBubbleBlue = Color(0xFFDEF0FF)
private val SsSelectBg   = Color(0x140167FF)

data class ScriptFolder(
    val id         : String,
    val name       : String,
    val count      : Int    = 0,
    val coverImage : String = "",
)

val dummyScriptFolders = listOf(
    ScriptFolder("f01", "영어",  15),
    ScriptFolder("f02", "일본어", 27),
)

data class ScriptSaveItem(
    val messageId      : String,
    val originalText   : String,
    val translatedText : String  = "",
    val memo1          : String  = "",
    val memo2          : String  = "",
    val selectedFolder : String? = null,
)

enum class CaptureOption { COMBINED, SEPARATE }

data class ScriptItem(
    val id             : String,
    val originalText   : String,
    val translatedText : String,
    val memo1          : String  = "",
    val memo2          : String  = "",
    val folderId       : String? = null,
)

val dummyScriptItems = listOf(
    ScriptItem("s01",
        "I open up the messages, then had to hit the zoom. Turns out the girl was really a dude.",
        "메시지를 열어보니 자세히 봐야 했어. 알고 보니 그녀가 사실 남자였던 거야.",
        "확대/축소하기",
        "have to와 함께 쓰면 \"자세히 보다\" 라고도 쓰임",
        "f01"),
    ScriptItem("s02",
        "HE think he slicked back 'til I slipped back.",
        "그놈은 교묘하게 숨겼다고 생각했지만 난 알아냈지.",
        "말만(겉만) 번드르르한, 살짝 돌아가다",
        "\"번드르르하게 넘겼다\"고 생각했겠지, 내가 \"살짝 돌아가기 전까지\"",
        "f01"),
    ScriptItem("s03",
        "She didn't know about me and I didn't know 'bout Sue",
        "그녀도 날 몰랐고, 나도 수에 대해 몰랐고",
        folderId = "f01"),
)

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
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                    if (label != "대화 스크립트 저장") {
                        HorizontalDivider(color = SsBorder, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptSaveSheet(
    message                  : ChatMessage,
    folders                  : List<ScriptFolder> = dummyScriptFolders,
    onSave                   : (ScriptSaveItem) -> Unit,
    onDismiss                : () -> Unit,
    onAddFolder              : () -> Unit = {},
    externalAutoSelectFolderId : String? = null,
    currentIdx               : Int = 0,
    totalCount               : Int = 0,
) {
    var editedText     by remember(message.id) { mutableStateOf(message.text) }
    var memo1          by remember(message.id) { mutableStateOf(message.translatedText) }
    var memo           by remember(message.id) { mutableStateOf("") }
    var selectedFolder by remember(message.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(externalAutoSelectFolderId) {
        if (externalAutoSelectFolderId != null) selectedFolder = externalAutoSelectFolderId
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFDDDDDD)))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text      = if (totalCount > 1) "스크립트 저장  ($currentIdx/$totalCount)" else "스크립트 저장",
                style     = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = SsTextPri),
                modifier  = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            // 원문 카드
            OriginalTextCard(text = editedText, onChange = { editedText = it })

            Spacer(Modifier.height(20.dp))
            SsLabel("뜻")
            Spacer(Modifier.height(8.dp))
            SsMemoField(
                value     = memo1,
                maxLength = 200,
                minHeight = 60.dp,
                hint      = "(번역 없음)",
                onChange  = { if (it.length <= 200) memo1 = it },
            )

            // 메모 섹션 (최대 150자, 힌트 있음)
            Spacer(Modifier.height(20.dp))
            SsLabel("메모")
            Spacer(Modifier.height(8.dp))
            SsMemoField(
                value     = memo,
                maxLength = 150,
                minHeight = 80.dp,
                hint      = "언어를 배울 때 처음 쓰기 좋은 인삿말",
                onChange  = { if (it.length <= 150) memo = it },
            )

            // 폴더 선택
            Spacer(Modifier.height(20.dp))
            SsLabel("폴더 선택")
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier            = Modifier
                            .size(width = 72.dp, height = 90.dp)
                            .clickable(onClick = onAddFolder),
                    ) {
                        Box(
                            modifier         = Modifier.size(44.dp).clip(CircleShape).border(1.dp, SsBorder, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("+", style = TextStyle(fontSize = 22.sp, color = SsTextSec))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("폴더추가", style = TextStyle(fontSize = 11.sp, color = SsTextSec))
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick  = {
                    onSave(ScriptSaveItem(
                        messageId      = message.id,
                        originalText   = editedText,
                        translatedText = message.translatedText,
                        memo1          = memo1,
                        memo2          = memo,
                        selectedFolder = selectedFolder,
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SsAccent),
            ) {
                Text("저장", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color.White))
            }
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("취소하기", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(500), color = SsTextRed))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun OriginalTextCard(text: String, onChange: (String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, SsBorder, RoundedCornerShape(10.dp))
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
                    if (!isEditing) {
                        Image(painterResource(R.drawable.ic_pencil), null, Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(if (isEditing) "완료하기" else "수정하기", style = TextStyle(fontSize = 12.sp, color = SsTextSec))
                }
            }
        }
    }
}

@Composable
fun ScriptFolderThumbnail(folder: ScriptFolder, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 120.dp, height = 90.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFD0D0D0))
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) SsAccent else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (folder.coverImage.isNotEmpty()) {
            AsyncImage(
                model              = folder.coverImage,
                contentDescription = folder.name,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(folder.name, style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(500), color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${folder.count}", style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f)))
        }
    }
}

@Composable
private fun SsLabel(text: String) {
    Text(text = text, style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = SsTextPri))
}

@Composable
private fun SsMemoField(
    value     : String,
    maxLength : Int,
    minHeight : androidx.compose.ui.unit.Dp,
    hint      : String,
    onChange  : (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8F8F8))
            .border(0.5.dp, SsBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Column {
            BasicTextField(
                value         = value,
                onValueChange = onChange,
                modifier      = Modifier.fillMaxWidth().defaultMinSize(minHeight = minHeight),
                textStyle     = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = SsTextPri),
                decorationBox = { inner ->
                    if (value.isEmpty() && hint.isNotEmpty()) Text(hint, style = TextStyle(fontSize = 14.sp, color = SsTextSec))
                    inner()
                },
            )
            Spacer(Modifier.height(4.dp))
            Text("${value.length}/$maxLength", style = TextStyle(fontSize = 11.sp, color = SsTextSec), modifier = Modifier.align(Alignment.End))
        }
    }
}

// 디자인 (채팅방_대화선택.png):
// - 전체 배경 딤, 선택 구역: 흰 배경 + 파란 점선
// - 번역 아이콘 文A (말풍선 오른쪽)
// - 하단: [≡ 저장옵션] / [저장하기] 파란 버튼

@Composable
fun ConversationSelectScreen(
    messages : List<ChatMessage>,
    roomName : String,
    onBack   : () -> Unit,
    onSave   : (List<ChatMessage>, CaptureOption) -> Unit,
) {
    var selectedRange     by remember { mutableStateOf<IntRange?>(null) }
    var captureOptionOpen by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val selectedMessages = remember(selectedRange, messages) {
        selectedRange?.let { r -> messages.filterIndexed { i, _ -> i in r } } ?: emptyList()
    }
    val guideText = if (selectedMessages.isEmpty()) "저장하고 싶은 대화를 선택해주세요." else "저장 버튼을 클릭해주세요."

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(Color.White).statusBarsPadding()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                            Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = SsTextPri)
                        }
                        Text(
                            "대화 선택",
                            modifier = Modifier.align(Alignment.Center),
                            style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = SsTextPri),
                        )
                        TextButton(
                            onClick  = { selectedRange = null },
                            enabled  = selectedMessages.isNotEmpty(),
                            modifier = Modifier.align(Alignment.CenterEnd),
                        ) {
                            Text(
                                "선택 해제",
                                style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily,
                                    color = if (selectedMessages.isNotEmpty()) SsTextPri else SsTextSec),
                            )
                        }
                    }
                    HorizontalDivider(color = SsBorder, thickness = 0.5.dp)
                }
            },
            bottomBar = {
                Column {
                    HorizontalDivider(color = SsBorder, thickness = 0.5.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(
                            onClick = {
                                if (selectedMessages.size > 1) {
                                    captureOptionOpen = true
                                } else if (selectedMessages.size == 1) {
                                    onSave(selectedMessages, CaptureOption.SEPARATE)
                                }
                            },
                            enabled  = selectedMessages.isNotEmpty(),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = SsAccent,
                                disabledContainerColor = Color(0xFFCCCCCC),
                            ),
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                        ) {
                            Text("저장하기", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color.White))
                        }
                    }
                }
            },
            containerColor = Color.Transparent,
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF888888).copy(alpha = 0.7f)))
                Text(
                    guideText,
                    style     = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = Color.White),
                    modifier  = Modifier.fillMaxWidth().padding(top = 10.dp),
                    textAlign = TextAlign.Center,
                )
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
                selected  = CaptureOption.COMBINED,
                onSelect  = { option ->
                    captureOptionOpen = false
                    onSave(selectedMessages, option)
                },
                onDismiss = { captureOptionOpen = false },
            )
        }
    }
}

@Composable
private fun SelectableMessageList(
    messages      : List<ChatMessage>,
    selectedRange : IntRange?,
    onRangeSelect : (IntRange?) -> Unit,
    modifier      : Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(top = 36.dp, bottom = 8.dp)) {
        items(messages.indices.toList()) { index ->
            val isInRange = selectedRange != null && index in selectedRange
            val isFirst   = selectedRange?.first == index
            val isLast    = selectedRange?.last  == index

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val cur = selectedRange
                        onRangeSelect(if (cur == null) index..index else minOf(cur.first, index)..maxOf(cur.last, index))
                    }
                    .then(
                        if (isInRange) {
                            Modifier.background(Color.White).drawBehind {
                                val sw   = 2.dp.toPx()
                                val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                                if (isFirst) drawLine(SsAccent, Offset(0f, sw / 2), Offset(size.width, sw / 2), sw, pathEffect = dash)
                                if (isLast)  drawLine(SsAccent, Offset(0f, size.height - sw / 2), Offset(size.width, size.height - sw / 2), sw, pathEffect = dash)
                                drawLine(SsAccent, Offset(sw / 2, 0f), Offset(sw / 2, size.height), sw, pathEffect = dash)
                                drawLine(SsAccent, Offset(size.width - sw / 2, 0f), Offset(size.width - sw / 2, size.height), sw, pathEffect = dash)
                            }
                        } else Modifier
                    ),
            ) {
                SelectableMessageItem(messages[index], messages[index].senderId == "me")
            }
        }
    }
}

@Composable
private fun SelectableMessageItem(message: ChatMessage, isMe: Boolean) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("a hh:mm") }

    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
    ) {
        if (!isMe) {
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFCCCCCC)))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(message.senderName, style = TextStyle(fontSize = 12.sp, color = SsTextSec), modifier = Modifier.padding(bottom = 3.dp))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                                .background(SsBubbleGray)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .widthIn(max = 220.dp),
                        ) {
                            Text(message.text, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = SsTextPri, lineHeight = 21.sp))
                        }
                        // 번역 아이콘 文A
                        Box(
                            modifier         = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF0F0F0)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("文A", style = TextStyle(fontSize = 9.sp, color = SsAccent, fontWeight = FontWeight(600)))
                        }
                        Text(message.timestamp.format(timeFormatter), style = TextStyle(fontSize = 10.sp, color = SsTextSec))
                    }
                    if (message.translatedText.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        Text(message.translatedText, style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = SsTextSec, lineHeight = 18.sp))
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Text(message.timestamp.format(timeFormatter), style = TextStyle(fontSize = 10.sp, color = SsTextSec))
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                        .background(SsBubbleBlue)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .widthIn(max = 220.dp),
                ) {
                    Text(message.text, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = SsTextPri, lineHeight = 21.sp))
                }
            }
        }
    }
}

// 디자인 (채팅방_대화선택-1.png):
// "저장옵션" 타이틀 / 부제목 / X닫기
// 통합저장/따로저장 — 선택: 파란테두리+파란텍스트+파란체크원
// 미선택: 회색테두리+검정텍스트+회색체크원

@Composable
fun CaptureOptionDialog(
    selected  : CaptureOption,
    onSelect  : (CaptureOption) -> Unit,
    onDismiss : () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("저장옵션", style = TextStyle(fontSize = 17.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = SsTextPri))
                        Spacer(Modifier.height(4.dp))
                        Text("항목을 통합 또는 개별 저장할 수 있습니다.", style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = SsTextSec))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text("✕", style = TextStyle(fontSize = 16.sp, color = SsTextSec))
                    }
                }
                Spacer(Modifier.height(16.dp))
                listOf(CaptureOption.COMBINED to "통합 저장", CaptureOption.SEPARATE to "따로 저장")
                    .forEachIndexed { idx, (option, label) ->
                        val isSel = selected == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(if (isSel) 1.5.dp else 1.dp, if (isSel) SsAccent else SsBorder, RoundedCornerShape(10.dp))
                                .background(if (isSel) SsSelectBg else Color.Transparent)
                                .clickable { onSelect(option) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically,
                        ) {
                            Text(label, style = TextStyle(
                                fontSize   = 15.sp, fontFamily = PretendardFamily,
                                fontWeight = if (isSel) FontWeight(500) else FontWeight(400),
                                color      = if (isSel) SsAccent else SsTextPri,
                            ))
                            Box(
                                modifier         = Modifier
                                    .size(24.dp).clip(CircleShape)
                                    .background(if (isSel) SsAccent else Color.Transparent)
                                    .border(1.5.dp, if (isSel) SsAccent else SsBorder, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSel) Text("✓", style = TextStyle(fontSize = 13.sp, color = Color.White, fontWeight = FontWeight(700)))
                            }
                        }
                        if (idx == 0) Spacer(Modifier.height(10.dp))
                    }
            }
        }
    }
}

@Composable
fun SavedToast(message: String = "저장되었습니다.") {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF444444))
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(message, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = Color.White))
    }
}

// 디자인 (폴더_추가하기.png):
// - AppBar: ← / "폴더 추가하기"
// - 커버 이미지 박스 (+ 커버사진추가, 카메라 아이콘 우하단 회색원)
// - "폴더 이름 *" 빨간 별표
// - 텍스트필드 (0/8 카운터)
// - 하단 [완료] 파란 풀버튼

@Composable
fun NewFolderScreen(
    roomName : String,
    onBack   : () -> Unit,
    onSave   : (name: String, imageUri: String?) -> Unit,
) {
    var folderName by remember { mutableStateOf("") }
    var imageUri   by remember { mutableStateOf<String?>(null) }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) imageUri = uri.toString() }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White).statusBarsPadding()) {
                Box(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = SsTextPri)
                    }
                    Text(
                        "폴더 추가하기",
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = SsTextPri),
                    )
                    Box(modifier = Modifier.size(48.dp).align(Alignment.CenterEnd))
                }
                HorizontalDivider(color = SsBorder, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = SsBorder, thickness = 0.5.dp)
                Button(
                    onClick  = { if (folderName.isNotEmpty()) onSave(folderName, imageUri) },
                    enabled  = folderName.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .height(52.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = SsAccent,
                        disabledContainerColor = Color(0xFFCCCCCC),
                    ),
                ) {
                    Text("완료", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color.White))
                }
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(
            modifier            = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // 커버 이미지 박스
            Box(
                modifier = Modifier
                    .size(width = 160.dp, height = 130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF0F0F0))
                    .clickable { imageLauncher.launch("image/*") },
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model              = imageUri,
                        contentDescription = "폴더 커버",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        modifier            = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Image(
                            painter            = painterResource(R.drawable.ic_media_camera),
                            contentDescription = "사진 추가",
                            modifier           = Modifier.size(32.dp),
                            colorFilter        = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFFAAAAAA)),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("커버사진추가", style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = SsTextSec))
                    }
                }
                // 카메라 아이콘 우하단
                Box(
                    modifier         = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(32.dp).clip(CircleShape).background(Color(0xFF888888)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(R.drawable.ic_media_camera), "카메라", Modifier.size(18.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White))
                }
            }

            Spacer(Modifier.height(28.dp))

            // "폴더 이름 *" 라벨
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("폴더 이름 ", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(500), color = SsTextPri))
                Text("*", style = TextStyle(fontSize = 15.sp, color = SsTextRed))
            }
            Spacer(Modifier.height(8.dp))

            // 입력 필드 (0/8)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, SsBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value         = folderName,
                    onValueChange = { if (it.length <= 8) folderName = it },
                    modifier      = Modifier.weight(1f),
                    textStyle     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SsTextPri),
                    decorationBox = { inner ->
                        if (folderName.isEmpty()) Text("폴더 이름을 입력해주세요", style = TextStyle(fontSize = 15.sp, color = SsTextSec))
                        inner()
                    },
                )
                Text("${folderName.length}/8", style = TextStyle(fontSize = 12.sp, color = SsTextSec))
            }
        }
    }
}

@Composable
fun NewFolderDialog(
    onDismiss : () -> Unit,
    onSave    : (String) -> Unit,
) {
    var folderName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        "폴더 추가하기",
                        style = TextStyle(fontSize = 17.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = SsTextPri),
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text("✕", style = TextStyle(fontSize = 16.sp, color = SsTextSec))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, SsBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value         = folderName,
                        onValueChange = { if (it.length <= 8) folderName = it },
                        modifier      = Modifier.weight(1f),
                        textStyle     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SsTextPri),
                        decorationBox = { inner ->
                            if (folderName.isEmpty()) Text("폴더 이름을 입력해주세요", style = TextStyle(fontSize = 15.sp, color = SsTextSec))
                            inner()
                        },
                    )
                    Text("${folderName.length}/8", style = TextStyle(fontSize = 12.sp, color = SsTextSec))
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("취소", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = SsTextSec))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick  = { if (folderName.isNotEmpty()) onSave(folderName) },
                        enabled  = folderName.isNotEmpty(),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = SsAccent,
                            disabledContainerColor = Color(0xFFCCCCCC),
                        ),
                    ) {
                        Text("완료", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = Color.White))
                    }
                }
            }
        }
    }
}

private val _previewMsg = ChatMessage(
    id             = "m01",
    roomId         = "r01",
    senderId       = "u01",
    senderName     = "Olivia",
    type           = MessageType.TEXT,
    text           = "Hi! I'm trying to learn Korean. Would you like to chat and practice together?",
    translatedText = "안녕하세요! 한국어를 배우려고 하고 있어요. 같이 대화하면서 연습해볼래요?",
)

@Preview(showBackground = true, widthDp = 412, heightDp = 300, name = "컨텍스트 메뉴")
@Composable
private fun Preview_ContextMenu() {
    EveryBuddyTheme { MessageContextMenu({}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 300, name = "캡처 옵션 다이얼로그")
@Composable
private fun Preview_CaptureOption() {
    EveryBuddyTheme { CaptureOptionDialog(CaptureOption.COMBINED, {}, {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "새 폴더 화면")
@Composable
private fun Preview_NewFolder() {
    EveryBuddyTheme { NewFolderScreen("Olivia", {}, { _, _ -> }) }
}
