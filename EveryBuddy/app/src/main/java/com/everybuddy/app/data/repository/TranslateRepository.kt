package com.everybuddy.app.data.repository

import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.dto.SpeechTranslateResponse
import com.everybuddy.app.data.dto.TextTranslateRequest
import com.everybuddy.app.data.dto.TextTranslateResponse
import com.everybuddy.app.data.network.TranslateApi
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslateRepository @Inject constructor(
    private val api  : TranslateApi,
    private val gson : Gson,
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
