package com.mirvsim.app.model

import kotlinx.serialization.Serializable

enum class SpreadPattern {
    circular, linear, elliptical, grid
}

enum class HOBMode {
    surface, optimal, custom
}

enum class TargetType {
    urban, suburban, rural
}

data class WarheadConfig(
    val count: Int,
    val yieldKt: Double,
    val separationKm: Double,
    val pattern: SpreadPattern,
    val hobMode: HOBMode,
    val customHobMeters: Double = 600.0,
    val targetLat: Double,
    val targetLng: Double,
    val targetType: TargetType
)

data class DamageEffects(
    val fireball: Double,
    val psi20: Double,
    val psi10: Double,
    val psi5: Double,
    val psi3: Double,
    val psi1: Double,
    val thermal: Double,
    val radiation: Double
)

data class WarheadPoint(
    val index: Int,
    val lat: Double,
    val lng: Double,
    val effects: DamageEffects,
    val yieldKt: Double = 0.0,
    val hobMeters: Double = 600.0
)

data class DamageArea(
    val type: RingType,
    val areaKm2: Double
)

data class SimulationResult(
    val warheadCount: Int,
    val totalArea: Double,
    val severeArea: Double,
    val deaths: Int,
    val injuries: Int,
    val totalCasualties: Int,
    val overlapRatio: Double,
    val damageAreas: Map<RingType, Double>,
    val singleAreas: Map<RingType, Double>,
    val effects: DamageEffects,
    val targetType: TargetType,
    val cityName: String?,
    val levels: List<DamageLevel> = emptyList(),
    val effectsList: List<NukeEffectsResult> = emptyList()
)

data class DamageLevel(
    val displayName: String,
    val areaKm2: Double
)

data class RingResult(
    val outerRadiusKm: Double,
    val color: Long
)

data class NukeEffectsResult(
    val rings: List<RingResult>,
    val centerLat: Double,
    val centerLng: Double,
    val totalArea: Double,
    val targetType: String
)

enum class RingType(
    val displayName: String,
    val shortName: String,
    val description: String
) {
    fireball("火球半径", "火球", "火球内部一切汽化，无人生还"),
    psi20("20 psi 重度毁伤", "20 psi", "钢筋混凝土建筑完全摧毁，致死率接近100%"),
    psi10("10 psi 严重毁伤", "10 psi", "大多数建筑物倒塌，致死率极高"),
    psi5("5 psi 中度毁伤", "5 psi", "住宅建筑摧毁，广泛人员伤亡"),
    psi3("3 psi 轻度毁伤", "3 psi", "多数建筑严重受损，伤亡率高"),
    psi1("1 psi 玻璃碎裂", "1 psi", "玻璃碎裂，轻质结构损坏"),
    thermal("热辐射 三度烧伤", "热辐射", "暴露皮肤三度烧伤，可燃物引燃")
}

@Serializable
data class City(
    val name: String,
    val display: String?,
    val lat: Double,
    val lng: Double,
    val pop: Double,
    val radius: Double,
    val group: String
)

data class Preset(
    val id: String,
    val name: String,
    val desc: String,
    val count: Int,
    val yield: Double,
    val separation: Double,
    val pattern: String,
    val hob: String,
    val lat: Double,
    val lng: Double,
    val target: String
)
