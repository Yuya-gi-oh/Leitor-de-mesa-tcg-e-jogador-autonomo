package com.yuyagioh.tcgreader.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2E7D32),
    secondary = androidx.compose.ui.graphics.Color(0xFF00695C),
    tertiary = androidx.compose.ui.graphics.Color(0xFF6A1B9A),
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF81C784),
    secondary = androidx.compose.ui.graphics.Color(0xFF4DB6AC),
    tertiary = androidx.compose.ui.graphics.Color(0xFFCE93D8),
)

@Composable
fun TcgReaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
