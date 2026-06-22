package com.carparking.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary           = DarkPrimary,
    onPrimary         = DarkOnPrimary,
    primaryContainer  = DarkPrimaryVar,
    onPrimaryContainer= Color(0xFFDDE1FF),
    secondary         = DarkSecondary,
    onSecondary       = Color(0xFF1A1400),
    secondaryContainer= Color(0xFF3A3000),
    onSecondaryContainer= Color(0xFFFFEE99),
    background        = DarkBackground,
    onBackground      = DarkOnBackground,
    surface           = DarkSurface,
    onSurface         = DarkOnSurface,
    surfaceVariant    = DarkCard,
    onSurfaceVariant  = Color(0xFFB0B7D4),
    outline           = Color(0xFF454B6B),
    error             = Color(0xFFCF6679)
)

private val LightColors = lightColorScheme(
    primary           = LightPrimary,
    onPrimary         = LightOnPrimary,
    primaryContainer  = Color(0xFFDDE1FF),
    onPrimaryContainer= LightPrimary,
    secondary         = LightSecondary,
    onSecondary       = LightOnPrimary,
    secondaryContainer= Color(0xFFE8EAFF),
    onSecondaryContainer= LightPrimary,
    background        = LightBackground,
    onBackground      = LightOnBackground,
    surface           = LightSurface,
    onSurface         = LightOnSurface,
    surfaceVariant    = Color(0xFFE8EAF6),
    onSurfaceVariant  = Color(0xFF46475F),
    outline           = Color(0xFFB0B7D4),
    error             = Color(0xFFB00020)
)

@Composable
fun CarParkingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else      -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
