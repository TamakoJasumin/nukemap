/**
 * 模拟执行用例（Use Case）
 *
 * 封装核打击模拟的核心业务逻辑，协调各引擎组件完成完整的模拟流程：
 * 1. 生成弹头散布落点（MIRVPatterns）
 * 2. 计算核爆效应参数（NukeEffects）
 * 3. 计算毁伤统计（StatsCalculator）
 * 4. 构建地图渲染数据
 *
 * 所有计算在 Default 调度器上异步执行，避免阻塞主线程。
 * 遵循 Clean Architecture 的用例层设计原则。
 */
package com.mirvsim.app.domain.usecase

import com.mirvsim.app.engine.CityDatabase
import com.mirvsim.app.engine.MIRVPatterns
import com.mirvsim.app.engine.NukeEffects
import com.mirvsim.app.engine.StatsCalculator
import com.mirvsim.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SimulationUseCase {
    
    /**
     * 执行完整的核打击模拟
     *
     * 模拟流程：
     * 1. 根据散布模式生成各弹头的经纬度落点
     * 2. 计算核爆的各级毁伤半径参数
     * 3. 将落点和毁伤参数组合为弹头点数据
     * 4. 使用网格采样法计算覆盖面积和人口伤亡
     * 5. 构建各弹头的毁伤环数据集（用于地图绘制）
     *
     * @param warheadCount 弹头数量
     * @param yieldKt 单弹头当量 (kt)
     * @param separationKm 弹头分离距离 (km)
     * @param pattern 散布模式标识
     * @param hobMode 爆高模式标识
     * @param targetLat 目标纬度
     * @param targetLng 目标经度
     * @param targetType 目标类型标识
     * @param cityDatabase 城市数据库（可选，用于人口密度校准）
     * @return 完整的模拟结果
     */
    suspend fun executeSimulation(
        warheadCount: Int,
        yieldKt: Double,
        separationKm: Double,
        pattern: String,
        hobMode: String,
        targetLat: Double,
        targetLng: Double,
        targetType: String,
        cityDatabase: CityDatabase?
    ): SimulationResult = withContext(Dispatchers.Default) {
        // Step 1: 根据散布模式生成弹头落点坐标
        val rawPoints = MIRVPatterns.generate(
            pattern,
            warheadCount,
            targetLat,
            targetLng,
            separationKm
        )
        
        // Step 2: 根据当量和爆高模式计算核爆效应参数
        val effects = NukeEffects.calculate(yieldKt, hobMode)
        
        // Step 3: 确定爆高值，转换为 WarheadPoint 列表
        val hobMVal = when (hobMode) {
            "surface" -> 0.0   // 地爆：爆高为 0
            "optimal" -> 600.0 // 空爆（最佳高度）：600m
            "custom" -> 600.0  // 自定义：暂固定为 600m
            else -> 600.0
        }
        
        val warheadPoints = rawPoints.mapIndexed { index, pt ->
            WarheadPoint(index, pt.lat, pt.lng, effects, yieldKt, hobMVal)
        }
        
        // Step 4: 使用网格采样法进行毁伤统计计算
        val targetTypeEnum = parseTargetType(targetType)
        val stats = StatsCalculator.compute(
            warheadPoints,
            yieldKt,
            hobMode,
            targetTypeEnum,
            targetLat,
            targetLng,
            cityDatabase
        )
        
        // Step 5: 构建各弹头的毁伤环数据（用于 osmdroid 地图渲染）
        val effectsList = buildEffectsList(warheadPoints, targetType)
        
        // 返回完整结果（包含地图渲染数据和统计结果）
        stats.copy(effectsList = effectsList)
    }
    
    /**
     * 仅计算弹头落点（不执行完整模拟）
     *
     * 用于在地图上快速预览弹头散布效果，不进行面积和伤亡统计。
     *
     * @return 弹头落点列表
     */
    suspend fun calculateWarheadPoints(
        count: Int,
        yieldKt: Double,
        pattern: String,
        targetLat: Double,
        targetLng: Double,
        separationKm: Double,
        hobMode: String
    ): List<WarheadPoint> = withContext(Dispatchers.Default) {
        val rawPoints = MIRVPatterns.generate(
            pattern, count, targetLat, targetLng, separationKm
        )
        val effects = NukeEffects.calculate(yieldKt, hobMode)
        val hobMVal = when (hobMode) {
            "surface" -> 0.0
            "optimal" -> 600.0
            else -> 600.0
        }
        rawPoints.mapIndexed { index, pt ->
            WarheadPoint(index, pt.lat, pt.lng, effects, yieldKt, hobMVal)
        }
    }
    
    /**
     * 构建各弹头的毁伤环数据集
     *
     * 为每个弹头生成包含各级毁伤半径（RingResult）的 NukeEffectsResult 列表，
     * 包含环形半径和对应的 ARGB 颜色值，用于 osmdroid Polygon 渲染。
     *
     * @param points 弹头落点列表
     * @param targetType 目标类型标识
     * @return 毁伤环数据列表
     */
    private fun buildEffectsList(
        points: List<WarheadPoint>,
        targetType: String
    ): List<NukeEffectsResult> {
        return points.map { wp ->
            val rings = listOf(
                RingResult(wp.effects.fireball, 0xFFFFD700),  // 金色
                RingResult(wp.effects.psi20, 0xFFE53935),      // 红色
                RingResult(wp.effects.psi10, 0xFFF4511E),      // 橙色
                RingResult(wp.effects.psi5, 0xFFFF8F00),       // 深黄
                RingResult(wp.effects.psi3, 0xFF00BCD4),       // 青色
                RingResult(wp.effects.psi1, 0xFF7CB342),       // 绿色
                RingResult(wp.effects.thermal, 0xFFE040FB)     // 紫色
            )
            val outerRadius = rings.maxOf { it.outerRadiusKm }
            val totalArea = Math.PI * outerRadius * outerRadius
            NukeEffectsResult(
                rings = rings,
                centerLat = wp.lat,
                centerLng = wp.lng,
                totalArea = totalArea,
                targetType = targetType
            )
        }
    }
    
    /** 将字符串类型标识解析为目标类型枚举 */
    private fun parseTargetType(t: String): TargetType = when (t) {
        "urban" -> TargetType.urban
        "suburban" -> TargetType.suburban
        "rural" -> TargetType.rural
        else -> TargetType.urban
    }
}
