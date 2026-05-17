package com.everybuddy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.everybuddy.app.navigation.AppNavGraph
import com.everybuddy.app.navigation.Route
import com.everybuddy.app.ui.theme.EveryBuddyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EveryBuddyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(
                        startDest = Route.SPLASH,
                    )
                }
            }
        }
    }
}