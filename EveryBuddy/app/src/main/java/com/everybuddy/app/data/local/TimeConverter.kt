package com.everybuddy.app.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 서버 시각 기준 timezone. REST의 LocalDateTime은 KST로 간주. */
val KST: ZoneId = ZoneId.of("Asia/Seoul")

private val ISO_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

/** Room이 LocalDateTime을 String으로 직렬화/역직렬화. */
class LocalDateTimeConverter {
    @TypeConverter
    fun fromString(value: String?): LocalDateTime? =
        value?.let { LocalDateTime.parse(it, ISO_FMT) }

    @TypeConverter
    fun toIsoString(date: LocalDateTime?): String? =
        date?.format(ISO_FMT)
}

/** REST 응답의 ISO 8601 LocalDateTime → LocalDateTime (서버 시각 KST 가정). */
fun parseRestLocalDateTime(value: String): LocalDateTime =
    LocalDateTime.parse(value, ISO_FMT)

/** LocalDateTime(KST) → REST 요청용 ISO 8601 문자열. */
fun formatRestLocalDateTime(value: LocalDateTime): String =
    value.format(ISO_FMT)

/** RTDB epoch ms → KST LocalDateTime. */
fun epochMsToKstLocalDateTime(epochMs: Long): LocalDateTime =
    Instant.ofEpochMilli(epochMs).atZone(KST).toLocalDateTime()

/** KST LocalDateTime → epoch ms (RTDB query startAt 등에 사용). */
fun kstLocalDateTimeToEpochMs(value: LocalDateTime): Long =
    value.atZone(KST).toInstant().toEpochMilli()
