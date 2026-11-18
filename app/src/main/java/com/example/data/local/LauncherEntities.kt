package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_items")
data class HomeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val pageIndex: Int,
    val orderIndex: Int,
    val componentKey: String? = null,
    val customLabel: String? = null,
    val appWidgetId: Int = -1,
    val widgetProviderComponent: String? = null,
    val spanCols: Int = 1,
    val spanRows: Int = 1,
    val folderTitle: String? = null,
    val folderComponentKeys: String? = null
)

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey val componentKey: String,
    val launchCount: Int = 0,
    val lastLaunchedAt: Long = 0L,
    val customLabel: String? = null,
    val isHidden: Boolean = false
)

@Entity(tableName = "indexed_folders")
data class IndexedFolderEntity(
    @PrimaryKey val treeUriString: String,
    val displayName: String,
    val addedAt: Long,
    val fileCount: Int
)

@Entity(tableName = "indexed_files")
data class IndexedFileEntity(
    @PrimaryKey val uriString: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val folderUri: String,
    val folderName: String
)
