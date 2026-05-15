/**
 * 主屏幕 Composable
 *
 * 应用的主界面，采用分层布局策略：
 * 1. 底层：地图（osmdroid）始终铺满全屏作为背景
 * 2. 顶层：UI 控制组件覆盖在地图上
 *
 * 屏幕尺寸适配（响应式设计）：
 * - 紧凑模式（Compact/手机竖屏）：底部导航栏 + ModalBottomSheet
 * - 中等模式（Medium/小平板）：全屏覆盖面板
 * - 展开模式（Expanded/大屏平板）：左右分栏布局
 *
 * 布局层次：
 * - 地图层：始终显示
 * - 提示层：错误消息、网络不可用提示
 * - 操作栏（手机端）：发射按钮、城市选择器、分享等
 * - 功能面板：模拟参数、预设场景、设置、统计
 */
package com.mirvsim.app.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.animation.*
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainScreen(
    viewModel: SimulationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 计算窗口尺寸分类（预览模式下使用固定手机尺寸）
    val windowSizeClass = if (LocalInspectionMode.current) {
        WindowSizeClass.calculateFromSize(androidx.compose.ui.unit.DpSize(360.dp, 640.dp))
    } else {
        calculateWindowSizeClass(context as Activity)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val widthClass = windowSizeClass.widthSizeClass
    val isCompact = widthClass == WindowWidthSizeClass.Compact   // 手机竖屏
    val isMedium = widthClass == WindowWidthSizeClass.Medium     // 小平板
    val isExpanded = widthClass == WindowWidthSizeClass.Expanded  // 大屏平板
    val isLargeScreen = !isCompact  // 非手机端

    // 状态栏图标颜色自适应：全屏覆盖层中强制白色，其他跟随主题
    val inOverlay = (isCompact || isMedium) && (uiState.settingsOpen || uiState.statsSheetOpen)
    val darkTheme = uiState.isDarkTheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            WindowCompat.getInsetsController((view.context as Activity).window, view).isAppearanceLightStatusBars =
                if (inOverlay) false else !darkTheme
        }
    }

    // 收集一次性事件（Toast 提示、错误信息）
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

    /** 地图点击回调：拾取模式下设置目标位置 */
    val handleMapClick: (Double, Double) -> Unit = { lat, lng ->
        if (uiState.isPickMode) {
            viewModel.updateTargetLocation(lat, lng)
        }
    }

    /**
     * 分享功能：将当前模拟参数编码为 URL 字符串存入剪贴板
     * 格式: lat=39.9&lng=116.4&count=4&yield=150.0&sep=1.5&pattern=circular&hob=optimal&target=urban
     */
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

    // ========== 主布局 Scaffold ==========
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = DarkSurfaceVariant,
                    contentColor = DarkOnBackground,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        bottomBar = {
            // 手机竖屏且未打开全屏界面时显示底部导航栏
            if (isCompact && !uiState.settingsOpen && !uiState.statsSheetOpen) {
                BottomNavigationBar(
                    currentRoute = uiState.currentNavRoute,
                    onItemSelected = viewModel::navigateTo,
                    hasSimulationResult = uiState.hasSimulationResult
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // ===== 图层 1：地图（始终铺满全屏作为背景） =====
            MapView(
                targetLat = uiState.targetLat,
                targetLng = uiState.targetLng,
                myLat = uiState.myLat,
                myLng = uiState.myLng,
                warheadPoints = uiState.warheadPoints,
                effects = uiState.simulationResult?.effectsList,
                pickMode = uiState.isPickMode,
                popupEnabled = uiState.popupEnabled,
                tileSource = uiState.tileSource,
                ringAnimEnabled = uiState.ringAnimation,
                onMapClick = handleMapClick,
                modifier = Modifier.fillMaxSize()
            )

            // ===== 图层 2：顶部提示条和操作栏 =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                // 错误提示（可关闭）
                uiState.errorMessage?.let { error ->
                    Surface(
                        color = NukeRed.copy(alpha = 0.9f),
                        modifier = Modifier
                            .fillMaxWidth(if (isLargeScreen) 0.5f else 1f)
                            .align(if (isLargeScreen) Alignment.CenterHorizontally else Alignment.Start)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
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
                        color = NukeYellow.copy(alpha = 0.9f),
                        modifier = Modifier
                            .fillMaxWidth(if (isLargeScreen) 0.4f else 1f)
                            .align(if (isLargeScreen) Alignment.CenterHorizontally else Alignment.Start)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
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

                // 手机端顶部操作栏（发射按钮、城市选择器、重置、分享）
                if (isCompact) {
                    TopActionBar(
                        uiState = uiState,
                        viewModel = viewModel,
                        handleShare = handleShare
                    )
                }
            }

            // ===== 图层 3：大屏幕/平板端分栏布局 =====
            if (isLargeScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // 左侧导航栏（图标 + 文字）
                    SideNavigationRail(
                        currentRoute = uiState.currentNavRoute,
                        onItemSelected = viewModel::navigateTo,
                        hasSimulationResult = uiState.hasSimulationResult
                    )
                    
                    // 可展开侧边抽屉（展开后显示完整菜单）
                    SideDrawer(
                        currentRoute = uiState.currentNavRoute,
                        onItemSelected = viewModel::navigateTo,
                        hasSimulationResult = uiState.hasSimulationResult,
                        isExpanded = uiState.sideNavExpanded,
                        onExpandToggle = viewModel::toggleSideNav
                    )

                    // 内容区：左侧面板和右侧统计面板分列显示
                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // 左侧功能面板（滑动显隐动画）
                            AnimatedVisibility(
                                visible = uiState.currentNavRoute != BottomNavItem.STATS,
                                enter = slideInHorizontally { -it },
                                exit = slideOutHorizontally { -it }
                            ) {
                                val panelWidth = if (isExpanded) 340.dp else 300.dp
                                Surface(
                                    modifier = Modifier.width(panelWidth).fillMaxHeight(),
                                    color = DarkSurface,
                                    shadowElevation = 2.dp
                                ) {
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
                                                onSavePreset = { viewModel.saveCurrentPreset("我的预设") },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        BottomNavItem.PRESETS -> {
                                            PresetsPanel(
                                                activePresetId = uiState.activePresetId,
                                                onPresetApply = onPresetApply,
                                                modifier = Modifier.fillMaxSize()
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
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        else -> {}
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // 右侧统计面板（Expanded 模式下显示）
                            if (isExpanded && uiState.statsSheetOpen && uiState.simulationResult != null) {
                                Surface(
                                    modifier = Modifier.width(380.dp).fillMaxHeight(),
                                    color = DarkSurface,
                                    shadowElevation = 8.dp
                                ) {
                                    uiState.simulationResult?.let { r ->
                                        StatsPanel(
                                            result = r,
                                            warheadPoints = uiState.warheadPoints,
                                            onClose = { viewModel.toggleStatsSheet() },
                                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // ===== 图层 4：手机/平板浮动面板 =====

            // 模拟参数面板（手机端底部抽屉）
            if (isCompact && uiState.controlDrawerOpen) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.toggleControlDrawer() },
                    containerColor = DarkSurface,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = { BottomSheetDefaults.DragHandle(color = DarkOnSurfaceVariant.copy(alpha = 0.6f)) },
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
                        onSavePreset = { viewModel.saveCurrentPreset("我的预设") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
 
            // 预设场景面板（手机端底部抽屉）
            if (isCompact && uiState.presetsDrawerOpen) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.togglePresetsDrawer() },
                    containerColor = DarkSurface,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = { BottomSheetDefaults.DragHandle(color = DarkOnSurfaceVariant.copy(alpha = 0.6f)) },
                    tonalElevation = 0.dp
                ) {
                    PresetsPanel(
                        activePresetId = uiState.activePresetId,
                        onPresetApply = onPresetApply,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 设置页面（手机/平板全屏覆盖，带动画）
            AnimatedVisibility(
                visible = !isExpanded && uiState.settingsOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkSurface
                ) {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                        CenterAlignedTopAppBar(
                            title = { Text("设置", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { viewModel.toggleSettings() }) {
                                    Icon(Icons.Filled.Close, "关闭", tint = DarkOnSurfaceVariant)
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = DarkSurface,
                                titleContentColor = DarkOnBackground
                            )
                        )
                        SettingsPanel(
                            state = uiState,
                            onDarkThemeChange = viewModel::setDarkTheme,
                            onDynamicColorChange = viewModel::setDynamicColor,
                            onTileSourceChange = viewModel::setTileSource,
                            onPopupEnabledChange = viewModel::setPopupEnabled,
                            onAutoLaunchChange = viewModel::setAutoLaunchPreset,
                            onRingAnimationChange = viewModel::setRingAnimation,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 统计详情页面（手机/平板全屏覆盖，带动画）
            AnimatedVisibility(
                visible = !isExpanded && uiState.statsSheetOpen && uiState.showStats && uiState.simulationResult != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkSurface
                ) {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                        uiState.simulationResult?.let { r ->
                            StatsPanel(
                                result = r,
                                warheadPoints = uiState.warheadPoints,
                                onClose = {
                                    viewModel.toggleStatsSheet()
                                },
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================
// 以下为辅助 Composable 组件：顶部操作栏、城市选择器等
// ====================================================================

/**
 * 手机端顶部操作栏
 *
 * 包含：发射按钮（带计算中状态）、城市选择器、重置、分享
 */
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
            colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface.copy(alpha = 0.95f))
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
                    colors = ButtonDefaults.buttonColors(containerColor = NukeOrange),
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
                    Text("发射", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    Icon(Icons.Filled.Refresh, "重置", tint = DarkOnSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = handleShare, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Share, "分享", tint = DarkOnSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/**
 * 顶部栏城市选择器
 *
 * 支持：
 * - 下拉搜索城市
 * - 按地区分组筛选
 * - 显示当前选中城市名称
 * - 模糊匹配城市名/显示名
 */
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

    // 根据当前坐标查找城市显示名
    val currentCityName = remember(currentLat, currentLng, cityList) {
        findCityName(cityList, currentLat, currentLng)
    }

    // 获取所有地区分组
    val groups = remember(cityList) {
        cityList.map { it.group }.distinct().sortedBy { g ->
            if (g.contains("🇨🇳")) 1 else 0
        }
    }

    // 根据搜索关键词和分组筛选城市列表
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
            color = DarkSurfaceVariant,
            border = BorderStroke(1.dp, if (dropdownExpanded) NukeOrange.copy(alpha = 0.5f) else DarkOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier.size(22.dp)
                        .background(NukeOrange.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(13.dp), tint = NukeOrange) }
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentCityName, fontSize = 12.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = DarkOnBackground, fontWeight = FontWeight.Medium)
                }
                Icon(Icons.Filled.ArrowDropDown, null,
                    modifier = Modifier.size(18.dp), tint = DarkOnSurfaceVariant.copy(alpha = 0.6f))
            }
        }

        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false; searchQuery = ""; showGroupPicker = false },
            modifier = Modifier.heightIn(max = 420.dp)
        ) {
            Surface(color = DarkSurface, shape = RoundedCornerShape(12.dp)) {
                Column {
                    // 地区选择 + 搜索框
                    Surface(color = DarkSurfaceVariant, shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 地区下拉
                            Box(modifier = Modifier.weight(0.5f)) {
                                Surface(
                                    onClick = { showGroupPicker = true },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedGroup.isNotEmpty()) NukeOrange.copy(alpha = 0.12f) else DarkSurfaceVariant,
                                    border = BorderStroke(0.5.dp, DarkOutline.copy(alpha = 0.4f))
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            selectedGroup.ifEmpty { "全部" },
                                            fontSize = 11.sp, maxLines = 1,
                                            color = if (selectedGroup.isNotEmpty()) NukeOrange else DarkOnSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Filled.ArrowDropDown, null,
                                            modifier = Modifier.size(14.dp), tint = DarkOnSurfaceVariant.copy(alpha = 0.6f))
                                    }
                                }
                                DropdownMenu(
                                    expanded = showGroupPicker,
                                    onDismissRequest = { showGroupPicker = false },
                                    modifier = Modifier.heightIn(max = 300.dp)
                                ) {
                                    Surface(color = DarkSurface, shape = RoundedCornerShape(8.dp)) {
                                        Column {
                                            DropdownMenuItem(
                                                text = { Text("全部", fontSize = 13.sp,
                                                    color = if (selectedGroup.isEmpty()) NukeOrange else DarkOnBackground) },
                                                onClick = { selectedGroup = ""; showGroupPicker = false }
                                            )
                                            groups.forEach { g ->
                                                DropdownMenuItem(
                                                    text = { Text(g, fontSize = 13.sp,
                                                        color = if (selectedGroup == g) NukeOrange else DarkOnBackground) },
                                                    onClick = { selectedGroup = g; showGroupPicker = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            // 搜索输入框
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("选择城市", fontSize = 12.sp, color = DarkOnSurfaceVariant.copy(alpha = 0.6f)) },
                                leadingIcon = { Icon(Icons.Filled.Search, null,
                                    modifier = Modifier.size(16.dp), tint = NukeOrange) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" },
                                            modifier = Modifier.size(16.dp)) {
                                            Icon(Icons.Filled.Clear, "清除",
                                                modifier = Modifier.size(14.dp), tint = DarkOnSurfaceVariant)
                                        }
                                    }
                                },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NukeOrange, unfocusedBorderColor = DarkOutline,
                                    cursorColor = NukeOrange,
                                    focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant
                                ),
                                modifier = Modifier.weight(0.5f)
                            )
                        }
                    }

                    HorizontalDivider(color = DarkOutline.copy(alpha = 0.5f), thickness = 0.5.dp)

                    if (displayList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("未找到匹配城市", fontSize = 13.sp, color = DarkOnSurfaceVariant.copy(alpha = 0.6f)) }
                    } else {
                        Text("${displayList.size} 个城市",
                            fontSize = 11.sp, color = DarkOnSurfaceVariant.copy(alpha = 0.6f),
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
                                                .background(NukeOrange.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) { Icon(Icons.Filled.LocationOn, null,
                                            modifier = Modifier.size(14.dp), tint = NukeOrange) }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(city.display ?: city.name, fontSize = 13.sp,
                                                color = DarkOnBackground)
                                            Text("${city.group} · ${formatCompactNumber((city.pop * 10000).toInt())}人",
                                                fontSize = 11.sp, color = DarkOnSurfaceVariant.copy(alpha = 0.6f))
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

/**
 * 根据经纬度从城市列表中查找最近的匹配城市名
 * 优先精确匹配，其次模糊匹配（0.5° 范围内的最近城市）
 */
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

/** 格式化人口数字为简洁显示（如 1.2M、500K） */
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
