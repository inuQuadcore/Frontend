// 재생(ExoPlayer/Media3) 래퍼
//
// TODO 기능: pause() / resume() 구현
//           현재는 stop()만 있음 — play() 호출 시 처음부터 재생됨
//           PlayerState.Paused 활용하여 일시정지 후 재개 지원
//
//
// TODO 에러: Player.Listener.onPlayerError() 구현
//           네트워크 오류 시 PlayerState.Error 추가 및 UI에 재시도 버튼 표시
//
// TODO 로컬: Firebase Storage URL뿐 아니라 캐시 디렉터리 로컬 파일 경로도 지원
//           (녹음 후 전송 전 미리 듣기용)
package com.everybuddy.app.data.chat

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class PlayerState {
    object Idle                               : PlayerState()
    data class Playing(val messageId: String) : PlayerState()
    data class Paused(val messageId: String)  : PlayerState()
    object Finished                           : PlayerState()
}

@Singleton
class VoicePlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionJob      : Job?        = null
    private var player           : ExoPlayer?  = null
    private var currentMessageId : String?     = null

    private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** 현재 재생 위치(ms). 재생 중 100ms마다 갱신, 정지 시 0으로 초기화. */
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    /** (messageId → durationSec) — STATE_READY 시 1회 emit 후 null로 초기화 */
    private val _durationReady = MutableStateFlow<Pair<String, Int>?>(null)
    val durationReady: StateFlow<Pair<String, Int>?> = _durationReady.asStateFlow()

    /** url은 원격 URL 또는 로컬 절대 경로. ExoPlayer가 둘 다 처리. */
    fun play(messageId: String, url: String) {
        stop()
        val p = ExoPlayer.Builder(context).build()
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val durationMs = p.duration
                        if (durationMs > 0) {
                            _durationReady.value = messageId to (durationMs / 1000).toInt()
                        }
                    }
                    Player.STATE_ENDED -> {
                        _state.value     = PlayerState.Finished
                        currentMessageId = null
                    }
                }
            }
        })
        // 로컬 경로면 file:// URI로, 그 외엔 그대로.
        val uri = if (url.startsWith("/")) "file://$url" else url
        p.setMediaItem(MediaItem.fromUri(uri))
        p.prepare()
        p.play()
        player           = p
        currentMessageId = messageId
        _state.value     = PlayerState.Playing(messageId)
        positionJob = scope.launch {
            while (true) {
                delay(100)
                val cur = player ?: break
                _positionMs.value = cur.currentPosition.coerceAtLeast(0)
                if (!cur.isPlaying) break
            }
        }
    }

    fun stop() {
        positionJob?.cancel()
        positionJob      = null
        player?.release()
        player           = null
        currentMessageId = null
        _positionMs.value = 0L
        _state.value     = PlayerState.Idle
    }

    fun release() {
        stop()
    }
}
