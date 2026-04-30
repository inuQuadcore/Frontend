// 채팅방 화면 — 구현 예정
package com.everybuddy.app.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.everybuddy.app.ui.theme.PretendardFamily

// TODO 구현 목록 (ChatRoomScreen.kt)
//
// 1. 채팅방 진입 & 메시지 로드
//     - ChatRoomViewModel.loadRoom(roomId) 호출
//     - GET /api/v1/messages?chatRoomId={id}&page=0 → 초기 메시지 목록 표시
//     - 페이지네이션: LazyColumn 맨 위 도달 시 추가 로드
//
// 2. 메시지 목록 UI (LazyColumn)
//     - 내 메시지(오른쪽) / 상대 메시지(왼쪽) 말풍선 레이아웃
//     - 텍스트 메시지 / 음성 메시지(재생 버튼 + 파형 + 시간) 분기
//     - 날짜 구분선 (ex: "2025년 1월 1일")
//     - 읽음 표시 (✓✓)
//
// 3. 번역 기능
//     - 메시지 꾹 눌러 번역 토글
//     - ChatRoomViewModel.onToggleTranslation(messageId)
//     - TODO API: POST /api/v1/translate (QWEN3-mt 연동)
//     - 자동 번역 토글 스위치 (isAutoTranslate)
//
// 4. 텍스트 입력 바
//     - OutlinedTextField + 전송 버튼
//     - 키보드 올라올 때 LazyColumn 자동 스크롤 (imePadding)
//     - ChatRoomViewModel.onInputChange() / onSendText()
//
// 5. 음성 녹음
//     - 마이크 버튼 꾹 누르면 녹음 시작 → 떼면 전송
//     - RECORD_AUDIO 권한 런타임 요청 (accompanist-permissions)
//     - 녹음 중 파형 애니메이션 + 초 단위 타이머 표시
//     - ChatRoomViewModel.onStartRecording() / onStopRecording() / onCancelRecording()
//     - TODO API: Firebase Storage 업로드 후 URL을 메시지로 전송
//
// 6. 미디어 패널 (+ 버튼 클릭 시)
//     - 이미지/동영상 첨부, 파일 첨부 등
//     - ChatRoomViewModel.onToggleMediaPanel()
//     - TODO: 파일 선택 → sendFileMessage() API 연동
//
// 7. 상단 앱바
//     - 뒤로가기 버튼 / 채팅방 이름 / 메뉴 버튼 (설정, 나가기)
//     - 상대방 프로필 이미지 + 국적 아이콘
//     - TODO: GET /api/v1/users/{userId} 프로필 조회
//
// 8. 실시간 메시지 수신
//     - TODO WebSocket/SSE: 서버 구현 후 연동
//       현재는 더미 데이터(dummyMessages)로 동작
//
// 9. 채팅방 설정
//     - 알림 끄기 / 채팅방 나가기
//     - TODO API: DELETE /api/v1/채팅방/{chatRoomId}

/**
 * ChatRoomScreen — 진입점
 *
 * TODO: 전체 구현 필요. AppNavGraph에서 "chat_room/{roomId}" 라우트로 진입.
 *       hideBottomBarRoutes에 "chat_room/{roomId}" 추가 필요 (MainScreen.kt 참고).
 */
@Composable
fun ChatRoomScreen(
    roomId    : String,
    onBack    : () -> Unit,
    viewModel : ChatRoomViewModel = hiltViewModel(),
) {
    // TODO: LaunchedEffect(roomId) { viewModel.loadRoom(roomId) }
    // TODO: val state by viewModel.uiState.collectAsState()
    // TODO: ChatRoomContent(state, onBack, viewModel::onInputChange, ...) 호출

    // ── 임시 플레이스홀더 ─────────────────────────────────────
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = "채팅방\n(구현 예정)",
            style = TextStyle(
                fontSize   = 16.sp,
                fontFamily = PretendardFamily,
                color      = Color(0xFF8F9399),
            ),
        )
    }
}
