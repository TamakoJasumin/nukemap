package com.mirvsim.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.DashPathEffect
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@Composable
fun MapView(
    targetLat: Double,
    targetLng: Double,
    warheadPoints: List<WarheadPoint>,
    effects: List<NukeEffectsResult>?,
    pickMode: Boolean,
    popupEnabled: Boolean = true,
    tileSource: String = "MAPNIK",
    ringAnimEnabled: Boolean = true,
    onMapClick: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Skip osmdroid initialization in Preview to avoid NullPointerException
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text("MapView (Preview Mode)")
        }
        return
    }

    var showRingAnimation by remember { mutableStateOf(false) }
    val ringAnimProgress = remember { Animatable(0f) }

    var popupInfo by remember { mutableStateOf<PopupInfo?>(null) }

    // Use rememberUpdatedState to keep closures fresh without recreating MapView
    val currentPickMode by rememberUpdatedState(pickMode)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentWarheadPoints by rememberUpdatedState(warheadPoints)
    val currentEffects by rememberUpdatedState(effects)
    val currentTargetLat by rememberUpdatedState(targetLat)
    val currentTargetLng by rememberUpdatedState(targetLng)
    val currentPopupEnabled by rememberUpdatedState(popupEnabled)

    val mapView = remember {
        OSMMapView(context).apply {
            id = android.R.id.content
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            minZoomLevel = 2.0
            maxZoomLevel = 18.0
            controller.setZoom(5.0)
            controller.setCenter(GeoPoint(35.0, 105.0))

            // 根据屏幕密度缩放瓦片，解决高 DPI 下文字和道路过小的问题
            setTilesScaleFactor(
                context.resources.displayMetrics.density.coerceIn(1.0f, 2.5f)
            )
        }
    }

    // MapEventsOverlay handles taps vs pinch-zoom correctly
    LaunchedEffect(Unit) {
        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (currentPickMode) {
                    currentOnMapClick(p.latitude, p.longitude)
                    return true
                }
                if (currentPopupEnabled) {
                    showDamagePopup(mapView, p, currentWarheadPoints, currentEffects) { popupInfo = it }
                }
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        })
        mapView.overlays.add(0, eventsOverlay)
    }

    // [PERF] Only rebuild overlays when data actually changes
    LaunchedEffect(warheadPoints, effects) {
        rebuildOverlays(mapView, currentTargetLat, currentTargetLng,
            currentWarheadPoints, currentEffects)
    }

    LaunchedEffect(targetLat, targetLng) {
        mapView.controller.animateTo(GeoPoint(targetLat, targetLng))
        // 如果当前缩放级别太小（说明是初始状态），自动放大到城市级别
        if (mapView.zoomLevelDouble < 8.0) {
            mapView.controller.setZoom(11.0)
        }
    }

    LaunchedEffect(effects) {
        if (effects != null && effects.isNotEmpty()) {
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

    LaunchedEffect(tileSource) {
        val source = when (tileSource) {
            "USGS_SAT" -> org.osmdroid.tileprovider.tilesource.TileSourceFactory.USGS_SAT
            else -> org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK
        }
        mapView.setTileSource(source)
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = pickMode,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Accent.copy(alpha = 0.9f))
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

        if (showRingAnimation && effects != null) {
            RingExpansionOverlay(
                effects = effects,
                progress = ringAnimProgress.value,
                modifier = Modifier.fillMaxSize()
            )
        }

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

private data class PopupRingInfo(
    val label: String,
    val color: Long,
    val type: String,
    val radiusKm: Double,
    val distKm: Double,
    val areaKm2: Double,
    val description: String,
    val warheadCount: Int
)

private data class PopupInfo(
    val rings: List<PopupRingInfo>,
    val warheadIndex: Int,
    val aimDistKm: Double
)

private val popupMarkers = mutableListOf<Marker>()

private fun showDamagePopup(
    mapView: OSMMapView,
    clickPoint: GeoPoint,
    warheadPoints: List<WarheadPoint>,
    effectsList: List<NukeEffectsResult>?,
    onPopup: (PopupInfo) -> Unit
) {
    if (warheadPoints.isEmpty() || effectsList == null || effectsList.isEmpty()) return

    for (m in popupMarkers) {
        m.closeInfoWindow()
        mapView.overlays.remove(m)
    }
    popupMarkers.clear()

    val clickLat = clickPoint.latitude
    val clickLng = clickPoint.longitude

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

    var closestDist = Double.MAX_VALUE
    for (ring in effects.rings) {
        if (ring.outerRadiusKm > 0 && bestDist <= ring.outerRadiusKm && ring.outerRadiusKm < closestDist) {
            closestDist = ring.outerRadiusKm
        }
    }

    val hasHit = closestDist < Double.MAX_VALUE

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

    if (hasHit && ringLabel != null) {
        val area = Math.PI * closestDist * closestDist
        rings.add(PopupRingInfo(
            label = ringLabel,
            color = ringColor.toLong(),
            type = ringType ?: "",
            radiusKm = closestDist,
            distKm = bestDist,
            areaKm2 = area,
            description = ringDesc ?: "",
            warheadCount = warheadPoints.size
        ))
    }

    val severeLevels = listOf(eff.fireball, eff.psi20, eff.psi10, eff.psi5)
    val showThermal = closestDist != eff.thermal && !severeLevels.contains(closestDist) && eff.thermal > 0 && bestDist <= eff.thermal

    if (showThermal) {
        val thermalColor = effects.rings.firstOrNull { it.outerRadiusKm == eff.thermal }?.color ?: 0xFFADFF2F
        val thermalArea = Math.PI * eff.thermal * eff.thermal
        rings.add(PopupRingInfo(
            label = "热辐射 三度烧伤",
            color = thermalColor.toLong(),
            type = "热辐射",
            radiusKm = eff.thermal,
            distKm = bestDist,
            areaKm2 = thermalArea,
            description = "暴露皮肤三度烧伤，可燃物引燃",
            warheadCount = warheadPoints.size
        ))
    }

    Marker(mapView).apply {
        position = clickPoint
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = createSimpleMarker(mapView.context, 0xFF6B35)
    }.also { popup ->
        mapView.overlays.add(popup)
        popupMarkers.add(popup)
    }
    mapView.invalidate()

    onPopup(PopupInfo(rings = rings, warheadIndex = wh.index, aimDistKm = bestDist))
}

private fun rebuildOverlays(
    mapView: OSMMapView,
    targetLat: Double,
    targetLng: Double,
    warheadPoints: List<WarheadPoint>,
    effects: List<NukeEffectsResult>?
) {
    for (m in popupMarkers) { mapView.overlays.remove(m) }
    popupMarkers.clear()

    val toRemove = mapView.overlays.filter { it !is MapEventsOverlay }
    for (o in toRemove) mapView.overlays.remove(o)

    if (warheadPoints.isNotEmpty() && effects != null) {
        for (wp in warheadPoints) {
            val effect = effects.firstOrNull { e ->
                e.centerLat == wp.lat && e.centerLng == wp.lng
            } ?: effects.firstOrNull() ?: continue

            effect.rings.forEachIndexed { ri, ring ->
                if (ring.outerRadiusKm <= 0) return@forEachIndexed
                val radiusM = ring.outerRadiusKm * 1000.0
                val points = Polygon.pointsAsCircle(
                    GeoPoint(effect.centerLat, effect.centerLng), radiusM
                )
                val dashPattern = when (ri) {
                    4 -> floatArrayOf(4f, 4f)    // psi3
                    5 -> floatArrayOf(2f, 4f)    // psi1
                    3 -> floatArrayOf(8f, 6f)    // psi5
                    6 -> floatArrayOf(10f, 4f, 2f, 4f) // thermal
                    else -> null // fireball=0, psi20=1, psi10=2
                }
                val polygon = Polygon().apply {
                    setPoints(points)
                    fillPaint.style = Paint.Style.FILL
                    fillPaint.isAntiAlias = true
                    fillPaint.color = ((ring.color and 0x00FFFFFF) or (0x15 shl 24)).toInt()
                    outlinePaint.style = Paint.Style.STROKE
                    outlinePaint.isAntiAlias = true
                    outlinePaint.strokeWidth = 2.5f
                    outlinePaint.color = ring.color.toInt()
                    if (dashPattern != null) {
                        outlinePaint.pathEffect = DashPathEffect(dashPattern, 0f)
                    }
                }
                mapView.overlays.add(polygon)
            }

            if (wp.index == 0) {
                effect.rings.forEachIndexed { ri, ring ->
                    if (ring.outerRadiusKm <= 0) return@forEachIndexed
                    val labelPos = GeoPoint(
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

    val targetMarker = Marker(mapView).apply {
        position = GeoPoint(targetLat, targetLng)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = createTargetMarker(mapView.context)
        title = "目标"
    }
    mapView.overlays.add(targetMarker)

    if (warheadPoints.isNotEmpty() && effects != null) {
        for (wp in warheadPoints) {
            val hue = (wp.index.toFloat() / maxOf(warheadPoints.size, 1)) * 300f
            val color = androidx.compose.ui.graphics.Color.hsl(hue, 0.9f, 0.6f)

            val marker = Marker(mapView).apply {
                position = GeoPoint(wp.lat, wp.lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createWarheadMarker(mapView.context, color)
                title = "#%d: %.0fkt".format(wp.index + 1, wp.yieldKt)
                snippet = "%.4f, %.4f".format(wp.lat, wp.lng)
            }
            mapView.overlays.add(marker)
        }
    }

    val eventsOverlay = mapView.overlays.find { it is MapEventsOverlay }
    if (eventsOverlay != null) {
        mapView.overlays.remove(eventsOverlay)
        mapView.overlays.add(eventsOverlay)
    }

    mapView.invalidate()
}

private fun haversineSimple(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2).pow(2)
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))
}

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
            val r = when (i % 4) {
                0 -> 204; 1 -> 255; 2 -> 255; else -> 255
            }
            val g = when (i % 4) {
                0 -> 0;   1 -> 102; 2 -> 153; else -> 204
            }
            val b = when (i % 4) {
                0 -> 0;   1 -> 0;   2 -> 0;   else -> 0
            }
            val alpha = ((1f - progress).coerceAtLeast(0f) * 255).toInt()
            val ringRadius = maxRadius * (0.6f + i * 0.15f).coerceAtMost(1f)

            drawCircle(
                color = Color(android.graphics.Color.argb(
                    (alpha.toFloat() * 0.2f).toInt(), r, g, b)),
                radius = ringRadius,
                center = Offset(centerX, centerY)
            )
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

private fun createTargetMarker(context: android.content.Context): android.graphics.drawable.Drawable {
    val d = context.resources.displayMetrics.density
    val size = (48 * d).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = size / 2f
    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x44FF4444.toInt(); style = Paint.Style.FILL
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

private fun createWarheadMarker(
    context: android.content.Context,
    color: androidx.compose.ui.graphics.Color
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

private fun createSimpleMarker(
    context: android.content.Context,
    colorInt: Int
): android.graphics.drawable.Drawable {
    val d = context.resources.displayMetrics.density
    val size = (12 * d).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt; style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, 5f * d, p)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

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

private fun formatNumber(value: Double, decimals: Int): String {
    return when {
        value >= 10000 -> "%.${decimals}fK".format(value / 1000)
        value >= 1 -> "%.${decimals}f".format(value)
        else -> "%.${decimals}f".format(value)
    }
}

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
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFF3A3F4B)
                    )
                }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatText("半径", "%.2f km".format(ring.radiusKm))
                    StatText("距爆心", "%.2f km".format(ring.distKm))
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "覆盖面积: ",
                        color = Color(0xFF6A6F7A),
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatNumber(ring.areaKm2, 1),
                        color = Color(0xFFE1E4E8),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = " km² (共 ${ring.warheadCount} 弹头)",
                        color = Color(0xFF6A6F7A),
                        fontSize = 10.sp
                    )
                }
                if (ring.description.isNotEmpty()) {
                    Text(
                        text = ring.description,
                        color = Color(0xFF6A6F7A),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
            Divider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = Color(0xFF3A3F4B)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近弹头 #${info.warheadIndex + 1}",
                    color = Color(0xFF6A6F7A),
                    fontSize = 10.sp
                )
                Text(
                    text = "距瞄点 ${"%.2f".format(info.aimDistKm)} km",
                    color = Color(0xFF6A6F7A),
                    fontSize = 10.sp
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text(
                        text = "✕",
                        color = Color(0xFF6A6F7A),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

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
        fireball = 0.5,
        psi20 = 1.0,
        psi10 = 2.0,
        psi5 = 4.0,
        psi3 = 6.0,
        psi1 = 10.0,
        thermal = 15.0,
        radiation = 3.0
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
            centerLat = 39.9042,
            centerLng = 116.4074,
            totalArea = 100.0,
            targetType = "urban"
        )
    )

    NukemapTheme {
        MapView(
            targetLat = 39.9042,
            targetLng = 116.4074,
            warheadPoints = dummyWarheadPoints,
            effects = dummyNukeEffectsResults,
            pickMode = false,
            onMapClick = { _, _ -> },
            modifier = Modifier.fillMaxSize()
        )
    }
}
