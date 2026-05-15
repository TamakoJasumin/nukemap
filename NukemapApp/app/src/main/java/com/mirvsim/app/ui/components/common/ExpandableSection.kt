/**
 * 可展开/收起的内容区域组件
 *
 * 提供带标题的可折叠内容区域，支持：
 * - 标题点击切换展开/收起状态
 * - 展开/收起动画（垂直展开/收缩）
 * - 箭头旋转动画指示状态
 * - 遵循 Material 3 设计规范
 */
package com.mirvsim.app.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

/**
 * 可展开/收起的内容区域
 *
 * @param title 分区标题
 * @param expanded 是否展开
 * @param onToggle 展开/收起切换回调
 * @param modifier 修饰符
 * @param content 内容区域（ColumnScope 内）
 */
@Composable
fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 可点击的标题栏
        Surface(
            onClick = onToggle,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 内容区域（带动画）
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

/**
 * 带旋转动画的箭头图标
 *
 * 根据展开状态控制箭头旋转角度（0° → 180°）。
 *
 * @param expanded 是否展开
 * @param modifier 修饰符
 */
@Composable
private fun AnimatedArrow(
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "arrowRotation"
    )
    
    Icon(
        imageVector = Icons.Filled.ExpandMore,
        contentDescription = if (expanded) "收起" else "展开",
        modifier = modifier.rotate(rotation)
    )
}
