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

data class VideoTranslateResponse(
    @SerializedName("translatedText") val translatedText: String,
    @SerializedName("segments")       val segments      : List<VideoSegment> = emptyList(),
)

data class VideoSegment(
    @SerializedName("index")            val index            : Int    = 0,
    @SerializedName("startSeconds")     val startSeconds     : Double = 0.0,
    @SerializedName("endSeconds")       val endSeconds       : Double = 0.0,
    @SerializedName("timestamp")        val timestamp        : String = "",
    @SerializedName("sourceText")       val sourceText       : String = "",
    @SerializedName("sourceLanguage")   val sourceLanguage   : String = "",
    @SerializedName("translatedText")   val translatedText   : String = "",
    @SerializedName("inferenceSeconds") val inferenceSeconds : Double = 0.0,
)

data class VideoStreamSegment(
    @SerializedName("index")             val index            : Int     = 0,
    @SerializedName("total_segments")    val totalSegments    : Int     = 0,
    @SerializedName("start_seconds")     val startSeconds     : Double  = 0.0,
    @SerializedName("end_seconds")       val endSeconds       : Double  = 0.0,
    @SerializedName("source_text")       val sourceText       : String  = "",
    @SerializedName("translated_text")   val translatedText   : String  = "",
    @SerializedName("is_final")          val isFinal          : Boolean = false,
    @SerializedName("error")             val error            : String? = null,
)
