package com.mirvsim.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mirvsim.app.model.DamageEffects
import com.mirvsim.app.model.DamageLevel
import com.mirvsim.app.model.SimulationResult
import com.mirvsim.app.model.TargetType
import com.mirvsim.app.model.WarheadPoint
import com.mirvsim.app.ui.theme.*

@Composable
fun StatsPanel(
    result: SimulationResult,
    warheadPoints: List<WarheadPoint>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(280.dp).animateContentSize(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = BgSecondary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "攻击结果统计",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, "关闭", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "死亡人数",
                    value = formatNumber(result.deaths),
                    color = Danger
                )
                StatCard(
                    title = "受伤人数",
                    value = formatNumber(result.injuries),
                    color = Warning
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "毁伤总面积",
                    value = formatArea(result.totalArea),
                    color = Accent
                )
                StatCard(
                    title = "弹头数量",
                    value = warheadPoints.size.toString(),
                    color = TextPrimary
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "毁伤等级分布",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))

            DamageBarChart(
                levels = result.levels,
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )

            Spacer(Modifier.height(8.dp))

            LevelLegend()

            Spacer(Modifier.height(16.dp))

            Text(
                "弹头落点详情",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))

            WarheadDetailList(
                warheadPoints = warheadPoints,
                targetType = result.targetType.name
            )
        }
    }
}

@Composable
private fun RowScope.StatCard(
    title: String,
    value: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(BgTertiary)
            .padding(10.dp)
    ) {
        Text(
            value,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            title,
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun DamageBarChart(
    levels: List<com.mirvsim.app.model.DamageLevel>,
    modifier: Modifier = Modifier
) {
    val maxArea = levels.maxOfOrNull { it.areaKm2 } ?: 1.0
    val levelColors = listOf(
        Color(0xFFCC0000),
        Color(0xFFFF6600),
        Color(0xFFFF9900),
        Color(0xFFFFCC00)
    )
    val levelLabels = listOf("V毁伤", "IV毁伤", "III毁伤", "II毁伤")

    val barAnim = remember { Animatable(0f) }
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

    LaunchedEffect(Unit) {
        barAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Canvas(modifier = modifier.padding(vertical = 4.dp)) {
        val barCount = levels.size.coerceAtMost(4)
        if (barCount == 0) return@Canvas

        val chartWidth = size.width
        val chartHeight = size.height - 24f
        val barWidth = chartWidth / barCount * 0.4f
        val gap = chartWidth / barCount * 0.15f

        for (i in 0 until barCount) {
            val level = levels[i]
            val ratio = (level.areaKm2 / maxArea).toFloat() * barAnim.value
            val color = levelColors[i % levelColors.size]
            val label = levelLabels[i % levelLabels.size]
            val x = i * (chartWidth / barCount) + gap

            val barHeight = chartHeight * ratio
            val barTop = chartHeight - barHeight

            drawRoundRect(
                color = color.copy(alpha = 0.75f),
                topLeft = Offset(x, barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )

            val measured = textMeasurer.measure(
                text = label,
                style = TextStyle(
                    fontSize = 9.sp,
                    color = Color.White
                )
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x + barWidth / 2 - measured.size.width / 2f,
                    barTop + 4f
                )
            )
        }
    }
}

@Composable
private fun LevelLegend() {
    val items = listOf(
        "V" to Color(0xFFCC0000),
        "IV" to Color(0xFFFF6600),
        "III" to Color(0xFFFF9900),
        "II" to Color(0xFFFFCC00)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    label,
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun WarheadDetailList(
    warheadPoints: List<WarheadPoint>,
    targetType: String
) {
    val sorted = warheadPoints.sortedBy { it.index }
    LazyColumn(
        modifier = Modifier.heightIn(max = 200.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(sorted) { idx, wp ->
            val singleArea = listOf(
                wp.effects.fireball, wp.effects.psi20, wp.effects.psi10,
                wp.effects.psi5, wp.effects.psi3, wp.effects.psi1, wp.effects.thermal
            ).max().let { Math.PI * it * it }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(BgTertiary)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "#${wp.index + 1}",
                            color = Accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "%.4f°, %.4f°".format(wp.lat, wp.lng),
                            color = TextPrimary,
                            fontSize = 11.sp
                        )
                        Text(
                            "HOB: %.0fm · %s".format(
                                wp.hobMeters,
                                when (targetType) {
                                    "urban" -> "城区"
                                    "suburban" -> "郊区"
                                    else -> "乡村"
                                }
                            ),
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatYieldCompact(wp.yieldKt),
                        color = Warning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "%.1f km²".format(singleArea),
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

private fun formatNumber(num: Int): String {
    return when {
        num >= 1_000_000_000 -> "%.1fB".format(num / 1_000_000_000.0)
        num >= 1_000_000 -> "%.1fM".format(num / 1_000_000.0)
        num >= 1_000 -> "%.1fK".format(num / 1_000.0)
        else -> num.toString()
    }
}

private fun formatArea(area: Double): String {
    return when {
        area >= 10_000 -> "%.0fK km²".format(area / 1_000)
        area >= 1 -> "%.0f km²".format(area)
        else -> "%.2f km²".format(area)
    }
}

private fun formatYieldCompact(kt: Double): String {
    return if (kt >= 1000) "%.1fMt".format(kt / 1000) else "%.0fkt".format(kt)
}

@Preview(showBackground = true)
@Composable
fun StatsPanelPreview() {
    val dummyEffects = DamageEffects(
        fireball = 0.5, psi20 = 1.0, psi10 = 2.0, psi5 = 4.0,
        psi3 = 6.0, psi1 = 10.0, thermal = 15.0, radiation = 3.0
    )
    val dummyResult = SimulationResult(
        warheadCount = 1,
        totalArea = 150.0,
        severeArea = 50.0,
        deaths = 1200000,
        injuries = 3500000,
        totalCasualties = 4700000,
        overlapRatio = 0.1,
        damageAreas = emptyMap(),
        singleAreas = emptyMap(),
        effects = dummyEffects,
        targetType = TargetType.urban,
        cityName = "北京",
        levels = listOf(
            DamageLevel("V毁伤", 40.0),
            DamageLevel("IV毁伤", 80.0),
            DamageLevel("III毁伤", 120.0),
            DamageLevel("II毁伤", 200.0)
        )
    )
    val dummyWarheadPoints = listOf(
        WarheadPoint(0, 39.9042, 116.4074, dummyEffects, 100.0, 600.0)
    )

    NukemapTheme {
        StatsPanel(
            result = dummyResult,
            warheadPoints = dummyWarheadPoints,
            onClose = {}
        )
    }
}
