package com.mirvsim.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi

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
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var warheadExpanded by remember { mutableStateOf(true) }
    var targetExpanded by remember { mutableStateOf(true) }
    var presetExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .background(BgSecondary)
            .fillMaxHeight()
            .widthIn(max = 320.dp)
            .verticalScroll(scrollState)
    ) {
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onLaunch,
                enabled = !isComputing,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isComputing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("发射模拟", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onClear,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(0.5f)
            ) {
                Text("清除")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
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

@Composable
fun PanelSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Surface(
            onClick = onToggle,
            color = BgTertiary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun PresetGrid(
    presets: List<Preset>,
    activeId: String?,
    onPresetClick: (Preset) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.heightIn(max = 360.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(presets) { preset ->
            val isActive = preset.id == activeId
            Surface(
                onClick = { onPresetClick(preset) },
                shape = RoundedCornerShape(6.dp),
                color = if (isActive) Accent.copy(alpha = 0.2f) else BgTertiary,
                border = if (isActive) androidx.compose.foundation.BorderStroke(
                    1.dp, Accent
                ) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = preset.name,
                        color = if (isActive) Accent else TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Text(
                        text = preset.desc,
                        color = TextMuted,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

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
    SliderField(
        label = "弹头数量",
        value = warheadCount.toFloat(),
        valueText = "$warheadCount",
        range = 1f..20f,
        steps = 18,
        onValueChange = { onWarheadCountChange(it.roundToInt()) }
    )

    SliderField(
        label = "单弹头当量 (kt)",
        value = yieldKt.toFloat(),
        valueText = formatYield(yieldKt),
        range = 1f..50000f,
        onValueChange = { onYieldChange(it.toDouble()) }
    )

    YieldPresetRow(
        currentYield = yieldKt,
        onYieldClick = onYieldChange
    )

    SliderField(
        label = "分离距离",
        value = separationKm.toFloat(),
        valueText = "%.1f km".format(separationKm),
        range = 0.1f..10f,
        steps = 98,
        onValueChange = { onSeparationChange(it.toDouble()) }
    )

    Text("散布模式", color = TextSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(4.dp))
    PatternSelector(
        current = pattern,
        onSelect = onPatternChange
    )
    Spacer(Modifier.height(8.dp))

    HOBSelector(
        current = hobMode,
        onSelect = onHobModeChange
    )
}

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text(valueText, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = BorderColor
            )
        )
    }
}

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
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            presets.forEach { (yield, label) ->
                val isActive = yield == currentYield
                Surface(
                    onClick = { onYieldClick(yield) },
                    shape = RoundedCornerShape(4.dp),
                    color = if (isActive) Accent else BgTertiary,
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = if (isActive) Color.White else TextSecondary,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PatternSelector(
    current: String,
    onSelect: (String) -> Unit
) {
    val patterns = listOf(
        "circular" to "圆形",
        "linear" to "线性",
        "elliptical" to "椭圆",
        "grid" to "网格"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        patterns.forEach { (value, label) ->
            val isActive = current == value
            Surface(
                onClick = { onSelect(value) },
                shape = RoundedCornerShape(6.dp),
                color = if (isActive) Accent else BgTertiary,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (isActive) Color.White else TextSecondary,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun HOBSelector(
    current: String,
    onSelect: (String) -> Unit
) {
    Text("爆高模式", color = TextSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(4.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf("surface" to "地爆", "optimal" to "空爆", "custom" to "自定义").forEach { (value, label) ->
            val isActive = current == value
            Surface(
                onClick = { onSelect(value) },
                shape = RoundedCornerShape(4.dp),
                color = if (isActive) Accent else BgTertiary,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (isActive) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}

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

    CitySearchDropdown(
        cityList = cityList,
        onCitySelect = onCitySelect
    )

    Spacer(Modifier.height(8.dp))

    Text("瞄点坐标", color = TextSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = latText,
            onValueChange = { latText = it },
            label = { Text("纬度", fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                latText.toDoubleOrNull()?.let { onTargetLatChange(it) }
                focusManager.clearFocus()
            }),
            modifier = Modifier.weight(1f),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        OutlinedTextField(
            value = lngText,
            onValueChange = { lngText = it },
            label = { Text("经度", fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                lngText.toDoubleOrNull()?.let { onTargetLngChange(it) }
                focusManager.clearFocus()
            }),
            modifier = Modifier.weight(1f),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
    }

    Spacer(Modifier.height(8.dp))

    Text("目标类型", color = TextSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("urban" to "城区", "suburban" to "郊区", "rural" to "乡村").forEach { (value, label) ->
            val isActive = targetType == value
            Surface(
                onClick = { onTargetTypeChange(value) },
                shape = RoundedCornerShape(4.dp),
                color = if (isActive) Accent else BgTertiary,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (isActive) Color.White else TextSecondary
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = onPickOnMap,
        colors = ButtonDefaults.buttonColors(containerColor = BgTertiary),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("在地图上点选目标", fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySearchDropdown(
    cityList: List<City>,
    onCitySelect: (City) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    val filtered = remember(searchQuery, cityList) {
        if (searchQuery.isBlank()) cityList.take(50)
        else cityList.filter {
            (it.display?.contains(searchQuery, ignoreCase = true) == true) ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.group.contains(searchQuery, ignoreCase = true)
        }.take(50)
    }

    Column {
        Text("城市选择", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded && filtered.isNotEmpty(),
            onExpandedChange = {
                if (filtered.isNotEmpty()) expanded = !expanded
            }
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    expanded = it.isNotEmpty() || isFocused
                },
                placeholder = { Text("搜索城市...", fontSize = 12.sp, color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null,
                        modifier = Modifier.size(18.dp), tint = TextSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            expanded = false
                        }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Filled.Clear, contentDescription = "清除",
                                modifier = Modifier.size(14.dp), tint = TextSecondary)
                        }
                    } else {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null,
                            modifier = Modifier.size(18.dp).rotate(if (expanded) 180f else 0f),
                            tint = TextSecondary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            ExposedDropdownMenu(
                expanded = expanded && filtered.isNotEmpty(),
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 240.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    items(filtered) { city ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Accent
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            city.display ?: city.name,
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            "${city.group} · %.0f万人口".format(city.pop),
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onCitySelect(city)
                                searchQuery = city.display ?: city.name
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

fun formatYield(kt: Double): String {
    return if (kt >= 1000) "%.1f Mt".format(kt / 1000)
    else "%.0f kt".format(kt)
}

@Preview(showBackground = true)
@Composable
fun ControlPanelPreview() {
    NukemapTheme {
        ControlPanel(
            warheadCount = 5,
            yieldKt = 100.0,
            separationKm = 2.0,
            pattern = "circular",
            hobMode = "optimal",
            targetLat = 39.9042,
            targetLng = 116.4074,
            targetType = "urban",
            activePresetId = null,
            cityList = emptyList(),
            isComputing = false,
            onWarheadCountChange = {},
            onYieldChange = {},
            onSeparationChange = {},
            onPatternChange = {},
            onHobModeChange = {},
            onTargetLatChange = {},
            onTargetLngChange = {},
            onTargetTypeChange = {},
            onCitySelect = {},
            onPresetApply = {},
            onPickOnMap = {},
            onLaunch = {},
            onClear = {},
            onReset = {},
            onShare = {},
            modifier = Modifier.fillMaxHeight()
        )
    }
}


