package com.trickcal.crayon.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.trickcal.crayon.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = CrayonCoral,
    onPrimary = CrayonPaper,
    secondary = CrayonMint,
    onSecondary = CrayonPaper,
    tertiary = CrayonLavender,
    background = CrayonCream,
    onBackground = CrayonNavy,
    surface = CrayonPaper,
    onSurface = CrayonNavy,
    surfaceVariant = ColorTokens.surfaceVariant,
    onSurfaceVariant = CrayonGray,
    outline = ColorTokens.outline,
)

private val DarkColors = darkColorScheme(
    primary = CrayonGold,
    onPrimary = Color(0xFF31220A),
    secondary = CrayonMint,
    onSecondary = Color(0xFF132119),
    tertiary = CrayonRose,
    background = Color(0xFF14161A),
    onBackground = Color(0xFFF7F1E7),
    surface = Color(0xFF1B1E24),
    onSurface = Color(0xFFF7F1E7),
    surfaceVariant = Color(0xFF242831),
    onSurfaceVariant = Color(0xFFC6C0B6),
    outline = Color(0xFF666C78),
)

private object ColorTokens {
    val surfaceVariant = CrayonPaper.copy(alpha = 0.92f)
    val outline = CrayonGray.copy(alpha = 0.35f)
}

@Composable
fun TrickcalCrayonTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (themeMode == ThemeMode.DARK) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val background = colorScheme.background.toArgb()
            window.setBackgroundDrawable(ColorDrawable(background))
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = themeMode == ThemeMode.LIGHT
                isAppearanceLightNavigationBars = themeMode == ThemeMode.LIGHT
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}
