package com.mirvsim.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mirvsim.app.data.local.entity.LocationBookmark
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM location_bookmarks ORDER BY createdAt DESC")
    fun getAll(): Flow<List<LocationBookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: LocationBookmark): Long

    @Query("DELETE FROM location_bookmarks WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM location_bookmarks")
    suspend fun clearAll()
}
