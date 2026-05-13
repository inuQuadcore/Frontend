package com.everybuddy.app.ui.script

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.everybuddy.app.R
import com.everybuddy.app.ui.chat.ScriptFolder
import com.everybuddy.app.ui.chat.ScriptItem
import com.everybuddy.app.ui.theme.PretendardFamily

private val SmAccent  = Color(0xFF0167FF)
private val SmTextPri = Color(0xFF000000)
private val SmTextSec = Color(0xFF797979)
private val SmBorder  = Color(0xFFE5E5E5)

@Composable
fun ScriptMainScreen(
    viewModel      : ScriptViewModel,
    onAddFolder    : () -> Unit = {},
    onFolderClick  : (ScriptFolder) -> Unit = {},
    onItemClick    : (ScriptItem) -> Unit = {},
    onItemAudio    : (ScriptItem) -> Unit = {},
    onNotification : () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val items = viewModel.filteredItems

    // 정렬 드롭다운
    var sortMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ScriptTopBar(
                onSearchClick       = { /* TODO: 검색창 열기 */ },
                onNotificationClick = onNotification,
            )
        },
        containerColor = Color.White,
    ) { innerPadding ->

        LazyColumn(
            modifier       = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
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

@Composable
private fun ScriptTopBar(
    onSearchClick       : () -> Unit,
    onNotificationClick : () -> Unit,
    hasNotification     : Boolean = false,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.White)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                "스크립트",
                modifier = Modifier.align(Alignment.Center),
                style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = SmTextPri),
            )
            Row(
                modifier              = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick  = onSearchClick,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_search),
                        contentDescription = "검색",
                        modifier           = Modifier.size(24.dp),
                        tint               = SmTextPri,
                    )
                }
                Box {
                    IconButton(
                        onClick  = onNotificationClick,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_alarm),
                            contentDescription = "알림",
                            modifier           = Modifier.size(24.dp),
                            tint               = SmTextPri,
                        )
                    }
                    if (hasNotification) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SmAccent)
                                .align(Alignment.TopEnd)
                                .offset(x = (-6).dp, y = 6.dp),
                        )
                    }
                }
            }
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
        // TODO: 실제 폴더 커버 이미지 로딩 (Coil AsyncImage)
        // AsyncImage(
        //     model          = folder.coverImage,
        //     contentDescription = folder.name,
        //     contentScale   = ContentScale.Crop,
        //     modifier       = Modifier.fillMaxSize(),
        // )

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
