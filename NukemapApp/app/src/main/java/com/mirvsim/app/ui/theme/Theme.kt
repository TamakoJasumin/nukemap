package com.mirvsim.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 深色主题配色方案
 */
private val DarkColorScheme = darkColorScheme(
    primary = NukeOrange,
    onPrimary = Color.White,
    primaryContainer = NukeOrange.copy(alpha = 0.2f),
    onPrimaryContainer = NukeOrange,
    
    secondary = NukeRed,
    onSecondary = Color.White,
    secondaryContainer = NukeRed.copy(alpha = 0.2f),
    onSecondaryContainer = NukeRed,
    
    tertiary = NukeYellow,
    onTertiary = Color.Black,
    tertiaryContainer = NukeYellow.copy(alpha = 0.2f),
    onTertiaryContainer = NukeYellow,
    
    error = DamageFatal,
    onError = Color.White,
    errorContainer = DamageFatal.copy(alpha = 0.2f),
    onErrorContainer = DamageFatal,
    
    background = DarkBackground,
    onBackground = DarkOnBackground,
    
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    
    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.5f),
    
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    inversePrimary = NukeOrange
)

/**
 * 浅色主题配色方案
 */
private val LightColorScheme = lightColorScheme(
    primary = NukeOrange,
    onPrimary = Color.White,
    primaryContainer = NukeOrange.copy(alpha = 0.1f),
    onPrimaryContainer = NukeOrange,
    
    secondary = NukeRed,
    onSecondary = Color.White,
    secondaryContainer = NukeRed.copy(alpha = 0.1f),
    onSecondaryContainer = NukeRed,
    
    tertiary = NukeYellow,
    onTertiary = Color.Black,
    tertiaryContainer = NukeYellow.copy(alpha = 0.1f),
    onTertiaryContainer = NukeYellow,
    
    error = DamageFatal,
    onError = Color.White,
    errorContainer = DamageFatal.copy(alpha = 0.1f),
    onErrorContainer = DamageFatal,
    
    background = LightBackground,
    onBackground = LightOnBackground,
    
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.5f)
)

/**
 * Nukemap 应用主题
 * 
 * @param darkTheme 是否使用深色主题，默认跟随系统设置
 * @param dynamicColor 是否使用动态色彩（Android 12+），默认开启
 */
@Composable
fun NukemapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Android 12+ 支持动态色彩
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
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
        typography = Typography,
        content = content
    )
}
