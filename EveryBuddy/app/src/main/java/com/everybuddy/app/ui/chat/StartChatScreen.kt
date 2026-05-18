package com.everybuddy.app.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuddy.app.R
import com.everybuddy.app.data.chat.ChatRoomUi
import com.everybuddy.app.ui.friend.FriendProfile
import com.everybuddy.app.ui.friend.FriendViewModel
import com.everybuddy.app.ui.theme.PretendardFamily
import kotlinx.coroutines.delay

private val ScAccent  = Color(0xFF0167FF)
private val ScTextPri = Color(0xFF1B1B1B)
private val ScTextSec = Color(0xFF8F9399)
private val ScBorder  = Color(0xFFE5E5E5)

@Composable
fun DirectChatScreen(
    onBack        : () -> Unit,
    onRoomCreated : (ChatRoomUi) -> Unit,
    chatVm        : ChatViewModel   = hiltViewModel(),
    friendVm      : FriendViewModel = hiltViewModel(),
) {
    BackHandler(onBack = onBack)

    val uiState     by friendVm.uiState.collectAsState()
    var searchQuery  by remember { mutableStateOf("") }
    var toastMsg     by remember { mutableStateOf<String?>(null) }
    var isCreating   by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { friendVm.loadFriends() }

    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(2000)
            toastMsg = null
        }
    }

    val friends = if (searchQuery.isBlank()) uiState.friends
    else uiState.friends.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.White)
                        .padding(horizontal = 8.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(40.dp)) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = ScTextPri)
                    }
                    Text(
                        "일반채팅",
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = ScTextPri),
                    )
                }
                BasicTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine    = true,
                    textStyle     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = ScTextPri),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF2F2F2))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_search), null, Modifier.size(16.dp), tint = ScTextSec)
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text("친구 검색", style = TextStyle(fontSize = 15.sp, color = ScTextSec, fontFamily = PretendardFamily))
                                }
                                innerTextField()
                            }
                        }
                    },
                )
                HorizontalDivider(color = ScBorder, thickness = 0.5.dp)
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(friends) { friend ->
                    FriendSelectRow(
                        friend     = friend,
                        isGroup    = false,
                        isSelected = false,
                        onClick    = {
                            if (!isCreating) {
                                isCreating = true
                                chatVm.createChatRoom(
                                    roomName       = friend.name,             // 1:1방: 상대 이름을 박아 전송 (표시 시점엔 무시되고 자체 렌더)
                                    isGroup        = false,
                                    participantIds = listOf(friend.id),
                                    onSuccess      = { room -> onRoomCreated(room) },
                                    onError        = { msg -> toastMsg = msg; isCreating = false },
                                )
                            }
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScBorder, thickness = 0.5.dp)
                }
            }

            toastMsg?.let { msg ->
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF444444))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Text(msg, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = Color.White))
                    }
                }
            }
        }
    }
}

@Composable
fun GroupChatScreen(
    onBack        : () -> Unit,
    onRoomCreated : (ChatRoomUi) -> Unit,
    chatVm        : ChatViewModel   = hiltViewModel(),
    friendVm      : FriendViewModel = hiltViewModel(),
) {
    BackHandler(onBack = onBack)

    val uiState       by friendVm.uiState.collectAsState()
    var searchQuery    by remember { mutableStateOf("") }
    var selectedIds    by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showNameDialog by remember { mutableStateOf(false) }
    var toastMsg       by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { friendVm.loadFriends() }

    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(2000)
            toastMsg = null
        }
    }

    val friends = if (searchQuery.isBlank()) uiState.friends
    else uiState.friends.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.White)
                        .padding(horizontal = 8.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(40.dp)) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = ScTextPri)
                    }
                    Text(
                        "그룹채팅",
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = ScTextPri),
                    )
                    if (selectedIds.isNotEmpty()) {
                        TextButton(
                            onClick  = { showNameDialog = true },
                            modifier = Modifier.align(Alignment.CenterEnd),
                        ) {
                            Text(
                                "완료 (${selectedIds.size})",
                                style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = ScAccent),
                            )
                        }
                    }
                }
                BasicTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine    = true,
                    textStyle     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = ScTextPri),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF2F2F2))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_search), null, Modifier.size(16.dp), tint = ScTextSec)
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text("친구 검색", style = TextStyle(fontSize = 15.sp, color = ScTextSec, fontFamily = PretendardFamily))
                                }
                                innerTextField()
                            }
                        }
                    },
                )
                HorizontalDivider(color = ScBorder, thickness = 0.5.dp)
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(friends) { friend ->
                    val selected = selectedIds.contains(friend.id)
                    FriendSelectRow(
                        friend     = friend,
                        isGroup    = true,
                        isSelected = selected,
                        onClick    = {
                            selectedIds = if (selected) selectedIds - friend.id else selectedIds + friend.id
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScBorder, thickness = 0.5.dp)
                }
            }

            toastMsg?.let { msg ->
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF444444))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Text(msg, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = Color.White))
                    }
                }
            }
        }
    }

    if (showNameDialog) {
        GroupNameDialog(
            onDismiss = { showNameDialog = false },
            onConfirm = { roomName ->
                showNameDialog = false
                val participantIds = uiState.friends
                    .filter { selectedIds.contains(it.id) }
                    .map { it.id }
                chatVm.createChatRoom(
                    roomName       = roomName,
                    isGroup        = true,
                    participantIds = participantIds,
                    onSuccess      = { room -> onRoomCreated(room) },
                    onError        = { msg -> toastMsg = msg },
                )
            },
        )
    }
}

@Composable
private fun FriendSelectRow(
    friend     : FriendProfile,
    isGroup    : Boolean,
    isSelected : Boolean,
    onClick    : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier         = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFDDDDDD)),
            contentAlignment = Alignment.Center,
        ) {
            if (friend.profileImageUrl != null) {
                coil.compose.AsyncImage(
                    model              = friend.profileImageUrl,
                    contentDescription = friend.name,
                    contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text  = friend.name.take(1),
                    style = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = Color(0xFF888888)),
                )
            }
        }
        Text(
            text     = friend.name,
            style    = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(500), color = ScTextPri),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isGroup) {
            if (isSelected) {
                Image(
                    painter            = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier           = Modifier.size(22.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, ScBorder, CircleShape),
                )
            }
        }
    }
}

@Composable
fun InviteMemberScreen(
    roomId    : String,
    onBack    : () -> Unit,
    onInvited : (List<Long>) -> Unit = {},
    friendVm  : FriendViewModel = hiltViewModel(),
) {
    val uiState       by friendVm.uiState.collectAsState()
    var searchQuery    by remember { mutableStateOf("") }
    var selectedIds    by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var toastMsg       by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { friendVm.loadFriends() }

    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(2000)
            toastMsg = null
        }
    }

    val friends = if (searchQuery.isBlank()) uiState.friends
    else uiState.friends.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.White)
                        .padding(horizontal = 8.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(40.dp)) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = ScTextPri)
                    }
                    Text(
                        "멤버 초대",
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = ScTextPri),
                    )
                    if (selectedIds.isNotEmpty()) {
                        TextButton(
                            onClick  = {
                                // TODO: API - POST /api/v1/chat/rooms/{roomId}/members 로 selectedIds 전송
                                onInvited(selectedIds.toList())
                            },
                            modifier = Modifier.align(Alignment.CenterEnd),
                        ) {
                            Text(
                                "초대 (${selectedIds.size})",
                                style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = ScAccent),
                            )
                        }
                    }
                }
                BasicTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine    = true,
                    textStyle     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = ScTextPri),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF2F2F2))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_search), null, Modifier.size(16.dp), tint = ScTextSec)
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text("친구 검색", style = TextStyle(fontSize = 15.sp, color = ScTextSec, fontFamily = PretendardFamily))
                                }
                                innerTextField()
                            }
                        }
                    },
                )
                HorizontalDivider(color = ScBorder, thickness = 0.5.dp)
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(friends) { friend ->
                    val selected = selectedIds.contains(friend.id)
                    FriendSelectRow(
                        friend     = friend,
                        isGroup    = true,
                        isSelected = selected,
                        onClick    = {
                            selectedIds = if (selected) selectedIds - friend.id else selectedIds + friend.id
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScBorder, thickness = 0.5.dp)
                }
            }

            toastMsg?.let { msg ->
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF444444))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Text(msg, style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = Color.White))
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupNameDialog(
    onDismiss : () -> Unit,
    onConfirm : (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        title = {
            Text(
                "방 이름 입력",
                style = TextStyle(fontSize = 17.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = ScTextPri),
            )
        },
        text = {
            BasicTextField(
                value         = name,
                onValueChange = { name = it },
                singleLine    = true,
                textStyle     = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = ScTextPri),
                modifier      = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF2F2F2))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (name.isEmpty()) {
                            Text("채팅방 이름", style = TextStyle(fontSize = 15.sp, color = ScTextSec, fontFamily = PretendardFamily))
                        }
                        innerTextField()
                    }
                },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, color = ScTextSec))
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text(
                    "확인",
                    style = TextStyle(fontSize = 15.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(600), color = ScAccent),
                )
            }
        },
    )
}
