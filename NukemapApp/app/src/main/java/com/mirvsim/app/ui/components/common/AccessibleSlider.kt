/**
 * 可访问的滑块组件
 *
 * 在 Material 3 Slider 基础上增强无障碍支持：
 * - 提供完整的语义描述（contentDescription）
 * - 支持屏幕阅读器（TalkBack）正确朗读滑块信息
 * - 显示当前值和范围描述
 * - 符合 WCAG 无障碍最佳实践
 */
package com.mirvsim.app.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 可访问的滑块组件
 *
 * @param label 滑块标签文字
 * @param value 当前值
 * @param valueText 当前值显示文本
 * @param range 取值范围 [start, endInclusive]
 * @param onValueChange 值变化回调
 * @param modifier 修饰符
 * @param steps 步进数（0 表示连续滑动）
 * @param valueRangeDescription 无障碍描述中取值范围说明（可选）
 */
@Composable
fun AccessibleSlider(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    valueRangeDescription: String? = null
) {
    Column(
        modifier = modifier
            .semantics {
                contentDescription = "$label: 当前值 $valueText"
            }
    ) {
        // 标签和当前值行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Material 3 Slider
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = valueRangeDescription
                        ?: "$label，从 ${range.start.toInt()} 到 ${range.endInclusive.toInt()}"
                },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
