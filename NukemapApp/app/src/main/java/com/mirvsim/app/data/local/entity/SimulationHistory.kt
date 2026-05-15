package com.mirvsim.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "simulation_history")
data class SimulationHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val targetLat: Double,
    val targetLng: Double,
    val cityName: String?,
    val warheadCount: Int,
    val yieldKt: Double,
    val deaths: Int,
    val totalCasualties: Int,
    val targetType: String
)
