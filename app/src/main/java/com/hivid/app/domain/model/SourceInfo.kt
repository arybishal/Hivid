package com.hivid.app.domain.model

import android.net.Uri

data class SourceInfo(
    val uri: Uri,
    val fileName: String,
    val fileSize: Long,
    val durationMs: Long,
    val videoMime: String,
    val codedWidth: Int,
    val codedHeight: Int,
    val visibleWidth: Int,
    val visibleHeight: Int,
    val rotation: Int,
    val frameRate: Float,
    val videoBitrateBps: Long,
    val audioMime: String?,
    val audioChannels: Int,
    val audioSampleRate: Int,
    val colorStandard: Int,
    val colorTransfer: Int,
    val colorRange: Int,
    val isAlreadyHdr: Boolean,
    val hdrTypeDescription: String?
) {
    val displayResolution: String
        get() = "${visibleWidth}x${visibleHeight}"

    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val formattedBitrateMbps: String
        get() = String.format("%.2f", videoBitrateBps / 1_000_000.0)

    val formattedSize: String
        get() = when {
            fileSize >= 1_073_741_824 -> String.format("%.2f GB", fileSize / 1_073_741_824.0)
            fileSize >= 1_048_576 -> String.format("%.1f MB", fileSize / 1_048_576.0)
            else -> String.format("%d KB", fileSize / 1024)
        }
}
