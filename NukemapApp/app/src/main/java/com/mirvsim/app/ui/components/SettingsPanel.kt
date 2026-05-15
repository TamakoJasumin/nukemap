/**
 * 设置面板
 *
 * 提供应用各项配置的修改界面，包含：
 * - 外观设置：深色/浅色主题、动态色彩
 * - 地图设置：图源切换、点击弹窗开关
 * - 模拟设置：预设自动发射、环展开动画
 * - 系统日志：查看和导出 logcat 日志
 * - 关于信息：版本号、数据来源、使用说明
 */
package com.mirvsim.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.mirvsim.app.ui.MainUiState
import com.mirvsim.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun SettingsPanel(
    state: MainUiState,
    onDarkThemeChange: (Boolean) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onTileSourceChange: (String) -> Unit,
    onPopupEnabledChange: (Boolean) -> Unit,
    onAutoLaunchChange: (Boolean) -> Unit,
    onRingAnimationChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(scrollState)
    ) {
        SettingsHeader("外观")
        ThemeSection(
            darkTheme = state.isDarkTheme,
            useDynamicColor = state.useDynamicColor,
            onDarkThemeChange = onDarkThemeChange,
            onDynamicColorChange = onDynamicColorChange
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = DarkOutline)

        SettingsHeader("地图")
        MapSettingsSection(
            tileSource = state.tileSource,
            popupEnabled = state.popupEnabled,
            onTileSourceChange = onTileSourceChange,
            onPopupEnabledChange = onPopupEnabledChange
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = DarkOutline)

        SettingsHeader("模拟")
        SimulationSettingsSection(
            autoLaunchPreset = state.autoLaunchPreset,
            ringAnimation = state.ringAnimation,
            onAutoLaunchChange = onAutoLaunchChange,
            onRingAnimationChange = onRingAnimationChange
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = DarkOutline)

        SystemLogSection()

        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = DarkOutline)

        AboutSection()

        Spacer(Modifier.height(24.dp))
    }
}

/** 设置分区标题 */
@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        color = NukeOrange,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 14.dp, top = 16.dp, bottom = 8.dp)
    )
}

/**
 * 外观设置区域
 *
 * 包含：深色/浅色主题切换、动态色彩（Android 12+）开关
 */
@Composable
private fun ThemeSection(
    darkTheme: Boolean,
    useDynamicColor: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit
) {
    val options = listOf("深色" to true, "浅色" to false)
    Text("主题模式", color = DarkOnSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(start = 14.dp))
    Spacer(Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (label, value) ->
            val selected = darkTheme == value
            Surface(onClick = { onDarkThemeChange(value) }, shape = RoundedCornerShape(6.dp),
                color = if (selected) NukeOrange else DarkSurfaceVariant, modifier = Modifier.weight(1f)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(label, fontSize = 12.sp, color = if (selected) Color.White else DarkOnSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    SettingToggle(title = "动态色彩 (Android 12+)", subtitle = "使用系统壁纸配色",
        checked = useDynamicColor, onCheckedChange = onDynamicColorChange)
}

/**
 * 地图设置区域
 *
 * 包含：图源选择（Mapnik / 卫星 / CartoDB 高清）、点击弹窗开关
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MapSettingsSection(
    tileSource: String,
    popupEnabled: Boolean,
    onTileSourceChange: (String) -> Unit,
    onPopupEnabledChange: (Boolean) -> Unit
) {
    val sources = listOf("AUTONAVI" to "高德地图", "GOOGLE_MAPS" to "Google 地图", "MAPNIK" to "Mapnik", "USGS_SAT" to "高清卫星", "CARTO_LIGHT" to "街区高清")
    Text("地图源", color = DarkOnSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(start = 14.dp))
    Spacer(Modifier.height(8.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        sources.forEach { (value, label) ->
            val selected = tileSource == value
            Surface(
                onClick = { onTileSourceChange(value) },
                shape = RoundedCornerShape(6.dp),
                color = if (selected) NukeOrange else DarkSurfaceVariant
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(label, fontSize = 11.sp, color = if (selected) Color.White else DarkOnSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    if (value == "AUTONAVI") {
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = if (selected) Color.White.copy(alpha = 0.25f) else NukeOrange.copy(alpha = 0.2f)
                        ) {
                            Text("默认", fontSize = 9.sp,
                                color = if (selected) Color.White.copy(alpha = 0.8f) else NukeOrange,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    SettingToggle(title = "地图点击弹窗", subtitle = "点击地图时显示毁伤详情",
        checked = popupEnabled, onCheckedChange = onPopupEnabledChange)
}

/**
 * 模拟行为设置
 *
 * 包含：预设自动发射、环展开动画开关
 */
@Composable
private fun SimulationSettingsSection(
    autoLaunchPreset: Boolean,
    ringAnimation: Boolean,
    onAutoLaunchChange: (Boolean) -> Unit,
    onRingAnimationChange: (Boolean) -> Unit
) {
    SettingToggle(title = "预设自动发射", subtitle = "选择预设后自动运行模拟",
        checked = autoLaunchPreset, onCheckedChange = onAutoLaunchChange)
    SettingToggle(title = "环展开动画", subtitle = "模拟完成后的环形扩散动画",
        checked = ringAnimation, onCheckedChange = onRingAnimationChange)
}

/** 通用开关设置行（标题 + 副标题 + Switch） */
@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DarkOnBackground, fontSize = 13.sp)
            Text(subtitle, color = DarkOnSurfaceVariant.copy(alpha = 0.7f), fontSize = 10.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = NukeOrange, checkedTrackColor = NukeOrange.copy(alpha = 0.4f)))
    }
}

/**
 * 系统日志区域
 *
 * 使用 logcat 命令过滤当前应用的日志，支持查看、复制和刷新。
 */
@Composable
private fun SystemLogSection() {
    var expanded by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(text = "系统日志", color = NukeOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.weight(1f))
        if (!expanded) {
            TextButton(onClick = { expanded = true; isLoading = true }) {
                Text("查看日志", fontSize = 12.sp, color = NukeOrange)
            }
        }
    }

    if (expanded) {
        if (isLoading && logs.isEmpty()) {
            LaunchedEffect(Unit) { logs = loadLogs(); isLoading = false }
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NukeOrange, strokeWidth = 2.dp)
            }
        }

        if (logs.isNotEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                shape = RoundedCornerShape(8.dp), color = DarkBackground) {
                Column {
                    Box(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                        Text(text = logs, color = DarkOnSurfaceVariant, fontSize = 10.sp, lineHeight = 14.sp,
                            modifier = Modifier.padding(10.dp))
                    }
                    HorizontalDivider(color = DarkOutline)
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { expanded = false }) { Text("关闭", fontSize = 12.sp, color = DarkOnSurfaceVariant) }
                        TextButton(onClick = {
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                .setPrimaryClip(ClipData.newPlainText("MIRV Sim Logs", logs))
                        }) {
                            Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(14.dp), tint = NukeOrange)
                            Spacer(Modifier.width(4.dp)); Text("复制", fontSize = 12.sp, color = NukeOrange)
                        }
                        TextButton(onClick = { isLoading = true; logs = "" }) {
                            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(14.dp), tint = NukeOrange)
                            Spacer(Modifier.width(4.dp)); Text("刷新", fontSize = 12.sp, color = NukeOrange)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * 后台加载 logcat 日志
 *
 * 过滤当前应用包名（com.mirvsim.app）和 FATAL 级别的日志。
 * 最多返回 200 行。
 */
private suspend fun loadLogs(): String = withContext(Dispatchers.IO) {
    try {
        val process = Runtime.getRuntime().exec("logcat -d")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()
        var line: String?
        var count = 0
        while (reader.readLine().also { line = it } != null && count < 200) {
            if (line?.contains("com.mirvsim.app") == true || line?.contains("FATAL") == true) {
                output.append(line).append("\n")
                count++
            }
        }
        if (output.isEmpty()) "没有找到相关日志" else output.toString()
    } catch (e: Exception) {
        "读取失败: ${e.message}"
    }
}

/** 关于信息区域 */
@Composable
private fun AboutSection() {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text("关于", color = NukeOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        AboutRow("版本", "1.0.0")
        AboutRow("数据源", "OpenStreetMap (osmdroid)")
        Spacer(Modifier.height(6.dp))
        Text(
            text = "基于 NUKEMAP (https://nuclearsecrecy.com/nukemap/) 设计理念开发的交互式核武器多弹头（MIRV）攻击模拟系统。",
            color = DarkOnSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = DarkOnSurfaceVariant, fontSize = 12.sp)
        Text(value, color = DarkOnBackground, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    }
}
