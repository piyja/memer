package com.piyja.memer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.piyja.memer.ui.theme.MemerTheme
import com.piyja.memer.util.AndroidContextHolder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidContextHolder.appContext = applicationContext
        enableEdgeToEdge()
        setContent {
            MemerTheme {
                MemerApp()
            }
        }
    }
}
