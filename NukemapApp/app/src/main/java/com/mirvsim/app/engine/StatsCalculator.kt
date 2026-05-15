/**
 * 毁伤统计计算器
 *
 * 核心计算引擎，使用网格采样法（Grid Sampling）估算多弹头核打击的：
 * - 覆盖面积（各级毁伤的实际覆盖面积）
 * - 人口伤亡（基于人口密度模型和伤亡率表）
 * - 毁伤重叠率（多弹头毁伤区域的重叠程度）
 *
 * 人口密度模型采用指数径向衰减：
 * - 有城市数据时，使用真实人口校准密度曲线
 * - 无城市数据时，使用预设的典型密度剖面
 *
 * 性能优化：
 * - 预计算 cos 值，减少三角函数的重复计算
 * - 缓存弹头距离数组，避免重复的 Haversine 计算
 * - 动态调整采样网格密度，平衡精度与性能
 */
package com.mirvsim.app.engine

import com.mirvsim.app.model.*
import kotlin.math.*

/**
 * 毁伤统计计算器（单例）
 *
 * 采用自适应网格采样法进行面积和伤亡估算。
 * 采样区域根据弹头落点范围自动确定，采样点数根据覆盖面积动态调整（500~3000 点）。
 */
object StatsCalculator {

    /**
     * 人口密度剖面参数
     *
     * @property peakDensity 峰值人口密度（人/km²），目标点处
     * @property decayScale 衰减尺度（km），密度随距离指数衰减的特征长度
     * @property backgroundDensity 背景人口密度（人/km²），远场最低密度
     */
    data class DensityProfile(
        val peakDensity: Double,
        val decayScale: Double,
        val backgroundDensity: Double
    )

    /** 三类目标的默认人口密度剖面（无城市校准数据时使用） */
    private val densityProfiles = mapOf(
        "urban" to DensityProfile(15000.0, 8.0, 500.0),    // 城区: 高密度，衰减慢
        "suburban" to DensityProfile(3000.0, 5.0, 150.0),   // 郊区: 中密度，衰减较快
        "rural" to DensityProfile(200.0, 3.0, 20.0)         // 乡村: 低密度，衰减快
    )

    /**
     * 各毁伤等级的伤亡率模型
     *
     * @property fatalityRate 致死率（0~1）
     * @property injuryRate 受伤率（0~1）
     */
    data class CasualtyRate(
        val fatalityRate: Double,
        val injuryRate: Double
    )

    /** 各级毁伤对应的伤亡率表（基于核武器效应医学研究数据） */
    private val casualtyRates = mapOf(
        RingType.fireball to CasualtyRate(1.0, 0.0),    // 火球内: 100%致死
        RingType.psi20 to CasualtyRate(0.90, 0.10),      // 20 psi: 90%致死，10%受伤
        RingType.psi10 to CasualtyRate(0.50, 0.40),      // 10 psi: 50%致死，40%受伤
        RingType.psi5 to CasualtyRate(0.15, 0.50),       // 5 psi: 15%致死，50%受伤
        RingType.psi3 to CasualtyRate(0.05, 0.35),       // 3 psi: 5%致死，35%受伤
        RingType.psi1 to CasualtyRate(0.0, 0.15),        // 1 psi: 0%致死，15%受伤
        RingType.thermal to CasualtyRate(0.0, 0.50)      // 热辐射: 0%致死，50%受伤
    )

    /** 毁伤等级优先级排序（用于确定最高覆盖等级，从重到轻） */
    val ringPriority = listOf(
        RingType.fireball,  // 最严重
        RingType.psi20,
        RingType.psi10,
        RingType.psi5,
        RingType.psi3,
        RingType.thermal,
        RingType.psi1       // 最轻
    )

    /**
     * Haversine 球面距离计算
     *
     * 计算地球上两点间的大圆距离，精度优于平面近似。
     *
     * @param lat1 点1纬度 (°)
     * @param lng1 点1经度 (°)
     * @param lat2 点2纬度 (°)
     * @param lng2 点2经度 (°)
     * @return 两点间距离 (km)
     */
    fun haversineDist(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0  // 地球平均半径 (km)
        val dLat = (lat2 - lat1) * PI / 180
        val dLng = (lng2 - lng1) * PI / 180
        val sinDLat = sin(dLat / 2)
        val sinDLng = sin(dLng / 2)
        val a = sinDLat * sinDLat +
                cos(lat1 * PI / 180) * cos(lat2 * PI / 180) *
                sinDLng * sinDLng
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /**
     * 获取某点的入口密度
     *
     * 首先尝试使用城市校准数据（城区目标且有城市数据时），
     * 否则使用默认密度剖面。
     *
     * 城市校准模型使用二维高斯分布：
     *   ρ(r) = (P / 2πσ²) × exp(-r/σ)
     * 其中 P 为都会区人口，σ 为城市半径/3.5
     *
     * @param distFromCenterKm 距目标中心的距离 (km)
     * @param targetType 目标类型
     * @param cityData 城市校准数据（可选）
     * @return 人口密度 (人/km²)
     */
    fun getDensityAtPoint(
        distFromCenterKm: Double,
        targetType: TargetType,
        cityData: CityPopulationData?
    ): Double {
        // 有城市校准数据且为城区目标时，使用真实人口校准
        if (cityData != null && targetType == TargetType.urban) {
            val scale = cityData.metroRadius / 3.5
            val peak = cityData.metroPop / (2 * PI * scale * scale)
            return max(100.0, peak * exp(-distFromCenterKm / scale))
        }
        // 使用默认密度剖面
        val profile = densityProfiles[targetType.name] ?: densityProfiles.getValue("urban")
        val density = profile.peakDensity * exp(-distFromCenterKm / profile.decayScale)
        return max(profile.backgroundDensity, density)
    }

    /**
     * 检查某点是否位于指定毁伤环内（使用预计算距离缓存）
     *
     * @param distToWarheads 该点到所有弹头的距离数组
     * @param effects 核爆效应参数
     * @param ringType 要检查的毁伤环类型
     * @return 是否在环内
     */
    private fun isPointInRingCached(
        distToWarheads: DoubleArray,
        effects: DamageEffects,
        ringType: RingType
    ): Boolean {
        val radius = getRingRadius(effects, ringType) ?: return false
        if (radius <= 0) return false
        for (dist in distToWarheads) {
            if (dist <= radius) return true
        }
        return false
    }

    /**
     * 获取某点遭受的最高毁伤等级（使用预计算距离缓存）
     *
     * 按优先级从高到低检查，返回第一个匹配的毁伤等级。
     *
     * @param distToWarheads 该点到所有弹头的距离数组
     * @param effects 核爆效应参数
     * @return 最高毁伤等级，若无覆盖返回 null
     */
    private fun getHighestDamageLevelCached(
        distToWarheads: DoubleArray,
        effects: DamageEffects
    ): RingType? {
        for (type in ringPriority) {
            if (isPointInRingCached(distToWarheads, effects, type)) {
                return type
            }
        }
        return null
    }

    /**
     * 执行完整的毁伤统计计算
     *
     * 计算流程：
     * 1. 确定采样区域边界（弹头落点范围 + 最大毁伤半径的扩展区域）
     * 2. 自适应确定采样网格密度
     * 3. 对每个网格点：
     *    a. 计算到所有弹头的距离
     *    b. 确定最高毁伤等级
     *    c. 估算该点人口密度
     *    d. 累加伤亡人数
     *    e. 统计各等级覆盖面积
     * 4. 计算毁伤重叠率
     * 5. 生成最终模拟结果
     *
     * @param warheadPoints 弹头落点列表
     * @param yieldKt 单弹头当量 (kt)
     * @param hobMode 爆高模式
     * @param targetType 目标类型
     * @param targetLat 目标纬度
     * @param targetLng 目标经度
     * @param cityDatabase 城市数据库（可选）
     * @return 完整的模拟结果
     */
    fun compute(
        warheadPoints: List<WarheadPoint>,
        yieldKt: Double,
        hobMode: String,
        targetType: TargetType,
        targetLat: Double,
        targetLng: Double,
        cityDatabase: CityDatabase?
    ): SimulationResult {
        val effects = NukeEffects.calculate(yieldKt, hobMode)
        val cityData = cityDatabase?.findNearestCity(targetLat, targetLng)

        // === Step 1: 确定弹头落点范围 ===
        val minLat = warheadPoints.minOf { it.lat }
        val maxLat = warheadPoints.maxOf { it.lat }
        val minLng = warheadPoints.minOf { it.lng }
        val maxLng = warheadPoints.maxOf { it.lng }

        // === Step 2: 扩展采样区域 ===
        val maxEffectRadius = effects.psi1.coerceAtLeast(20.0)
        val latExtent = maxEffectRadius / 111.32
        val avgLatRad = (minLat + maxLat) / 2 * PI / 180
        val cosAvgLat = cos(avgLatRad)
        val lngExtent = maxEffectRadius / (111.32 * cosAvgLat)

        val boundsMinLat = minLat - latExtent
        val boundsMaxLat = maxLat + latExtent
        val boundsMinLng = minLng - lngExtent
        val boundsMaxLng = maxLng + lngExtent

        // === Step 3: 自适应采样网格 ===
        val bboxArea = (boundsMaxLat - boundsMinLat) * (boundsMaxLng - boundsMinLng) *
                (111.32 * 111.32 * cosAvgLat)
        val targetSamples = minOf(3000, maxOf(500, (bboxArea / 0.5).roundToInt()))
        val gridCols = maxOf(20, round(sqrt(targetSamples.toDouble())).toInt())
        val gridRows = gridCols

        val dLat = (boundsMaxLat - boundsMinLat) / gridRows
        val dLng = (boundsMaxLng - boundsMinLng) / gridCols

        val centerLat = (minLat + maxLat) / 2
        val centerLng = (minLng + maxLng) / 2

        val cellAreaKm2 = (dLat * 111.32) * (dLng * 111.32 * cosAvgLat)

        // === Step 4: 网格采样计算 ===
        val areaPoints = IntArray(ringPriority.size) { 0 }
        var totalCoveredPoints = 0
        var totalSamplePoints = 0
        var totalDeaths = 0.0
        var totalInjuries = 0.0

        val damageAreas = DoubleArray(ringPriority.size) { 0.0 }

        // 预计算所有弹头的 cos(lat) 值，避免重复计算
        val warheadCosLats = warheadPoints.map { cos(it.lat * PI / 180) }

        for (i in 0 until gridRows) {
            for (j in 0 until gridCols) {
                val ptLat = boundsMinLat + (i + 0.5) * dLat
                val ptLng = boundsMinLng + (j + 0.5) * dLng
                totalSamplePoints++

                // 预计算当前点的 cos 值
                val cosPtLat = cos(ptLat * PI / 180)

                // 计算当前采样点到所有弹头的距离（一次性缓存）
                val distToWarheads = DoubleArray(warheadPoints.size) { idx ->
                    haversineDistFast(ptLat, ptLng, cosPtLat,
                        warheadPoints[idx].lat, warheadPoints[idx].lng, warheadCosLats[idx])
                }

                // 确定该点的最高毁伤等级
                val level = getHighestDamageLevelCached(distToWarheads, effects)
                if (level != null) {
                    totalCoveredPoints++

                    // 估算人口密度和伤亡
                    val distFromCenter = haversineDistFast(ptLat, ptLng, cosPtLat,
                        centerLat, centerLng, cos(centerLat * PI / 180))
                    val density = getDensityAtPoint(distFromCenter, targetType, cityData)

                    val population = density * cellAreaKm2
                    val rates = casualtyRates[level] ?: CasualtyRate(0.0, 0.0)
                    totalDeaths += population * rates.fatalityRate
                    totalInjuries += population * rates.injuryRate
                }

                // 统计各毁伤等级覆盖点数
                for (ri in ringPriority.indices) {
                    if (isPointInRingCached(distToWarheads, effects, ringPriority[ri])) {
                        areaPoints[ri]++
                    }
                }
            }
        }

        // === Step 5: 计算面积和重叠率 ===
        val totalAreaKm2 = totalCoveredPoints * cellAreaKm2
        for (ti in ringPriority.indices) {
            damageAreas[ti] = areaPoints[ti] * cellAreaKm2
        }

        val damageAreasMap = ringPriority.indices.associate { ringPriority[it] to damageAreas[it] }

        // 重叠率计算：1 - (实际覆盖面积 / 各弹头独立覆盖面积之和)
        // 值越接近 1 表示重叠越严重
        var sumArea = 0.0
        for (ti in ringPriority.indices) {
            val r = getRingRadius(effects, ringPriority[ti])
            if (r != null) sumArea += PI * r * r * warheadPoints.size
        }
        val overlapRatio = if (sumArea > 0) {
            maxOf(0.0, minOf(0.95, 1.0 - totalAreaKm2 / sumArea))
        } else 0.0

        // 单弹头各级毁伤面积
        val singleAreas = mutableMapOf<RingType, Double>()
        for (type in ringPriority) {
            val r = getRingRadius(effects, type)
            singleAreas[type] = if (r != null) PI * r * r else 0.0
        }

        // 生成毁伤等级汇总
        val levels = listOf(
            DamageLevel("V毁伤", damageAreasMap[RingType.psi20] ?: 0.0),
            DamageLevel("IV毁伤", damageAreasMap[RingType.psi10] ?: 0.0),
            DamageLevel("III毁伤", damageAreasMap[RingType.psi5] ?: 0.0),
            DamageLevel("II毁伤", damageAreasMap[RingType.psi3] ?: 0.0)
        )

        return SimulationResult(
            warheadCount = warheadPoints.size,
            totalArea = totalAreaKm2,
            severeArea = (damageAreasMap[RingType.fireball] ?: 0.0) +
                    (damageAreasMap[RingType.psi20] ?: 0.0) +
                    (damageAreasMap[RingType.psi10] ?: 0.0),
            deaths = totalDeaths.roundToInt(),
            injuries = totalInjuries.roundToInt(),
            totalCasualties = (totalDeaths + totalInjuries).roundToInt(),
            overlapRatio = overlapRatio,
            damageAreas = damageAreasMap,
            singleAreas = singleAreas,
            effects = effects,
            targetType = targetType,
            cityName = cityData?.name,
            levels = levels
        )
    }

    /**
     * 快速 Haversine 距离计算（使用预计算的 cos 值）
     *
     * 相比标准 haversineDist，减少了 2 次三角函数调用，性能提升约 30%。
     *
     * @param lat1 点1纬度 (°)
     * @param lng1 点1经度 (°)
     * @param cosLat1 点1纬度的余弦值（预计算）
     * @param lat2 点2纬度 (°)
     * @param lng2 点2经度 (°)
     * @param cosLat2 点2纬度的余弦值（预计算）
     * @return 两点间距离 (km)
     */
    private fun haversineDistFast(
        lat1: Double, lng1: Double, cosLat1: Double,
        lat2: Double, lng2: Double, cosLat2: Double
    ): Double {
        val R = 6371.0
        val dLat = (lat2 - lat1) * PI / 180
        val dLng = (lng2 - lng1) * PI / 180
        val sinDLat = sin(dLat / 2)
        val sinDLng = sin(dLng / 2)
        val a = sinDLat * sinDLat +
                cosLat1 * cosLat2 *
                sinDLng * sinDLng
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** 根据毁伤环类型获取对应的毁伤半径 */
    private fun getRingRadius(effects: DamageEffects, ringType: RingType): Double? {
        return when (ringType) {
            RingType.fireball -> effects.fireball
            RingType.psi20 -> effects.psi20
            RingType.psi10 -> effects.psi10
            RingType.psi5 -> effects.psi5
            RingType.psi3 -> effects.psi3
            RingType.psi1 -> effects.psi1
            RingType.thermal -> effects.thermal
        }
    }
}

/**
 * 城市人口数据（用于密度曲线校准）
 *
 * @property name 城市名
 * @property metroPop 都会区人口
 * @property metroRadius 都会区等效半径 (km)
 */
data class CityPopulationData(
    val name: String,
    val metroPop: Double,
    val metroRadius: Double
)

/**
 * 城市数据库 — 提供精确匹配和最近邻搜索
 *
 * 用于在目标点附近查找城市，以便使用真实人口数据校准密度模型。
 * 搜索策略：
 * 1. 精确坐标匹配（经纬度精确到 4 位小数）
 * 2. 最近邻搜索（20km 半径内的最近城市）
 */
class CityDatabase(private val cities: List<City>) {

    /** 过滤掉无效坐标的城市 */
    private val indexedCities: List<City> = cities.filter { it.lat.isFinite() }

    /**
     * 精确坐标查找城市
     *
     * @param lat 纬度
     * @param lng 经度
     * @return 匹配到的城市人口数据，无匹配返回 null
     */
    fun lookupCityData(lat: Double, lng: Double): CityPopulationData? {
        val key = "%.4f,%.4f".format(lat, lng)
        val matched = indexedCities.firstOrNull { c ->
            "%.4f,%.4f".format(c.lat, c.lng) == key
        }
        return matched?.let { toPopulationData(it) }
    }

    /**
     * 最近邻城市搜索
     *
     * 遍历所有城市，找到与目标点最近且距离在 maxRadiusKm 内的城市。
     *
     * @param lat 目标纬度
     * @param lng 目标经度
     * @param maxRadiusKm 最大搜索半径 (km)，默认 20km
     * @return 最近城市的人口数据，超出范围返回 null
     */
    fun findNearestCity(lat: Double, lng: Double, maxRadiusKm: Double = 20.0): CityPopulationData? {
        if (indexedCities.isEmpty()) return null

        var bestCity: City? = null
        var bestDist = Double.MAX_VALUE

        for (city in indexedCities) {
            val dist = StatsCalculator.haversineDist(lat, lng, city.lat, city.lng)
            if (dist < bestDist) {
                bestDist = dist
                bestCity = city
            }
        }

        return if (bestCity != null && bestDist <= maxRadiusKm) {
            toPopulationData(bestCity)
        } else {
            null
        }
    }

    /** 将 City 模型转换为人口数据结构 */
    private fun toPopulationData(city: City): CityPopulationData {
        return CityPopulationData(
            name = city.name,
            metroPop = (city.pop.coerceAtLeast(100.0)) * 10000,  // 万人转为人
            metroRadius = city.radius.coerceAtLeast(25.0)
        )
    }

    /** 获取所有城市列表 */
    val allCities: List<City> get() = indexedCities
}
