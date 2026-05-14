package com.mirvsim.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// 品牌色彩
// ============================================
val NukeOrange = Color(0xFFFF6B35)
val NukeRed = Color(0xFFE5534B)
val NukeYellow = Color(0xFFF0A020)

// ============================================
// 深色主题色彩
// ============================================
val DarkBackground = Color(0xFF1A1D23)
val DarkSurface = Color(0xFF21252B)
val DarkSurfaceVariant = Color(0xFF282C34)
val DarkOutline = Color(0xFF3A3F4B)
val DarkOnBackground = Color(0xFFE1E4E8)
val DarkOnSurface = Color(0xFFE1E4E8)
val DarkOnSurfaceVariant = Color(0xFFA0A6B0)

// ============================================
// 浅色主题色彩
// ============================================
val LightBackground = Color(0xFFFFFBFE)
val LightSurface = Color(0xFFFFFBFE)
val LightSurfaceVariant = Color(0xFFF5F5F5)
val LightOutline = Color(0xFF79747E)
val LightOnBackground = Color(0xFF1C1B1F)
val LightOnSurface = Color(0xFF1C1B1F)
val LightOnSurfaceVariant = Color(0xFF49454F)

// ============================================
// 语义色彩 - 毁伤等级
// ============================================
val DamageFatal = Color(0xFFCC0000)
val DamageSevere = Color(0xFFFF6600)
val DamageModerate = Color(0xFFFF9900)
val DamageLight = Color(0xFFFFCC00)
val DamageNone = Color(0xFF4CAF50)

// ============================================
// 核爆效应色彩
// ============================================
val FireballColor = Color(0xFFFFD700)
val Psi20Color = Color(0xFFE53935)
val Psi10Color = Color(0xFFF4511E)
val Psi5Color = Color(0xFFFF8F00)
val Psi3Color = Color(0xFF00BCD4)
val Psi1Color = Color(0xFF7CB342)
val ThermalColor = Color(0xFFE040FB)

// ============================================
// 向后兼容别名
// ============================================
@Deprecated("使用 NukeOrange", ReplaceWith("NukeOrange"))
val Accent = NukeOrange

@Deprecated("使用 NukeRed", ReplaceWith("NukeRed"))
val Danger = NukeRed

@Deprecated("使用 NukeYellow", ReplaceWith("NukeYellow"))
val Warning = NukeYellow

@Deprecated("使用 DarkBackground", ReplaceWith("DarkBackground"))
val BgPrimary = DarkBackground

@Deprecated("使用 DarkSurface", ReplaceWith("DarkSurface"))
val BgSecondary = DarkSurface

@Deprecated("使用 DarkSurfaceVariant", ReplaceWith("DarkSurfaceVariant"))
val BgTertiary = DarkSurfaceVariant

@Deprecated("使用 DarkOutline", ReplaceWith("DarkOutline"))
val BorderColor = DarkOutline

@Deprecated("使用 DarkOnBackground", ReplaceWith("DarkOnBackground"))
val TextPrimary = DarkOnBackground

@Deprecated("使用 DarkOnSurfaceVariant", ReplaceWith("DarkOnSurfaceVariant"))
val TextSecondary = DarkOnSurfaceVariant

@Deprecated("使用 SurfaceVariant with reduced alpha", ReplaceWith("DarkSurfaceVariant.copy(alpha = 0.6f)"))
val TextMuted = DarkOnSurfaceVariant.copy(alpha = 0.7f)
