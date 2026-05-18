// 재생(ExoPlayer/Media3) 래퍼
//
// TODO 기능: pause() / resume() 구현
//           현재는 stop()만 있음 — play() 호출 시 처음부터 재생됨
//           PlayerState.Paused 활용하여 일시정지 후 재개 지원
//
// TODO 진행률: 현재 재생 위치(ms)를 StateFlow<Long>으로 노출
//             Player.Listener.onPositionDiscontinuity / 주기적 poll(Handler)로 업데이트
//             ChatRoomScreen의 음성 메시지 진행 바(Slider) 연동
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
    private var player           : ExoPlayer? = null
    private var currentMessageId : String?    = null

    private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** url은 원격 URL 또는 로컬 절대 경로. ExoPlayer가 둘 다 처리. */
    fun play(messageId: String, url: String) {
        stop()
        val p = ExoPlayer.Builder(context).build()
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    _state.value     = PlayerState.Finished
                    currentMessageId = null
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
    }

    fun stop() {
        player?.release()
        player           = null
        currentMessageId = null
        _state.value     = PlayerState.Idle
    }

    fun release() {
        stop()
    }
}
