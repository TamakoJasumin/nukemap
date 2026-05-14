package com.mirvsim.app.engine

import com.mirvsim.app.model.*
import kotlin.math.*

object StatsCalculator {

    data class DensityProfile(
        val peakDensity: Double,
        val decayScale: Double,
        val backgroundDensity: Double
    )

    private val densityProfiles = mapOf(
        "urban" to DensityProfile(15000.0, 8.0, 500.0),
        "suburban" to DensityProfile(3000.0, 5.0, 150.0),
        "rural" to DensityProfile(200.0, 3.0, 20.0)
    )

    data class CasualtyRate(
        val fatalityRate: Double,
        val injuryRate: Double
    )

    private val casualtyRates = mapOf(
        RingType.fireball to CasualtyRate(1.0, 0.0),
        RingType.psi20 to CasualtyRate(0.90, 0.10),
        RingType.psi10 to CasualtyRate(0.50, 0.40),
        RingType.psi5 to CasualtyRate(0.15, 0.50),
        RingType.psi3 to CasualtyRate(0.05, 0.35),
        RingType.psi1 to CasualtyRate(0.0, 0.15),
        RingType.thermal to CasualtyRate(0.0, 0.50)
    )

    val ringPriority = listOf(
        RingType.fireball,
        RingType.psi20,
        RingType.psi10,
        RingType.psi5,
        RingType.psi3,
        RingType.thermal,
        RingType.psi1
    )

    fun haversineDist(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0
        val dLat = (lat2 - lat1) * PI / 180
        val dLng = (lng2 - lng1) * PI / 180
        val sinDLat = sin(dLat / 2)
        val sinDLng = sin(dLng / 2)
        val a = sinDLat * sinDLat +
                cos(lat1 * PI / 180) * cos(lat2 * PI / 180) *
                sinDLng * sinDLng
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun getDensityAtPoint(
        distFromCenterKm: Double,
        targetType: TargetType,
        cityData: CityPopulationData?
    ): Double {
        if (cityData != null && targetType == TargetType.urban) {
            val scale = cityData.metroRadius / 3.5
            val peak = cityData.metroPop / (2 * PI * scale * scale)
            return max(100.0, peak * exp(-distFromCenterKm / scale))
        }
        val profile = densityProfiles[targetType.name] ?: densityProfiles["urban"]!!
        val density = profile.peakDensity * exp(-distFromCenterKm / profile.decayScale)
        return max(profile.backgroundDensity, density)
    }

    // Optimized: uses pre-computed distance cache
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

    // Optimized: uses pre-computed distance cache
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

        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLng = Double.MAX_VALUE
        var maxLng = -Double.MAX_VALUE

        warheadPoints.forEach { p ->
            if (p.lat < minLat) minLat = p.lat
            if (p.lat > maxLat) maxLat = p.lat
            if (p.lng < minLng) minLng = p.lng
            if (p.lng > maxLng) maxLng = p.lng
        }

        val maxEffectRadius = effects.psi1.coerceAtLeast(20.0)
        val latExtent = maxEffectRadius / 111.32
        val avgLatRad = (minLat + maxLat) / 2 * PI / 180
        val cosAvgLat = cos(avgLatRad)
        val lngExtent = maxEffectRadius / (111.32 * cosAvgLat)

        val boundsMinLat = minLat - latExtent
        val boundsMaxLat = maxLat + latExtent
        val boundsMinLng = minLng - lngExtent
        val boundsMaxLng = maxLng + lngExtent

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

        val areaPoints = IntArray(ringPriority.size) { 0 }
        var totalCoveredPoints = 0
        var totalSamplePoints = 0
        var totalDeaths = 0.0
        var totalInjuries = 0.0

        val damageAreas = DoubleArray(ringPriority.size) { 0.0 }

        // Pre-compute warhead cos(lat) values for haversine
        val warheadCosLats = warheadPoints.map { cos(it.lat * PI / 180) }

        for (i in 0 until gridRows) {
            for (j in 0 until gridCols) {
                val ptLat = boundsMinLat + (i + 0.5) * dLat
                val ptLng = boundsMinLng + (j + 0.5) * dLng
                totalSamplePoints++

                // Pre-compute cos for current point
                val cosPtLat = cos(ptLat * PI / 180)

                // [OPTIMIZATION] Pre-compute distance from this grid point to ALL warheads
                val distToWarheads = DoubleArray(warheadPoints.size) { idx ->
                    haversineDistFast(ptLat, ptLng, cosPtLat,
                        warheadPoints[idx].lat, warheadPoints[idx].lng, warheadCosLats[idx])
                }

                val level = getHighestDamageLevelCached(distToWarheads, effects)
                if (level != null) {
                    totalCoveredPoints++

                    val distFromCenter = haversineDistFast(ptLat, ptLng, cosPtLat,
                        centerLat, centerLng, cos(centerLat * PI / 180))
                    val density = getDensityAtPoint(distFromCenter, targetType, cityData)

                    val population = density * cellAreaKm2
                    val rates = casualtyRates[level] ?: CasualtyRate(0.0, 0.0)
                    totalDeaths += population * rates.fatalityRate
                    totalInjuries += population * rates.injuryRate
                }

                for (ri in ringPriority.indices) {
                    if (isPointInRingCached(distToWarheads, effects, ringPriority[ri])) {
                        areaPoints[ri]++
                    }
                }
            }
        }

        val totalAreaKm2 = totalCoveredPoints * cellAreaKm2
        for (ti in ringPriority.indices) {
            damageAreas[ti] = areaPoints[ti] * cellAreaKm2
        }

        val damageAreasMap = mutableMapOf<RingType, Double>()
        for (ti in ringPriority.indices) {
            damageAreasMap[ringPriority[ti]] = damageAreas[ti]
        }

        var sumArea = 0.0
        for (ti in ringPriority.indices) {
            val r = getRingRadius(effects, ringPriority[ti])
            if (r != null) sumArea += PI * r * r * warheadPoints.size
        }
        val overlapRatio = if (sumArea > 0) {
            maxOf(0.0, minOf(0.95, 1.0 - totalAreaKm2 / sumArea))
        } else 0.0

        val singleAreas = mutableMapOf<RingType, Double>()
        for (type in ringPriority) {
            val r = getRingRadius(effects, type)
            singleAreas[type] = if (r != null) PI * r * r else 0.0
        }

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

    // Fast haversine with pre-computed cos values
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

data class CityPopulationData(
    val name: String,
    val metroPop: Double,
    val metroRadius: Double
)

class CityDatabase(private val cities: List<City>) {

    private val indexedCities: List<City> = cities.filter { it.lat.isFinite() }

    fun lookupCityData(lat: Double, lng: Double): CityPopulationData? {
        val key = "%.4f,%.4f".format(lat, lng)
        val matched = indexedCities.firstOrNull { c ->
            "%.4f,%.4f".format(c.lat, c.lng) == key
        }
        return matched?.let { toPopulationData(it) }
    }

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
            toPopulationData(bestCity!!)
        } else {
            null
        }
    }

    private fun toPopulationData(city: City): CityPopulationData {
        return CityPopulationData(
            name = city.name,
            metroPop = (city.pop.coerceAtLeast(100.0)) * 10000,
            metroRadius = city.radius.coerceAtLeast(25.0)
        )
    }

    val allCities: List<City> get() = indexedCities
}
