@file:Suppress("DEPRECATION")

package com.everybuddy.app.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuddy.app.ui.theme.LoadingIndicator
import com.everybuddy.app.ui.theme.PretendardFamily

@Composable
fun SplashScreen(
    onNavigate : (String) -> Unit,
    viewModel  : SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsState()

    LaunchedEffect(destination) {
        destination?.let { onNavigate(it) }
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text  = "EveryBuddy",
                style = TextStyle(
                    fontSize   = 28.sp,
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF0167FF),
                ),
            )
            LoadingIndicator()
        }
    }
}
