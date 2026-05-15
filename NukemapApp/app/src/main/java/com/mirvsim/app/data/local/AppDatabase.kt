package com.mirvsim.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mirvsim.app.data.local.dao.BookmarkDao
import com.mirvsim.app.data.local.dao.HistoryDao
import com.mirvsim.app.data.local.dao.PresetDao
import com.mirvsim.app.data.local.entity.LocationBookmark
import com.mirvsim.app.data.local.entity.SimulationHistory
import com.mirvsim.app.data.local.entity.UserPreset

@Database(
    entities = [SimulationHistory::class, UserPreset::class, LocationBookmark::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun presetDao(): PresetDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nukemap_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
