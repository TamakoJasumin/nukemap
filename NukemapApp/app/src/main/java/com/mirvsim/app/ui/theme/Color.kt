/**
 * 应用色彩系统定义
 *
 * 集中管理所有颜色常量，采用分层色彩体系：
 * 1. 品牌色彩（Brand Colors）：应用主色调和辅助色
 * 2. 深色主题色彩（Dark Theme）：深色模式的颜色方案
 * 3. 浅色主题色彩（Light Theme）：浅色模式的颜色方案
 * 4. 语义色彩（Semantic Colors）：毁伤等级指示色
 * 5. 核爆效应色彩（Effect Colors）：各毁伤环的专用颜色
 *
 * 注意：后向兼容别名标记为 @Deprecated，建议使用标准命名。
 */
package com.mirvsim.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// 品牌色彩 — 应用主色调/辅助色
// ============================================

/** 品牌主色 — 核弹橙，用于强调和主操作按钮 */
val NukeOrange = Color(0xFFFF6B35)
/** 品牌辅助色 — 核弹红，用于错误和危险状态 */
val NukeRed = Color(0xFFE5534B)
/** 品牌辅助色 — 核弹黄，用于警告状态 */
val NukeYellow = Color(0xFFF0A020)

// ============================================
// 深色主题色彩 — 默认暗色配色方案
// ============================================

val DarkBackground = Color(0xFF1A1D23)             // 背景
val DarkSurface = Color(0xFF21252B)                 // 表面/卡片
val DarkSurfaceVariant = Color(0xFF282C34)           // 表面变体（强调背景）
val DarkOutline = Color(0xFF3A3F4B)                 // 边框/分割线
val DarkOnBackground = Color(0xFFE1E4E8)            // 背景上文字
val DarkOnSurface = Color(0xFFE1E4E8)               // 表面上文字
val DarkOnSurfaceVariant = Color(0xFFA0A6B0)        // 表面变体上文字（次要）

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
// 语义色彩 — 毁伤等级标识色
// ============================================

val DamageFatal = Color(0xFFCC0000)     // 致命毁伤（红色）
val DamageSevere = Color(0xFFFF6600)    // 重度毁伤（橙色）
val DamageModerate = Color(0xFFFF9900)  // 中度毁伤（深黄）
val DamageLight = Color(0xFFFFCC00)     // 轻度毁伤（黄色）
val DamageNone = Color(0xFF4CAF50)      // 无毁伤（绿色）

// ============================================
// 核爆效应色彩 — 各毁伤环专用色
// ============================================

val FireballColor = Color(0xFFFFD700)   // 火球 — 金色
val Psi20Color = Color(0xFFE53935)      // 20 psi — 红色
val Psi10Color = Color(0xFFF4511E)      // 10 psi — 橙色
val Psi5Color = Color(0xFFFF8F00)       // 5 psi — 深黄
val Psi3Color = Color(0xFF00BCD4)       // 3 psi — 青色
val Psi1Color = Color(0xFF7CB342)       // 1 psi — 绿色
val ThermalColor = Color(0xFFE040FB)    // 热辐射 — 紫色

// ============================================
// 后向兼容别名（保留旧代码中的引用）
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
