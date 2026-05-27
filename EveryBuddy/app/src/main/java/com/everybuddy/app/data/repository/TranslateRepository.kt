package com.everybuddy.app.data.repository

import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.SpeechTranslateResponse
import com.everybuddy.app.data.dto.TextTranslateRequest
import com.everybuddy.app.data.dto.TextTranslateResponse
import com.everybuddy.app.data.dto.VideoStreamSegment
import com.everybuddy.app.data.dto.VideoTranslateResponse
import com.everybuddy.app.data.network.TranslateApi
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TranslateRepository @Inject constructor(
    private val api          : TranslateApi,
    private val gson         : Gson,
    @Named("translate") private val okHttpClient : OkHttpClient,
    @Named("base_url")  private val baseUrl      : String,
) {
    suspend fun translateText(text: String): ApiResult<TextTranslateResponse> =
        safeApiCall(gson, { api.translateText(TextTranslateRequest(text)) })

    suspend fun tts(text: String): ApiResult<ByteArray> = try {
        val response = api.tts(TextTranslateRequest(text))
        if (response.isSuccessful) {
            val bytes = response.body()?.bytes()
            if (bytes != null) ApiResult.Success(bytes)
            else ApiResult.Error(200, "EMPTY_RESPONSE", "빈 응답")
        } else {
            ApiResult.Error(response.code(), "TTS_ERROR", "TTS 변환 실패")
        }
    } catch (e: Exception) {
        ApiResult.NetworkError(e)
    }

    /**
     * 음성 번역 — 로컬 File을 multipart로 업로드.
     * 호출자가 파일 준비 책임 (MediaFileStore로 영속/다운로드 캐시).
     */
    suspend fun translateSpeech(audio: File): ApiResult<SpeechTranslateResponse> {
        val mediaType = guessAudioMediaType(audio.extension)
        val part = MultipartBody.Part.createFormData(
            name     = "file",
            filename = audio.name,
            body     = audio.asRequestBody(mediaType),
        )
        return safeApiCall(gson, { api.translateSpeech(part) })
    }

    /**
     * 영상 번역 — POST /api/v1/translate/video (multipart)
     * VAD로 음성 구간 감지 → 자동 언어 감지 → 사용자 주 언어로 번역.
     * 반환: segments 배열 (startSeconds/endSeconds/sourceText/translatedText)
     */
    suspend fun translateVideo(file: File): ApiResult<VideoTranslateResponse> {
        val mimeType = when (file.extension.lowercase()) {
            "mov" -> "video/quicktime"
            else  -> "video/mp4"
        }.toMediaTypeOrNull()
        val part = MultipartBody.Part.createFormData(
            name     = "file",
            filename = file.name,
            body     = file.asRequestBody(mimeType),
        )
        return safeApiCall(gson, { api.translateVideo(part) })
    }

    /**
     * 영상 번역 스트리밍 — POST /api/v1/translate/video/stream (SSE)
     * 구간별로 VideoStreamSegment를 emit. error 필드가 있거나 isFinal=true이면 스트림 종료.
     * 네트워크 오류·5xx 시 최대 MAX_STREAM_RETRIES회 재시도 (지수 백오프).
     * 재시도마다 Request를 새로 빌드하므로 JWT 인터셉터가 Authorization 헤더를 매번 주입.
     */
    fun translateVideoStream(file: File): Flow<VideoStreamSegment> = flow {
        val mimeType = when (file.extension.lowercase()) {
            "mov" -> "video/quicktime"
            else  -> "video/mp4"
        }
        repeat(MAX_STREAM_RETRIES + 1) { attempt ->
            val requestBody = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody(mimeType.toMediaType()))
                .build()
            // Request를 매번 새로 빌드 → JWT 인터셉터가 최신 토큰으로 Authorization 헤더 주입
            val request = Request.Builder()
                .url("${baseUrl}api/v1/translate/video/stream")
                .post(requestBody)
                .addHeader("Accept", "text/event-stream")
                .build()
            val call = okHttpClient.newCall(request)
            try {
                val response = call.execute()
                if (!response.isSuccessful) {
                    response.body?.close()
                    val canRetry = response.code in 500..599 && attempt < MAX_STREAM_RETRIES
                    if (!canRetry) {
                        emit(VideoStreamSegment(error = "HTTP ${response.code}", isFinal = true))
                        return@flow
                    }
                } else {
                    val source = response.body?.source() ?: run {
                        emit(VideoStreamSegment(error = "빈 응답", isFinal = true))
                        return@flow
                    }
                    try {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (!line.startsWith("data:")) continue
                            val json = line.removePrefix("data:").trim()
                            if (json.isEmpty()) continue
                            val segment = try {
                                gson.fromJson(json, VideoStreamSegment::class.java)
                            } catch (_: Exception) { continue }
                            emit(segment)
                            if (segment.isFinal) return@flow
                        }
                    } finally {
                        response.body?.close()
                    }
                    return@flow
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                call.cancel()
                throw e
            } catch (e: java.io.IOException) {
                if (attempt >= MAX_STREAM_RETRIES) {
                    emit(VideoStreamSegment(error = e.message ?: "연결 오류", isFinal = true))
                    return@flow
                }
            } catch (e: Exception) {
                emit(VideoStreamSegment(error = e.message ?: "연결 오류", isFinal = true))
                return@flow
            }
            delay(1000L shl attempt) // 1s, 2s
        }
    }

    companion object {
        private const val MAX_STREAM_RETRIES = 2
    }

    private fun guessAudioMediaType(extension: String) =
        when (extension.lowercase()) {
            "mp3"  -> "audio/mpeg"
            "wav"  -> "audio/wav"
            "m4a"  -> "audio/mp4"
            "aac"  -> "audio/aac"
            "ogg"  -> "audio/ogg"
            else   -> "application/octet-stream"
        }.toMediaTypeOrNull()
}

/**
 * Translate 도메인 에러 → 사용자 친화 토스트 메시지.
 * 명세 권장 메시지 반영 (502/504, UNSUPPORTED_LANGUAGE, 413 등).
 */
fun ApiResult<*>.videoTranslateUserMessage(): String = when (this) {
    is ApiResult.Success      -> ""
    is ApiResult.NetworkError -> "네트워크 연결을 확인해주세요."
    is ApiResult.Error        -> when {
        name == "INVALID_VIDEO_FORMAT" -> "지원하지 않는 영상 형식입니다. (mp4, mov)"
        name == "EMPTY_FILE"           -> "영상 파일이 비어 있습니다."
        code == 403                    -> "서버에서 요청을 거부했습니다. (파일 크기 제한 초과)"
        code == 413                    -> "영상 파일이 너무 큽니다. (최대 50MB)"
        code == 500                    -> "영상 변환 중 오류가 발생했습니다."
        code == 502 || code == 504     -> "번역 서버 응답이 늦어요. 잠시 후 다시 시도해주세요."
        else                           -> message ?: "번역에 실패했습니다."
    }
}

fun ApiResult<*>.translateUserMessage(): String? = when (this) {
    is ApiResult.Success      -> null
    is ApiResult.NetworkError -> "네트워크 연결을 확인해주세요."
    is ApiResult.Error        -> when {
        code == 502 || code == 504        -> "번역 서버 응답이 늦어요. 잠시 후 다시 시도해주세요."
        name == "UNSUPPORTED_LANGUAGE"    -> "지원하지 않는 언어입니다."
        name == "INVALID_AUDIO_FORMAT"    -> "지원하지 않는 음성 형식입니다."
        name == "EMPTY_FILE"              -> "음성 파일이 비어 있습니다."
        code == 413                       -> "음성 파일이 너무 큽니다 (최대 50MB)."
        else                              -> message
    }
}
