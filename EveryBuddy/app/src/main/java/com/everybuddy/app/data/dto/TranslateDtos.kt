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
    @SerializedName("totalSegments")    val totalSegments    : Int    = 0,
    @SerializedName("startSeconds")     val startSeconds     : Double = 0.0,
    @SerializedName("endSeconds")       val endSeconds       : Double = 0.0,
    @SerializedName("timestamp")        val timestamp        : String = "",
    @SerializedName("sourceText")       val sourceText       : String = "",
    @SerializedName("sourceLanguage")   val sourceLanguage   : String = "",
    @SerializedName("translatedText")   val translatedText   : String = "",
    @SerializedName("inferenceSeconds") val inferenceSeconds : Double = 0.0,
)

sealed class VideoStreamEvent {
    data class SegmentResult(
        val index            : Int,
        val startSeconds     : Double,
        val endSeconds       : Double,
        val timestamp        : String,
        val sourceText       : String,
        val sourceLanguage   : String,
        val translatedText   : String,
        val inferenceSeconds : Double,
    ) : VideoStreamEvent()

    data class Completed(val totalSegments: Int) : VideoStreamEvent()

    data class SegmentError(
        val index   : Int,
        val code    : Int,
        val name    : String,
        val message : String,
    ) : VideoStreamEvent()

    data class StreamError(
        val code    : Int,
        val name    : String,
        val message : String,
    ) : VideoStreamEvent()
}
