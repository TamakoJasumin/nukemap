package com.mirvsim.app.ui.components

import kotlin.math.*

object CoordTransform {
    private const val PI = 3.1415926535897932384626
    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323

    private fun outOfChina(lng: Double, lat: Double): Boolean {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
    }

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }

    /** WGS-84 → GCJ-02，返回 [lng, lat] */
    fun wgs84togcj02(lng: Double, lat: Double): DoubleArray {
        if (outOfChina(lng, lat)) return doubleArrayOf(lng, lat)
        var dlat = transformLat(lng - 105.0, lat - 35.0)
        var dlng = transformLng(lng - 105.0, lat - 35.0)
        val radlat = lat / 180.0 * PI
        var magic = sin(radlat)
        magic = 1 - EE * magic * magic
        val sqrtmagic = sqrt(magic)
        dlat = (dlat * 180.0) / ((A * (1 - EE)) / (magic * sqrtmagic) * PI)
        dlng = (dlng * 180.0) / (A / sqrtmagic * cos(radlat) * PI)
        return doubleArrayOf(lng + dlng, lat + dlat)
    }

    /** GCJ-02 → WGS-84（近似），返回 [lng, lat] */
    fun gcj02towgs84(lng: Double, lat: Double): DoubleArray {
        if (outOfChina(lng, lat)) return doubleArrayOf(lng, lat)
        val gcj = wgs84togcj02(lng, lat)
        return doubleArrayOf(2 * lng - gcj[0], 2 * lat - gcj[1])
    }
}
