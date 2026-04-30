package com.everybuddy.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary        = EBBlue,
    onPrimary      = EBWhite,
    background     = EBWhite,
    onBackground   = EBBlack,
    surface        = EBWhite,
    onSurface      = EBBlack,
    error          = EBError,
)

@Composable
fun EveryBuddyTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content,
    )
}
