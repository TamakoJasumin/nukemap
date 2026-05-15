/**
 * 主屏幕预览定义
 *
 * 提供多种设备形态和主题组合的预览配置，用于 Android Studio 预览面板：
 * - 紧凑布局（手机竖屏）：深色/浅色主题
 * - 展开布局（平板横屏）：深色主题
 * - 折叠屏（Foldable）：深色主题
 * - 动态色彩（Android 12+）：深色/浅色主题
 *
 * 预览分组组织便于在 Preview 面板中筛选。
 */
package com.mirvsim.app.ui.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.mirvsim.app.ui.MainScreen
import com.mirvsim.app.ui.theme.NukemapTheme

/**
 * 手机竖屏紧凑布局预览
 */
@Preview(
    name = "紧凑布局 - 深色主题",
    group = "MainScreen",
    showSystemUi = true,
    device = Devices.PHONE,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun MainScreenCompactDarkPreview() {
    NukemapTheme(darkTheme = true) { MainScreen() }
}

@Preview(
    name = "紧凑布局 - 浅色主题",
    group = "MainScreen",
    showSystemUi = true,
    device = Devices.PHONE,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun MainScreenCompactLightPreview() {
    NukemapTheme(darkTheme = false) { MainScreen() }
}

/**
 * 平板展开布局预览（横屏模式）
 */
@Preview(
    name = "展开布局 - 深色主题",
    group = "MainScreen",
    showSystemUi = true,
    device = Devices.TABLET,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun MainScreenExpandedDarkPreview() {
    NukemapTheme(darkTheme = true) { MainScreen() }
}

/**
 * 折叠屏设备预览
 */
@Preview(
    name = "折叠屏 - 深色主题",
    group = "MainScreen",
    showSystemUi = true,
    device = Devices.FOLDABLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun MainScreenFoldableDarkPreview() {
    NukemapTheme(darkTheme = true) { MainScreen() }
}

/**
 * Android 12+ 动态色彩预览
 */
@Preview(
    name = "动态色彩 - 深色",
    group = "DynamicColor",
    showSystemUi = true,
    device = Devices.PHONE,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DynamicColorDarkPreview() {
    NukemapTheme(darkTheme = true, dynamicColor = true) { MainScreen() }
}

@Preview(
    name = "动态色彩 - 浅色",
    group = "DynamicColor",
    showSystemUi = true,
    device = Devices.PHONE,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun DynamicColorLightPreview() {
    NukemapTheme(darkTheme = false, dynamicColor = true) { MainScreen() }
}
