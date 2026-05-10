package com.everybuddy.app.ui.theme

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everybuddy.app.R

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    tint: Color? = null,
) {
    val transition = rememberInfiniteTransition(label = "loading")

    val rotation by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
        ),
        label = "rotation",
    )

    val animatedColor by transition.animateColor(
        initialValue  = Color(0xFF0167FF),
        targetValue   = Color(0xFF7B2FBE),
        animationSpec = infiniteRepeatable(
            animation  = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "color",
    )

    Image(
        painter            = painterResource(R.drawable.ic_loading),
        contentDescription = null,
        modifier           = modifier.size(size).rotate(rotation),
        colorFilter        = ColorFilter.tint(tint ?: animatedColor),
    )
}
