package com.hivid.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val outputUriString: String,
    val profile: String,
    val resolution: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val reportJson: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() = when {
            fileSizeBytes >= 1_073_741_824 -> String.format("%.2f GB", fileSizeBytes / 1_073_741_824.0)
            fileSizeBytes >= 1_048_576 -> String.format("%.1f MB", fileSizeBytes / 1_048_576.0)
            else -> String.format("%d KB", fileSizeBytes / 1024)
        }
}
