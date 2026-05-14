package com.mirvsim.app.domain.usecase

import com.mirvsim.app.engine.CityDatabase
import com.mirvsim.app.engine.MIRVPatterns
import com.mirvsim.app.engine.NukeEffects
import com.mirvsim.app.engine.StatsCalculator
import com.mirvsim.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 模拟执行用例
 * 封装核打击模拟的核心业务逻辑
 */
class SimulationUseCase {
    
    /**
     * 执行核打击模拟
     * 
     * @param warheadCount 弹头数量
     * @param yieldKt 单弹头当量 (kt)
     * @param separationKm 弹头分离距离
     * @param pattern 散布模式
     * @param hobMode 爆高模式
     * @param targetLat 目标纬度
     * @param targetLng 目标经度
     * @param targetType 目标类型
     * @param cityDatabase 城市数据库
     * @return 模拟结果
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
        // 1. 生成弹头落点
        val rawPoints = MIRVPatterns.generate(
            pattern,
            warheadCount,
            targetLat,
            targetLng,
            separationKm
        )
        
        // 2. 计算核爆效应
        val effects = NukeEffects.calculate(yieldKt, hobMode)
        
        // 3. 转换为弹头点列表
        val hobMVal = when (hobMode) {
            "surface" -> 0.0
            "optimal" -> 600.0
            "custom" -> 600.0
            else -> 600.0
        }
        
        val warheadPoints = rawPoints.mapIndexed { index, pt ->
            WarheadPoint(index, pt.lat, pt.lng, effects, yieldKt, hobMVal)
        }
        
        // 4. 计算毁伤统计
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
        
        // 5. 构建毁伤效应列表
        val effectsList = buildEffectsList(warheadPoints, targetType)
        
        // 6. 返回完整结果
        stats.copy(effectsList = effectsList)
    }
    
    /**
     * 快速计算弹头落点
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
    
    private fun buildEffectsList(
        points: List<WarheadPoint>,
        targetType: String
    ): List<NukeEffectsResult> {
        return points.map { wp ->
            val rings = listOf(
                RingResult(wp.effects.fireball, 0xFFFFD700),
                RingResult(wp.effects.psi20, 0xFFE53935),
                RingResult(wp.effects.psi10, 0xFFF4511E),
                RingResult(wp.effects.psi5, 0xFFFF8F00),
                RingResult(wp.effects.psi3, 0xFF00BCD4),
                RingResult(wp.effects.psi1, 0xFF7CB342),
                RingResult(wp.effects.thermal, 0xFFE040FB)
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
    
    private fun parseTargetType(t: String): TargetType = when (t) {
        "urban" -> TargetType.urban
        "suburban" -> TargetType.suburban
        "rural" -> TargetType.rural
        else -> TargetType.urban
    }
}
