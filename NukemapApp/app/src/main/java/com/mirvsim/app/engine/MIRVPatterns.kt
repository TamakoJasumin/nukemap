/**
 * 弹头散布模式生成器
 *
 * MIRV（Multiple Independently Targetable Reentry Vehicle，分导式多弹头）
 * 在再入大气层后可以通过弹头母舱的末助推控制，实现不同的散布模式。
 *
 * 本模块支持四种经典散布模式：
 * - circular（圆形）：弹头均匀分布在圆周上
 * - linear（线性）：弹头沿直线均匀排列
 * - elliptical（椭圆）：弹头分布在椭圆周上
 * - grid（网格）：弹头按网格矩阵排列
 *
 * 坐标计算基于 Haversine 近似：
 * - 1° 纬度 ≈ 111.32 km
 * - 1° 经度 ≈ 111.32 × cos(lat) km（需纬度余弦修正）
 */
package com.mirvsim.app.engine

import kotlin.math.*

/**
 * MIRV 弹头散布模式生成器（单例）
 *
 * 提供各种散布模式的落点坐标生成算法。
 * 所有模式以目标点为中心，separationKm 控制散布范围。
 */
object MIRVPatterns {

    /** 经纬度坐标对 */
    data class LatLng(val lat: Double, val lng: Double)

    /**
     * 圆形散布模式
     *
     * 弹头以目标点为中心，均匀分布在半径为 separationKm 的圆周上。
     * 起始角度为 -90°（正上方），逆时针均匀分布。
     *
     * @param count 弹头数量
     * @param centerLat 目标中心纬度
     * @param centerLng 目标中心经度
     * @param separationKm 散布半径 (km)
     * @return 弹头落点坐标列表
     */
    fun circular(count: Int, centerLat: Double, centerLng: Double, separationKm: Double): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val radius = separationKm
        for (i in 0 until count) {
            val angle = (2 * PI * i) / count - PI / 2  // 从正上方向开始
            val dLat = (radius * sin(angle)) / 111.32
            val dLng = (radius * cos(angle)) / (111.32 * cos(centerLat * PI / 180))
            points.add(LatLng(centerLat + dLat, centerLng + dLng))
        }
        return points
    }

    /**
     * 线性散布模式
     *
     * 弹头沿 30° 方位角（东北方向）均匀排布。
     * 适用于打击跑道、铁路线等线性目标。
     *
     * @param count 弹头数量
     * @param centerLat 目标中心纬度
     * @param centerLng 目标中心经度
     * @param separationKm 相邻弹头间距 (km)
     * @return 弹头落点坐标列表
     */
    fun linear(count: Int, centerLat: Double, centerLng: Double, separationKm: Double): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val totalLen = separationKm * (count - 1)
        val angle = 30 * PI / 180  // 30° 方位角
        for (i in 0 until count) {
            val offset = -totalLen / 2 + (totalLen * i) / max(count - 1, 1).toDouble()
            val dLat = (offset * sin(angle)) / 111.32
            val dLng = (offset * cos(angle)) / (111.32 * cos(centerLat * PI / 180))
            points.add(LatLng(centerLat + dLat, centerLng + dLng))
        }
        return points
    }

    /**
     * 椭圆散布模式
     *
     * 弹头分布在以目标点为中心的椭圆周上。
     * - 长轴 (a) = separationKm × 1.5（沿经度方向）
     * - 短轴 (b) = separationKm × 0.7（沿纬度方向）
     * 适用于覆盖长方形区域目标（如机场、港口）。
     *
     * @param count 弹头数量
     * @param centerLat 目标中心纬度
     * @param centerLng 目标中心经度
     * @param separationKm 基准分离距离 (km)
     * @return 弹头落点坐标列表
     */
    fun elliptical(count: Int, centerLat: Double, centerLng: Double, separationKm: Double): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val a = separationKm * 1.5   // 长轴
        val b = separationKm * 0.7   // 短轴
        for (i in 0 until count) {
            val angle = (2 * PI * i) / count - PI / 2
            val dLat = (b * sin(angle)) / 111.32
            val dLng = (a * cos(angle)) / (111.32 * cos(centerLat * PI / 180))
            points.add(LatLng(centerLat + dLat, centerLng + dLng))
        }
        return points
    }

    /**
     * 网格散布模式
     *
     * 弹头按行列均匀排列，形成矩形网格覆盖。
     * 行列数尽可能相等（接近正方形网格）。
     * 适用于大面积区域覆盖或多目标同时打击。
     *
     * @param count 弹头数量
     * @param centerLat 目标中心纬度
     * @param centerLng 目标中心经度
     * @param separationKm 网格间距 (km)
     * @return 弹头落点坐标列表
     */
    fun grid(count: Int, centerLat: Double, centerLng: Double, separationKm: Double): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val cols = ceil(sqrt(count.toDouble())).toInt()
        val rows = ceil(count.toDouble() / cols).toInt()
        val cellSize = separationKm
        val totalWidth = (cols - 1) * cellSize
        val totalHeight = (rows - 1) * cellSize

        for (i in 0 until count) {
            val row = i / cols
            val col = i % cols
            val dLat = (row * cellSize - totalHeight / 2) / 111.32
            val dLng = (col * cellSize - totalWidth / 2) / (111.32 * cos(centerLat * PI / 180))
            points.add(LatLng(centerLat + dLat, centerLng + dLng))
        }
        return points
    }

    /**
     * 统一的散布模式生成入口
     *
     * 根据字符串标识符选择对应的散布模式生成算法。
     * 默认模式为 circular（未知标识符时回退）。
     *
     * @param pattern 散布模式标识: "circular" / "linear" / "elliptical" / "grid"
     * @param count 弹头数量
     * @param centerLat 目标中心纬度
     * @param centerLng 目标中心经度
     * @param separationKm 分离距离 (km)
     * @return 弹头落点坐标列表
     */
    fun generate(pattern: String, count: Int, centerLat: Double, centerLng: Double, separationKm: Double): List<LatLng> {
        return when (pattern) {
            "circular" -> circular(count, centerLat, centerLng, separationKm)
            "linear" -> linear(count, centerLat, centerLng, separationKm)
            "elliptical" -> elliptical(count, centerLat, centerLng, separationKm)
            "grid" -> grid(count, centerLat, centerLng, separationKm)
            else -> circular(count, centerLat, centerLng, separationKm)
        }
    }
}
