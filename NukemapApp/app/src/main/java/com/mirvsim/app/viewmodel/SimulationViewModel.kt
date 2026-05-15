/**
 * 模拟计算 ViewModel
 *
 * 应用核心 ViewModel，负责：
 * 1. 管理 UI 状态（MainUiState）通过 StateFlow 对外暴露
 * 2. 处理所有用户交互事件（参数调整、模拟执行、导航等）
 * 3. 协调业务逻辑（SimulationUseCase）执行异步计算
 * 4. 管理城市数据加载和网络状态监听
 * 5. 发送一次性事件（Toast、错误提示）通过 SharedFlow
 *
 * 继承 AndroidViewModel 以获取 Application Context，
 * 用于访问 ConnectivityManager 和资源文件。
 */
package com.mirvsim.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mirvsim.app.data.repository.CityRepositoryImpl
import com.mirvsim.app.domain.repository.CityRepository
import com.mirvsim.app.domain.usecase.SimulationUseCase
import com.mirvsim.app.engine.CityDatabase
import com.mirvsim.app.model.*
import com.mirvsim.app.ui.MainUiEvent
import com.mirvsim.app.ui.MainUiState
import com.mirvsim.app.ui.components.BottomNavItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SimulationViewModel(application: Application) : AndroidViewModel(application) {

    // ========== 状态管理 ==========

    /** UI 状态 — 通过 StateFlow 对外暴露，供 Compose 层收集 */
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /** 一次性事件 — 用于 Toast 和错误提示 */
    private val _events = MutableSharedFlow<MainUiEvent>()
    val events: SharedFlow<MainUiEvent> = _events.asSharedFlow()

    // ========== 依赖 ==========

    private val cityRepository: CityRepository = CityRepositoryImpl(application)
    private val simulationUseCase = SimulationUseCase()
    private var cityDatabase: CityDatabase? = null

    // ========== 网络状态监听 ==========

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE)
        as ConnectivityManager

    /** 网络状态回调 — 实时监听网络连接变化 */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            viewModelScope.launch {
                _uiState.update { it.copy(isNetworkAvailable = true) }
            }
        }
        override fun onLost(network: Network) {
            viewModelScope.launch {
                _uiState.update { it.copy(isNetworkAvailable = false) }
            }
        }
    }

    init {
        loadCities()
        registerNetworkCallback()
    }

    /** 注册网络状态监听器 */
    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun onCleared() {
        super.onCleared()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    /** 从仓库加载城市数据并初始化 CityDatabase */
    private fun loadCities() {
        viewModelScope.launch {
            try {
                val cities = cityRepository.getAllCities()
                cityDatabase = CityDatabase(cities)
                _uiState.update { it.copy(cityList = cities) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "城市数据加载失败: ${e.message}") }
            }
        }
    }

    // ========== 模拟参数更新方法 ==========

    /** 更新弹头数量，清除预设选中状态 */
    fun updateWarheadCount(count: Int) {
        _uiState.update { it.copy(warheadCount = count, activePresetId = null) }
    }

    /** 更新单弹头当量，清除预设选中状态 */
    fun updateYield(yieldKt: Double) {
        _uiState.update { it.copy(yieldKt = yieldKt, activePresetId = null) }
    }

    /** 更新弹头分离距离，清除预设选中状态 */
    fun updateSeparation(separationKm: Double) {
        _uiState.update { it.copy(separationKm = separationKm, activePresetId = null) }
    }

    /** 更新散布模式，清除预设选中状态 */
    fun updatePattern(pattern: String) {
        _uiState.update { it.copy(pattern = pattern, activePresetId = null) }
    }

    /** 更新爆高模式，清除预设选中状态 */
    fun updateHobMode(hobMode: String) {
        _uiState.update { it.copy(hobMode = hobMode, activePresetId = null) }
    }

    /** 更新目标类型，清除预设选中状态 */
    fun updateTargetType(targetType: String) {
        _uiState.update { it.copy(targetType = targetType, activePresetId = null) }
    }

    /** 更新目标位置，关闭拾取模式并显示 Toast */
    fun updateTargetLocation(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                targetLat = lat,
                targetLng = lng,
                isPickMode = false  // 关闭地图点选模式
            )
        }
        viewModelScope.launch {
            _events.emit(MainUiEvent.ShowToast("目标已设置为: %.4f°, %.4f°".format(lat, lng)))
        }
    }

    // ========== UI 交互方法 ==========

    /** 切换地图点选模式 */
    fun togglePickMode() {
        _uiState.update { it.copy(isPickMode = !it.isPickMode) }
    }

    /** 切换控制面板（手机端底部抽屉） */
    fun toggleControlDrawer() {
        _uiState.update { it.copy(controlDrawerOpen = !it.controlDrawerOpen) }
    }

    /** 切换预设面板（手机端底部抽屉） */
    fun togglePresetsDrawer() {
        _uiState.update { it.copy(presetsDrawerOpen = !it.presetsDrawerOpen) }
    }

    /** 切换设置面板 */
    fun toggleSettings() {
        _uiState.update { it.copy(settingsOpen = !it.settingsOpen) }
    }

    /** 切换统计详情面板 */
    fun toggleStatsSheet() {
        _uiState.update { it.copy(statsSheetOpen = !it.statsSheetOpen) }
    }

    /**
     * 应用预设场景配置
     *
     * 将预设的弹头参数、目标位置等批量应用到 UI 状态。
     * 如果开启了自动发射（autoLaunchPreset），则自动执行模拟。
     */
    fun applyPreset(preset: Preset) {
        _uiState.update {
            it.copy(
                warheadCount = preset.count,
                yieldKt = preset.yield,
                separationKm = preset.separation,
                pattern = preset.pattern,
                hobMode = preset.hob,
                targetType = preset.target,
                targetLat = preset.lat,
                targetLng = preset.lng,
                activePresetId = preset.id,
                controlDrawerOpen = false,
                presetsDrawerOpen = false,
                showStats = false,
                statsSheetOpen = false
            )
        }
        if (_uiState.value.autoLaunchPreset) {
            executeSimulation(showResult = false)
        }
    }

    /** 应用城市选择（旧版，保留兼容性） */
    fun applyCity(city: City) {
        _uiState.update {
            it.copy(
                targetLat = city.lat,
                targetLng = city.lng,
                controlDrawerOpen = false
            )
        }
    }

    /** 选择城市作为目标位置 */
    fun selectCity(city: City) {
        _uiState.update {
            it.copy(
                targetLat = city.lat,
                targetLng = city.lng
            )
        }
    }

    /** 更新到当前位置（GPS 定位结果） */
    fun updateToCurrentLocation(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                targetLat = lat,
                targetLng = lng
            )
        }
        viewModelScope.launch {
            _events.emit(MainUiEvent.ShowToast("已定位到当前城市"))
        }
    }

    // ========== 模拟执行 ==========

    /**
     * 执行核打击模拟计算
     *
     * 在 Default 调度器上执行耗时的计算任务。
     * 计算完成后更新 UI 状态，包括模拟结果和弹头落点数据。
     *
     * @param showResult 是否在计算完成后显示结果界面
     */
    fun executeSimulation(showResult: Boolean = true) {
        viewModelScope.launch {
            val currentState = _uiState.value

            _uiState.update { it.copy(isComputing = true, errorMessage = null) }

            try {
                val result = withContext(Dispatchers.Default) {
                    simulationUseCase.executeSimulation(
                        warheadCount = currentState.warheadCount,
                        yieldKt = currentState.yieldKt,
                        separationKm = currentState.separationKm,
                        pattern = currentState.pattern,
                        hobMode = currentState.hobMode,
                        targetLat = currentState.targetLat,
                        targetLng = currentState.targetLng,
                        targetType = currentState.targetType,
                        cityDatabase = cityDatabase
                    )
                }

                // 从计算结果中提取弹头落点数据用于地图渲染
                _uiState.update {
                    it.copy(
                        simulationResult = result,
                        warheadPoints = result.effectsList.mapIndexed { index, effect ->
                            WarheadPoint(
                                index = index,
                                lat = effect.centerLat,
                                lng = effect.centerLng,
                                effects = DamageEffects(
                                    fireball = effect.rings.getOrNull(0)?.outerRadiusKm ?: 0.0,
                                    psi20 = effect.rings.getOrNull(1)?.outerRadiusKm ?: 0.0,
                                    psi10 = effect.rings.getOrNull(2)?.outerRadiusKm ?: 0.0,
                                    psi5 = effect.rings.getOrNull(3)?.outerRadiusKm ?: 0.0,
                                    psi3 = effect.rings.getOrNull(4)?.outerRadiusKm ?: 0.0,
                                    psi1 = effect.rings.getOrNull(5)?.outerRadiusKm ?: 0.0,
                                    thermal = effect.rings.getOrNull(6)?.outerRadiusKm ?: 0.0,
                                    radiation = effect.rings.getOrNull(4)?.outerRadiusKm ?: 0.0
                                ),
                                yieldKt = currentState.yieldKt,
                                hobMeters = when (currentState.hobMode) {
                                    "surface" -> 0.0
                                    "optimal" -> 600.0
                                    else -> 600.0
                                }
                            )
                        },
                        showStats = showResult,
                        isComputing = false,
                        statsSheetOpen = showResult
                    )
                }

                _events.emit(MainUiEvent.ShowToast("模拟完成: ${currentState.warheadCount} 枚弹头已投放"))

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isComputing = false,
                        errorMessage = "模拟计算失败: ${e.message}"
                    )
                }
                _events.emit(MainUiEvent.ShowError("模拟计算失败: ${e.message}"))
            }
        }
    }

    /** 清除模拟结果 */
    fun clearResult() {
        _uiState.update {
            it.copy(
                simulationResult = null,
                warheadPoints = emptyList(),
                showStats = false,
                statsSheetOpen = false
            )
        }
    }

    /** 重置所有参数到默认值（保留设置项） */
    fun resetAll() {
        _uiState.update {
            MainUiState(
                cityList = it.cityList,
                isDarkTheme = it.isDarkTheme,
                useDynamicColor = it.useDynamicColor,
                tileSource = it.tileSource,
                popupEnabled = it.popupEnabled,
                autoLaunchPreset = it.autoLaunchPreset,
                ringAnimation = it.ringAnimation
            )
        }
    }

    /** 清除错误信息 */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ========== 设置方法 ==========

    fun setDarkTheme(dark: Boolean) {
        _uiState.update { it.copy(isDarkTheme = dark) }
    }

    fun setDynamicColor(use: Boolean) {
        _uiState.update { it.copy(useDynamicColor = use) }
    }

    fun setTileSource(source: String) {
        _uiState.update { it.copy(tileSource = source) }
    }

    fun setPopupEnabled(enabled: Boolean) {
        _uiState.update { it.copy(popupEnabled = enabled) }
    }

    fun setAutoLaunchPreset(auto: Boolean) {
        _uiState.update { it.copy(autoLaunchPreset = auto) }
    }

    fun setRingAnimation(enabled: Boolean) {
        _uiState.update { it.copy(ringAnimation = enabled) }
    }

    // ========== 导航方法 ==========

    /**
     * 处理导航事件
     *
     * 根据选中的导航项执行对应的 UI 操作：
     * - SIMULATE: 切换控制面板
     * - PRESETS: 切换预设面板
     * - STATS: 打开统计详情（无结果时提示）
     * - SETTINGS: 切换设置面板
     */
    fun navigateTo(route: BottomNavItem) {
        _uiState.update { it.copy(currentNavRoute = route) }

        when (route) {
            BottomNavItem.SIMULATE -> {
                toggleControlDrawer()
            }
            BottomNavItem.PRESETS -> {
                togglePresetsDrawer()
            }
            BottomNavItem.STATS -> {
                if (_uiState.value.simulationResult != null) {
                    _uiState.update { it.copy(showStats = true, statsSheetOpen = true) }
                } else {
                    viewModelScope.launch {
                        _events.emit(MainUiEvent.ShowToast("请先运行模拟"))
                    }
                }
            }
            BottomNavItem.SETTINGS -> {
                toggleSettings()
            }
        }
    }

    /** 切换侧边导航栏展开/收起（平板端） */
    fun toggleSideNav() {
        _uiState.update { it.copy(sideNavExpanded = !it.sideNavExpanded) }
    }
}
