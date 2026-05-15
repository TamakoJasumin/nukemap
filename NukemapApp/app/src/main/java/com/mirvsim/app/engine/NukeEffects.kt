/**
 * 核爆效应计算引擎
 *
 * 基于 Glasstone & Dolan "The Effects of Nuclear Weapons" (1977) 经典模型，
 * 计算核武器爆炸后的各项物理毁伤效应参数：
 *
 * - 火球半径（fireball）：核爆瞬间形成的超高温等离子体球
 * - 超压毁伤半径（psi20/10/5/3/1）：不同峰值超压对应的冲击波毁伤范围
 * - 热辐射半径（thermal）：足以造成三度烧伤的热脉冲范围
 * - 初期核辐射半径（radiation）：致命剂量电离辐射范围
 *
 * 地爆（surface）与空爆（optimal）采用不同的经验系数，
 * 空爆的火球和热辐射范围略大于地爆。
 */
package com.mirvsim.app.engine

import com.mirvsim.app.model.DamageEffects
import com.mirvsim.app.model.RingType
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * 核爆效应计算引擎（单例）
 *
 * 提供核爆毁伤半径的计算方法以及各级毁伤环的渲染样式定义。
 * 所有计算均基于弹头当量（kt）和爆高模式（地爆/空爆）。
 */
object NukeEffects {

    /**
     * 根据弹头当量和爆高模式计算各级毁伤半径
     *
     * @param yieldKt 弹头当量，单位千吨 TNT
     * @param hobMode 爆高模式: "surface"=地爆, "optimal"=空爆(最佳高度)
     * @return DamageEffects 包含各级毁伤半径 (km)
     *
     * 计算公式来源于 Glasstone & Dolan 经验模型:
     * - 火球半径: R ∝ Y^0.4
     * - 冲击波半径: R ∝ Y^(1/3)（立方根定律）
     * - 热辐射半径: R ∝ Y^0.42（地爆）或 Y^0.41（空爆）
     */
    fun calculate(yieldKt: Double, hobMode: String): DamageEffects {
        val y = yieldKt
        val isSurface = hobMode == "surface"

        return DamageEffects(
            // 火球半径：地爆受地面反射影响略小
            fireball = if (isSurface) 0.142 * y.pow(0.4)
            else 0.17 * y.pow(0.4),
            // 各级超压毁伤半径：基于经典立方根缩放定律
            psi20 = 0.45 * y.pow(1.0 / 3),    // 20 psi — 钢筋混凝土建筑摧毁
            psi10 = 0.63 * y.pow(1.0 / 3),    // 10 psi — 建筑物倒塌
            psi5 = 0.87 * y.pow(1.0 / 3),     // 5 psi — 住宅摧毁
            psi3 = 1.2 * y.pow(1.0 / 3),      // 3 psi — 建筑受损
            psi1 = 2.4 * y.pow(1.0 / 3),      // 1 psi — 玻璃碎裂
            // 热辐射半径：空爆时因大气吸收略小
            thermal = if (isSurface) 0.57 * y.pow(0.42)
            else 0.67 * y.pow(0.41),
            // 初期核辐射半径：受大气衰减影响，缩放系数较小
            radiation = 0.24 * y.pow(0.19)
        )
    }

    /**
     * 毁伤环样式定义
     *
     * @property color 描边颜色
     * @property fillColor 填充颜色
     * @property fillOpacity 填充透明度 (0~1)
     * @property weight 描边线宽
     * @property dashArray 虚线模式（null 为实线）
     */
    data class RingStyle(
        val color: Color,
        val fillColor: Color,
        val fillOpacity: Float,
        val weight: Float,
        val dashArray: FloatArray?
    )

    /**
     * 根据毁伤环类型获取对应的渲染样式
     *
     * 不同级别的毁伤使用不同的颜色和线型以便区分：
     * - 火球：金色实线（最内层）
     * - 重度毁伤：红色/橙色实线
     * - 中度毁伤：橙色虚线
     * - 轻度毁伤：青色/绿色虚线
     * - 热辐射：紫色点划线（最外层）
     */
    fun getRingStyle(ringType: RingType): RingStyle {
        return when (ringType) {
            RingType.fireball -> RingStyle(
                Color(0xFFFFD700), Color(0xFFFFD700), 0.04f, 3f, null
            )
            RingType.psi20 -> RingStyle(
                Color(0xFFE53935), Color(0xFFE53935), 0.03f, 3f, null
            )
            RingType.psi10 -> RingStyle(
                Color(0xFFF4511E), Color(0xFFF4511E), 0.025f, 2.5f, null
            )
            RingType.psi5 -> RingStyle(
                Color(0xFFFF8F00), Color(0xFFFF8F00), 0.02f, 2f,
                floatArrayOf(8f, 6f)  // 长虚线
            )
            RingType.psi3 -> RingStyle(
                Color(0xFF00BCD4), Color(0xFF00BCD4), 0.015f, 2f,
                floatArrayOf(4f, 4f)  // 短虚线
            )
            RingType.psi1 -> RingStyle(
                Color(0xFF7CB342), Color(0xFF7CB342), 0.01f, 1.5f,
                floatArrayOf(2f, 4f)  // 点线
            )
            RingType.thermal -> RingStyle(
                Color(0xFFE040FB), Color(0xFFE040FB), 0.025f, 2.5f,
                floatArrayOf(10f, 4f, 2f, 4f)  // 点划线
            )
        }
    }

}
