package com.piyja.memer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.piyja.memer.data.MemeTemplate
import com.piyja.memer.ui.screen.MemeEditorScreen
import com.piyja.memer.ui.screen.TemplatePickerScreen
import com.piyja.memer.ui.theme.MemerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MemerTheme {
                MemerApp()
            }
        }
    }
}

private sealed interface Screen {
    data object Picker : Screen
    data class Editor(val template: MemeTemplate) : Screen
}

@Composable
private fun MemerApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Picker) }

    when (val current = screen) {
        is Screen.Picker -> TemplatePickerScreen(
            onTemplateSelected = { template ->
                screen = Screen.Editor(template)
            },
            modifier = Modifier.fillMaxSize()
        )
        is Screen.Editor -> MemeEditorScreen(
            template = current.template,
            onBack = { screen = Screen.Picker },
            modifier = Modifier.fillMaxSize()
        )
    }
}