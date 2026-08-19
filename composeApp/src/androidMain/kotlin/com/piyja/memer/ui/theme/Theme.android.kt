package com.piyja.memer.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun isDynamicColorAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
actual fun dynamicDarkColorScheme(): ColorScheme {
    val context = LocalContext.current
    return dynamicDarkColorScheme(context)
}

@Composable
actual fun dynamicLightColorScheme(): ColorScheme {
    val context = LocalContext.current
    return dynamicLightColorScheme(context)
}
