package com.everybuddy.app.ui.chat

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
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

private val VpBg           = Color(0xFF000000)
private val VpSubtitleBg   = Color(0xCC000000)
private val VpControlBg    = Color(0xB3000000)
private val VpAccent       = Color(0xFF0167FF)
private val VpTextSec      = Color(0xFFAAAAAA)
private val VpScriptBg     = Color(0xFF111111)
private val VpDivider      = Color(0xFF2A2A2A)

@Composable
fun VideoPlayerScreen(
    message           : ChatMessage,
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
    var showControls by remember { mutableStateOf(true) }
    var isScriptOpen by remember { mutableStateOf(false) }
    var isLandscape  by remember { mutableStateOf(false) }

    // Position polling
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

    // Auto-hide controls after 3s while playing
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    // Scroll script panel to active subtitle
    val activeSubtitleIdx = remember(positionMs, subtitles) {
        subtitles.indexOfFirst { it.startMs <= positionMs && positionMs < it.endMs }
    }
    val currentSubtitle = subtitles.getOrNull(activeSubtitleIdx)

    val seekProgress = if (isDragging) dragProgress
                       else if (durationMs > 0) positionMs.toFloat() / durationMs
                       else 0f

    DisposableEffect(Unit) {
        onDispose {
            player.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties       = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(VpBg)) {
            Row(modifier = Modifier.fillMaxSize()) {

                // ── Video Section ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(if (isScriptOpen) 0.6f else 1f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures { showControls = !showControls }
                        },
                ) {
                    // ExoPlayer surface
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

                    // Movie-style subtitle overlay
                    val bottomPad = if (showControls) 88.dp else 20.dp
                    currentSubtitle?.let { sub ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = bottomPad, start = 16.dp, end = 16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(VpSubtitleBg)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text  = sub.translated,
                                style = TextStyle(
                                    fontSize   = 17.sp,
                                    fontFamily = PretendardFamily,
                                    fontWeight = FontWeight(600),
                                    color      = Color.White,
                                ),
                            )
                        }
                    }

                    // Subtitle loading spinner
                    if (isSubtitleLoading) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = bottomPad),
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(22.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp,
                            )
                        }
                    }

                    // Controls overlay
                    AnimatedVisibility(
                        visible  = showControls,
                        enter    = fadeIn(),
                        exit     = fadeOut(),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {

                            // ── Top bar ─────────────────────────────────
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(VpControlBg)
                                    .statusBarsPadding()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .align(Alignment.TopStart),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment     = Alignment.CenterVertically,
                            ) {
                                // Script toggle
                                VpIconButton(
                                    onClick    = { isScriptOpen = !isScriptOpen },
                                    isActive   = isScriptOpen,
                                ) {
                                    Text("📄", fontSize = 16.sp) // TODO: ic_script 교체
                                }
                                Spacer(Modifier.width(4.dp))
                                // Rotate
                                VpIconButton(onClick = {
                                    isLandscape = !isLandscape
                                    activity?.requestedOrientation =
                                        if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }) {
                                    Text("⟳", fontSize = 20.sp, color = Color.White) // TODO: ic_rotate 교체
                                }
                                Spacer(Modifier.width(4.dp))
                                // Close
                                VpIconButton(onClick = onClose) {
                                    Image(
                                        painter            = painterResource(R.drawable.ic_cancel),
                                        contentDescription = "닫기",
                                        modifier           = Modifier.size(18.dp),
                                        colorFilter        = ColorFilter.tint(Color.White),
                                    )
                                }
                            }

                            // ── Center play/pause ────────────────────────
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable {
                                        if (isPlaying) player.pause() else player.play()
                                        showControls = true
                                    }
                                    .align(Alignment.Center),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text  = if (isPlaying) "⏸" else "▶", // TODO: ic_pause / ic_play 교체
                                    style = TextStyle(fontSize = 26.sp, color = Color.White),
                                )
                            }

                            // ── Bottom seek bar + time ───────────────────
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(VpControlBg)
                                    .navigationBarsPadding()
                                    .padding(horizontal = 12.dp)
                                    .align(Alignment.BottomStart),
                            ) {
                                Slider(
                                    value    = seekProgress,
                                    onValueChange = { isDragging = true; dragProgress = it },
                                    onValueChangeFinished = {
                                        player.seekTo((dragProgress * durationMs).toLong())
                                        isDragging = false
                                    },
                                    colors   = SliderDefaults.colors(
                                        thumbColor         = Color.White,
                                        activeTrackColor   = VpAccent,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(
                                    modifier              = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
                        }
                    }
                }

                // ── Script Panel ───────────────────────────────────────────
                if (isScriptOpen) {
                    ScriptPanel(
                        modifier          = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                            .background(VpScriptBg),
                        subtitles         = subtitles,
                        activeIdx         = activeSubtitleIdx,
                        isLoading         = isSubtitleLoading,
                    )
                }
            }
        }
    }
}

@Composable
private fun VpIconButton(
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
) {
    val listState = rememberLazyListState()

    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0) {
            listState.animateScrollToItem(
                index       = activeIdx,
                scrollOffset = -80,
            )
        }
    }

    Box(modifier = modifier) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier    = Modifier.align(Alignment.Center).size(32.dp),
                    color       = VpAccent,
                    strokeWidth = 2.dp,
                )
            }
            subtitles.isEmpty() -> {
                Text(
                    text     = "자막 없음",
                    modifier = Modifier.align(Alignment.Center),
                    style    = TextStyle(fontSize = 14.sp, color = VpTextSec, fontFamily = PretendardFamily),
                )
            }
            else -> {
                LazyColumn(
                    state          = listState,
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    itemsIndexed(subtitles, key = { i, _ -> i }) { idx, sub ->
                        val isActive = idx == activeIdx
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isActive) VpAccent else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Column {
                                Text(
                                    text  = sub.translated,
                                    style = TextStyle(
                                        fontSize   = 16.sp,
                                        fontFamily = PretendardFamily,
                                        color      = if (isActive) Color.White else Color(0xFFE0E0E0),
                                        fontWeight = if (isActive) FontWeight(700) else FontWeight(400),
                                    ),
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text  = sub.original,
                                    style = TextStyle(
                                        fontSize   = 12.sp,
                                        fontFamily = PretendardFamily,
                                        color      = if (isActive) Color.White.copy(0.75f) else Color(0xFF777777),
                                    ),
                                )
                            }
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
