package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeItemDao {
    @Query("SELECT * FROM home_items ORDER BY pageIndex ASC, orderIndex ASC, id ASC")
    fun observeHomeItems(): Flow<List<HomeItemEntity>>

    @Query("SELECT COUNT(*) FROM home_items")
    suspend fun countItems(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: HomeItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HomeItemEntity>)

    @Update
    suspend fun updateItem(item: HomeItemEntity)

    @Query("DELETE FROM home_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM home_items WHERE componentKey = :componentKey AND type IN ('APP', 'DOCK_APP')")
    suspend fun deleteAppItemsByComponent(componentKey: String)

    @Query("DELETE FROM home_items")
    suspend fun clearAll()
}

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_usage")
    fun observeAllUsage(): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE componentKey = :componentKey LIMIT 1")
    suspend fun getByComponentKey(componentKey: String): AppUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppUsageEntity)
}

@Dao
interface FileIndexDao {
    @Query("SELECT * FROM indexed_folders ORDER BY addedAt DESC")
    fun observeFolders(): Flow<List<IndexedFolderEntity>>

    @Query("SELECT * FROM indexed_files ORDER BY lastModified DESC")
    fun observeAllFiles(): Flow<List<IndexedFileEntity>>

    @Query("SELECT COUNT(*) FROM indexed_files")
    fun observeFileCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: IndexedFolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<IndexedFileEntity>)

    @Query("DELETE FROM indexed_files WHERE folderUri = :folderUri")
    suspend fun deleteFilesForFolder(folderUri: String)

    @Query("DELETE FROM indexed_folders WHERE treeUriString = :folderUri")
    suspend fun deleteFolder(folderUri: String)
}
