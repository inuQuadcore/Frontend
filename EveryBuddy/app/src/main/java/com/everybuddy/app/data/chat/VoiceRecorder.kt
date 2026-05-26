// 녹음(MediaRecorder) 래퍼
//
// TODO 권한: startRecording() 호출 전 RECORD_AUDIO 런타임 권한 확인 필요
//           (ChatRoomScreen에서 accompanist-permissions로 처리)
//
// TODO 제한: 최대 녹음 시간(예: 120초) 초과 시 자동 stopRecording() 호출
//           timerJob에서 _seconds >= MAX_SECONDS 체크 추가
//
// TODO 업로드: stopRecording() 반환 경로를 Firebase Storage에 업로드
//             → POST /api/v1/messages (type=VOICE, voiceUrl 포함) 로 메시지 전송
//             업로드 진행률 StateFlow<Float> 추가 고려
//
// TODO 파형: _amplitudes 값을 ChatRoomScreen에서 실시간 파형 UI로 시각화
package com.everybuddy.app.data.chat

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var recorder    : MediaRecorder? = null
    private var outputFile  : String?        = null
    private var timerJob    : Job?           = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _seconds = MutableStateFlow(0)
    val seconds: StateFlow<Int> = _seconds.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    fun startRecording(): Boolean {
        return try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = file.absolutePath

            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(44_100)
                setAudioEncodingBitRate(96_000)
                setOutputFile(outputFile)
                prepare()
                start()
            }
            recorder = rec

            _seconds.value    = 0
            _amplitudes.value = emptyList()
            timerJob = scope.launch {
                while (isActive) {
                    delay(1_000)
                    _seconds.value++
                    val amp = (recorder?.maxAmplitude ?: 0).toFloat() / 32767f
                    _amplitudes.value = (_amplitudes.value + amp).takeLast(30)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun stopRecording(): String? {
        timerJob?.cancel()
        timerJob = null
        return try {
            recorder?.apply { stop(); release() }
            recorder = null
            outputFile
        } catch (e: Exception) {
            recorder = null
            null
        }
    }

    fun pauseRecording() {
        try {
            recorder?.pause()
            timerJob?.cancel()
        } catch (_: Exception) {}
    }

    fun resumeRecording() {
        try {
            recorder?.resume()
            timerJob = scope.launch {
                while (isActive) {
                    delay(1_000)
                    _seconds.value++
                    val amp = (recorder?.maxAmplitude ?: 0).toFloat() / 32767f
                    _amplitudes.value = (_amplitudes.value + amp).takeLast(30)
                }
            }
        } catch (_: Exception) {}
    }

    fun cancelRecording() {
        timerJob?.cancel()
        timerJob = null
        try { recorder?.apply { stop(); release() } } catch (_: Exception) {}
        recorder = null
        outputFile?.let { File(it).delete() }
        outputFile        = null
        _seconds.value    = 0
        _amplitudes.value = emptyList()
    }

    fun release() {
        cancelRecording()
        scope.cancel()
    }
}
