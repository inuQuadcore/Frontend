package com.everybuddy.app.data.repository

import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.SpeechTranslateResponse
import com.everybuddy.app.data.dto.TextTranslateRequest
import com.everybuddy.app.data.dto.TextTranslateResponse
import com.everybuddy.app.data.network.TranslateApi
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslateRepository @Inject constructor(
    private val api          : TranslateApi,
    private val gson         : Gson,
    private val okHttpClient : OkHttpClient,
) {
    suspend fun translateText(text: String): ApiResult<TextTranslateResponse> =
        safeApiCall(gson, { api.translateText(TextTranslateRequest(text)) })

    /**
     * 음성 번역 — voiceUrl에서 다운로드받은 File을 multipart로 업로드.
     * 호출자가 다운로드 책임 (다양한 캐싱/임시 파일 전략 가능).
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
     * 음성 번역 — voiceUrl에서 다운로드 후 multipart 업로드.
     * 명세상 file 필수이므로 클라가 원격 음성 파일을 받아서 다시 보내는 구조.
     */
    suspend fun translateSpeechFromUrl(url: String): ApiResult<SpeechTranslateResponse> {
        val bytes = downloadBytes(url)
            ?: return ApiResult.Error(-1, "VOICE_DOWNLOAD_FAILED", "음성 파일을 받지 못했습니다.")
        val filename = url.substringAfterLast('/').substringBefore('?').ifBlank { "audio.m4a" }
        return translateSpeech(bytes, filename)
    }

    private suspend fun downloadBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { res ->
                if (res.isSuccessful) res.body?.bytes() else null
            }
        } catch (e: Exception) { null }
    }

    /**
     * 음성 번역 — voiceUrl 다운로드 없이 ByteArray 직접 업로드 (메모리 캐시 활용 케이스).
     */
    suspend fun translateSpeech(bytes: ByteArray, filename: String): ApiResult<SpeechTranslateResponse> {
        val mediaType = guessAudioMediaType(filename.substringAfterLast('.', missingDelimiterValue = ""))
        val part = MultipartBody.Part.createFormData(
            name     = "file",
            filename = filename,
            body     = bytes.toRequestBody(mediaType),
        )
        return safeApiCall(gson, { api.translateSpeech(part) })
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
