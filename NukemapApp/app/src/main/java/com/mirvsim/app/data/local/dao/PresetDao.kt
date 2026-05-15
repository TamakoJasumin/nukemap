package com.mirvsim.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mirvsim.app.data.local.entity.UserPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM user_presets ORDER BY id DESC")
    fun getAll(): Flow<List<UserPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: UserPreset): Long

    @Query("DELETE FROM user_presets WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM user_presets")
    suspend fun clearAll()
}
