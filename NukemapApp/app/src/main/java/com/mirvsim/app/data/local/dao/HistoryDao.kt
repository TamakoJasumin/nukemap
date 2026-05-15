package com.mirvsim.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mirvsim.app.data.local.entity.SimulationHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM simulation_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SimulationHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SimulationHistory)

    @Query("DELETE FROM simulation_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM simulation_history")
    suspend fun clearAll()
}
