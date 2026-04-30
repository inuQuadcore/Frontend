package com.everybuddy.app.ui.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.R
import com.everybuddy.app.data.dummy.*
import com.everybuddy.app.data.network.AuthResult
import com.everybuddy.app.data.network.ChatRepository
import com.everybuddy.app.ui.explore.ExploreTagChip
import com.everybuddy.app.ui.theme.*
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// UiState
data class StartChatUiState(
    val friends     : List<DummyUser> = friendUsers,
    val selectedIds : Set<String>     = emptySet(),
    val searchQuery : String          = "",
)

val StartChatUiState.filteredFriends: List<DummyUser>
    get() = if (searchQuery.isEmpty()) friends
    else friends.filter { it.name.contains(searchQuery, ignoreCase = true) }

val StartChatUiState.selectedUsers: List<DummyUser>
    get() = friends.filter { it.id in selectedIds }


// ViewModel
/**
 * StartChatViewModel
 *
 * 채팅 시작 화면의 친구 선택 상태를 관리.
 * 완료 버튼 클릭 시 ChatViewModel.createChatRoom()을 호출.
 * TODO: POST /api/v1/채팅방 API 연동
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class StartChatViewModel @Inject constructor(
    private val sharedRepo     : SharedUserRepository,
    private val chatRepository : ChatRepository,
) : androidx.lifecycle.ViewModel() {

    private val _uiState = MutableStateFlow(StartChatUiState(friends = sharedRepo.friends))
    val uiState: StateFlow<StartChatUiState> = _uiState.asStateFlow()

    fun onFriendSelect(userId: String) {
        _uiState.update { state ->
            val newSelected = if (userId in state.selectedIds)
                state.selectedIds - userId
            else
                state.selectedIds + userId
            state.copy(selectedIds = newSelected)
        }
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onConfirm(onSuccess: (String) -> Unit) {
        val selected = _uiState.value.selectedUsers
        if (selected.isEmpty()) return

        val roomName       = if (selected.size == 1) selected.first().name
                             else selected.joinToString(", ") { it.name }
        val participantIds = selected.map { it.id.filter(Char::isDigit).toLongOrNull() ?: 1L }

        viewModelScope.launch {
            val result = chatRepository.createChatRoom(roomName, participantIds)
            if (result is AuthResult.Success) {
                result.data?.let { room -> onSuccess(room.chatRoomId.toString()) }
            }
        }
    }
}

// 진입점
@Composable
fun StartChatScreen(
    onBack    : () -> Unit,
    onSuccess : (String) -> Unit,
    viewModel : StartChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    StartChatContent(
        state          = state,
        onBack         = onBack,
        onFriendSelect = viewModel::onFriendSelect,
        onSearchChange = viewModel::onSearchChange,
        onConfirm      = { viewModel.onConfirm(onSuccess) },
    )
}

// Content
@Composable
fun StartChatContent(
    state          : StartChatUiState,
    onBack         : () -> Unit,
    onFriendSelect : (String) -> Unit,
    onSearchChange : (String) -> Unit,
    onConfirm      : () -> Unit,
) {
    val filtered   = state.filteredFriends
    val selected   = state.selectedUsers
    val canConfirm = selected.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            StartChatTopBar(onBack = onBack)

            // 선택된 친구 칩 (1명 이상 선택 시)
            if (selected.isNotEmpty()) {
                LazyRow(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(selected, key = { "sel_${it.id}" }) { user ->
                        SelectedFriendChip(
                            user     = user,
                            onRemove = { onFriendSelect(user.id) },
                        )
                    }
                }
            }

            StartChatSearchBar(
                query         = state.searchQuery,
                onValueChange = onSearchChange,
            )

            // 친구 목록 — 완료 버튼 등장 시 마지막 아이템 가려짐 방지
            LazyColumn(
                modifier       = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = if (canConfirm) 140.dp else 20.dp),
            ) {
                if (filtered.isEmpty()) {
                    item { EmptySearchResult() }
                } else {
                    items(filtered, key = { "sc_${it.id}" }) { user ->
                        StartChatFriendItem(
                            user       = user,
                            isSelected = user.id in state.selectedIds,
                            onSelect   = { onFriendSelect(user.id) },
                        )
                    }
                }
            }
        }

        // 완료 버튼 오버레이 (1명 이상 선택 시)
        if (canConfirm) {
            // 흰색 그라디언트 스크림!!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0f), Color.White),
                        ),
                    )
                    .pointerInput(Unit) { detectTapGestures { /* 터치 전파 방지 */ } },
            )

            // 완료 버튼 — 높이 60dp, 하단 여백 51dp
            Button(
                onClick  = onConfirm,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 51.dp)
                    .height(60.dp),
                shape  = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0167FF),
                    contentColor   = Color.White,
                ),
            ) {
                Text(
                    text  = "완료",
                    style = TextStyle(
                        fontSize   = 20.sp,
                        fontFamily = PretendardFamily,
                        fontWeight = FontWeight(600),
                        color      = Color.White,
                    ),
                )
            }
        }

    }
}

// 서브 컴포넌트
@Composable
fun StartChatTopBar(onBack: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .padding(start = 27.dp, end = 27.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                painter            = painterResource(R.drawable.ic_back),
                contentDescription = "뒤로가기",
                modifier           = Modifier.size(width = 15.dp, height = 8.dp),
                tint               = Color.Black,
            )
        }
        Text(
            text      = "채팅시작",
            style     = TextStyle(
                fontSize   = 20.sp,
                fontFamily = PretendardFamily,
                fontWeight = FontWeight(600),
                color      = Color(0xFF000000),
            ),
            modifier  = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(40.dp))
    }
}

@Composable
fun StartChatSearchBar(query: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value         = query,
        onValueChange = onValueChange,
        placeholder   = {
            Text(
                text  = "친구를 검색해보세요",
                style = TextStyle(
                    fontSize   = 15.sp,
                    fontFamily = PretendardFamily,
                    color      = Color(0xFFA9A9A9),
                ),
            )
        },
        leadingIcon = {
            Icon(
                painter            = painterResource(R.drawable.ic_search),
                contentDescription = null,
                modifier           = Modifier.size(16.dp),
                tint               = Color(0xFFA9A9A9),
            )
        },
        singleLine = true,
        modifier   = Modifier
            .fillMaxWidth()
            .padding(horizontal = 23.dp, vertical = 14.dp)
            .height(48.dp),
        shape  = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color(0xFFF0F0F0),
            focusedContainerColor   = Color(0xFFF0F0F0),
            unfocusedBorderColor    = Color.Transparent,
            focusedBorderColor      = Color.Transparent,
            cursorColor             = Color(0xFF0167FF),
        ),
        textStyle = TextStyle(
            fontSize   = 15.sp,
            fontFamily = PretendardFamily,
            color      = Color(0xFF1B1B1B),
        ),
    )
}

@Composable
fun EmptySearchResult() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = "검색 결과가 없습니다.",
            style = TextStyle(
                fontSize   = 15.sp,
                fontFamily = PretendardFamily,
                color      = Color(0xFF8F9399),
            ),
        )
    }
}

/**
 * 선택된 친구 칩
 * 프로필 45dp + 우상단 ic_cancel.xml(18dp, offset x=3 y=-2) + 이름 12sp
 */
@Composable
fun SelectedFriendChip(user: DummyUser, onRemove: () -> Unit) {
    Column(
        modifier            = Modifier.width(58.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                modifier         = Modifier
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8EEF7)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter            = painterResource(R.drawable.ic_profile_default),
                    contentDescription = user.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.size(45.dp),
                )
            }
            Box(
                modifier         = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B1B1B))
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-2).dp)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter            = painterResource(R.drawable.ic_cancel),
                    contentDescription = "선택 취소",
                    modifier           = Modifier.size(10.dp),
                    tint               = Color.White,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text      = user.name,
            style     = TextStyle(
                fontSize   = 12.sp,
                fontFamily = PretendardFamily,
                color      = Color(0xFF1B1B1B),
            ),
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 친구 선택 리스트 아이템
 * 프로필 69dp / 국적 아이콘 28dp (offset x=7 y=7)
 * 미선택: ic_check_empty.xml 회색 / 선택: ic_check.xml 파란색
 */
@Composable
fun StartChatFriendItem(
    user       : DummyUser,
    isSelected : Boolean,
    onSelect   : () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(start = 26.dp, end = 14.dp, top = 25.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 프로필 이미지 + 국적 아이콘
        Box(modifier = Modifier.size(97.dp)) {
            Box(
                modifier         = Modifier
                    .size(69.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8EEF7))
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter            = painterResource(R.drawable.ic_profile_default),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.size(69.dp),
                )
            }
            Box(
                modifier         = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.BottomEnd)
                    .offset(x = 7.dp, y = 7.dp),  // 쉼표 오류 수정
                contentAlignment = Alignment.Center,
            ) {
                Text(nationFlagEmoji(user.nationCode), fontSize = 21.sp)
            }
        }

        Spacer(Modifier.width(15.dp))

        // 텍스트 블록
        Column(modifier = Modifier.weight(1f)) {  // Column 명시적 시작
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                user.topTags().forEach { tag ->
                    ExploreTagChip(label = tag.replace("_", " "))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text  = user.name,
                style = TextStyle(
                    fontSize   = 16.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(600),
                    color      = Color(0xFF000000),
                ),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text     = user.bio.take(15),
                style    = TextStyle(
                    fontSize   = 13.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight(400),
                    color      = Color(0xFF434343),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            // 사용 언어 + 레벨 점
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                user.myLangs.forEach { (langCode, level) ->
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text  = langCode,
                            style = TextStyle(
                                fontSize   = 13.sp,
                                fontFamily = PretendardFamily,
                                fontWeight = FontWeight(500),
                                color      = Color(0xFF9A9A9A),
                            ),
                        )
                        Spacer(Modifier.width(4.dp))
                        repeat(5) { i ->
                            Icon(
                                painter            = painterResource(
                                    if (i < level) R.drawable.ic_dot_filled
                                    else           R.drawable.ic_dot_empty
                                ),
                                contentDescription = null,
                                modifier           = Modifier
                                    .padding(1.dp)
                                    .size(6.dp),
                                tint               = if (i < level) Color(0xFF0167FF)
                                else           Color(0xFFDEDEDE),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // 체크 아이콘
        Icon(
            painter            = painterResource(
                if (isSelected) R.drawable.ic_check else R.drawable.ic_check_empty
            ),
            contentDescription = if (isSelected) "선택됨" else "미선택",
            modifier           = Modifier.size(20.dp),
            tint               = if (isSelected) Color(0xFF0167FF) else Color(0xFFDCDCDC),
        )
    }
}

// Preview
@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "미선택")
@Composable
private fun PreviewEmpty() {
    EveryBuddyTheme {
        StartChatContent(
            state          = StartChatUiState(),
            onBack         = {},
            onFriendSelect = {},
            onSearchChange = {},
            onConfirm      = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "1명 선택")
@Composable
private fun PreviewSelected() {
    EveryBuddyTheme {
        StartChatContent(
            state          = StartChatUiState(selectedIds = setOf("u01")),
            onBack         = {},
            onFriendSelect = {},
            onSearchChange = {},
            onConfirm      = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917, name = "검색 중")
@Composable
private fun PreviewSearch() {
    EveryBuddyTheme {
        StartChatContent(
            state          = StartChatUiState(searchQuery = "Potter"),
            onBack         = {},
            onFriendSelect = {},
            onSearchChange = {},
            onConfirm      = {},
        )
    }
}
