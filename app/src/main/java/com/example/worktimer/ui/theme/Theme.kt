package com.example.worktimer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6EA3FF),
    onPrimary = Color(0xFF081B34),
    background = Color(0xFF0F1117),
    onBackground = Color(0xFFF4F7FB),
    surface = Color(0xFF171A22),
    onSurface = Color(0xFFF4F7FB),
    surfaceVariant = Color(0xFF242B36),
    onSurfaceVariant = Color(0xFFA5ADBA),
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2962FF),
    onPrimary = Color.White,
    background = Color(0xFFF4F6FA),
    onBackground = Color(0xFF1A1D26),
    surface = Color.White,
    onSurface = Color(0xFF1A1D26),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF6B7280),
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun WorkTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
