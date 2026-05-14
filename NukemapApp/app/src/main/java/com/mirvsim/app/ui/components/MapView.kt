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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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

    // Use rememberUpdatedState to keep closures fresh without recreating MapView
    val currentPickMode by rememberUpdatedState(pickMode)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentWarheadPoints by rememberUpdatedState(warheadPoints)
    val currentEffects by rememberUpdatedState(effects)
    val currentTargetLat by rememberUpdatedState(targetLat)
    val currentTargetLng by rememberUpdatedState(targetLng)

    val mapView = remember {
        OSMMapView(context).apply {
            id = android.R.id.content
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            minZoomLevel = 2.0
            maxZoomLevel = 18.0
            controller.setZoom(3.0)
            controller.setCenter(GeoPoint(35.0, 105.0))
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
                showDamagePopup(mapView, p, currentWarheadPoints, currentEffects)
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
            showRingAnimation = true
            ringAnimProgress.snapTo(0f)
            ringAnimProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
            )
            delay(600)
            showRingAnimation = false
        }
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
    }
}

private val popupMarkers = mutableListOf<Marker>()

private fun showDamagePopup(
    mapView: OSMMapView,
    clickPoint: GeoPoint,
    warheadPoints: List<WarheadPoint>,
    effectsList: List<NukeEffectsResult>?
) {
    if (warheadPoints.isEmpty() || effectsList == null || effectsList.isEmpty()) return

    for (m in popupMarkers) { mapView.overlays.remove(m) }
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

    val effects = effectsList.firstOrNull { it.centerLat == bestWh.lat && it.centerLng == bestWh.lng }
        ?: effectsList.first()
    val eff = bestWh.effects

    var closestRing: NukeEffectsResult? = null
    var closestDist = Double.MAX_VALUE
    for (ring in effects.rings) {
        if (ring.outerRadiusKm > 0 && bestDist <= ring.outerRadiusKm && ring.outerRadiusKm < closestDist) {
            closestDist = ring.outerRadiusKm
            closestRing = effects
        }
    }

    val popupTitle = if (closestRing != null) {
        when (closestDist) {
            eff.fireball -> "火球半径"
            eff.psi20 -> "20 psi 重度毁伤"
            eff.psi10 -> "10 psi 严重毁伤"
            eff.psi5 -> "5 psi 中度毁伤"
            eff.psi3 -> "3 psi 轻度毁伤"
            eff.psi1 -> "1 psi 玻璃碎裂"
            eff.thermal -> "热辐射 三度烧伤"
            else -> "毁伤区域"
        }
    } else {
        "无毁伤"
    }

    val popupSnippet = buildString {
        appendLine("距爆心: %.2f km".format(bestDist))
        appendLine("最近弹头: #%d".format(bestWh!!.index + 1))
        if (closestRing != null) {
            appendLine("半径: %.2f km".format(closestDist))
            when (closestDist) {
                eff.fireball -> append("火球内部一切汽化，无人生还")
                eff.psi20 -> append("钢筋混凝土建筑完全摧毁，致死率接近100%")
                eff.psi10 -> append("大多数建筑物倒塌，致死率极高")
                eff.psi5 -> append("住宅建筑摧毁，广泛人员伤亡")
                eff.psi3 -> append("多数建筑严重受损，伤亡率高")
                eff.psi1 -> append("玻璃碎裂，轻质结构损坏")
                eff.thermal -> append("暴露皮肤三度烧伤，可燃物引燃")
                else -> append("")
            }
        }
    }

    val popup = Marker(mapView).apply {
        position = clickPoint
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = createSimpleMarker(0xFF6B35)
        setTitle(popupTitle)
        setSnippet(popupSnippet)
    }
    mapView.overlays.add(popup)
    popupMarkers.add(popup)
    popup.showInfoWindow()
    mapView.invalidate()
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
                        icon = createLabelIcon(ring.color.toInt())
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
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = size / 2f
    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x44FF4444.toInt(); style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, 22f, glow)
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF4444.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f
    }
    canvas.drawCircle(c, c, 14f, ring)
    val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF4444.toInt(); style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, 4f, dot)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

private fun createWarheadMarker(
    context: android.content.Context,
    color: androidx.compose.ui.graphics.Color
): android.graphics.drawable.Drawable {
    val size = 24
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = size / 2f
    val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb(); style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, 7f, inner)
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 2f
    }
    canvas.drawCircle(c, c, 7f, border)
    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.copy(alpha = 0.3f).toArgb(); style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, 12f, glow)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

private fun createSimpleMarker(colorInt: Int): android.graphics.drawable.Drawable {
    val size = 12
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt; style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, 5f, p)
    return android.graphics.drawable.BitmapDrawable(null, bitmap)
}

private fun createLabelIcon(colorInt: Int): android.graphics.drawable.Drawable {
    val size = 4
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt; style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, 2f, p)
    return android.graphics.drawable.BitmapDrawable(null, bitmap)
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
