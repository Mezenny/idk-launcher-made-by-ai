package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HomeItemEntity::class,
        AppUsageEntity::class,
        IndexedFolderEntity::class,
        IndexedFileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun homeItemDao(): HomeItemDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun fileIndexDao(): FileIndexDao

    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        fun getInstance(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "zstar_launcher.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
