package com.everybuddy.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight(400),
        fontSize   = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight(400),
        fontSize   = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight(400),
        fontSize   = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight(500),
        fontSize   = 11.sp,
        lineHeight = 16.sp,
    ),
)
