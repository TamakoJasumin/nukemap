/**
 * 应用主题系统
 *
 * 基于 Material 3（Material You）设计系统，支持：
 * - 深色/浅色主题切换
 * - Android 12+ 动态色彩（Dynamic Color）
 * - 自定义配色方案（当动态色彩不可用或禁用时）
 *
 * 主题优先级：
 * 1. 动态色彩 + 深色（Android 12+ 且开启）
 * 2. 自定义深色配色
 * 3. 动态色彩 + 浅色
 * 4. 自定义浅色配色
 *
 * 状态栏图标颜色自动跟随主题模式切换。
 */
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
 * 深色主题自定义配色方案
 *
 * 符合 Material 3 色彩规范的自定义深色配色，
 * 使用品牌色 NukeOrange 作为 Primary 色调。
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
 * 浅色主题自定义配色方案
 *
 * 使用品牌色 NukeOrange，透明度调整适用于浅色背景。
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
 * Nukemap 应用主题入口
 *
 * @param darkTheme 是否使用深色主题（默认跟随系统设置）
 * @param dynamicColor 是否使用 Android 12+ 动态色彩（默认开启）
 * @param content 子 Composable 内容
 */
@Composable
fun NukemapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Android 12+ 系统支持动态色彩且用户开启时优先使用
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // 设置状态栏图标颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
