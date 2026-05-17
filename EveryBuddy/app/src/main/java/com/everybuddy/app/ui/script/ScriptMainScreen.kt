package com.everybuddy.app.ui.script

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.everybuddy.app.R
import com.everybuddy.app.ui.chat.ScriptFolder
import com.everybuddy.app.ui.chat.ScriptFolderThumbnail
import com.everybuddy.app.ui.chat.ScriptItem
import com.everybuddy.app.ui.theme.PretendardFamily

private val SmAccent  = Color(0xFF0167FF)
private val SmTextPri = Color(0xFF000000)
private val SmTextSec = Color(0xFF797979)
private val SmBorder  = Color(0xFFE5E5E5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptMainScreen(
    viewModel      : ScriptViewModel,
    isRefreshing   : Boolean    = false,
    onRefresh      : () -> Unit = {},
    onAddFolder    : () -> Unit = {},
    onFolderClick  : (ScriptFolder) -> Unit = {},
    onItemClick    : (ScriptItem) -> Unit = {},
    onItemAudio    : (ScriptItem) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val items = viewModel.filteredItems

    var sortMenuOpen   by remember { mutableStateOf(false) }
    var isSearchOpen   by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ScriptTopBar(
                isSearchOpen   = isSearchOpen,
                searchQuery    = uiState.searchQuery,
                onSearchClick  = {
                    isSearchOpen = !isSearchOpen
                    if (!isSearchOpen) viewModel.updateSearchQuery("")
                },
                onSearchChange = viewModel::updateSearchQuery,
            )
        },
        containerColor = Color.White,
    ) { innerPadding ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh    = onRefresh,
            modifier     = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {

            item {
                // TODO padding: 폴더 섹션 horizontal 16dp / top 16dp
                Spacer(Modifier.height(16.dp))
                Text(
                    "나의 폴더",
                    style    = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = SmTextPri),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                // TODO padding: 폴더 리스트 상단 12dp
                Spacer(Modifier.height(12.dp))

                LazyRow(
                    contentPadding      = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 폴더 목록
                    items(uiState.folders) { folder ->
                        FolderThumbnailCard(
                            folder    = folder,
                            isSelected = false,  // 폴더 필터 선택과 별개 — 클릭 시 폴더 상세 이동
                            onClick   = { onFolderClick(folder) },
                        )
                    }
                    // 폴더 추가 버튼
                    item {
                        AddFolderButton(onClick = onAddFolder)
                    }
                }

                // 구분선
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = SmBorder, thickness = 1.dp)
                Spacer(Modifier.height(12.dp))
            }

            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        // TODO padding: 헤더 horizontal 16dp
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    // 개수 표시
                    Text(
                        "${items.size}개",
                        style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = SmTextSec),
                    )

                    // 정렬 드롭다운
                    Box {
                        Row(
                            modifier          = Modifier.clickable { sortMenuOpen = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                uiState.sortOption.label,
                                style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = SmTextSec),
                            )
                            // ic_chevron_down.xml — 정렬 화살표
                            Icon(
                                painter            = painterResource(R.drawable.ic_chevron_down),
                                contentDescription = "정렬",
                                modifier           = Modifier.size(14.dp),
                                tint               = SmTextSec,
                            )
                        }

                        DropdownMenu(
                            expanded         = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false },
                            containerColor   = Color.White,
                        ) {
                            ScriptSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text    = { Text(option.label, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily)) },
                                    onClick = {
                                        viewModel.changeSortOption(option)
                                        sortMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                }
                // TODO padding: 헤더-아이템 간격 8dp
                Spacer(Modifier.height(8.dp))
            }

            items(items) { item ->
                ScriptItemCard(
                    item      = item,
                    onClick   = { onItemClick(item) },
                    onAudio   = { onItemAudio(item) },
                )
            }
        }
        }
    }
}

@Composable
private fun ScriptTopBar(
    isSearchOpen   : Boolean  = false,
    searchQuery    : String   = "",
    onSearchClick  : () -> Unit,
    onSearchChange : (String) -> Unit = {},
) {
    Column(modifier = Modifier.background(Color.White)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                "스크립트",
                modifier = Modifier.align(Alignment.Center),
                style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = SmTextPri),
            )
            IconButton(
                onClick  = onSearchClick,
                modifier = Modifier.size(40.dp).align(Alignment.CenterEnd),
            ) {
                Icon(
                    painter            = painterResource(if (isSearchOpen) R.drawable.ic_back else R.drawable.ic_search),
                    contentDescription = if (isSearchOpen) "검색 닫기" else "검색",
                    modifier           = Modifier.size(24.dp),
                    tint               = SmTextPri,
                )
            }
        }

        AnimatedVisibility(visible = isSearchOpen) {
            BasicTextField(
                value         = searchQuery,
                onValueChange = onSearchChange,
                singleLine    = true,
                textStyle     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SmTextPri),
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF2F2F2))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_search), null, Modifier.size(16.dp), tint = SmTextSec)
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text("본문 / 뜻 / 메모 검색", style = TextStyle(fontSize = 15.sp, color = SmTextSec, fontFamily = PretendardFamily))
                            }
                            innerTextField()
                        }
                    }
                },
            )
        }

        HorizontalDivider(color = SmBorder, thickness = 0.5.dp)
    }
}

@Composable
private fun FolderThumbnailCard(
    folder     : ScriptFolder,
    isSelected : Boolean,
    onClick    : () -> Unit,
) {
    Box(
        modifier = Modifier
            // TODO size: 폴더 카드 크기 — 이미지 기준 약 140x110dp
            .size(width = 140.dp, height = 110.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) SmAccent else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .background(Color(0xFFD8D8D8))  // 플레이스홀더 배경
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (folder.coverImage.isNotEmpty()) {
            coil.compose.AsyncImage(
                model              = folder.coverImage,
                contentDescription = folder.name,
                contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        }

        // 하단 오버레이 — 이름 + 개수
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.40f))
                // TODO padding: 오버레이 내부 패딩 horizontal 10dp, vertical 6dp
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text     = folder.name,
                style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text  = "${folder.count}",
                style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = Color.White.copy(alpha = 0.85f)),
            )
        }
    }
}

@Composable
private fun AddFolderButton(onClick: () -> Unit) {
    Column(
        modifier            = Modifier
            // TODO size: 폴더추가 버튼 크기 약 80x110dp
            .size(width = 80.dp, height = 110.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // + 원형 버튼
        Box(
            modifier         = Modifier
                // TODO size: + 원형 크기 48dp
                .size(48.dp)
                .clip(CircleShape)
                .border(1.5.dp, SmBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // ic_plus.xml — 원형 + 아이콘
            Icon(
                painter            = painterResource(R.drawable.ic_fab_plus),
                contentDescription = "폴더 추가",
                modifier           = Modifier.size(20.dp),
                tint               = SmTextSec,
            )
        }
        // TODO padding: "폴더추가" 텍스트 상단 6dp
        Spacer(Modifier.height(6.dp))
        Text(
            "폴더추가",
            style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = SmTextSec),
        )
    }
}

@Composable
fun ScriptItemCard(
    item    : ScriptItem,
    onClick : () -> Unit,
    onAudio : () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // TODO padding: 카드 horizontal 16dp, vertical 2dp
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
        shape  = RoundedCornerShape(12.dp),
        color  = Color.White,
        border = BorderStroke(0.8.dp, SmBorder),
        // TODO: elevation 없음 — 이미지와 동일
    ) {
        Row(
            modifier = Modifier
                // TODO padding: 카드 내부 패딩 horizontal 16dp, vertical 14dp
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // ic_speaker.xml — 클릭 시 TTS 재생
            IconButton(
                onClick  = onAudio,
                modifier = Modifier
                    .size(26.dp)
                    // TODO padding: 스피커 아이콘 우측 4dp
                    .padding(end = 0.dp),
            ) {
                Icon(
                    painter            = painterResource(R.drawable.ic_speaker),
                    contentDescription = "발음 듣기",
                    modifier           = Modifier.size(18.dp),
                    tint               = SmTextPri,
                )
            }
            // TODO padding: 스피커-텍스트 간격 4dp
            Spacer(Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 원문 (Bold)
                Text(
                    text  = item.originalText,
                    style = TextStyle(
                        fontSize   = 16.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(700),
                        color      = SmTextPri,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // 번역문
                if (item.translatedText.isNotEmpty()) {
                    // TODO padding: 번역문 상단 2dp
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = item.translatedText,
                        style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = SmTextPri),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // 설명 (memo1 — gray 작은 글씨)
                if (item.memo1.isNotEmpty()) {
                    // TODO padding: 설명 상단 2dp
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = item.memo1,
                        style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = SmTextSec),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    // 카드 간 구분선
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        color     = SmBorder,
        thickness = 0.5.dp,
    )
}

// 임시 BorderStroke import (ScriptItemCard 에서 사용)
private fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptDetailScreen(
    item     : ScriptItem,
    folders  : List<ScriptFolder> = emptyList(),
    onBack   : () -> Unit,
    onAudio  : (ScriptItem) -> Unit = {},
    onSave   : (ScriptItem) -> Unit = {},
    onDelete : (ScriptItem) -> Unit = {},
) {
    BackHandler(onBack = onBack)

    var editingOriginal    by remember { mutableStateOf(false) }
    var editedOriginal     by remember { mutableStateOf(item.originalText) }
    var editingTranslation by remember { mutableStateOf(false) }
    var editedTranslation  by remember { mutableStateOf(item.translatedText) }
    var editingMemo        by remember { mutableStateOf(false) }
    var editedMemo         by remember { mutableStateOf(item.memo1) }
    var showDeleteConfirm  by remember { mutableStateOf(false) }
    var showFolderSheet    by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White).statusBarsPadding()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp).align(Alignment.CenterStart)) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = SmTextPri)
                    }
                    Text(
                        "스크립트 상세",
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = SmTextPri),
                    )
                }
                HorizontalDivider(color = SmBorder, thickness = 0.5.dp)
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editingOriginal) {
                    Box(
                        modifier = Modifier.weight(1f).border(1.dp, SmAccent, RoundedCornerShape(8.dp)).padding(12.dp),
                    ) {
                        BasicTextField(
                            value         = editedOriginal,
                            onValueChange = { editedOriginal = it },
                            textStyle     = TextStyle(fontSize = 22.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = SmTextPri, lineHeight = 32.sp),
                            modifier      = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Text(
                        item.originalText,
                        modifier = Modifier.weight(1f),
                        style    = TextStyle(fontSize = 22.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = SmTextPri, lineHeight = 32.sp),
                    )
                }
                IconButton(onClick = { onAudio(item) }, modifier = Modifier.size(36.dp)) {
                    Icon(painterResource(R.drawable.ic_speaker), "발음 듣기", Modifier.size(22.dp), tint = SmAccent)
                }
            }
            Spacer(Modifier.height(4.dp))
            if (editingOriginal) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "취소",
                        style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = SmTextSec),
                        modifier = Modifier.clickable { editingOriginal = false; editedOriginal = item.originalText },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "완료",
                        style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = SmAccent, fontWeight = FontWeight(600)),
                        modifier = Modifier.clickable { onSave(item.copy(originalText = editedOriginal)); editingOriginal = false },
                    )
                }
            } else {
                Text(
                    "수정하기",
                    style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = SmTextSec),
                    modifier = Modifier.clickable { editingOriginal = true; editedOriginal = item.originalText },
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("뜻", style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = SmTextSec))
                Spacer(Modifier.weight(1f))
                if (editingTranslation) {
                    Text(
                        "취소",
                        style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = SmTextSec),
                        modifier = Modifier.clickable { editingTranslation = false; editedTranslation = item.translatedText },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "완료",
                        style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = SmAccent, fontWeight = FontWeight(600)),
                        modifier = Modifier.clickable {
                            onSave(item.copy(translatedText = editedTranslation))
                            editingTranslation = false
                        },
                    )
                } else {
                    Text(
                        "수정하기",
                        style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = SmTextSec),
                        modifier = Modifier.clickable { editingTranslation = true; editedTranslation = item.translatedText },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            if (editingTranslation) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SmAccent, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                ) {
                    BasicTextField(
                        value         = editedTranslation,
                        onValueChange = { editedTranslation = it },
                        textStyle     = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, color = SmTextPri, lineHeight = 24.sp),
                        modifier      = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (editedTranslation.isEmpty()) {
                                Text("뜻을 입력하세요", style = TextStyle(fontSize = 16.sp, color = SmTextSec, fontFamily = PretendardFamily))
                            }
                            inner()
                        },
                    )
                }
            } else {
                Text(
                    text  = item.translatedText.ifEmpty { "—" },
                    style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, color = if (item.translatedText.isEmpty()) SmTextSec else SmTextPri, lineHeight = 24.sp),
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("메모", style = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = SmTextSec))
                Spacer(Modifier.weight(1f))
                if (editingMemo) {
                    Text(
                        "취소",
                        style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = SmTextSec),
                        modifier = Modifier.clickable { editingMemo = false; editedMemo = item.memo1 },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "완료",
                        style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = SmAccent, fontWeight = FontWeight(600)),
                        modifier = Modifier.clickable {
                            onSave(item.copy(memo1 = editedMemo))
                            editingMemo = false
                        },
                    )
                } else {
                    Text(
                        "수정하기",
                        style    = TextStyle(fontSize = 13.sp, fontFamily = PretendardFamily, color = SmTextSec),
                        modifier = Modifier.clickable { editingMemo = true; editedMemo = item.memo1 },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            if (editingMemo) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SmAccent, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                ) {
                    BasicTextField(
                        value         = editedMemo,
                        onValueChange = { editedMemo = it },
                        textStyle     = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, color = SmTextPri, lineHeight = 24.sp),
                        modifier      = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (editedMemo.isEmpty()) {
                                Text("메모를 입력하세요", style = TextStyle(fontSize = 16.sp, color = SmTextSec, fontFamily = PretendardFamily))
                            }
                            inner()
                        },
                    )
                }
            } else {
                Text(
                    text  = item.memo1.ifEmpty { "—" },
                    style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, color = if (item.memo1.isEmpty()) SmTextSec else SmTextPri, lineHeight = 24.sp),
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick  = { showFolderSheet = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SmAccent),
            ) {
                Text("폴더에 저장하기", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color.White))
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable { showDeleteConfirm = true }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("스크립트 삭제하기", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(500), color = Color(0xFFFF3333)))
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showFolderSheet) {
        FolderMoveSheet(
            currentFolderId = item.folderId,
            folders         = folders,
            onDismiss       = { showFolderSheet = false },
            onConfirm       = { newFolderId ->
                onSave(item.copy(folderId = newFolderId))
                showFolderSheet = false
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            text             = {
                Text(
                    "정말 삭제하시겠습니까?",
                    style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, color = SmTextPri),
                )
            },
            confirmButton    = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete(item) }) {
                    Text("삭제", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = Color(0xFFFF3333), fontWeight = FontWeight(600)))
                }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = SmTextSec))
                }
            },
            containerColor   = Color.White,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderMoveSheet(
    currentFolderId : String?,
    folders         : List<ScriptFolder>,
    onDismiss       : () -> Unit,
    onConfirm       : (String?) -> Unit,
) {
    var selectedId by remember { mutableStateOf(currentFolderId) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color(0xFFDDDDDD)))
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "폴더 선택",
                style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = SmTextPri),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            if (folders.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("저장된 폴더가 없습니다.", style = TextStyle(fontSize = 14.sp, color = SmTextSec, fontFamily = PretendardFamily))
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(folders) { folder ->
                        ScriptFolderThumbnail(
                            folder     = folder,
                            isSelected = selectedId == folder.id,
                            onClick    = { selectedId = folder.id },
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            val changed = selectedId != currentFolderId
            Button(
                onClick  = { onConfirm(selectedId) },
                enabled  = changed,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = SmAccent,
                    disabledContainerColor = Color(0xFFCCCCCC),
                ),
            ) {
                Text("확인", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color.White))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun ScriptFolderScreen(
    folder      : ScriptFolder,
    allItems    : List<ScriptItem>,
    onBack      : () -> Unit,
    onItemClick : (ScriptItem) -> Unit = {},
    onItemAudio : (ScriptItem) -> Unit = {},
) {
    val folderItems = remember(folder.id, allItems) { allItems.filter { it.folderId == folder.id } }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White).statusBarsPadding()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp).align(Alignment.CenterStart)) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = SmTextPri)
                    }
                    Text(
                        folder.name,
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = SmTextPri),
                    )
                }
                HorizontalDivider(color = SmBorder, thickness = 0.5.dp)
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        if (folderItems.isEmpty()) {
            Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("저장된 스크립트가 없습니다.", style = TextStyle(fontSize = 14.sp, color = SmTextSec))
            }
        } else {
            LazyColumn(
                modifier       = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            ) {
                items(folderItems) { item ->
                    ScriptItemCard(item = item, onClick = { onItemClick(item) }, onAudio = { onItemAudio(item) })
                }
            }
        }
    }
}
