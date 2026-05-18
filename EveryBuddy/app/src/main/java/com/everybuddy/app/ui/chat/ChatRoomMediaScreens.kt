package com.everybuddy.app.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.everybuddy.app.R
import com.everybuddy.app.ui.theme.PretendardFamily

private val MedAccent  = Color(0xFF0167FF)
private val MedTextPri = Color(0xFF1B1B1B)
private val MedTextSec = Color(0xFF8F9399)
private val MedBorder  = Color(0xFFE5E5E5)

@Composable
fun MediaPhotoScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.White)
                        .padding(horizontal = 8.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(40.dp)) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = MedTextPri)
                    }
                    Text(
                        "사진 및 동영상",
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = MedTextPri),
                    )
                }
                HorizontalDivider(color = MedBorder, thickness = 0.5.dp)
            }
        },
        contentWindowInsets = WindowInsets(0),
        containerColor = Color.White,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "저장된 사진 및 동영상이 없습니다.",
                style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = MedTextSec),
            )
        }
    }
}

@Composable
fun MediaLinkScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.White)
                        .padding(horizontal = 8.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(40.dp)) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = MedTextPri)
                    }
                    Text(
                        "링크",
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = MedTextPri),
                    )
                }
                HorizontalDivider(color = MedBorder, thickness = 0.5.dp)
            }
        },
        contentWindowInsets = WindowInsets(0),
        containerColor = Color.White,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "저장된 링크가 없습니다.",
                style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = MedTextSec),
            )
        }
    }
}

@Composable
fun MediaFileScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.White)
                        .padding(horizontal = 8.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(40.dp)) {
                        Icon(painterResource(R.drawable.ic_back), "뒤로", Modifier.size(24.dp), tint = MedTextPri)
                    }
                    Text(
                        "파일",
                        modifier = Modifier.align(Alignment.Center),
                        style    = TextStyle(fontSize = 18.sp, fontFamily = PretendardFamily, fontWeight = FontWeight(700), color = MedTextPri),
                    )
                }
                HorizontalDivider(color = MedBorder, thickness = 0.5.dp)
            }
        },
        contentWindowInsets = WindowInsets(0),
        containerColor = Color.White,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "저장된 파일이 없습니다.",
                style = TextStyle(fontSize = 14.sp, fontFamily = PretendardFamily, color = MedTextSec),
            )
        }
    }
}
