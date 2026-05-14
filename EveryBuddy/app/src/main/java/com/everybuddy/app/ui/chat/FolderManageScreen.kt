package com.everybuddy.app.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.everybuddy.app.R
import com.everybuddy.app.data.chat.ChatFolder
import com.everybuddy.app.data.chat.ChatRoomUi
import com.everybuddy.app.ui.theme.PretendardFamily

private val FmTextPri = Color(0xFF1B1B1B)
private val FmTextSec = Color(0xFF8F9399)
private val FmBorder  = Color(0xFFE5E5E5)
private val FmAccent  = Color(0xFF0167FF)

@Composable
fun FolderManageScreen(
    folders         : List<ChatFolder>,
    onBack          : () -> Unit,
    onCreateNew     : () -> Unit,
    onEdit          : (ChatFolder) -> Unit,
    onDelete        : (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 8.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(40.dp)) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = FmTextPri)
                    }
                    Text(
                        "채팅방 폴더",
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = FmTextPri),
                    )
                }
                HorizontalDivider(color = FmBorder, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = FmBorder, thickness = 0.5.dp)
                Button(
                    onClick  = onCreateNew,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .height(52.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FmAccent),
                ) {
                    Text("새 폴더 만들기", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = Color.White))
                }
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        if (folders.isEmpty()) {
            Box(
                modifier         = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "폴더가 없어요.\n새 폴더를 만들어보세요!",
                    style     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = FmTextSec, textAlign = TextAlign.Center, lineHeight = 22.sp),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                items(folders, key = { it.id }) { folder ->
                    FolderManageRow(
                        folder   = folder,
                        onEdit   = { onEdit(folder) },
                        onDelete = { onDelete(folder.id) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = FmBorder, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun FolderManageRow(
    folder   : ChatFolder,
    onEdit   : () -> Unit,
    onDelete : () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFEEF3FF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_folder_plus), "폴더", Modifier.size(22.dp), tint = FmAccent)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(folder.name, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = FmTextPri))
            Text("${folder.chatRoomIds.size}개 채팅방", style = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = FmTextSec))
        }

        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(painterResource(R.drawable.ic_pencil), "편집", Modifier.size(18.dp), tint = FmTextSec)
        }
        IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
            Icon(painterResource(R.drawable.ic_trash), "삭제", Modifier.size(18.dp), tint = Color(0xFFFF3333))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor   = Color.White,
            title = {
                Text("폴더 삭제", style = TextStyle(fontSize = 17.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = FmTextPri))
            },
            text = {
                Text("'${folder.name}' 폴더를 삭제하시겠어요?", style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = FmTextSec))
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = FmTextSec))
                }
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("삭제", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color(0xFFFF3333)))
                }
            },
        )
    }
}

@Composable
fun FolderCreateScreen(
    editingFolder : ChatFolder?,
    allRooms      : List<ChatRoomUi>,
    onBack        : () -> Unit,
    onSave        : (ChatFolder) -> Unit,
) {
    BackHandler(onBack = onBack)

    val isEditing = editingFolder != null
    var folderName   by remember { mutableStateOf(editingFolder?.name ?: "") }
    var selectedIds  by remember { mutableStateOf(editingFolder?.chatRoomIds?.toSet() ?: emptySet()) }

    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 8.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(40.dp)) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = FmTextPri)
                    }
                    Text(
                        if (isEditing) "폴더 편집" else "폴더 만들기",
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = FmTextPri),
                    )
                }
                HorizontalDivider(color = FmBorder, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = FmBorder, thickness = 0.5.dp)
                Button(
                    onClick  = {
                        val folderId = editingFolder?.id ?: System.currentTimeMillis().toString()
                        onSave(ChatFolder(id = folderId, name = folderName.trim(), chatRoomIds = selectedIds.toList()))
                    },
                    enabled  = folderName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .height(52.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = FmAccent,
                        disabledContainerColor = Color(0xFFCCCCCC),
                    ),
                ) {
                    Text("완료", style = TextStyle(fontSize = 16.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = Color.White))
                }
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {

            Spacer(Modifier.height(20.dp))
            Text(
                "폴더 이름",
                style    = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = FmTextPri),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape    = RoundedCornerShape(10.dp),
                color    = Color(0xFFF8F8F8),
                border   = BorderStroke(0.5.dp, FmBorder),
            ) {
                BasicTextField(
                    value         = folderName,
                    onValueChange = { if (it.length <= 20) folderName = it },
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                    textStyle     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = FmTextPri),
                    singleLine    = true,
                    decorationBox = { inner ->
                        if (folderName.isEmpty()) Text("폴더 이름을 입력하세요 (최대 20자)", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = FmTextSec))
                        inner()
                    },
                )
            }

            if (allRooms.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                Text(
                    "채팅방 선택",
                    style    = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = FmTextPri),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "이 폴더에 포함할 채팅방을 선택하세요.",
                    style    = TextStyle(fontSize = 12.sp, fontFamily = PretendardFamily, color = FmTextSec),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))

                allRooms.forEach { room ->
                    val isChecked = selectedIds.contains(room.id)
                    RoomSelectRow(
                        room      = room,
                        isChecked = isChecked,
                        onToggle  = {
                            selectedIds = if (isChecked) selectedIds - room.id else selectedIds + room.id
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = FmBorder, thickness = 0.5.dp)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RoomSelectRow(
    room      : ChatRoomUi,
    isChecked : Boolean,
    onToggle  : () -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter            = painterResource(if (isChecked) R.drawable.ic_check else R.drawable.ic_check_empty),
            contentDescription = null,
            modifier           = Modifier.size(22.dp),
            tint               = if (isChecked) FmAccent else FmTextSec,
        )
        Text(room.name, style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(500), color = FmTextPri))
    }
}
