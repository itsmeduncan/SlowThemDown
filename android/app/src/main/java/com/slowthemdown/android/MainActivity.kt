package com.slowthemdown.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.slowthemdown.android.ui.SlowThemDownApp
import com.slowthemdown.android.ui.theme.SlowThemDownTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SlowThemDownTheme {
                SlowThemDownApp()
            }
        }
    }
}
