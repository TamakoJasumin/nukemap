/**
 * 控制面板 — 参数配置、预设选择、发射操作
 *
 * 包含三个主要功能区域：
 * 1. 预设场景选择：快速加载各国导弹配置
 * 2. 弹头参数配置：数量、当量、散布模式、爆高模式
 * 3. 目标定位：坐标输入、城市搜索、地图点选
 *
 * 提供三种面板变体：
 * - ControlPanel: 带折叠区（适用于手机端）
 * - SimulationPanel: 全部展开（适用于平板端）
 * - PresetsPanel: 仅显示预设列表
 */
package com.mirvsim.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mirvsim.app.data.Presets
import com.mirvsim.app.model.City
import com.mirvsim.app.model.Preset
import com.mirvsim.app.ui.theme.*

// ====================================================================
// ControlPanel — 完整控制面板（带折叠区）
// ====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlPanel(
    warheadCount: Int,
    yieldKt: Double,
    separationKm: Double,
    pattern: String,
    hobMode: String,
    targetLat: Double,
    targetLng: Double,
    targetType: String,
    activePresetId: String?,
    cityList: List<City>,
    isComputing: Boolean,
    onWarheadCountChange: (Int) -> Unit,
    onYieldChange: (Double) -> Unit,
    onSeparationChange: (Double) -> Unit,
    onPatternChange: (String) -> Unit,
    onHobModeChange: (String) -> Unit,
    onTargetLatChange: (Double) -> Unit,
    onTargetLngChange: (Double) -> Unit,
    onTargetTypeChange: (String) -> Unit,
    onCitySelect: (City) -> Unit,
    onPresetApply: (Preset) -> Unit,
    onPickOnMap: () -> Unit,
    onLaunch: () -> Unit,
    onClear: () -> Unit,
    onReset: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var warheadExpanded by remember { mutableStateOf(value = true) }
    var targetExpanded by remember { mutableStateOf(value = true) }
    var presetExpanded by remember { mutableStateOf(value = true) }

    Column(
        modifier = modifier
            .background(DarkSurface)
            .fillMaxHeight()
            .widthIn(max = 320.dp)
            .verticalScroll(scrollState)
    ) {
        // 预设场景区域
        PanelSection(
            title = "预设场景",
            expanded = presetExpanded,
            onToggle = { presetExpanded = !presetExpanded }
        ) {
            PresetGrid(
                presets = Presets.all,
                activeId = activePresetId,
                onPresetClick = onPresetApply
            )
        }

        // 弹头参数配置区域
        PanelSection(
            title = "弹头参数配置",
            expanded = warheadExpanded,
            onToggle = { warheadExpanded = !warheadExpanded }
        ) {
            WarheadParams(
                warheadCount = warheadCount,
                yieldKt = yieldKt,
                separationKm = separationKm,
                pattern = pattern,
                hobMode = hobMode,
                onWarheadCountChange = onWarheadCountChange,
                onYieldChange = onYieldChange,
                onSeparationChange = onSeparationChange,
                onPatternChange = onPatternChange,
                onHobModeChange = onHobModeChange
            )
        }

        // 目标定位区域
        PanelSection(
            title = "目标定位",
            expanded = targetExpanded,
            onToggle = { targetExpanded = !targetExpanded }
        ) {
            TargetParams(
                targetLat = targetLat,
                targetLng = targetLng,
                targetType = targetType,
                cityList = cityList,
                onTargetLatChange = onTargetLatChange,
                onTargetLngChange = onTargetLngChange,
                onTargetTypeChange = onTargetTypeChange,
                onCitySelect = onCitySelect,
                onPickOnMap = onPickOnMap
            )
        }

        Spacer(Modifier.height(8.dp))

        // 操作按钮行
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onLaunch,
                enabled = !isComputing,
                colors = ButtonDefaults.buttonColors(containerColor = NukeOrange),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isComputing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("发射", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onClear, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(0.5f)) {
                Text("清除")
            }
        }

        // 辅助按钮行（重置、分享）
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onReset) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("重置", fontSize = 12.sp)
            }
            TextButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("分享", fontSize = 12.sp)
            }
        }
    }
}

// ====================================================================
// PresetsPanel — 预设场景专用面板
// ====================================================================

@Composable
fun PresetsPanel(
    activePresetId: String?,
    onPresetApply: (Preset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(DarkSurface)
            .fillMaxHeight()
            .widthIn(max = 320.dp)
            .verticalScroll(rememberScrollState())
    ) {
        PanelSection(title = "预设场景", expanded = true, onToggle = {}) {
            PresetGrid(presets = Presets.all, activeId = activePresetId, onPresetClick = onPresetApply)
        }
    }
}

// ====================================================================
// SimulationPanel — 模拟专用面板（全部展开）
// ====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationPanel(
    warheadCount: Int,
    yieldKt: Double,
    separationKm: Double,
    pattern: String,
    hobMode: String,
    targetLat: Double,
    targetLng: Double,
    targetType: String,
    cityList: List<City>,
    isComputing: Boolean,
    onWarheadCountChange: (Int) -> Unit,
    onYieldChange: (Double) -> Unit,
    onSeparationChange: (Double) -> Unit,
    onPatternChange: (String) -> Unit,
    onHobModeChange: (String) -> Unit,
    onTargetLatChange: (Double) -> Unit,
    onTargetLngChange: (Double) -> Unit,
    onTargetTypeChange: (String) -> Unit,
    onCitySelect: (City) -> Unit,
    onPickOnMap: () -> Unit,
    onLaunch: () -> Unit,
    onClear: () -> Unit,
    onReset: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(DarkSurface)
            .fillMaxHeight()
            .widthIn(max = 320.dp)
            .verticalScroll(rememberScrollState())
    ) {
        PanelSection(title = "弹头参数配置", expanded = true, onToggle = {}) {
            WarheadParams(
                warheadCount = warheadCount, yieldKt = yieldKt, separationKm = separationKm,
                pattern = pattern, hobMode = hobMode,
                onWarheadCountChange = onWarheadCountChange, onYieldChange = onYieldChange,
                onSeparationChange = onSeparationChange, onPatternChange = onPatternChange,
                onHobModeChange = onHobModeChange)
        }
        PanelSection(title = "目标定位", expanded = true, onToggle = {}) {
            TargetParams(
                targetLat = targetLat, targetLng = targetLng, targetType = targetType,
                cityList = cityList, onTargetLatChange = onTargetLatChange,
                onTargetLngChange = onTargetLngChange, onTargetTypeChange = onTargetTypeChange,
                onCitySelect = onCitySelect, onPickOnMap = onPickOnMap)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onLaunch, enabled = !isComputing,
                colors = ButtonDefaults.buttonColors(containerColor = NukeOrange),
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                if (isComputing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("发射", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onClear, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(0.5f)) {
                Text("清除")
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onReset) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp)); Text("重置", fontSize = 12.sp)
            }
            TextButton(onClick = onShare) {
                Icon(Icons.Filled.Share, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp)); Text("分享", fontSize = 12.sp)
            }
        }
    }
}

// ====================================================================
// 面板区域折叠组件
// ====================================================================

/**
 * 可折叠面板区域
 *
 * @param title 区域标题
 * @param expanded 是否展开
 * @param onToggle 切换展开/收起
 * @param content 内容区域
 */
@Composable
fun PanelSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Surface(onClick = onToggle, color = DarkSurfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = DarkOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Icon(Icons.Filled.ArrowDropDown, null, tint = DarkOnSurfaceVariant,
                    modifier = Modifier.size(20.dp).rotate(if (expanded) 180f else 0f))
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { content() }
        }
    }
}

// ====================================================================
// 预设场景网格
// ====================================================================

/**
 * 预设场景卡片网格（2列布局）
 *
 * 展示所有预设导弹型号，选中状态高亮显示。
 */
@Composable
fun PresetGrid(
    presets: List<Preset>,
    activeId: String?,
    onPresetClick: (Preset) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in presets.indices step 2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PresetCard(presets[i], presets[i].id == activeId, { onPresetClick(presets[i]) }, Modifier.weight(1f))
                if ((i + 1) < presets.size) {
                    PresetCard(presets[i + 1], presets[i + 1].id == activeId, { onPresetClick(presets[i + 1]) }, Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** 单个预设场景卡片 */
@Composable
private fun PresetCard(
    preset: Preset,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isActive) NukeOrange.copy(alpha = 0.2f) else DarkSurfaceVariant,
        border = if (isActive) BorderStroke(1.dp, NukeOrange) else null,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = preset.name, color = if (isActive) NukeOrange else DarkOnBackground,
                fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1)
            Text(text = preset.desc, color = DarkOnSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 10.sp, maxLines = 1)
        }
    }
}

// ====================================================================
// 弹头参数配置组件
// ====================================================================

/**
 * 弹头参数配置区域
 *
 * 包含：弹头数量、当量（带预设）、分离距离、散布模式、爆高模式
 */
@Composable
fun WarheadParams(
    warheadCount: Int,
    yieldKt: Double,
    separationKm: Double,
    pattern: String,
    hobMode: String,
    onWarheadCountChange: (Int) -> Unit,
    onYieldChange: (Double) -> Unit,
    onSeparationChange: (Double) -> Unit,
    onPatternChange: (String) -> Unit,
    onHobModeChange: (String) -> Unit
) {
    SliderField(label = "弹头数量", value = warheadCount.toFloat(), valueText = warheadCount.toString(),
        range = 1f..20f, steps = 18, onValueChange = { onWarheadCountChange(it.roundToInt()) })
    SliderField(label = "单弹头当量 (kt)", value = yieldKt.toFloat(), valueText = formatYield(yieldKt),
        range = 1f..50000f, onValueChange = { onYieldChange(it.toDouble()) })
    YieldPresetRow(currentYield = yieldKt, onYieldClick = onYieldChange)
    SliderField(label = "分离距离", value = separationKm.toFloat(), valueText = "%.1f km".format(separationKm),
        range = 0.1f..10f, steps = 98, onValueChange = { onSeparationChange(it.toDouble()) })
    Text("散布模式", color = TextSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(4.dp))
    PatternSelector(current = pattern, onSelect = onPatternChange)
    Spacer(Modifier.height(8.dp))
    HOBSelector(current = hobMode, onSelect = onHobModeChange)
}

/** 滑块输入组件（带标签和当前值显示） */
@Composable
fun SliderField(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = DarkOnSurfaceVariant, fontSize = 12.sp)
            Text(valueText, color = DarkOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(thumbColor = NukeOrange, activeTrackColor = NukeOrange, inactiveTrackColor = DarkOutline))
    }
}

/** 当量快速预设按钮行 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YieldPresetRow(
    currentYield: Double,
    onYieldClick: (Double) -> Unit
) {
    val presets = listOf(
        15.0 to "15 kt", 100.0 to "100 kt", 150.0 to "150 kt", 300.0 to "300 kt",
        500.0 to "500 kt", 1000.0 to "1 Mt", 10000.0 to "10 Mt", 50000.0 to "50 Mt"
    )
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("当量预设", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            presets.forEach { (yield, label) ->
                val isActive = yield == currentYield
                Surface(onClick = { onYieldClick(yield) }, shape = RoundedCornerShape(4.dp),
                    color = if (isActive) Accent else BgTertiary, modifier = Modifier.height(28.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(text = label, fontSize = 10.sp,
                            color = if (isActive) Color.White else TextSecondary,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

/**
 * 散布模式选择器
 *
 * 四种模式按钮：圆形 / 线性 / 椭圆 / 网格
 */
@Composable
fun PatternSelector(
    current: String,
    onSelect: (String) -> Unit
) {
    val patterns = listOf("circular" to "圆形", "linear" to "线性", "elliptical" to "椭圆", "grid" to "网格")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        patterns.forEach { (value, label) ->
            val isActive = current == value
            Surface(onClick = { onSelect(value) }, shape = RoundedCornerShape(6.dp),
                color = if (isActive) Accent else BgTertiary, modifier = Modifier.weight(1f)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(text = label, fontSize = 12.sp, color = if (isActive) Color.White else TextSecondary,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

/**
 * 爆高模式选择器
 *
 * 三种模式：地爆（surface）/ 空爆（optimal）/ 自定义（custom）
 */
@Composable
fun HOBSelector(
    current: String,
    onSelect: (String) -> Unit
) {
    Text("爆高模式", color = TextSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("surface" to "地爆", "optimal" to "空爆", "custom" to "自定义").forEach { (value, label) ->
            val isActive = current == value
            Surface(onClick = { onSelect(value) }, shape = RoundedCornerShape(4.dp),
                color = if (isActive) Accent else BgTertiary, modifier = Modifier.weight(1f)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(text = label, fontSize = 11.sp, color = if (isActive) Color.White else TextSecondary)
                }
            }
        }
    }
}

// ====================================================================
// 目标定位组件
// ====================================================================

/**
 * 目标参数配置区域
 *
 * 包含：城市搜索、经纬度输入、目标类型选择、地图点选按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetParams(
    targetLat: Double,
    targetLng: Double,
    targetType: String,
    cityList: List<City>,
    onTargetLatChange: (Double) -> Unit,
    onTargetLngChange: (Double) -> Unit,
    onTargetTypeChange: (String) -> Unit,
    onCitySelect: (City) -> Unit,
    onPickOnMap: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var latText by remember(targetLat) { mutableStateOf("%.4f".format(targetLat)) }
    var lngText by remember(targetLng) { mutableStateOf("%.4f".format(targetLng)) }

    CitySearchDropdown(cityList = cityList, onCitySelect = onCitySelect)

    Spacer(Modifier.height(8.dp))
    Text("瞄点坐标", color = TextSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = latText, onValueChange = { latText = it },
            label = { Text("纬度", fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { latText.toDoubleOrNull()?.let { onTargetLatChange(it) }; focusManager.clearFocus() }),
            modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NukeOrange, unfocusedBorderColor = DarkOutline,
                focusedTextColor = DarkOnBackground, unfocusedTextColor = DarkOnBackground))
        OutlinedTextField(value = lngText, onValueChange = { lngText = it },
            label = { Text("经度", fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { lngText.toDoubleOrNull()?.let { onTargetLngChange(it) }; focusManager.clearFocus() }),
            modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = BorderColor,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
    }

    Spacer(Modifier.height(8.dp))
    Text("目标类型", color = TextSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("urban" to "城区", "suburban" to "郊区", "rural" to "乡村").forEach { (value, label) ->
            val isActive = targetType == value
            Surface(onClick = { onTargetTypeChange(value) }, shape = RoundedCornerShape(4.dp),
                color = if (isActive) Accent else BgTertiary, modifier = Modifier.weight(1f)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(text = label, fontSize = 11.sp, color = if (isActive) Color.White else TextSecondary)
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Button(onClick = onPickOnMap, colors = ButtonDefaults.buttonColors(containerColor = BgTertiary),
        shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Explore, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("在地图上点选目标", fontSize = 12.sp)
    }
}

/**
 * 城市搜索下拉选择器
 *
 * 支持按地区分组筛选和模糊搜索。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySearchDropdown(
    cityList: List<City>,
    onCitySelect: (City) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("") }
    var showGroupPicker by remember { mutableStateOf(false) }

    val groups = remember(cityList) {
        cityList.asSequence().map { it.group }.distinct().sortedBy { g ->
            if (g.contains("🇨🇳")) 1 else 0
        }.toList()
    }

    val displayList = remember(searchQuery, selectedGroup, cityList) {
        val byGroup = if (selectedGroup.isEmpty()) cityList else cityList.filter { it.group == selectedGroup }
        if (searchQuery.isBlank()) byGroup.take(150)
        else byGroup.filter { (it.display?.contains(searchQuery, ignoreCase = true) == true) || it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, "选择城市", modifier = Modifier.size(14.dp), tint = Accent)
            Spacer(Modifier.width(6.dp))
            Text("选择城市", color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 地区选择下拉
            Box(modifier = Modifier.weight(0.5f)) {
                Surface(onClick = { showGroupPicker = true }, shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant, border = BorderStroke(0.5.dp, DarkOutline.copy(alpha = 0.4f))) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                        Text(selectedGroup.ifEmpty { "全部" }, fontSize = 12.sp, maxLines = 1,
                            color = if (selectedGroup.isNotEmpty()) NukeOrange else DarkOnSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(16.dp), tint = DarkOnSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
                DropdownMenu(expanded = showGroupPicker, onDismissRequest = { showGroupPicker = false },
                    modifier = Modifier.heightIn(max = 320.dp)) {
                    Surface(color = BgSecondary, shape = RoundedCornerShape(8.dp)) {
                        Column {
                            DropdownMenuItem(text = { Text("全部", fontSize = 13.sp, color = if (selectedGroup.isEmpty()) Accent else TextPrimary) },
                                onClick = { selectedGroup = ""; showGroupPicker = false })
                            groups.forEach { g ->
                                DropdownMenuItem(text = { Text(g, fontSize = 13.sp, color = if (selectedGroup == g) Accent else TextPrimary) },
                                    onClick = { selectedGroup = g; showGroupPicker = false })
                            }
                        }
                    }
                }
            }
            // 城市搜索输入框
            ExposedDropdownMenuBox(expanded = dropdownExpanded && displayList.isNotEmpty(),
                onExpandedChange = { if (displayList.isNotEmpty()) dropdownExpanded = !dropdownExpanded },
                modifier = Modifier.weight(0.5f)) {
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it; dropdownExpanded = true },
                    placeholder = { Text("选择城市", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(16.dp), tint = Accent) },
                    trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = ""; dropdownExpanded = false }, modifier = Modifier.size(16.dp)) { Icon(Icons.Filled.Clear, "清除", modifier = Modifier.size(14.dp), tint = TextSecondary) } } },
                    singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = BorderColor,
                        cursorColor = Accent, focusedContainerColor = BgTertiary, unfocusedContainerColor = BgTertiary),
                    modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = dropdownExpanded && displayList.isNotEmpty(),
                    onDismissRequest = { dropdownExpanded = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                    Surface(color = BgSecondary, shape = RoundedCornerShape(8.dp)) {
                        Column {
                            DropdownMenuItem(text = { Text("${displayList.size} 个城市", fontSize = 11.sp, color = TextMuted) }, onClick = {}, enabled = false)
                            displayList.forEach { city ->
                                DropdownMenuItem(onClick = { onCitySelect(city); searchQuery = city.display ?: city.name; dropdownExpanded = false },
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(28.dp).background(Accent.copy(alpha = 0.1f), RoundedCornerShape(7.dp)),
                                                contentAlignment = Alignment.Center) {
                                                Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(14.dp), tint = Accent)
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(city.display ?: city.name, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                                Text("${city.group} · %.0f万".format(city.pop), fontSize = 10.sp, color = TextMuted, maxLines = 1)
                                            }
                                        }
                                    })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 格式化当量显示（≥1000kt 时显示为 Mt） */
fun formatYield(kt: Double): String {
    return if (kt >= 1000) "%.1f Mt".format(kt / 1000)
    else "%.0f kt".format(kt)
}

@Preview(showBackground = true)
@Composable
fun ControlPanelPreview() {
    NukemapTheme {
        ControlPanel(
            warheadCount = 5, yieldKt = 100.0, separationKm = 2.0, pattern = "circular",
            hobMode = "optimal", targetLat = 39.9042, targetLng = 116.4074, targetType = "urban",
            activePresetId = null, cityList = emptyList(), isComputing = false,
            onWarheadCountChange = {}, onYieldChange = {}, onSeparationChange = {},
            onPatternChange = {}, onHobModeChange = {}, onTargetLatChange = {},
            onTargetLngChange = {}, onTargetTypeChange = {}, onCitySelect = {},
            onPresetApply = {}, onPickOnMap = {}, onLaunch = {}, onClear = {},
            onReset = {}, onShare = {}, modifier = Modifier.fillMaxHeight()
        )
    }
}
