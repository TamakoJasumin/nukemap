/**
 * 数据模型定义
 *
 * 本文件定义了 Nukemap 应用中所有核心数据模型，包括：
 * - 核武器配置参数（弹头数量、当量、散布模式等）
 * - 核爆毁伤效应（火球半径、超压毁伤半径等）
 * - 模拟结果数据结构
 * - 城市数据和预设场景模型
 *
 * 所有模型使用不可变 data class，确保线程安全与状态可预测性。
 */
package com.mirvsim.app.model

import kotlinx.serialization.Serializable

/** 弹头散布模式: 圆形 / 线性 / 椭圆 / 网格 */
enum class SpreadPattern {
    circular, linear, elliptical, grid
}

/** 爆高模式: 地爆(地面接触) / 空爆(最佳高度) / 自定义高度 */
enum class HOBMode {
    surface, optimal, custom
}

/** 目标区域类型 — 影响人口密度模型和伤亡估算 */
enum class TargetType {
    urban,       // 城区 — 高密度人口
    suburban,    // 郊区 — 中等密度
    rural        // 乡村 — 低密度
}

/**
 * 核弹头完整配置参数
 *
 * @property count 弹头数量（MIRV 分导式弹头数）
 * @property yieldKt 单弹头当量，单位千吨 (kt)
 * @property separationKm 弹头分离距离，单位千米
 * @property pattern 散布模式（圆形/线性/椭圆/网格）
 * @property hobMode 爆高模式（地爆/空爆/自定义）
 * @property customHobMeters 自定义爆高，单位米（仅 hobMode=custom 时生效）
 * @property targetLat 目标纬度
 * @property targetLng 目标经度
 * @property targetType 目标类型（城区/郊区/乡村）
 */
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

/**
 * 各级毁伤效应半径 (km)
 *
 * 基于 Glasstone & Dolan 核武器效应模型计算得出：
 * - fireball: 火球半径，内部一切汽化
 * - psi20~psi1: 不同超压等级毁伤半径
 * - thermal: 热辐射三度烧伤半径
 * - radiation: 初期核辐射半径
 */
data class DamageEffects(
    val fireball: Double,   // 火球半径 (km)
    val psi20: Double,      // 20 psi 超压 — 钢筋混凝土建筑摧毁
    val psi10: Double,      // 10 psi 超压 — 大部分建筑物倒塌
    val psi5: Double,       // 5 psi 超压 — 住宅建筑摧毁
    val psi3: Double,       // 3 psi 超压 — 建筑严重受损
    val psi1: Double,       // 1 psi 超压 — 玻璃碎裂
    val thermal: Double,    // 热辐射三度烧伤半径
    val radiation: Double   // 初期核辐射半径（致命剂量）
)

/**
 * 单个弹头落点信息
 *
 * @property index 弹头序号（从 0 开始）
 * @property lat 落点纬度
 * @property lng 落点经度
 * @property effects 该弹头的毁伤效应数据
 * @property yieldKt 弹头当量
 * @property hobMeters 爆高（米）
 */
data class WarheadPoint(
    val index: Int,
    val lat: Double,
    val lng: Double,
    val effects: DamageEffects,
    val yieldKt: Double = 0.0,
    val hobMeters: Double = 600.0
)

/** 某级毁伤的覆盖面积 */
data class DamageArea(
    val type: RingType,
    val areaKm2: Double
)

/**
 * 完整模拟结果
 *
 * @property warheadCount 弹头总数
 * @property totalArea 总覆盖面积 (km²)
 * @property severeArea 重度毁伤面积 (km²)
 * @property deaths 预估死亡人数
 * @property injuries 预估受伤人数
 * @property totalCasualties 总伤亡人数
 * @property overlapRatio 毁伤区域重叠率（0~0.95）
 * @property damageAreas 各级毁伤的实际覆盖面积
 * @property singleAreas 单弹头各级毁伤面积
 * @property effects 核爆效应参数
 * @property targetType 目标类型
 * @property cityName 最近城市名称
 * @property levels 毁伤等级汇总列表
 * @property effectsList 各弹头毁伤环数据（用于地图渲染）
 */
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

/** 毁伤等级显示名及对应面积 */
data class DamageLevel(
    val displayName: String,  // 显示名称，如 "V毁伤"
    val areaKm2: Double       // 覆盖面积 (km²)
)

/** 单个毁伤环（用于地图 osmdroid 渲染） */
data class RingResult(
    val outerRadiusKm: Double,  // 外半径 (km)
    val color: Long             // ARGB 颜色值
)

/** 全部毁伤环集合（单个弹头的完整毁伤数据，用于地图渲染） */
data class NukeEffectsResult(
    val rings: List<RingResult>,    // 各级毁伤环列表
    val centerLat: Double,          // 弹头落点纬度
    val centerLng: Double,          // 弹头落点经度
    val totalArea: Double,          // 总覆盖面积
    val targetType: String          // 目标类型标识
)

/**
 * 毁伤环类型枚举 — 含中文显示名称和描述
 *
 * @property displayName 完整显示名称
 * @property shortName 简短名称
 * @property description 毁伤效果描述
 */
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

/**
 * 城市数据模型（从 cities.json 反序列化）
 *
 * @property name 城市英文名
 * @property display 显示用中文名（可选）
 * @property lat 纬度
 * @property lng 经度
 * @property pop 人口（单位：万）
 * @property radius 城市半径 (km)
 * @property group 所属地区分组
 */
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

/**
 * 预设场景 — 对应一种已知型号的导弹配置
 *
 * @property id 唯一标识符
 * @property name 显示名称
 * @property desc 简短描述
 * @property count 弹头数量
 * @property yield 单弹头当量 (kt)
 * @property separation 分离距离 (km)
 * @property pattern 散布模式
 * @property hob 爆高模式
 * @property lat 目标纬度
 * @property lng 目标经度
 * @property target 目标类型
 */
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
