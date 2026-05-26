package com.everybuddy.app.ui.chat

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
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

@Composable
fun VideoPlayerScreen(
    message           : ChatMessage,
    senderName        : String,
    subtitles         : List<VideoSubtitle>,
    isSubtitleLoading : Boolean,
    onClose           : () -> Unit,
) {
    val context  = LocalContext.current
    val activity = context as? Activity

    val player = remember(message.id) {
        ExoPlayer.Builder(context).build().apply {
            val url = message.localFilePath?.takeIf { File(it).exists() } ?: message.voiceUrl
            if (url.isNotBlank()) {
                val uri = if (url.startsWith("/")) Uri.fromFile(File(url)) else Uri.parse(url)
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
            }
        }
    }

    var positionMs   by remember { mutableLongStateOf(0L) }
    var durationMs   by remember { mutableLongStateOf(0L) }
    var isPlaying    by remember { mutableStateOf(false) }
    var isDragging   by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var isScriptOpen by remember { mutableStateOf(false) }
    var isLandscape  by remember { mutableStateOf(false) }

    LaunchedEffect(player) {
        while (true) {
            delay(100)
            if (!isDragging) {
                positionMs = player.currentPosition.coerceAtLeast(0)
                durationMs = player.duration.coerceAtLeast(0)
            }
            isPlaying = player.isPlaying
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val activeSubtitleIdx = remember(positionMs, subtitles) {
        subtitles.indexOfLast { it.startMs <= positionMs }
    }
    val currentSubtitle = subtitles.getOrNull(activeSubtitleIdx)

    val seekProgress = if (isDragging) dragProgress
                       else if (durationMs > 0) positionMs.toFloat() / durationMs
                       else 0f

    Dialog(
        onDismissRequest = onClose,
        properties       = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(VpBg)) {

            Column(modifier = Modifier.fillMaxSize()) {

                // ① Sender name (48dp, centered)
                Box(
                    modifier         = Modifier.fillMaxWidth().height(48.dp).statusBarsPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = senderName,
                        style = TextStyle(
                            fontSize   = 16.sp,
                            fontFamily = PretendardFamily,
                            fontWeight = FontWeight.Medium,
                            color      = Color.White,
                        ),
                    )
                }

                // ③ Video area (fills remaining space)
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
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
                }

                // ④ Subtitle text area (below video, min 72dp)
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
                        Text(
                            text  = currentSubtitle?.translated ?: "",
                            style = TextStyle(
                                fontSize   = 15.sp,
                                fontFamily = PretendardFamily,
                                color      = Color.White,
                                lineHeight  = 21.sp,
                            ),
                        )
                    }
                }

                // ⑤ Controls row: [Play/Pause] [Seekbar] [Rotate] [Caption]
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .background(VpBg)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VpCtrlBtn(onClick = { if (isPlaying) player.pause() else player.play() }) {
                        if (isPlaying) {
                            Image(
                                painter            = painterResource(R.drawable.ic_play),
                                contentDescription = "일시정지",
                                modifier           = Modifier.size(24.dp),
                                colorFilter        = ColorFilter.tint(Color.White),
                            )
                        } else {
                            // TODO: play 아이콘 이름 확인 후 Image(painterResource(R.drawable.ic_???)) 로 교체
                            Text("▶", style = TextStyle(fontSize = 20.sp, color = Color.White))
                        }
                    }

                    Slider(
                        value                = seekProgress,
                        onValueChange        = { isDragging = true; dragProgress = it },
                        onValueChangeFinished = {
                            player.seekTo((dragProgress * durationMs).toLong())
                            isDragging = false
                        },
                        colors   = SliderDefaults.colors(
                            thumbColor         = VpAccent,
                            activeTrackColor   = VpAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                        ),
                        modifier = Modifier.weight(1f),
                    )

                    // Rotate button — TODO: ic_rotate 아이콘 추가 시 교체
                    VpCtrlBtn(onClick = {
                        isLandscape = !isLandscape
                        activity?.requestedOrientation =
                            if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }) {
                        Text("⟳", style = TextStyle(fontSize = 20.sp, color = Color.White))
                    }

                    VpCtrlBtn(
                        onClick    = { isScriptOpen = !isScriptOpen },
                        isActive   = isScriptOpen,
                    ) {
                        Image(
                            painter            = painterResource(R.drawable.ic_caption),
                            contentDescription = "스크립트",
                            modifier           = Modifier.size(22.dp),
                            colorFilter        = ColorFilter.tint(Color.White),
                        )
                    }
                }

                // ⑥ Time row
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .background(VpBg)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text  = vpFmtDuration((positionMs / 1000).toInt()),
                        style = TextStyle(fontSize = 12.sp, color = VpTextSec, fontFamily = PretendardFamily),
                    )
                    Text(
                        text  = vpFmtDuration((durationMs / 1000).toInt()),
                        style = TextStyle(fontSize = 12.sp, color = VpTextSec, fontFamily = PretendardFamily),
                    )
                }
            }

            // ② Close button — top-end overlay, circular semi-transparent bg
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
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
                    colorFilter        = ColorFilter.tint(Color.White),
                )
            }

            // ⑧ Script panel — slides in from right, centered vertically
            Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()) {
                AnimatedVisibility(
                    visible = isScriptOpen,
                    enter   = slideInHorizontally { it },
                    exit    = slideOutHorizontally { it },
                ) {
                    ScriptPanel(
                        modifier  = Modifier.width(260.dp).fillMaxHeight().background(VpScriptBg),
                        subtitles = subtitles,
                        activeIdx = activeSubtitleIdx,
                        isLoading = isSubtitleLoading,
                        onSeekTo  = { ms -> player.seekTo(ms) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VpCtrlBtn(
    onClick  : () -> Unit,
    isActive : Boolean = false,
    content  : @Composable () -> Unit,
) {
    Box(
        modifier         = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isActive) VpAccent else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content          = { content() },
    )
}

@Composable
private fun ScriptPanel(
    modifier  : Modifier,
    subtitles : List<VideoSubtitle>,
    activeIdx : Int,
    isLoading : Boolean,
    onSeekTo  : (Long) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0) listState.animateScrollToItem(activeIdx)
    }

    Column(modifier = modifier) {
        // Panel header
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(VpScriptHdr)
                .padding(start = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text  = "스크립트",
                style = TextStyle(
                    fontSize   = 14.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight.Medium,
                    color      = Color.White,
                ),
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

private fun vpFmtDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
