package com.everybuddy.app.ui.chat

import android.app.Activity
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.everybuddy.app.R
import com.everybuddy.app.data.chat.ChatMessage
import com.everybuddy.app.data.chat.VideoSubtitle
import com.everybuddy.app.ui.theme.PretendardFamily
import kotlinx.coroutines.delay
import java.io.File

private val VpBg        = Color(0xFF000000)
private val VpAccent    = Color(0xFF2979FF)
private val VpTextSec   = Color(0xFFAAAAAA)
private val VpScriptBg  = Color(0xFF1A1A1A)
private val VpScriptHdr = Color(0xFF222222)
private val VpDivider   = Color(0xFF2A2A2A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    message           : ChatMessage,
    senderName        : String,
    subtitles         : List<VideoSubtitle>,
    isSubtitleLoading : Boolean,
    isVideoLoading    : Boolean,
    onClose           : () -> Unit,
) {
    val context = LocalContext.current

    // 로컬 파일이 준비된 후에만 ExoPlayer 생성 — 다운로드 중에는 S3 스트리밍을 피하려 보류한다.
    // 다운로드 실패 등으로 파일이 없는 채 in-flight도 끝났으면 voiceUrl로 fallback 스트리밍.
    val localFile = message.localFilePath?.takeIf { File(it).exists() }
    val playerSource: String? = when {
        localFile != null            -> localFile
        isVideoLoading               -> null
        message.voiceUrl.isNotBlank() -> message.voiceUrl
        else                          -> null
    }

    val player = remember(playerSource) {
        playerSource?.let { src ->
            ExoPlayer.Builder(context).build().apply {
                val uri = if (src.startsWith("/")) Uri.fromFile(File(src)) else Uri.parse(src)
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
            }
        }
    }

    // position/duration은 root에서 .value를 읽지 않는다 — 자식 컴포저블이 직접 구독해야
    // 100ms tick에 root 전체(AndroidView/ScriptPanel 포함)가 리컴포즈되는 사태를 피한다.
    val positionState = remember { mutableLongStateOf(0L) }
    val durationState = remember { mutableLongStateOf(0L) }
    var isPlaying    by remember { mutableStateOf(false) }
    var isDragging   by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var isScriptOpen by remember { mutableStateOf(false) }

    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        while (true) {
            delay(100)
            if (p.playbackState == Player.STATE_ENDED) {
                p.seekTo(0)
                p.pause()
                positionState.longValue = 0
            } else if (!isDragging) {
                positionState.longValue = p.currentPosition.coerceAtLeast(0)
                durationState.longValue = p.duration.coerceAtLeast(0)
            }
            isPlaying = p.isPlaying
        }
    }

    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    // derivedStateOf — 활성 인덱스가 실제로 바뀌는 순간(~1초)에만 구독자가 리컴포즈된다.
    val activeSubtitleIdxState = remember(subtitles) {
        derivedStateOf { subtitles.indexOfLast { it.startMs <= positionState.longValue } }
    }

    Dialog(
        onDismissRequest = onClose,
        properties       = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(VpBg)) {

            Column(modifier = Modifier.fillMaxSize()) {

                // 상단 바: 발신자 이름(중앙) + 닫기 버튼(우측)
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(52.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.width(36.dp))
                    Text(
                        text      = senderName,
                        modifier  = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style     = TextStyle(
                            fontSize   = 16.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight.Medium,
                            color      = Color.White,
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter            = painterResource(R.drawable.ic_cancel),
                            contentDescription = "닫기",
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                }

                // 영상 영역 — 다운로드 중이거나 player 미생성 시 스피너만 노출
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    if (player != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    this.player = player
                                    useController = false
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier    = Modifier.align(Alignment.Center).size(32.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp,
                        )
                    }
                }

                // 자막 텍스트 영역
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 72.dp)
                        .background(VpBg)
                        .padding(16.dp),
                ) {
                    if (isSubtitleLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.align(Alignment.Center).size(22.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        VpSubtitleText(activeIdxState = activeSubtitleIdxState, subtitles = subtitles)
                    }
                }

                // 컨트롤 영역
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VpBg)
                        .padding(horizontal = 8.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 48.dp),
                ) {
                    // 스크립트 아이콘 — 시크바 오른쪽 상단 (시크바 우측 끝과 정렬)
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(start = 40.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clickable(
                                    indication        = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick           = { isScriptOpen = !isScriptOpen },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter            = painterResource(R.drawable.ic_caption),
                                contentDescription = "스크립트",
                                modifier           = Modifier.fillMaxSize(),
                                colorFilter        = ColorFilter.tint(if (isScriptOpen) VpAccent else Color.White),
                                alpha              = if (isScriptOpen) 1f else 0.65f,
                            )
                        }
                    }

                    // 재생/정지 버튼 + 시크바 — 같은 행. Spacer로 스크립트 아이콘 왼쪽 끝에 정렬
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VpCtrlBtn(onClick = {
                            val p = player ?: return@VpCtrlBtn
                            if (isPlaying) p.pause() else p.play()
                        }) {
                            Image(
                                painter            = painterResource(
                                    if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play
                                ),
                                contentDescription = if (isPlaying) "정지" else "재생",
                                modifier           = Modifier.size(24.dp),
                                colorFilter        = ColorFilter.tint(Color.White),
                            )
                        }

                        VpSeekBar(
                            positionState = positionState,
                            durationState = durationState,
                            isDragging    = isDragging,
                            dragProgress  = dragProgress,
                            onDragChange  = { isDragging = true; dragProgress = it },
                            onDragFinish  = {
                                val p = player
                                if (p != null) p.seekTo((dragProgress * durationState.longValue).toLong())
                                isDragging = false
                            },
                            modifier      = Modifier.weight(1f),
                        )

                    }

                    // 재생 시간 — 시크바 오른쪽 하단
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(end = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        VpTimeText(positionState = positionState, durationState = durationState)
                    }
                }
            }

            // 스크립트 패널 — 우측에서 슬라이드 인
            Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()) {
                AnimatedVisibility(
                    visible = isScriptOpen,
                    enter   = slideInHorizontally { it },
                    exit    = slideOutHorizontally { it },
                ) {
                    ScriptPanel(
                        modifier       = Modifier.width(260.dp).fillMaxHeight().background(VpScriptBg),
                        subtitles      = subtitles,
                        activeIdxState = activeSubtitleIdxState,
                        isLoading      = isSubtitleLoading,
                        onSeekTo       = { ms -> player?.seekTo(ms) },
                        onClose        = { isScriptOpen = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun VpCtrlBtn(
    onClick : () -> Unit,
    content : @Composable () -> Unit,
) {
    Box(
        modifier         = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content          = { content() },
    )
}

@Composable
private fun ScriptPanel(
    modifier       : Modifier,
    subtitles      : List<VideoSubtitle>,
    activeIdxState : State<Int>,
    isLoading      : Boolean,
    onSeekTo       : (Long) -> Unit,
    onClose        : () -> Unit,
) {
    val listState = rememberLazyListState()
    val activeIdx by activeIdxState

    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0) listState.animateScrollToItem(activeIdx)
    }

    Column(modifier = modifier) {
        // 헤더: 좌측 뒤로가기 + 중앙 제목
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(40.dp)
                .background(VpScriptHdr),
        ) {
            Image(
                painter            = painterResource(R.drawable.ic_back),
                contentDescription = "닫기",
                modifier           = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(24.dp)
                    .clickable(onClick = onClose),
                colorFilter        = ColorFilter.tint(Color.White),
            )
            Text(
                text     = "스크립트",
                style    = TextStyle(
                    fontSize   = 14.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight.Medium,
                    color      = Color.White,
                ),
                modifier = Modifier.align(Alignment.Center),
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier         = Modifier.height(280.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = VpAccent, strokeWidth = 2.dp)
                }
            }
            subtitles.isEmpty() -> {
                Box(
                    modifier         = Modifier.height(280.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "자막 없음",
                        style = TextStyle(fontSize = 14.sp, color = VpTextSec, fontFamily = PretendardFamily),
                    )
                }
            }
            else -> {
                LazyColumn(
                    state          = listState,
                    modifier       = Modifier.height(280.dp).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    itemsIndexed(subtitles, key = { i, _ -> i }) { idx, sub ->
                        val isActive = idx == activeIdx
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isActive) 1f else 0.55f)
                                .clickable { onSeekTo(sub.startMs) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text  = "[${vpFmtDuration((sub.startMs / 1000).toInt())}] ${sub.translated}",
                                style = TextStyle(
                                    fontSize   = 13.sp,
                                    fontFamily = PretendardFamily,
                                    color      = Color.White,
                                    lineHeight  = 17.sp,
                                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                ),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text  = sub.original,
                                style = TextStyle(
                                    fontSize   = 11.sp,
                                    fontFamily = PretendardFamily,
                                    color      = Color(0xFF888888),
                                ),
                            )
                        }
                        if (idx < subtitles.lastIndex) {
                            HorizontalDivider(color = VpDivider, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

/** 자막 한 줄 — activeIdxState만 읽으므로 인덱스가 바뀌는 순간(~1초)에만 리컴포즈된다. */
@Composable
private fun VpSubtitleText(activeIdxState: State<Int>, subtitles: List<VideoSubtitle>) {
    val idx by activeIdxState
    Text(
        text      = subtitles.getOrNull(idx)?.translated ?: "",
        textAlign = TextAlign.Center,
        style     = TextStyle(
            fontSize    = 15.sp,
            fontFamily  = PretendardFamily,
            color       = Color.White,
            lineHeight  = 21.sp,
            textAlign   = TextAlign.Center,
        ),
        modifier  = Modifier.fillMaxWidth(),
    )
}

/** Slider만 100ms tick에 리컴포즈되도록 격리. position 읽기는 함수 내부에서. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VpSeekBar(
    positionState : LongState,
    durationState : LongState,
    isDragging    : Boolean,
    dragProgress  : Float,
    onDragChange  : (Float) -> Unit,
    onDragFinish  : () -> Unit,
    modifier      : Modifier = Modifier,
) {
    val pos = positionState.longValue
    val dur = durationState.longValue
    val seekProgress = if (isDragging) dragProgress
                       else if (dur > 0) pos.toFloat() / dur
                       else 0f
    Slider(
        value                 = seekProgress,
        onValueChange         = onDragChange,
        onValueChangeFinished = onDragFinish,
        thumb  = {
            Image(
                painter            = painterResource(R.drawable.ic_dot_filled),
                contentDescription = null,
                modifier           = Modifier.size(16.dp),
                colorFilter        = ColorFilter.tint(VpAccent),
            )
        },
        track  = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier    = Modifier.height(3.dp),
                colors      = SliderDefaults.colors(
                    activeTrackColor   = VpAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                ),
            )
        },
        modifier = modifier,
    )
}

/** 재생시간 표시 — 시간 텍스트만 100ms tick에 리컴포즈. */
@Composable
private fun VpTimeText(positionState: LongState, durationState: LongState) {
    val pos = positionState.longValue
    val dur = durationState.longValue
    Text(
        text  = "${vpFmtDuration((pos / 1000).toInt())}/${vpFmtDuration((dur / 1000).toInt())}",
        style = TextStyle(fontSize = 12.sp, color = VpTextSec, fontFamily = PretendardFamily),
    )
}

private fun vpFmtDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
