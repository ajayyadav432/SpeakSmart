package com.crispr.ai.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CrisprDarkColorScheme = darkColorScheme(
    primary           = PrimaryDark,
    onPrimary         = Color(0xFF001412),
    primaryContainer  = PrimaryContainer,
    onPrimaryContainer= OnPrimaryContainer,
    secondary         = SecondaryDark,
    onSecondary       = Color(0xFF1A1200),
    background        = SurfaceDark,
    onBackground      = OnSurfaceDark,
    surface           = SurfaceDark,
    onSurface         = OnSurfaceDark,
    surfaceVariant    = SurfaceVariantDark,
    onSurfaceVariant  = OnSurfaceVariantDark,
    error             = ErrorRed,
    onError           = Color.Black,
    outline           = OnSurfaceMutedDark,
)

@Composable
fun SpeakSmartTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SurfaceDark.toArgb()
            window.navigationBarColor = SurfaceNavDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = CrisprDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
