package com.mirvsim.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirvsim.app.model.*
import com.mirvsim.app.ui.components.BottomNavigationBar
import com.mirvsim.app.ui.components.BottomNavItem
import com.mirvsim.app.ui.components.ControlPanel
import com.mirvsim.app.ui.components.MapView
import com.mirvsim.app.ui.components.SideNavigationRail
import com.mirvsim.app.ui.components.SideDrawer
import com.mirvsim.app.ui.components.StatsPanel
import com.mirvsim.app.ui.theme.*
import com.mirvsim.app.viewmodel.SimulationViewModel
import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun LogViewerDialog(onDismiss: () -> Unit) {
    var logs by remember { mutableStateOf("正在读取日志...") }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec("logcat -d")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.contains("com.mirvsim.app") == true || line?.contains("FATAL") == true) {
                        output.append(line).append("\n")
                    }
                }
                logs = if (output.isEmpty()) "没有找到相关日志" else output.toString()
            } catch (e: Exception) {
                logs = "读取失败: ${e.message}"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = BgSecondary
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("系统日志 (过滤: MIRV Sim)", fontWeight = FontWeight.Bold, color = TextPrimary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, null, tint = TextSecondary)
                    }
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(BgPrimary, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    val scrollState = rememberScrollState()
                    Text(
                        text = logs,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }
            }
        }
    }
}

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
    var showLogDialog by remember { mutableStateOf(false) }

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
                is MainUiEvent.NavigateToLogs -> {
                    showLogDialog = true
                }
            }
        }
    }

    if (showLogDialog) {
        LogViewerDialog(onDismiss = { showLogDialog = false })
    }

    val handleCitySelect: (City) -> Unit = { city ->
        viewModel.applyCity(city)
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
        containerColor = BgPrimary
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 错误提示
            uiState.errorMessage?.let { error ->
                Surface(
                    color = Danger.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            error,
                            color = Danger,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Close, "关闭", tint = Danger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // 网络不可用提示
            if (!uiState.isNetworkAvailable) {
                Surface(
                    color = Warning.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.WifiOff, null, tint = Warning, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "网络不可用，地图可能无法加载",
                            color = Warning,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 主内容区
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isExpanded -> ExpandedLayout(
                        uiState = uiState,
                        viewModel = viewModel,
                        handleCitySelect = handleCitySelect,
                        handleMapClick = handleMapClick,
                        handleShare = handleShare,
                        onPresetApply = onPresetApply
                    )
                    else -> CompactMediumLayout(
                        uiState = uiState,
                        viewModel = viewModel,
                        handleCitySelect = handleCitySelect,
                        handleMapClick = handleMapClick,
                        handleShare = handleShare,
                        onPresetApply = onPresetApply
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedLayout(
    uiState: MainUiState,
    viewModel: SimulationViewModel,
    handleCitySelect: (City) -> Unit,
    handleMapClick: (Double, Double) -> Unit,
    handleShare: () -> Unit,
    onPresetApply: (Preset) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // 侧边导航栏（仅在展开时显示完整侧边栏）
        SideNavigationRail(
            currentRoute = uiState.currentNavRoute,
            onItemSelected = viewModel::navigateTo,
            hasSimulationResult = uiState.hasSimulationResult,
            modifier = Modifier.statusBarsPadding()
        )

        // 可折叠侧边栏
        SideDrawer(
            currentRoute = uiState.currentNavRoute,
            onItemSelected = viewModel::navigateTo,
            hasSimulationResult = uiState.hasSimulationResult,
            isExpanded = uiState.sideNavExpanded,
            onExpandToggle = viewModel::toggleSideNav,
            modifier = Modifier
        )

        ControlPanel(
            warheadCount = uiState.warheadCount,
            yieldKt = uiState.yieldKt,
            separationKm = uiState.separationKm,
            pattern = uiState.pattern,
            hobMode = uiState.hobMode,
            targetLat = uiState.targetLat,
            targetLng = uiState.targetLng,
            targetType = uiState.targetType,
            activePresetId = uiState.activePresetId,
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
            onPresetApply = onPresetApply,
            onPickOnMap = viewModel::togglePickMode,
            onLaunch = { viewModel.executeSimulation() },
            onClear = { viewModel.clearResult() },
            onReset = { viewModel.resetAll() },
            onShare = handleShare,
            modifier = Modifier.widthIn(max = 300.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            MapView(
                targetLat = uiState.targetLat,
                targetLng = uiState.targetLng,
                warheadPoints = uiState.warheadPoints,
                effects = uiState.simulationResult?.effectsList,
                pickMode = uiState.isPickMode,
                onMapClick = handleMapClick,
                modifier = Modifier.fillMaxSize()
            )

            // 大屏幕设备显示展开/收起侧边栏按钮
            FloatingActionButton(
                onClick = viewModel::toggleSideNav,
                containerColor = BgSecondary,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(8.dp)
                    .size(32.dp)
            ) {
                Icon(
                    if (uiState.sideNavExpanded) Icons.Filled.Close else Icons.Filled.Menu,
                    contentDescription = "切换侧边栏",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.showStats && uiState.simulationResult != null,
            enter = slideInHorizontally(animationSpec = tween(350)) { it },
            exit = slideOutHorizontally(animationSpec = tween(300)) { it }
        ) {
            uiState.simulationResult?.let { r ->
                StatsPanel(
                    result = r,
                    warheadPoints = uiState.warheadPoints,
                    onClose = { viewModel.clearResult() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactMediumLayout(
    uiState: MainUiState,
    viewModel: SimulationViewModel,
    handleCitySelect: (City) -> Unit,
    handleMapClick: (Double, Double) -> Unit,
    handleShare: () -> Unit,
    onPresetApply: (Preset) -> Unit
) {
        Box(modifier = Modifier.fillMaxSize()) {
            MapView(
                targetLat = uiState.targetLat,
                targetLng = uiState.targetLng,
                warheadPoints = uiState.warheadPoints,
                effects = uiState.simulationResult?.effectsList,
                pickMode = uiState.isPickMode,
                onMapClick = handleMapClick,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
            // 统计摘要
            AnimatedVisibility(
                visible = uiState.showStats && uiState.simulationResult != null,
                enter = slideInVertically(animationSpec = tween(350)) { it },
                exit = slideOutVertically(animationSpec = tween(300)) { it }
            ) {
                uiState.simulationResult?.let { r ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BgSecondary)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "攻击结果统计",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    StatMiniCard("死亡", formatCompactNumber(r.deaths), Danger)
                                    StatMiniCard("受伤", formatCompactNumber(r.injuries), Warning)
                                    StatMiniCard("面积", formatCompactArea(r.totalArea), Accent)
                                }
                                IconButton(
                                    onClick = { viewModel.clearResult() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Close, "关闭", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 底部操作栏（与底部导航栏配合使用）
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
                    // 模拟按钮
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
                    // 清除按钮
                    OutlinedButton(
                        onClick = { viewModel.clearResult() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.6f)
                    ) {
                        Text("清除", fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    // 重置按钮
                    IconButton(onClick = { viewModel.resetAll() }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Refresh, "重置", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    // 分享按钮
                    IconButton(onClick = handleShare, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Share, "分享", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    // 控制面板抽屉
    if (uiState.controlDrawerOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleControlDrawer() },
            containerColor = BgSecondary,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) },
            tonalElevation = 0.dp
        ) {
            ControlPanel(
                warheadCount = uiState.warheadCount,
                yieldKt = uiState.yieldKt,
                separationKm = uiState.separationKm,
                pattern = uiState.pattern,
                hobMode = uiState.hobMode,
                targetLat = uiState.targetLat,
                targetLng = uiState.targetLng,
                targetType = uiState.targetType,
                activePresetId = uiState.activePresetId,
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
                onPresetApply = onPresetApply,
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
                        viewModel.clearResult()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatMiniCard(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Text(
            label,
            color = TextMuted,
            fontSize = 9.sp
        )
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

private fun formatCompactArea(area: Double): String {
    return when {
        area >= 10_000 -> "%.0fK km²".format(area / 1_000)
        area >= 1_000 -> "%.1fK km²".format(area / 1_000)
        area >= 1 -> "%.0f km²".format(area)
        else -> "%.2f km²".format(area)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MainScreenPreview() {
    NukemapTheme {
        MainScreen()
    }
}
