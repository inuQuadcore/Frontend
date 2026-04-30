package com.everybuddy.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.everybuddy.app.navigation.AppNavGraph
import com.everybuddy.app.ui.theme.EveryBuddyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DEBUG", "onCreate started")
        enableEdgeToEdge()
        try {
            setContent {
                Log.d("DEBUG", "setContent started")
                EveryBuddyTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavGraph()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DEBUG", "Crash in setContent", e)
        }
    }
}