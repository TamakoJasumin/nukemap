/**
 * 加载指示器组件
 *
 * 提供两种加载指示器：
 * - LoadingIndicator：全屏居中加载状态（带可选文字说明）
 * - InlineLoadingIndicator：内联加载状态（用于按钮等小区域）
 *
 * 使用无限重复旋转动画表示加载中的状态。
 */
package com.mirvsim.app.ui.components.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

/**
 * 加载指示器（带旋转动画和可选文字说明）
 *
 * 适用于全屏加载状态、数据加载中等场景。
 *
 * @param modifier 修饰符
 * @param message 加载说明文字（可选）
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    // 无限重复旋转动画（1000ms 一周，线性匀速）
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
                .rotate(rotation),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        
        if (message != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 内联加载指示器（小尺寸）
 *
 * 适用于按钮内部、列表项等小区域。
 *
 * @param modifier 修饰符
 * @param color 指示器颜色（默认白色）
 */
@Composable
fun InlineLoadingIndicator(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White
) {
    CircularProgressIndicator(
        modifier = modifier.size(16.dp),
        color = color,
        strokeWidth = 2.dp
    )
}
