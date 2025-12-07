package com.devil.phoenixproject.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.devil.phoenixproject.ui.theme.ThemeMode as SharedThemeMode
import com.devil.phoenixproject.ui.theme.VitruvianTheme as SharedVitruvianTheme

/**
 * Android-specific theme wrapper.
 * Delegates to shared theme and adds platform-specific status bar coloring.
 */
@Composable
fun VitruvianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled - breaks brand identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val themeMode = if (darkTheme) SharedThemeMode.DARK else SharedThemeMode.LIGHT

    SharedVitruvianTheme(themeMode = themeMode) {
        val colorScheme = MaterialTheme.colorScheme
        val view = LocalView.current

        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                window.statusBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }

        content()
    }
}
