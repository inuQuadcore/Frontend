package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

data class TextTranslateRequest(
    @SerializedName("text") val text: String,
)

data class TextTranslateResponse(
    @SerializedName("translatedText") val translatedText: String,
)

data class SpeechTranslateResponse(
    @SerializedName("sourceText")     val sourceText    : String,
    @SerializedName("translatedText") val translatedText: String,
)
