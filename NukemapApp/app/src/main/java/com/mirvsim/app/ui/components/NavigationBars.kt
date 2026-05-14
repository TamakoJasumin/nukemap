package com.mirvsim.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mirvsim.app.ui.theme.*

/**
 * 底部导航栏项目
 */
enum class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val description: String
) {
    SIMULATE(
        title = "模拟",
        selectedIcon = Icons.Filled.RocketLaunch,
        unselectedIcon = Icons.Outlined.RocketLaunch,
        description = "核武器模拟"
    ),
    PRESETS(
        title = "预设",
        selectedIcon = Icons.Filled.FolderOpen,
        unselectedIcon = Icons.Outlined.FolderOpen,
        description = "预设场景"
    ),
    STATS(
        title = "统计",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        description = "模拟统计"
    ),
    SETTINGS(
        title = "设置",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        description = "应用设置"
    )
}

/**
 * 底部导航栏组件
 * 用于手机和平板竖屏模式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
    currentRoute: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit,
    hasSimulationResult: Boolean,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = BgSecondary,
        contentColor = TextPrimary,
        tonalElevation = 0.dp
    ) {
        BottomNavItem.entries.forEach { item ->
            val selected = currentRoute == item
            val showBadge = item == BottomNavItem.STATS && hasSimulationResult

            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(item) },
                icon = {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            modifier = Modifier.size(24.dp)
                        )
                        // 统计结果徽章
                        if (showBadge) {
                            Badge(
                                modifier = Modifier
                                    .offset(x = 8.dp, y = (-4).dp),
                                containerColor = Accent
                            ) {
                                Text(
                                    "●",
                                    fontSize = 8.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Accent,
                    selectedTextColor = Accent,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = Accent.copy(alpha = 0.15f)
                )
            )
        }
    }
}

/**
 * 侧边导航栏组件
 * 用于大屏幕和横屏模式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideNavigationRail(
    currentRoute: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit,
    hasSimulationResult: Boolean,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = BgSecondary,
        contentColor = TextPrimary
    ) {
        Spacer(Modifier.height(8.dp))

        BottomNavItem.entries.forEach { item ->
            val selected = currentRoute == item
            val showBadge = item == BottomNavItem.STATS && hasSimulationResult

            NavigationRailItem(
                selected = selected,
                onClick = { onItemSelected(item) },
                icon = {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            modifier = Modifier.size(24.dp)
                        )
                        if (showBadge) {
                            Badge(
                                modifier = Modifier
                                    .offset(x = 6.dp, y = (-4).dp),
                                containerColor = Accent
                            ) {
                                Text(
                                    "●",
                                    fontSize = 8.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Accent,
                    selectedTextColor = Accent,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}

/**
 * 侧边抽屉组件
 * 用于大屏幕设备的可折叠侧边栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideDrawer(
    currentRoute: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit,
    hasSimulationResult: Boolean,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = slideInHorizontally { -it },
        exit = slideOutHorizontally { -it }
    ) {
        PermanentDrawerSheet(
            modifier = modifier.width(200.dp),
            drawerContainerColor = BgSecondary
        ) {
            Spacer(Modifier.height(16.dp))

            // 头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MIRV Sim",
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(
                    onClick = onExpandToggle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "收起",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 导航项
            BottomNavItem.entries.forEach { item ->
                val selected = currentRoute == item
                val showBadge = item == BottomNavItem.STATS && hasSimulationResult

                Surface(
                    onClick = { onItemSelected(item) },
                    color = if (selected) Accent.copy(alpha = 0.12f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                tint = if (selected) Accent else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            if (showBadge) {
                                Badge(
                                    modifier = Modifier
                                        .offset(x = 8.dp, y = (-4).dp),
                                    containerColor = Accent
                                ) {
                                    Text("●", fontSize = 8.sp, color = Color.White)
                                }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = item.title,
                                color = if (selected) Accent else TextPrimary,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            Text(
                                text = item.description,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 底部信息
            Divider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = BorderColor
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "版本 1.0.0",
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    // 展开按钮（当侧边栏收起时显示）
    if (!isExpanded) {
        SmallFloatingActionButton(
            onClick = onExpandToggle,
            containerColor = BgSecondary,
            contentColor = Accent,
            shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "展开菜单",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
