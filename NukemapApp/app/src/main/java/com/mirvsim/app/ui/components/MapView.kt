/**
 * 地图视图组件（基于 osmdroid）
 *
 * 核心地图组件，负责：
 * 1. 显示 OpenStreetMap 瓦片地图（支持多种图源动态切换）
 * 2. 渲染目标位置标记（十字准星）
 * 3. 绘制弹头落点标记（彩色圆点）
 * 4. 绘制各级毁伤环（Polygon，支持虚线/实线样式）
 * 5. 点击弹窗显示毁伤详情
 * 6. 环展开动画效果
 *
 * 图源支持：
 * - AUTONAVI: 高德地图（GCJ-02，默认）
 * - MAPNIK: OpenStreetMap
 * - CARTO_LIGHT: CartoDB 浅色高清（512px 瓦片）
 * - USGS_SAT: ESRI 卫星混合图
 * - OPEN_TOPO_MAP: OpenTopoMap 地形图
 */
package com.mirvsim.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.DashPathEffect
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mirvsim.app.model.DamageEffects
import com.mirvsim.app.model.NukeEffectsResult
import com.mirvsim.app.model.RingResult
import com.mirvsim.app.model.WarheadPoint
import com.mirvsim.app.ui.theme.*
import kotlin.math.*
import kotlinx.coroutines.delay
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView as OSMMapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex

/** 高德地图瓦片源（GCJ-02 坐标系） */
private val AUTONAVI = object : OnlineTileSourceBase(
    "AutoNavi", 2, 18, 256, "",
    arrayOf(
        "https://webrd01.is.autonavi.com/appmaptile",
        "https://webrd02.is.autonavi.com/appmaptile",
        "https://webrd03.is.autonavi.com/appmaptile",
        "https://webrd04.is.autonavi.com/appmaptile",
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        return baseUrl + "?lang=zh_cn&size=1&scale=1&style=8&x=" +
                MapTileIndex.getX(pMapTileIndex) + "&y=" +
                MapTileIndex.getY(pMapTileIndex) + "&z=" +
                MapTileIndex.getZoom(pMapTileIndex)
    }
}

/** CartoDB 浅色高清瓦片源（512px Retina 级别） */
private val CARTO_LIGHT_RETINA = XYTileSource(
    "CartoDB_Light_Retina",
    0, 19, 512, "@2x.png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/light_all/",
        "https://b.basemaps.cartocdn.com/light_all/",
        "https://c.basemaps.cartocdn.com/light_all/",
        "https://d.basemaps.cartocdn.com/light_all/",
    )
)

@Composable
fun MapView(
    targetLat: Double,
    targetLng: Double,
    warheadPoints: List<WarheadPoint>,
    effects: List<NukeEffectsResult>?,
    pickMode: Boolean,
    onMapClick: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
    myLat: Double = 0.0,
    myLng: Double = 0.0,
    myLocationTrigger: Int = 0,
    popupEnabled: Boolean = true,
    tileSource: String = "AUTONAVI",
    ringAnimEnabled: Boolean = true,
) {
    val context = LocalContext.current

    // 预览模式下跳过 osmdroid 初始化
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text("MapView (Preview Mode)")
        }
        return
    }

    val useGcj02 = tileSource == "AUTONAVI"

    var showRingAnimation by remember { mutableStateOf(value = false) }
    val ringAnimProgress = remember { Animatable(0f) }

    var popupInfo by remember { mutableStateOf<PopupInfo?>(null) }

    // 使用 rememberUpdatedState 保持闭包最新，避免每次重组重建 MapView
    val currentPickMode by rememberUpdatedState(pickMode)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentWarheadPoints by rememberUpdatedState(warheadPoints)
    val currentEffects by rememberUpdatedState(effects)
    val currentTargetLat by rememberUpdatedState(targetLat)
    val currentTargetLng by rememberUpdatedState(targetLng)
    val currentMyLat by rememberUpdatedState(myLat)
    val currentMyLng by rememberUpdatedState(myLng)
    val currentPopupEnabled by rememberUpdatedState(popupEnabled)
    val currentUseGcj02 by rememberUpdatedState(useGcj02)

    // 创建 osmdroid MapView 实例（仅创建一次）
    val mapView = remember {
        val density = context.resources.displayMetrics.density
        OSMMapView(context).apply {
            id = android.R.id.content
            setTileSource(AUTONAVI)
            setMultiTouchControls(true)                       // 启用手势缩放
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 2.0
            maxZoomLevel = 18.0
            controller.setZoom(5.0)                           // 默认缩放级别
            if (useGcj02) {
                val g = CoordTransform.wgs84togcj02(105.0, 35.0)
                controller.setCenter(GeoPoint(g[1], g[0]))
            } else {
                controller.setCenter(GeoPoint(35.0, 105.0))
            }

            // 降低瓦片缩放上限以减少拉伸模糊，高 DPI 下字体更清晰
            setTilesScaleFactor(
                density.coerceIn(1.0f, 1.5f)
            )
        }
    }

    // 添加地图点击事件监听器
    LaunchedEffect(Unit) {
        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (currentPickMode) {
                    if (currentUseGcj02) {
                        val g = CoordTransform.gcj02towgs84(p.longitude, p.latitude)
                        currentOnMapClick(g[1], g[0])
                    } else {
                        currentOnMapClick(p.latitude, p.longitude)
                    }
                    return true
                }
                if (currentPopupEnabled) {
                    // 传原始地图坐标（GCJ-02）用于标记位置，传递 WGS-84 用于距离计算
                    showDamagePopup(mapView, p, currentWarheadPoints, currentEffects, currentUseGcj02) { popupInfo = it }
                }
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        })
        mapView.overlays.add(0, eventsOverlay)
    }

    // 数据变化时重建覆盖层（标记、毁伤环等）
    LaunchedEffect(warheadPoints, effects, targetLat, targetLng, myLat, myLng, useGcj02) {
        rebuildOverlays(mapView, currentTargetLat, currentTargetLng,
            currentMyLat, currentMyLng, currentWarheadPoints, currentEffects, useGcj02)
    }

    // 目标位置变化或点击回到我的位置时，地图自动移动
    LaunchedEffect(targetLat, targetLng, myLocationTrigger, useGcj02) {
        if (targetLat == 0.0 && targetLng == 0.0) return@LaunchedEffect
        val center = if (useGcj02) {
            val g = CoordTransform.wgs84togcj02(targetLng, targetLat)
            GeoPoint(g[1], g[0])
        } else {
            GeoPoint(targetLat, targetLng)
        }
        mapView.controller.animateTo(center)
        if (mapView.zoomLevelDouble < 8.0) {
            mapView.controller.setZoom(12.0)  // 初次定位或回到位置自动放大
        }
    }

    // 环展开动画效果
    LaunchedEffect(effects) {
        if (!effects.isNullOrEmpty()) {
            showRingAnimation = ringAnimEnabled
            ringAnimProgress.snapTo(0f)
            ringAnimProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
            )
            delay(600)
            showRingAnimation = false
        }
    }

    // 图源切换
    LaunchedEffect(tileSource) {
        val density = context.resources.displayMetrics.density
        val adaptiveScale = (density * 0.6f).coerceIn(1.2f, 2.0f)
        
        when (tileSource) {
            "AUTONAVI" -> {
                mapView.setTileSource(AUTONAVI)
                mapView.setTilesScaleFactor(
                    (density / 2.0f).coerceIn(1.0f, 1.4f)
                )
            }
            "CARTO_LIGHT" -> {
                mapView.setTileSource(CARTO_LIGHT_RETINA)
                mapView.setTilesScaleFactor((density / 1.8f).coerceIn(1.0f, 1.6f))
            }
            "USGS_SAT" -> {
                val esriHybrid = object : OnlineTileSourceBase(
                    "ESRI_Satellite_Hybrid", 0, 19, 256, "",
                    arrayOf("https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
                ) {
                    override fun getTileURLString(pMapTileIndex: Long): String {
                        return baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                                MapTileIndex.getY(pMapTileIndex) + "/" +
                                MapTileIndex.getX(pMapTileIndex)
                    }
                }
                mapView.setTileSource(esriHybrid)
                mapView.setTilesScaleFactor(adaptiveScale)
            }
            "OPEN_TOPO_MAP" -> {
                mapView.setTileSource(TileSourceFactory.OpenTopo)
                mapView.setTilesScaleFactor(adaptiveScale)
            }
            else -> {
                mapView.setTileSource(TileSourceFactory.MAPNIK)
                mapView.setTilesScaleFactor(adaptiveScale)
            }
        }
    }

    Box(modifier = modifier) {
        // osmdroid 地图 View
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // 拾取模式提示
        AnimatedVisibility(
            visible = pickMode,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NukeOrange.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "点击地图选择目标位置",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 环展开动画覆盖层
        if (showRingAnimation && effects != null) {
            RingExpansionOverlay(
                effects = effects,
                progress = ringAnimProgress.value,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 点击弹窗信息卡片
        popupInfo?.let { info ->
            PopupCard(
                info = info,
                onDismiss = { popupInfo = null },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(start = 8.dp, end = 8.dp, top = 148.dp)
            )
        }
    }
}

// ====================================================================
// 弹窗数据模型
// ====================================================================

/** 弹窗中显示的单个毁伤环信息 */
private data class PopupRingInfo(
    val label: String,         // 显示名称
    val color: Long,           // 颜色
    val type: String,          // 类型（冲击波/热辐射）
    val radiusKm: Double,      // 半径
    val distKm: Double,        // 距爆心距离
    val areaKm2: Double,       // 覆盖面积
    val description: String,   // 毁伤描述
    val warheadCount: Int      // 弹头总数
)

/** 弹窗完整信息 */
private data class PopupInfo(
    val rings: List<PopupRingInfo>,
    val warheadIndex: Int,  // 最近弹头索引
    val aimDistKm: Double   // 点击点距瞄点距离
)

/** 已显示的弹窗标记列表（用于清理） */
private val popupMarkers = mutableListOf<Marker>()

/**
 * 处理地图点击，显示最近弹头的毁伤详情弹窗
 *
 * 流程：
 * 1. 找到离点击点最近的弹头
 * 2. 确定该弹头在点击点处造成的最高毁伤等级
 * 3. 同时显示热辐射信息（如果非最严重等级）
 * 4. 在点击点添加临时标记
 * 5. 通过回调触发 PopupCard 显示
 */
private fun showDamagePopup(
    mapView: OSMMapView,
    clickPoint: GeoPoint,
    warheadPoints: List<WarheadPoint>,
    effectsList: List<NukeEffectsResult>?,
    useGcj02: Boolean = false,
    onPopup: (PopupInfo) -> Unit
) {
    if (warheadPoints.isEmpty() || effectsList.isNullOrEmpty()) return

    // 清除之前的弹窗标记
    for (m in popupMarkers) {
        m.closeInfoWindow()
        mapView.overlays.remove(m)
    }
    popupMarkers.clear()

    // 将点击坐标统一为 WGS-84 用于与弹头数据做距离计算
    val (clickLat, clickLng) = if (useGcj02) {
        val g = CoordTransform.gcj02towgs84(clickPoint.longitude, clickPoint.latitude)
        g[1] to g[0]
    } else {
        clickPoint.latitude to clickPoint.longitude
    }

    // 找到离点击点最近的弹头
    var bestDist = Double.MAX_VALUE
    var bestWh: WarheadPoint? = null
    for (wh in warheadPoints) {
        val d = haversineSimple(clickLat, clickLng, wh.lat, wh.lng)
        if (d < bestDist) { bestDist = d; bestWh = wh }
    }
    if (bestWh == null) return

    val wh = bestWh
    val effects = effectsList.firstOrNull { it.centerLat == wh.lat && it.centerLng == wh.lng }
        ?: effectsList.first()
    val eff = wh.effects

    // 确定点击点处在哪个毁伤环内
    var closestDist = Double.MAX_VALUE
    for (ring in effects.rings) {
        if (ring.outerRadiusKm > 0 && bestDist <= ring.outerRadiusKm && ring.outerRadiusKm < closestDist) {
            closestDist = ring.outerRadiusKm
        }
    }

    val hasHit = closestDist < Double.MAX_VALUE

    // 通过半径匹配确定毁伤等级标签
    val ringLabel = when (closestDist) {
        eff.fireball -> "火球半径"
        eff.psi20 -> "20 psi 重度毁伤"
        eff.psi10 -> "10 psi 严重毁伤"
        eff.psi5 -> "5 psi 中度毁伤"
        eff.psi3 -> "3 psi 轻度毁伤"
        eff.psi1 -> "1 psi 玻璃碎裂"
        eff.thermal -> "热辐射 三度烧伤"
        else -> null
    }

    val ringType = when (closestDist) {
        eff.fireball, eff.psi20, eff.psi10, eff.psi5, eff.psi3, eff.psi1 -> "冲击波"
        eff.thermal -> "热辐射"
        else -> null
    }

    val ringDesc = when (closestDist) {
        eff.fireball -> "火球内部一切汽化，无人生还"
        eff.psi20 -> "钢筋混凝土建筑完全摧毁，致死率接近100%"
        eff.psi10 -> "大多数建筑物倒塌，致死率极高"
        eff.psi5 -> "住宅建筑摧毁，广泛人员伤亡"
        eff.psi3 -> "多数建筑严重受损，伤亡率高"
        eff.psi1 -> "玻璃碎裂，轻质结构损坏"
        eff.thermal -> "暴露皮肤三度烧伤，可燃物引燃"
        else -> null
    }

    val ringColor = effects.rings.firstOrNull { it.outerRadiusKm == closestDist }?.color ?: 0xFF888888

    val rings = mutableListOf<PopupRingInfo>()

    // 主毁伤环信息
    if (hasHit && ringLabel != null) {
        val area = Math.PI * closestDist * closestDist
        rings.add(PopupRingInfo(
            label = ringLabel,
            color = ringColor,
            type = ringType ?: "",
            radiusKm = closestDist,
            distKm = bestDist,
            areaKm2 = area,
            description = ringDesc ?: "",
            warheadCount = warheadPoints.size
        ))
    }

    // 如果主毁伤不是热辐射，单独显示热辐射信息
    val severeLevels = listOf(eff.fireball, eff.psi20, eff.psi10, eff.psi5)
    val showThermal = closestDist != eff.thermal && !severeLevels.contains(closestDist) && eff.thermal > 0 && bestDist <= eff.thermal

    if (showThermal) {
        val thermalColor = effects.rings.firstOrNull { it.outerRadiusKm == eff.thermal }?.color ?: 0xFFADFF2F
        val thermalArea = Math.PI * eff.thermal * eff.thermal
        rings.add(PopupRingInfo(
            label = "热辐射 三度烧伤",
            color = thermalColor,
            type = "热辐射",
            radiusKm = eff.thermal,
            distKm = bestDist,
            areaKm2 = thermalArea,
            description = "暴露皮肤三度烧伤，可燃物引燃",
            warheadCount = warheadPoints.size
        ))
    }

    // 在点击位置添加临时标记点
    Marker(mapView).apply {
        position = clickPoint
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = createSimpleMarker(mapView.context)
    }.also { popup ->
        mapView.overlays.add(popup)
        popupMarkers.add(popup)
    }
    mapView.invalidate()

    onPopup(PopupInfo(rings = rings, warheadIndex = wh.index, aimDistKm = bestDist))
}

/**
 * 重建地图覆盖层
 *
 * 清除旧的覆盖层后重新添加：
 * 1. 各弹头的毁伤环（Polygon，带虚线样式）
 * 2. 目标位置标记（十字准星）
 * 3. 设备位置标记（蓝色圆点）
 * 4. 弹头落点标记（彩色圆点，HLS 色相区分）
 * 5. 第一个弹头的毁伤环标签
 */
private fun rebuildOverlays(
    mapView: OSMMapView,
    targetLat: Double,
    targetLng: Double,
    myLat: Double,
    myLng: Double,
    warheadPoints: List<WarheadPoint>,
    effects: List<NukeEffectsResult>?,
    useGcj02: Boolean = false
) {
    fun toGcj(lat: Double, lng: Double): GeoPoint {
        if (!useGcj02) return GeoPoint(lat, lng)
        val g = CoordTransform.wgs84togcj02(lng, lat)
        return GeoPoint(g[1], g[0])
    }
    // 清除旧的弹窗标记
    for (m in popupMarkers) { mapView.overlays.remove(m) }
    popupMarkers.clear()

    // 清除除事件监听器外的所有覆盖层
    val toRemove = mapView.overlays.filter { it !is MapEventsOverlay }
    for (o in toRemove) mapView.overlays.remove(o)

    if (warheadPoints.isNotEmpty() && effects != null) {
        for (wp in warheadPoints) {
            val effect = effects.firstOrNull { e ->
                e.centerLat == wp.lat && e.centerLng == wp.lng
            } ?: effects.firstOrNull() ?: continue

            // 绘制各级毁伤环
            effect.rings.forEachIndexed { ri, ring ->
                if (ring.outerRadiusKm <= 0) return@forEachIndexed
                val radiusM = ring.outerRadiusKm * 1000.0
                val points = Polygon.pointsAsCircle(
                    toGcj(effect.centerLat, effect.centerLng), radiusM
                )
                // 不同毁伤等级使用不同虚线样式
                val dashPattern = when (ri) {
                    4 -> floatArrayOf(4f, 4f)    // psi3: 短虚线
                    5 -> floatArrayOf(2f, 4f)    // psi1: 点线
                    3 -> floatArrayOf(8f, 6f)    // psi5: 长虚线
                    6 -> floatArrayOf(10f, 4f, 2f, 4f) // thermal: 点划线
                    else -> null // fireball/psi20/psi10: 实线
                }
                val polygon = Polygon().apply {
                    setPoints(points)
                    fillPaint.style = Paint.Style.FILL
                    fillPaint.isAntiAlias = true
                    fillPaint.color = ((ring.color and 0x00FFFFFF) or (0x15 shl 24)).toInt()  // ~8% 透明度填充
                    outlinePaint.style = Paint.Style.STROKE
                    outlinePaint.isAntiAlias = true
                    outlinePaint.strokeWidth = 2.5f
                    outlinePaint.color = ring.color.toInt()
                    dashPattern?.let {
                        outlinePaint.pathEffect = DashPathEffect(it, 0f)
                    }
                }
                mapView.overlays.add(polygon)
            }

            // 第一个弹头的毁伤环标签（小圆点标记半径位置）
            if (wp.index == 0) {
                effect.rings.forEach { ring ->
                    if (ring.outerRadiusKm <= 0) return@forEach
                    val labelPos = toGcj(
                        effect.centerLat + ring.outerRadiusKm / 111.32 * 1.02,
                        effect.centerLng
                    )
                    val labelMarker = Marker(mapView).apply {
                        position = labelPos
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = createLabelIcon(mapView.context, ring.color.toInt())
                        setInfoWindow(null)
                    }
                    mapView.overlays.add(labelMarker)
                }
            }
        }
    }

    // 目标位置标记
    val targetMarker = Marker(mapView).apply {
        position = toGcj(targetLat, targetLng)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = createTargetMarker(mapView.context)
        title = "目标"
    }
    mapView.overlays.add(targetMarker)

    // 弹头落点标记（每个弹头使用 HLS 色相区分颜色）
    if (warheadPoints.isNotEmpty() && effects != null) {
        for (wp in warheadPoints) {
            val hue = (wp.index.toFloat() / maxOf(warheadPoints.size, 1)) * 300f
            val color = Color.hsl(hue, 0.9f, 0.6f)

            val marker = Marker(mapView).apply {
                position = toGcj(wp.lat, wp.lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createWarheadMarker(mapView.context, color)
                title = "#%d: %.0fkt".format(wp.index + 1, wp.yieldKt)
                snippet = "%.4f, %.4f".format(wp.lat, wp.lng)
            }
            mapView.overlays.add(marker)
        }
    }

    // 设备当前位置标记（蓝色圆点，仅在有效坐标时显示）
    // 放在最后添加以确保显示在弹头标记之上
    if (myLat != 0.0 && myLng != 0.0) {
        val myLocationMarker = Marker(mapView).apply {
            position = toGcj(myLat, myLng)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createMyLocationMarker(mapView.context)
            title = "我的位置"
            setInfoWindow(null)
        }
        mapView.overlays.add(myLocationMarker)
    }

    // 将事件监听器移到最上层，确保点击响应
    val eventsOverlay = mapView.overlays.find { it is MapEventsOverlay }
    if (eventsOverlay != null) {
        mapView.overlays.remove(eventsOverlay)
        mapView.overlays.add(eventsOverlay)
    }

    mapView.invalidate()
}

/** 简化的 Haversine 距离计算 */
private fun haversineSimple(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

/**
 * 环展开动画覆盖层
 *
 * 模拟核爆瞬间的白色光环扩散效果，从中心向外扩散并逐渐消失。
 * 使用 Compose Canvas 绘制多层彩色光环。
 */
@Composable
private fun RingExpansionOverlay(
    effects: List<NukeEffectsResult>,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (effects.isEmpty()) return@Canvas
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = minOf(size.width, size.height) * 0.45f * progress
        for (i in effects.indices) {
            val r = when (i % 4) { 0 -> 204; 1 -> 255; 2 -> 255; else -> 255 }
            val g = when (i % 4) { 0 -> 0;   1 -> 102; 2 -> 153; else -> 204 }
            val b = when (i % 4) { 0 -> 0;   1 -> 0;   2 -> 0;   else -> 0 }
            val alpha = ((1f - progress).coerceAtLeast(0f) * 255).toInt()
            val ringRadius = maxRadius * (0.6f + i * 0.15f).coerceAtMost(1f)

            // 填充圆
            drawCircle(
                color = Color(android.graphics.Color.argb(
                    (alpha.toFloat() * 0.2f).toInt(), r, g, b)),
                radius = ringRadius,
                center = Offset(centerX, centerY)
            )
            // 描边圆
            drawCircle(
                color = Color(android.graphics.Color.argb(
                    (alpha.toFloat() * 0.5f).toInt(), r, g, b)),
                radius = ringRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )
        }
    }
}

// ====================================================================
// 标记图标生成器（Canvas 绘制）
// ====================================================================

/** 创建目标位置标记（红色十字准星，带发光效果） */
private fun createTargetMarker(context: android.content.Context): android.graphics.drawable.Drawable {
    val d = context.resources.displayMetrics.density
    val size = (48 * d).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = size / 2f
    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x44FF4444; style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, 22f * d, glow)
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF4444.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f * d
    }
    canvas.drawCircle(c, c, 14f * d, ring)
    val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF4444.toInt(); style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, 4f * d, dot)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

/** 创建设备当前位置标记（蓝色脉冲圆点） */
private fun createMyLocationMarker(context: android.content.Context): android.graphics.drawable.Drawable {
    val d = context.resources.displayMetrics.density
    val size = (32 * d).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = size / 2f
    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x221A73E8; style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, 14f * d, glow)
    val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1A73E8.toInt(); style = Paint.Style.STROKE; strokeWidth = 2.5f * d
    }
    canvas.drawCircle(c, c, 9f * d, outer)
    val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1A73E8.toInt(); style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, 4.5f * d, inner)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

/** 创建弹头落点标记（彩色圆点，带白色描边和发光） */
private fun createWarheadMarker(
    context: android.content.Context,
    color: Color
): android.graphics.drawable.Drawable {
    val d = context.resources.displayMetrics.density
    val size = (24 * d).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = size / 2f
    val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb(); style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, 7f * d, inner)
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 2f * d
    }
    canvas.drawCircle(c, c, 7f * d, border)
    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.copy(alpha = 0.3f).toArgb(); style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, 12f * d, glow)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

/** 创建简单点击标记点 */
private fun createSimpleMarker(
    context: android.content.Context
): android.graphics.drawable.Drawable {
    val d = context.resources.displayMetrics.density
    val size = (12 * d).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6B35; style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, 5f * d, p)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

/** 创建毁伤环标签标记（小圆点） */
private fun createLabelIcon(
    context: android.content.Context,
    colorInt: Int
): android.graphics.drawable.Drawable {
    val d = context.resources.displayMetrics.density
    val size = (6 * d).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt; style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, 3f * d, p)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

/** 格式化数值显示（K 单位） */
private fun formatNumber(value: Double): String {
    return when {
        value >= 10000 -> "%.1fK".format(value / 1000)
        value >= 1 -> "%.1f".format(value)
        else -> "%.1f".format(value)
    }
}

// ====================================================================
// 弹窗信息卡片组件
// ====================================================================

/**
 * 毁伤详情弹窗卡片
 *
 * 显示点击位置处各毁伤环的详细信息：
 * - 毁伤等级名称和类型
 * - 半径和距爆心距离
 * - 覆盖面积
 * - 毁伤描述
 */
@Composable
private fun PopupCard(
    info: PopupInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            info.rings.forEachIndexed { idx, ring ->
                if (idx > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFF3A3F4B)
                    )
                }
                // 等级标题行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(ring.color.toInt()))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = ring.label,
                        color = Color(ring.color.toInt()),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = ring.type,
                        color = Color(0xFF6A6F7A),
                        fontSize = 10.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                // 半径和距离
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatText("半径", "%.2f km".format(ring.radiusKm))
                    StatText("距爆心", "%.2f km".format(ring.distKm))
                }
                Spacer(Modifier.height(2.dp))
                // 覆盖面积
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "覆盖面积: ", color = Color(0xFF6A6F7A), fontSize = 11.sp)
                    Text(text = formatNumber(ring.areaKm2), color = Color(0xFFE1E4E8),
                        fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Text(text = " km² (共 ${ring.warheadCount} 弹头)", color = Color(0xFF6A6F7A), fontSize = 10.sp)
                }
                // 毁伤描述
                if (ring.description.isNotEmpty()) {
                    Text(text = ring.description, color = Color(0xFF6A6F7A), fontSize = 11.sp,
                        modifier = Modifier.padding(top = 3.dp))
                }
            }
            // 底部信息
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFF3A3F4B))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "最近弹头 #${info.warheadIndex + 1}", color = Color(0xFF6A6F7A), fontSize = 10.sp)
                Text(text = "距瞄点 ${"%.2f".format(info.aimDistKm)} km", color = Color(0xFF6A6F7A), fontSize = 10.sp)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Text(text = "✕", color = Color(0xFF6A6F7A), fontSize = 12.sp)
                }
            }
        }
    }
}

/** 统计数据行（标签+数值） */
@Composable
private fun StatText(label: String, value: String) {
    Row {
        Text(text = label, color = Color(0xFFA0A6B0), fontSize = 12.sp)
        Text(text = value, color = Color(0xFFE1E4E8), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun MapViewPreview() {
    val dummyEffects = DamageEffects(
        fireball = 0.5, psi20 = 1.0, psi10 = 2.0, psi5 = 4.0,
        psi3 = 6.0, psi1 = 10.0, thermal = 15.0, radiation = 3.0
    )
    val dummyWarheadPoints = listOf(
        WarheadPoint(0, 39.9042, 116.4074, dummyEffects, 100.0, 600.0)
    )
    val dummyNukeEffectsResults = listOf(
        NukeEffectsResult(
            rings = listOf(
                RingResult(0.5, 0xFFFF0000),
                RingResult(1.0, 0xFFFF4500),
                RingResult(2.0, 0xFFFF8C00),
                RingResult(4.0, 0xFFFFA500),
                RingResult(6.0, 0xFFFFD700),
                RingResult(10.0, 0xFFFFFF00),
                RingResult(15.0, 0xFFADFF2F)
            ),
            centerLat = 39.9042, centerLng = 116.4074,
            totalArea = 100.0, targetType = "urban"
        )
    )
    NukemapTheme {
        MapView(
            targetLat = 39.9042, targetLng = 116.4074,
            warheadPoints = dummyWarheadPoints,
            effects = dummyNukeEffectsResults,
            pickMode = false, onMapClick = { _, _ -> },
            modifier = Modifier.fillMaxSize()
        )
    }
}
