package com.wishtrace.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishtrace.app.ui.WishTraceApp
import com.wishtrace.app.ui.theme.WishTraceTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val pravaReturnUri = MutableStateFlow<String?>(null)

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
        pravaReturnUri.value = intent?.data?.toString()
        setContent {
            val returnUri by pravaReturnUri.collectAsStateWithLifecycle()
            WishTraceTheme {
                WishTraceApp(
                    pravaReturnUri = returnUri,
                    onPravaReturnConsumed = { pravaReturnUri.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pravaReturnUri.value = intent.data?.toString()
    }
}
