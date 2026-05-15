package com.mirvsim.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_bookmarks")
data class LocationBookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val lat: Double,
    val lng: Double,
    val createdAt: Long = System.currentTimeMillis()
)
