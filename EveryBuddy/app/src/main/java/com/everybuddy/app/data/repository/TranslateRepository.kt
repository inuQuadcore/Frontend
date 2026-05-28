package com.everybuddy.app.data.repository

import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.SpeechTranslateResponse
import com.everybuddy.app.data.dto.TextTranslateRequest
import com.everybuddy.app.data.dto.TextTranslateResponse
import com.everybuddy.app.data.dto.VideoStreamEvent
import com.everybuddy.app.data.dto.VideoTranslateResponse
import com.everybuddy.app.data.network.TranslateApi
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TranslateRepository @Inject constructor(
    private val api        : TranslateApi,
    private val gson       : Gson,
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
     * 각 segment 번역 완료 시 즉시 SegmentResult 이벤트를 emit한다.
     * segment_error는 해당 구간만 실패이므로 스트림이 계속 진행된다.
     * stream_error 또는 Flow 종료 시 호출자가 로딩 상태를 해제해야 한다.
     */
    fun translateVideoStream(file: File): Flow<VideoStreamEvent> = flow {
        val mimeType = when (file.extension.lowercase()) {
            "mov" -> "video/quicktime"
            else  -> "video/mp4"
        }
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull()))
            .build()
        val request = Request.Builder()
            .url("${baseUrl}api/v1/translate/video/stream")
            .post(requestBody)
            .build()

        val streamClient = okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val call = streamClient.newCall(request)
        currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }

        val response = call.execute()
        if (!response.isSuccessful) {
            val body = runCatching { response.body?.string() }.getOrNull()
            val err = runCatching {
                val obj = gson.fromJson(body, JsonObject::class.java)
                VideoStreamEvent.StreamError(
                    code    = response.code,
                    name    = obj?.get("name")?.asString ?: "HTTP_ERROR",
                    message = obj?.get("message")?.asString ?: "HTTP ${response.code}",
                )
            }.getOrElse {
                VideoStreamEvent.StreamError(response.code, "HTTP_ERROR", "HTTP ${response.code}")
            }
            emit(err)
            return@flow
        }

        val source = response.body?.source() ?: run {
            emit(VideoStreamEvent.StreamError(-1, "EMPTY_RESPONSE", "응답이 비어있습니다."))
            return@flow
        }

        var pendingDataLine: String? = null
        while (!source.exhausted()) {
            currentCoroutineContext().ensureActive()
            val line = source.readUtf8Line() ?: break
            when {
                line.startsWith("data:") -> pendingDataLine = line
                line.isEmpty() -> {
                    pendingDataLine?.let { dl ->
                        val json = dl.removePrefix("data:").trim()
                        parseStreamEvent(json)?.let { emit(it) }
                    }
                    pendingDataLine = null
                }
            }
        }
    }

    private fun parseStreamEvent(json: String): VideoStreamEvent? = try {
        val obj = gson.fromJson(json, JsonObject::class.java)
        when (obj.get("type")?.asString) {
            "segment_result" -> VideoStreamEvent.SegmentResult(
                index            = obj.get("index").asInt,
                startSeconds     = obj.get("startSeconds").asDouble,
                endSeconds       = obj.get("endSeconds").asDouble,
                timestamp        = obj.get("timestamp")?.asString ?: "",
                sourceText       = obj.get("sourceText")?.asString ?: "",
                sourceLanguage   = obj.get("sourceLanguage")?.asString ?: "",
                translatedText   = obj.get("translatedText")?.asString ?: "",
                inferenceSeconds = obj.get("inferenceSeconds")?.asDouble ?: 0.0,
            )
            "completed" -> VideoStreamEvent.Completed(
                totalSegments = obj.get("totalSegments").asInt,
            )
            "segment_error" -> VideoStreamEvent.SegmentError(
                index   = obj.get("index").asInt,
                code    = obj.get("code").asInt,
                name    = obj.get("name")?.asString ?: "",
                message = obj.get("message")?.asString ?: "",
            )
            "stream_error" -> VideoStreamEvent.StreamError(
                code    = obj.get("code").asInt,
                name    = obj.get("name")?.asString ?: "",
                message = obj.get("message")?.asString ?: "",
            )
            else -> null
        }
    } catch (_: Exception) { null }

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
