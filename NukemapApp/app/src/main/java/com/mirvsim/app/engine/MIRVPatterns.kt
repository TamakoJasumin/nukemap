package com.mirvsim.app.engine

import kotlin.math.*

object MIRVPatterns {

    data class LatLng(val lat: Double, val lng: Double)

    fun circular(count: Int, centerLat: Double, centerLng: Double, separationKm: Double): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val radius = separationKm
        for (i in 0 until count) {
            val angle = (2 * PI * i) / count - PI / 2
            val dLat = (radius * sin(angle)) / 111.32
            val dLng = (radius * cos(angle)) / (111.32 * cos(centerLat * PI / 180))
            points.add(LatLng(centerLat + dLat, centerLng + dLng))
        }
        return points
    }

    fun linear(count: Int, centerLat: Double, centerLng: Double, separationKm: Double): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val totalLen = separationKm * (count - 1)
        val angle = 30 * PI / 180
        for (i in 0 until count) {
            val offset = -totalLen / 2 + (totalLen * i) / max(count - 1, 1).toDouble()
            val dLat = (offset * sin(angle)) / 111.32
            val dLng = (offset * cos(angle)) / (111.32 * cos(centerLat * PI / 180))
            points.add(LatLng(centerLat + dLat, centerLng + dLng))
        }
        return points
    }

    fun elliptical(count: Int, centerLat: Double, centerLng: Double, separationKm: Double): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val a = separationKm * 1.5
        val b = separationKm * 0.7
        for (i in 0 until count) {
            val angle = (2 * PI * i) / count - PI / 2
            val dLat = (b * sin(angle)) / 111.32
            val dLng = (a * cos(angle)) / (111.32 * cos(centerLat * PI / 180))
            points.add(LatLng(centerLat + dLat, centerLng + dLng))
        }
        return points
    }

    fun grid(count: Int, centerLat: Double, centerLng: Double, separationKm: Double): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val cols = ceil(sqrt(count.toDouble())).toInt()
        val rows = ceil(count.toDouble() / cols).toInt()
        val cellSize = separationKm
        val totalWidth = (cols - 1) * cellSize
        val totalHeight = (rows - 1) * cellSize

        for (i in 0 until count) {
            val row = i / cols
            val col = i % cols
            val dLat = (row * cellSize - totalHeight / 2) / 111.32
            val dLng = (col * cellSize - totalWidth / 2) / (111.32 * cos(centerLat * PI / 180))
            points.add(LatLng(centerLat + dLat, centerLng + dLng))
        }
        return points
    }

    fun generate(pattern: String, count: Int, centerLat: Double, centerLng: Double, separationKm: Double): List<LatLng> {
        return when (pattern) {
            "circular" -> circular(count, centerLat, centerLng, separationKm)
            "linear" -> linear(count, centerLat, centerLng, separationKm)
            "elliptical" -> elliptical(count, centerLat, centerLng, separationKm)
            "grid" -> grid(count, centerLat, centerLng, separationKm)
            else -> circular(count, centerLat, centerLng, separationKm)
        }
    }
}
