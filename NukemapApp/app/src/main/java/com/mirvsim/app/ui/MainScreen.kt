package com.mirvsim.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirvsim.app.model.*
import com.mirvsim.app.ui.components.BottomNavigationBar
import com.mirvsim.app.ui.components.BottomNavItem
import com.mirvsim.app.ui.components.PresetsPanel
import com.mirvsim.app.ui.components.SettingsPanel
import com.mirvsim.app.ui.components.SimulationPanel
import com.mirvsim.app.ui.components.MapView
import com.mirvsim.app.ui.components.SideNavigationRail
import com.mirvsim.app.ui.components.SideDrawer
import com.mirvsim.app.ui.components.StatsPanel
import com.mirvsim.app.ui.theme.*
import com.mirvsim.app.viewmodel.SimulationViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainScreen(
    viewModel: SimulationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val windowSizeClass = if (LocalInspectionMode.current) {
        WindowSizeClass.calculateFromSize(androidx.compose.ui.unit.DpSize(360.dp, 640.dp))
    } else {
        calculateWindowSizeClass(context as android.app.Activity)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    // 收集一次性事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainUiEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is MainUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val handleCitySelect: (City) -> Unit = { city ->
        viewModel.selectCity(city)
    }

    val handleMapClick: (Double, Double) -> Unit = { lat, lng ->
        if (uiState.isPickMode) {
            viewModel.updateTargetLocation(lat, lng)
        }
    }

    val handleShare: () -> Unit = {
        val params = mapOf(
            "lat" to "%.4f".format(uiState.targetLat),
            "lng" to "%.4f".format(uiState.targetLng),
            "count" to uiState.warheadCount.toString(),
            "yield" to "%.1f".format(uiState.yieldKt),
            "sep" to "%.1f".format(uiState.separationKm),
            "pattern" to uiState.pattern,
            "hob" to uiState.hobMode,
            "target" to uiState.targetType
        )
        val url = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("MIRV Sim", url))
        scope.launch {
            snackbarHostState.showSnackbar("链接已复制到剪贴板")
        }
    }

    val onPresetApply: (Preset) -> Unit = { preset ->
        viewModel.applyPreset(preset)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = BgTertiary,
                    contentColor = TextPrimary,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        bottomBar = {
            // 小屏幕设备显示底部导航栏
            if (!isExpanded) {
                BottomNavigationBar(
                    currentRoute = uiState.currentNavRoute,
                    onItemSelected = viewModel::navigateTo,
                    hasSimulationResult = uiState.hasSimulationResult
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. 底层：地图 (铺满全屏)
            MapView(
                targetLat = uiState.targetLat,
                targetLng = uiState.targetLng,
                warheadPoints = uiState.warheadPoints,
                effects = uiState.simulationResult?.effectsList,
                pickMode = uiState.isPickMode,
                popupEnabled = uiState.popupEnabled,
                tileSource = uiState.tileSource,
                ringAnimEnabled = uiState.ringAnimation,
                onMapClick = handleMapClick,
                modifier = Modifier.fillMaxSize()
            )

            // 2. 顶层：UI 组件 (处理状态栏内边距)
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                // 错误提示
                uiState.errorMessage?.let { error ->
                    Surface(
                        color = Danger.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(error, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, "关闭", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // 网络不可用提示
                if (!uiState.isNetworkAvailable) {
                    Surface(
                        color = Warning.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.WifiOff, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("网络不可用，地图可能无法加载", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                // 操作栏 (根据屏幕尺寸显示不同内容)
                if (!isExpanded) {
                    TopActionBar(
                        uiState = uiState,
                        viewModel = viewModel,
                        handleShare = handleShare
                    )
                }
            }

            // 3. 展开模式下的侧边栏
            if (isExpanded) {
                Row(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    SideNavigationRail(
                        currentRoute = uiState.currentNavRoute,
                        onItemSelected = viewModel::navigateTo,
                        hasSimulationResult = uiState.hasSimulationResult
                    )
                    SideDrawer(
                        currentRoute = uiState.currentNavRoute,
                        onItemSelected = viewModel::navigateTo,
                        hasSimulationResult = uiState.hasSimulationResult,
                        isExpanded = uiState.sideNavExpanded,
                        onExpandToggle = viewModel::toggleSideNav
                    )
                    when (uiState.currentNavRoute) {
                        BottomNavItem.SIMULATE -> {
                            SimulationPanel(
                                warheadCount = uiState.warheadCount,
                                yieldKt = uiState.yieldKt,
                                separationKm = uiState.separationKm,
                                pattern = uiState.pattern,
                                hobMode = uiState.hobMode,
                                targetLat = uiState.targetLat,
                                targetLng = uiState.targetLng,
                                targetType = uiState.targetType,
                                cityList = uiState.cityList,
                                isComputing = uiState.isComputing,
                                onWarheadCountChange = viewModel::updateWarheadCount,
                                onYieldChange = viewModel::updateYield,
                                onSeparationChange = viewModel::updateSeparation,
                                onPatternChange = viewModel::updatePattern,
                                onHobModeChange = viewModel::updateHobMode,
                                onTargetLatChange = { viewModel.updateTargetLocation(it, uiState.targetLng) },
                                onTargetLngChange = { viewModel.updateTargetLocation(uiState.targetLat, it) },
                                onTargetTypeChange = viewModel::updateTargetType,
                                onCitySelect = handleCitySelect,
                                onPickOnMap = viewModel::togglePickMode,
                                onLaunch = { viewModel.executeSimulation() },
                                onClear = { viewModel.clearResult() },
                                onReset = { viewModel.resetAll() },
                                onShare = handleShare,
                                modifier = Modifier.widthIn(max = 300.dp)
                            )
                        }
                        BottomNavItem.PRESETS -> {
                            PresetsPanel(
                                activePresetId = uiState.activePresetId,
                                onPresetApply = onPresetApply,
                                modifier = Modifier.widthIn(max = 300.dp)
                            )
                        }
                        BottomNavItem.SETTINGS -> {
                            SettingsPanel(
                                state = uiState,
                                onDarkThemeChange = viewModel::setDarkTheme,
                                onDynamicColorChange = viewModel::setDynamicColor,
                                onTileSourceChange = viewModel::setTileSource,
                                onPopupEnabledChange = viewModel::setPopupEnabled,
                                onAutoLaunchChange = viewModel::setAutoLaunchPreset,
                                onRingAnimationChange = viewModel::setRingAnimation,
                                modifier = Modifier.widthIn(max = 300.dp)
                            )
                        }
                        else -> {}
                    }
                }
            }
            
            // 4. 其他浮动组件 (BottomSheet 等)
            // 模拟参数面板（紧凑模式）
            if (uiState.controlDrawerOpen) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.toggleControlDrawer() },
                    containerColor = BgSecondary,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) },
                    tonalElevation = 0.dp
                ) {
                    SimulationPanel(
                        warheadCount = uiState.warheadCount,
                        yieldKt = uiState.yieldKt,
                        separationKm = uiState.separationKm,
                        pattern = uiState.pattern,
                        hobMode = uiState.hobMode,
                        targetLat = uiState.targetLat,
                        targetLng = uiState.targetLng,
                        targetType = uiState.targetType,
                        cityList = uiState.cityList,
                        isComputing = uiState.isComputing,
                        onWarheadCountChange = viewModel::updateWarheadCount,
                        onYieldChange = viewModel::updateYield,
                        onSeparationChange = viewModel::updateSeparation,
                        onPatternChange = viewModel::updatePattern,
                        onHobModeChange = viewModel::updateHobMode,
                        onTargetLatChange = { viewModel.updateTargetLocation(it, uiState.targetLng) },
                        onTargetLngChange = { viewModel.updateTargetLocation(uiState.targetLat, it) },
                        onTargetTypeChange = viewModel::updateTargetType,
                        onCitySelect = handleCitySelect,
                        onPickOnMap = viewModel::togglePickMode,
                        onLaunch = {
                            viewModel.executeSimulation()
                            viewModel.toggleControlDrawer()
                        },
                        onClear = { viewModel.clearResult() },
                        onReset = { viewModel.resetAll() },
                        onShare = handleShare,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 预设场景面板（紧凑模式）
            if (uiState.presetsDrawerOpen) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.togglePresetsDrawer() },
                    containerColor = BgSecondary,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) },
                    tonalElevation = 0.dp
                ) {
                    PresetsPanel(
                        activePresetId = uiState.activePresetId,
                        onPresetApply = onPresetApply,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 设置面板（紧凑模式）
            if (uiState.settingsOpen) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.toggleSettings() },
                    containerColor = BgSecondary,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) },
                    tonalElevation = 0.dp
                ) {
                    SettingsPanel(
                        state = uiState,
                        onDarkThemeChange = viewModel::setDarkTheme,
                        onDynamicColorChange = viewModel::setDynamicColor,
                        onTileSourceChange = viewModel::setTileSource,
                        onPopupEnabledChange = viewModel::setPopupEnabled,
                        onAutoLaunchChange = viewModel::setAutoLaunchPreset,
                        onRingAnimationChange = viewModel::setRingAnimation,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 统计详情底部弹窗
            if (uiState.statsSheetOpen && uiState.showStats && uiState.simulationResult != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        viewModel.toggleStatsSheet()
                    },
                    containerColor = BgSecondary,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) },
                    tonalElevation = 0.dp
                ) {
                    uiState.simulationResult?.let { r ->
                        StatsPanel(
                            result = r,
                            warheadPoints = uiState.warheadPoints,
                            onClose = {
                                viewModel.toggleStatsSheet()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopActionBar(
    uiState: MainUiState,
    viewModel: SimulationViewModel,
    handleShare: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = BgSecondary.copy(alpha = 0.95f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.executeSimulation() },
                    enabled = !uiState.isComputing,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isComputing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("发射模拟", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(8.dp))
                TopBarCitySelector(
                    cityList = uiState.cityList,
                    currentLat = uiState.targetLat,
                    currentLng = uiState.targetLng,
                    onCitySelect = { viewModel.selectCity(it) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { viewModel.resetAll() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Refresh, "重置", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = handleShare, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Share, "分享", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBarCitySelector(
    cityList: List<City>,
    currentLat: Double,
    currentLng: Double,
    onCitySelect: (City) -> Unit,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("") }
    var showGroupPicker by remember { mutableStateOf(false) }

    val currentCityName = remember(currentLat, currentLng, cityList) {
        findCityName(cityList, currentLat, currentLng)
    }

    val groups = remember(cityList) {
        cityList.map { it.group }.distinct().sortedBy { g ->
            if (g.contains("🇨🇳")) 1 else 0
        }
    }

    val displayList = remember(searchQuery, selectedGroup, cityList) {
        val byGroup = if (selectedGroup.isEmpty()) cityList
                      else cityList.filter { it.group == selectedGroup }
        if (searchQuery.isBlank()) byGroup.take(150)
        else byGroup.filter {
            (it.display?.contains(searchQuery, ignoreCase = true) == true) ||
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = modifier) {
        Surface(
            onClick = { dropdownExpanded = true },
            shape = RoundedCornerShape(8.dp),
            color = BgTertiary,
            border = BorderStroke(1.dp, if (dropdownExpanded) Accent.copy(alpha = 0.5f) else BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier.size(22.dp)
                        .background(Accent.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(13.dp), tint = Accent) }
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentCityName, fontSize = 12.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimary, fontWeight = FontWeight.Medium)
                    Text(if (currentCityName == "选择城市") "点击选择" else "切换",
                        fontSize = 8.sp, color = TextMuted, maxLines = 1)
                }
                Icon(Icons.Filled.ArrowDropDown, null,
                    modifier = Modifier.size(18.dp), tint = TextMuted)
            }
        }

        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false; searchQuery = ""; showGroupPicker = false },
            modifier = Modifier.heightIn(max = 420.dp)
        ) {
            Surface(color = BgSecondary, shape = RoundedCornerShape(12.dp)) {
                Column {
                    // 地区 + 搜索同行
                    Surface(color = BgTertiary, shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 地区选择
                            Box(modifier = Modifier.weight(0.35f)) {
                                Surface(
                                    onClick = { showGroupPicker = true },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedGroup.isNotEmpty()) Accent.copy(alpha = 0.12f) else BgTertiary,
                                    border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.4f))
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            if (selectedGroup.isEmpty()) "全部" else selectedGroup,
                                            fontSize = 11.sp, maxLines = 1,
                                            color = if (selectedGroup.isNotEmpty()) Accent else TextMuted,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Filled.ArrowDropDown, null,
                                            modifier = Modifier.size(14.dp), tint = TextMuted)
                                    }
                                }
                                DropdownMenu(
                                    expanded = showGroupPicker,
                                    onDismissRequest = { showGroupPicker = false },
                                    modifier = Modifier.heightIn(max = 300.dp)
                                ) {
                                    Surface(color = BgSecondary, shape = RoundedCornerShape(8.dp)) {
                                        Column {
                                            DropdownMenuItem(
                                                text = { Text("全部", fontSize = 13.sp,
                                                    color = if (selectedGroup.isEmpty()) Accent else TextPrimary) },
                                                onClick = { selectedGroup = ""; showGroupPicker = false }
                                            )
                                            groups.forEach { g ->
                                                DropdownMenuItem(
                                                    text = { Text(g, fontSize = 13.sp,
                                                        color = if (selectedGroup == g) Accent else TextPrimary) },
                                                    onClick = { selectedGroup = g; showGroupPicker = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            // 搜索框
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("搜索城市", fontSize = 12.sp, color = TextMuted) },
                                leadingIcon = { Icon(Icons.Filled.Search, null,
                                    modifier = Modifier.size(16.dp), tint = Accent) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" },
                                            modifier = Modifier.size(16.dp)) {
                                            Icon(Icons.Filled.Clear, "清除",
                                                modifier = Modifier.size(14.dp), tint = TextSecondary)
                                        }
                                    }
                                },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Accent, unfocusedBorderColor = BorderColor,
                                    cursorColor = Accent,
                                    focusedContainerColor = BgTertiary, unfocusedContainerColor = BgTertiary
                                ),
                                modifier = Modifier.weight(0.65f)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp)

                    if (displayList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("未找到匹配城市", fontSize = 13.sp, color = TextMuted) }
                    } else {
                        Text("${displayList.size} 个城市",
                            fontSize = 11.sp, color = TextMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                        Column(
                            modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())
                        ) {
                            displayList.forEach { city ->
                                Surface(
                                    onClick = {
                                        onCitySelect(city)
                                        searchQuery = ""
                                        dropdownExpanded = false
                                        showGroupPicker = false
                                    },
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier.size(28.dp)
                                                .background(Accent.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) { Icon(Icons.Filled.LocationOn, null,
                                            modifier = Modifier.size(14.dp), tint = Accent) }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(city.display ?: city.name, fontSize = 13.sp,
                                                color = TextPrimary)
                                            Text("${city.group} · ${formatCompactNumber((city.pop * 10000).toInt())}人",
                                                fontSize = 11.sp, color = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun findCityName(cityList: List<City>, lat: Double, lng: Double): String {
    val exact = cityList.find { it.lat == lat && it.lng == lng }
    if (exact != null) return exact.display ?: exact.name

    val nearest = cityList.minByOrNull { c ->
        val dlat = c.lat - lat
        val dlng = c.lng - lng
        dlat * dlat + dlng * dlng
    }
    return if (nearest != null && kotlin.math.sqrt(
            (nearest.lat - lat) * (nearest.lat - lat) +
            (nearest.lng - lng) * (nearest.lng - lng)
        ) < 0.5) {
        nearest.display ?: nearest.name
    } else {
        "选择城市"
    }
}

private fun formatCompactNumber(num: Int): String {
    return when {
        num >= 1_000_000_000 -> "%.1fB".format(num / 1_000_000_000.0)
        num >= 1_000_000 -> "%.1fM".format(num / 1_000_000.0)
        num >= 1_000 -> "%.1fK".format(num / 1_000.0)
        else -> num.toString()
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MainScreenPreview() {
    NukemapTheme {
        MainScreen()
    }
}
