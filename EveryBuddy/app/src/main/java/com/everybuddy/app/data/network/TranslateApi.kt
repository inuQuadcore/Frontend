package com.everybuddy.app.data.network

import com.everybuddy.app.data.dto.SpeechTranslateResponse
import com.everybuddy.app.data.dto.TextTranslateRequest
import com.everybuddy.app.data.dto.TextTranslateResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface TranslateApi {

    /**
     * POST /api/v1/translate/text — 텍스트 번역
     * 200: { translatedText } | 400: INVALID_INPUT_VALUE / UNSUPPORTED_LANGUAGE
     * 502: MODEL_ERROR / MODEL_UNAVAILABLE | 504: MODEL_TIMEOUT
     */
    @POST("api/v1/translate/text")
    suspend fun translateText(
        @Body body: TextTranslateRequest,
    ): Response<TextTranslateResponse>

    /**
     * POST /api/v1/translate/speech — 음성 STT + 번역 (multipart)
     * 200: { sourceText, translatedText }
     * 400: INVALID_AUDIO_FORMAT / EMPTY_FILE / UNSUPPORTED_LANGUAGE
     * 413: AUDIO_FILE_TOO_LARGE (50MB 초과)
     * 502/504: 모델 오류/타임아웃
     */
    @Multipart
    @POST("api/v1/translate/speech")
    suspend fun translateSpeech(
        @Part file: MultipartBody.Part,
    ): Response<SpeechTranslateResponse>
}
