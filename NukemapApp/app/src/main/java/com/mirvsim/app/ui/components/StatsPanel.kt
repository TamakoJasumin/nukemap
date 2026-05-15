/**
 * 统计结果面板
 *
 * 显示核打击模拟的详细统计结果，包含：
 * 1. 四项核心指标卡片：总毁伤面积、死亡人数、受伤人数、总伤亡
 * 2. 各毁伤等级覆盖面积条形图（带动画）
 * 3. 弹头落点坐标详情列表（按弹头序号排列）
 *
 * 所有数值格式化使用 Locale.US 确保千位分隔符的一致性。
 */
package com.mirvsim.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mirvsim.app.model.DamageEffects
import com.mirvsim.app.model.RingType
import com.mirvsim.app.model.SimulationResult
import com.mirvsim.app.model.TargetType
import com.mirvsim.app.model.WarheadPoint
import com.mirvsim.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/** 毁伤环类型对应的渲染颜色 */
private val ringColors = mapOf(
    RingType.fireball to Color(0xFFFFD700),  // 金色
    RingType.psi20 to Color(0xFFE53935),      // 红色
    RingType.psi10 to Color(0xFFF4511E),      // 橙色
    RingType.psi5 to Color(0xFFFF8F00),       // 深黄
    RingType.psi3 to Color(0xFF00BCD4),       // 青色
    RingType.thermal to Color(0xFFE040FB),    // 紫色
    RingType.psi1 to Color(0xFF7CB342)        // 绿色
)

/** 毁伤环显示顺序（从最严重到最轻） */
private val ringOrder = listOf(
    RingType.fireball, RingType.psi20, RingType.psi10,
    RingType.psi5, RingType.psi3, RingType.thermal, RingType.psi1
)

@Composable
fun StatsPanel(
    result: SimulationResult,
    warheadPoints: List<WarheadPoint>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 顶部标题栏（含关闭按钮）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("攻击结果统计", color = DarkOnBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (result.cityName != null) {
                        Text("\uD83D\uDCCD ${result.cityName} · ${targetTypeLabel(result.targetType.name)}",
                            color = DarkOnSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, "关闭", tint = DarkOnSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // 四项核心指标卡片
            StatsCards(result)

            Spacer(Modifier.height(16.dp))

            // 各毁伤等级覆盖面积条形图
            Text("各毁伤等级覆盖面积", color = DarkOnSurfaceVariant, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            DamageBars(damageAreas = result.damageAreas)

            Spacer(Modifier.height(16.dp))

            // 弹头落点详情列表
            Text("弹头落点详情", color = DarkOnSurfaceVariant, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            WarheadList(warheadPoints = warheadPoints)
        }
    }
}

/** 四项核心指标卡片（竖向排列） */
@Composable
private fun StatsCards(result: SimulationResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            icon = { Icon(Icons.Filled.Warning, null, tint = NukeOrange, modifier = Modifier.size(20.dp)) },
            color = NukeOrange, value = formatArea(result.totalArea), label = "总毁伤面积 (km²)")
        StatCard(
            icon = { Icon(Icons.Filled.Close, null, tint = NukeRed, modifier = Modifier.size(20.dp)) },
            color = NukeRed, value = formatCount(result.deaths), label = "预估死亡人数", valueColor = NukeRed)
        StatCard(
            icon = { Icon(Icons.Filled.Warning, null, tint = NukeYellow, modifier = Modifier.size(20.dp)) },
            color = NukeYellow, value = formatCount(result.injuries), label = "预估受伤人数")
        StatCard(
            icon = { Icon(Icons.Filled.People, null, tint = NukeOrange, modifier = Modifier.size(20.dp)) },
            color = NukeOrange, value = formatCount(result.totalCasualties), label = "总伤亡人数")
    }
}

/** 单个统计指标卡片 */
@Composable
private fun StatCard(
    icon: @Composable () -> Unit,
    color: Color,
    value: String,
    label: String,
    valueColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(value, color = valueColor ?: DarkOnBackground, fontWeight = FontWeight.Bold,
                fontSize = 20.sp, fontFamily = FontFamily.Monospace, lineHeight = 24.sp)
            Text(label, color = DarkOnSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

/**
 * 各毁伤等级覆盖面积条形图
 *
 * 使用 AnimatedVisibility 实现条形图动画展开效果。
 * 每个等级显示：名称 + 进度条 + 面积数值。
 */
@Composable
private fun DamageBars(damageAreas: Map<RingType, Double>) {
    val maxArea = remember(damageAreas) {
        ringOrder.maxOf { damageAreas[it] ?: 0.0 }.coerceAtLeast(1.0)
    }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(damageAreas) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ringOrder.forEach { type ->
            val area = damageAreas[type] ?: 0.0
            val pct = (area / maxArea * animProgress.value).toFloat().coerceIn(0f, 1f)
            val color = ringColors[type] ?: NukeOrange

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(type.displayName, fontSize = 11.sp, color = DarkOnSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.width(68.dp))
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.weight(1f).height(12.dp)
                    .clip(RoundedCornerShape(6.dp)).background(DarkSurfaceVariant)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction = pct)
                        .clip(RoundedCornerShape(6.dp)).background(color))
                }
                Spacer(Modifier.width(6.dp))
                Text(formatArea(area), fontSize = 11.sp, color = DarkOnBackground,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.width(52.dp))
            }
        }
    }
}

/** 弹头落点详细信息列表 */
@Composable
private fun WarheadList(warheadPoints: List<WarheadPoint>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        warheadPoints.sortedBy { it.index }.forEachIndexed { i, wp ->
            val hue = (i.toFloat() / warheadPoints.size.coerceAtLeast(1)) * 300f
            val dotColor = Color.hsl(hue, 0.9f, 0.6f)  // 弹头标记色（与地图一致）

            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).background(dotColor, RoundedCornerShape(50)))
                Spacer(Modifier.width(8.dp))
                Text("#${wp.index + 1}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = DarkOnBackground, modifier = Modifier.width(30.dp))
                Text("%.4f, %.4f".format(wp.lat, wp.lng), fontSize = 11.sp,
                    color = DarkOnSurfaceVariant.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace)
            }
        }
    }
}

/** 整数格式化（千位分隔符） */
private val numberFormatter = NumberFormat.getIntegerInstance(Locale.US)

/** 面积格式化（1 位小数） */
private val areaFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
    minimumFractionDigits = 1
    maximumFractionDigits = 1
}

private fun formatCount(num: Int): String = numberFormatter.format(num)

private fun formatArea(area: Double): String = areaFormatter.format(area) + " km²"

/** 目标类型中文显示名 */
private fun targetTypeLabel(type: String): String = when (type) {
    "urban" -> "城区"; "suburban" -> "郊区"; else -> "乡村"
}

@Preview(showBackground = true)
@Composable
fun StatsPanelPreview() {
    val dummyEffects = DamageEffects(fireball = 0.5, psi20 = 1.0, psi10 = 2.0, psi5 = 4.0,
        psi3 = 6.0, psi1 = 10.0, thermal = 15.0, radiation = 3.0)
    val dummyResult = SimulationResult(
        warheadCount = 1, totalArea = 150.0, severeArea = 50.0,
        deaths = 1200000, injuries = 3500000, totalCasualties = 4700000, overlapRatio = 0.1,
        damageAreas = mapOf(RingType.fireball to 0.5, RingType.psi20 to 1.0, RingType.psi10 to 2.0,
            RingType.psi5 to 4.0, RingType.psi3 to 6.0, RingType.thermal to 8.0, RingType.psi1 to 10.0),
        singleAreas = emptyMap(), effects = dummyEffects, targetType = TargetType.urban, cityName = "北京")
    val dummyWarheadPoints = listOf(WarheadPoint(0, 39.9042, 116.4074, dummyEffects, 100.0, 600.0))

    NukemapTheme {
        StatsPanel(result = dummyResult, warheadPoints = dummyWarheadPoints, onClose = {})
    }
}
