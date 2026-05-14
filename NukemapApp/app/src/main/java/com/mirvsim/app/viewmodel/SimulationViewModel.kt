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

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainUiEvent>()
    val events: SharedFlow<MainUiEvent> = _events.asSharedFlow()

    // 依赖注入 - 使用简单的手动 DI
    private val cityRepository: CityRepository = CityRepositoryImpl(application)
    private val simulationUseCase = SimulationUseCase()
    private var cityDatabase: CityDatabase? = null

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE)
        as ConnectivityManager

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

    // === 状态更新方法 ===

    fun updateWarheadCount(count: Int) {
        _uiState.update { it.copy(warheadCount = count, activePresetId = null) }
    }

    fun updateYield(yieldKt: Double) {
        _uiState.update { it.copy(yieldKt = yieldKt, activePresetId = null) }
    }

    fun updateSeparation(separationKm: Double) {
        _uiState.update { it.copy(separationKm = separationKm, activePresetId = null) }
    }

    fun updatePattern(pattern: String) {
        _uiState.update { it.copy(pattern = pattern, activePresetId = null) }
    }

    fun updateHobMode(hobMode: String) {
        _uiState.update { it.copy(hobMode = hobMode, activePresetId = null) }
    }

    fun updateTargetType(targetType: String) {
        _uiState.update { it.copy(targetType = targetType, activePresetId = null) }
    }

    fun updateTargetLocation(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                targetLat = lat,
                targetLng = lng,
                isPickMode = false
            )
        }
        viewModelScope.launch {
            _events.emit(MainUiEvent.ShowToast("目标已设置为: %.4f°, %.4f°".format(lat, lng)))
        }
    }

    fun togglePickMode() {
        _uiState.update { it.copy(isPickMode = !it.isPickMode) }
    }

    fun toggleControlDrawer() {
        _uiState.update { it.copy(controlDrawerOpen = !it.controlDrawerOpen) }
    }

    fun toggleStatsSheet() {
        _uiState.update { it.copy(statsSheetOpen = !it.statsSheetOpen) }
    }

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
                controlDrawerOpen = false
            )
        }
        executeSimulation()
    }

    fun applyCity(city: City) {
        _uiState.update {
            it.copy(
                targetLat = city.lat,
                targetLng = city.lng,
                controlDrawerOpen = false
            )
        }
    }

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

    fun executeSimulation() {
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
                        showStats = true,
                        isComputing = false,
                        statsSheetOpen = true
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

    fun resetAll() {
        _uiState.update {
            MainUiState(cityList = it.cityList)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // === 导航方法 ===

    fun navigateTo(route: BottomNavItem) {
        _uiState.update { it.copy(currentNavRoute = route) }

        when (route) {
            BottomNavItem.SIMULATE -> {
                // 点击模拟：切换控制面板
                toggleControlDrawer()
            }
            BottomNavItem.PRESETS -> {
                // 点击预设：打开控制面板并定位到预设区域
                if (!_uiState.value.controlDrawerOpen) {
                    toggleControlDrawer()
                }
            }
            BottomNavItem.STATS -> {
                // 点击统计：打开统计详情
                if (_uiState.value.simulationResult != null) {
                    _uiState.update { it.copy(showStats = true, statsSheetOpen = true) }
                } else {
                    viewModelScope.launch {
                        _events.emit(MainUiEvent.ShowToast("请先运行模拟"))
                    }
                }
            }
            BottomNavItem.SETTINGS -> {
                viewModelScope.launch {
                    _events.emit(MainUiEvent.NavigateToLogs)
                }
            }
        }
    }

    fun toggleSideNav() {
        _uiState.update { it.copy(sideNavExpanded = !it.sideNavExpanded) }
    }
}
