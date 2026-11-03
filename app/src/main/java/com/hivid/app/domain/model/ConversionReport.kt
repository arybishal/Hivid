package com.hivid.app.domain.model

data class ConversionReport(
    val outputName: String,
    val profile: String,
    val colorStandard: String,
    val colorTransfer: String,
    val encoder: String,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val outputWidth: Int,
    val outputHeight: Int,
    val rotation: Int,
    val sourceFps: Float,
    val targetFps: Float,
    val sourceBitrateMbps: String,
    val requestedBitrateMbps: String,
    val actualBitrateMbps: String,
    val encodedSamples: Long,
    val measuredCadence: String,
    val fileSizeBytes: Long,
    val audioPolicy: String,
    val compatibilityNote: String,
    val storageLocation: String,
    val isRemux: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() = when {
            fileSizeBytes >= 1_073_741_824 -> String.format("%.2f GB", fileSizeBytes / 1_073_741_824.0)
            fileSizeBytes >= 1_048_576 -> String.format("%.2f MB", fileSizeBytes / 1_048_576.0)
            else -> String.format("%d KB", fileSizeBytes / 1024)
        }
}
