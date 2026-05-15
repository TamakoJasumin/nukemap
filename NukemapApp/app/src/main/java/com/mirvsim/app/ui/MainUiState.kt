/**
 * 主界面 UI 状态定义
 *
 * 使用不可变 data class 集中管理所有 UI 状态，确保：
 * - 状态变化可追踪（单一数据源）
 * - 线程安全（不可变性）
 * - 重组可预测（StateFlow + collectAsStateWithLifecycle）
 *
 * 状态分类：
 * - 模拟参数（弹头配置、目标位置）
 * - 模拟结果（弹头落点、统计结果）
 * - UI 交互状态（面板展开/收起、导航）
 * - 系统状态（网络可用性、错误信息）
 * - 设置参数（主题、地图、动画等）
 */
package com.mirvsim.app.ui

import com.mirvsim.app.model.City
import com.mirvsim.app.model.SimulationResult
import com.mirvsim.app.model.WarheadPoint
import com.mirvsim.app.ui.components.BottomNavItem

data class MainUiState(
    // ========== 模拟参数 ==========
    val warheadCount: Int = 1,         // 弹头数量（默认 1 枚）
    val yieldKt: Double = 50000.0,     // 单弹头当量，单位 kt（默认最大当量 50Mt）
    val separationKm: Double = 1.5,    // 弹头分离距离，单位 km
    val pattern: String = "circular",   // 散布模式
    val hobMode: String = "optimal",    // 爆高模式
    val targetType: String = "urban",   // 目标类型

    // ========== 目标位置 ==========
    val targetLat: Double = 0.0,       // 目标纬度（初始未设置，由 GPS 或用户选择确定）
    val targetLng: Double = 0.0,       // 目标经度（初始未设置，由 GPS 或用户选择确定）
    val myLat: Double = 0.0,           // 设备当前位置纬度
    val myLng: Double = 0.0,           // 设备当前位置经度
    val myLocationTrigger: Int = 0,     // 触发回到我的位置（计数器）

    // ========== 模拟结果 ==========
    val warheadPoints: List<WarheadPoint> = emptyList(),   // 弹头落点列表
    val simulationResult: SimulationResult? = null,         // 模拟计算结果
    val showStats: Boolean = false,                         // 是否显示统计

    // ========== UI 交互状态 ==========
    val isComputing: Boolean = false,            // 是否正在计算
    val isPickMode: Boolean = false,             // 地图点选模式
    val controlDrawerOpen: Boolean = false,      // 控制面板展开（手机端）
    val presetsDrawerOpen: Boolean = false,       // 预设面板展开（手机端）
    val statsSheetOpen: Boolean = false,         // 统计面板展开
    val settingsOpen: Boolean = false,           // 设置面板展开
    val activePresetId: String? = null,          // 当前选中的预设 ID

    // ========== 导航状态 ==========
    val currentNavRoute: BottomNavItem = BottomNavItem.SIMULATE,  // 当前路由
    val sideNavExpanded: Boolean = true,                          // 侧边栏展开（平板端）

    // ========== 系统状态 ==========
    val isNetworkAvailable: Boolean = true,     // 网络是否可用
    val errorMessage: String? = null,           // 错误信息

    // ========== 数据 ==========
    val cityList: List<City> = emptyList(),     // 城市列表

    // ========== 设置参数 ==========
    val isDarkTheme: Boolean = false,            // 深色主题
    val useDynamicColor: Boolean = true,         // 动态色彩（Android 12+）
    val tileSource: String = "AUTONAVI",          // 地图瓦片源
    val popupEnabled: Boolean = true,            // 地图点击弹窗
    val autoLaunchPreset: Boolean = true,        // 选择预设后自动运行
    val ringAnimation: Boolean = true            // 环展开动画
) {
    /** 是否有模拟结果（便捷计算属性） */
    val hasSimulationResult: Boolean get() = simulationResult != null
}

/**
 * UI 一次性事件
 *
 * 用于处理不需要持久化状态的一次性事件（如 Toast 提示、导航等）。
 * 使用 SharedFlow 发射，确保事件不会被重复消费。
 */
sealed interface MainUiEvent {
    /** 显示 Toast 提示消息 */
    data class ShowToast(val message: String) : MainUiEvent
    /** 显示错误提示 */
    data class ShowError(val message: String) : MainUiEvent
    /** 请求刷新位置 */
    data object RefreshLocation : MainUiEvent
}
