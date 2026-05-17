package com.everybuddy.app.data.chat

import com.everybuddy.app.data.local.KST
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/** 채팅 리스트의 timestamp 컬럼용 — KST 기준 상대 시간 텍스트. */
object RelativeTimeFormatter {

    private val TIME_FMT = DateTimeFormatter.ofPattern("a h:mm")

    fun format(epochMs: Long, now: LocalDateTime = LocalDateTime.now(KST)): String {
        if (epochMs <= 0L) return ""
        val target = Instant.ofEpochMilli(epochMs).atZone(KST).toLocalDateTime()
        val diffMin = abs(java.time.Duration.between(target, now).toMinutes())
        return when {
            diffMin < 1                                              -> "방금"
            diffMin < 60                                             -> "${diffMin}분 전"
            target.toLocalDate() == now.toLocalDate()                -> target.format(TIME_FMT)
            target.toLocalDate() == now.toLocalDate().minusDays(1)   -> "어제"
            target.year == now.year                                  -> "${target.monthValue}/${target.dayOfMonth}"
            else                                                     -> "${target.year}.${target.monthValue}.${target.dayOfMonth}"
        }
    }
}
