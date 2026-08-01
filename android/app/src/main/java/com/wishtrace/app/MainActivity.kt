package com.wishtrace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wishtrace.app.ui.WishTraceApp
import com.wishtrace.app.ui.theme.WishTraceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = 0xFFF9FAFF.toInt(),
                darkScrim = 0xFFF9FAFF.toInt(),
            ),
        )
        super.onCreate(savedInstanceState)
        setContent {
            WishTraceTheme {
                WishTraceApp()
            }
        }
    }
}
