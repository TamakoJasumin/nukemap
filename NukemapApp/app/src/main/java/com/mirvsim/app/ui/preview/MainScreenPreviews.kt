package com.mirvsim.app.ui.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.mirvsim.app.ui.MainScreen
import com.mirvsim.app.ui.theme.NukemapTheme

/**
 * 主屏幕预览 - 紧凑布局
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
    NukemapTheme(darkTheme = true) {
        MainScreen()
    }
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
    NukemapTheme(darkTheme = false) {
        MainScreen()
    }
}

/**
 * 主屏幕预览 - 平板展开布局
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
    NukemapTheme(darkTheme = true) {
        MainScreen()
    }
}

/**
 * 主屏幕预览 - 折叠屏
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
    NukemapTheme(darkTheme = true) {
        MainScreen()
    }
}

/**
 * 动态色彩预览
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
    NukemapTheme(darkTheme = true, dynamicColor = true) {
        MainScreen()
    }
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
    NukemapTheme(darkTheme = false, dynamicColor = true) {
        MainScreen()
    }
}
