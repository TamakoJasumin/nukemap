package com.mirvsim.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_presets")
data class UserPreset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val warheadCount: Int,
    val yieldKt: Double,
    val separationKm: Double,
    val pattern: String,
    val hobMode: String
)
