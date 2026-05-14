package com.mirvsim.app.ui

import com.mirvsim.app.model.City
import com.mirvsim.app.model.SimulationResult
import com.mirvsim.app.model.WarheadPoint
import com.mirvsim.app.ui.components.BottomNavItem

/**
 * 主界面 UI 状态
 * 使用不可变数据类管理所有 UI 状态
 */
data class MainUiState(
    // === 模拟参数 ===
    val warheadCount: Int = 4,
    val yieldKt: Double = 150.0,
    val separationKm: Double = 1.5,
    val pattern: String = "circular",
    val hobMode: String = "optimal",
    val targetType: String = "urban",

    // === 目标位置 ===
    val targetLat: Double = 39.9042,
    val targetLng: Double = 116.4074,

    // === 模拟结果 ===
    val warheadPoints: List<WarheadPoint> = emptyList(),
    val simulationResult: SimulationResult? = null,
    val showStats: Boolean = false,

    // === UI 交互状态 ===
    val isComputing: Boolean = false,
    val isPickMode: Boolean = false,
    val controlDrawerOpen: Boolean = false,
    val statsSheetOpen: Boolean = false,
    val activePresetId: String? = null,

    // === 导航状态 ===
    val currentNavRoute: BottomNavItem = BottomNavItem.SIMULATE,
    val sideNavExpanded: Boolean = true,

    // === 系统状态 ===
    val isNetworkAvailable: Boolean = true,
    val errorMessage: String? = null,

    // === 数据 ===
    val cityList: List<City> = emptyList()
) {
    val hasSimulationResult: Boolean get() = simulationResult != null
}

/**
 * UI 一次性事件
 * 用于处理 Toast、导航等一次性事件
 */
sealed interface MainUiEvent {
    data class ShowToast(val message: String) : MainUiEvent
    data class ShowError(val message: String) : MainUiEvent
    object NavigateToLogs : MainUiEvent
}
