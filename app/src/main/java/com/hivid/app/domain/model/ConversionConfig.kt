package com.hivid.app.domain.model

enum class HdrProfile(val displayName: String) {
    HDR10("HDR10"),
    HDR10_PLUS("HDR10+ MAX")
}

enum class FpsMode(val displayName: String) {
    SOURCE_CADENCE("SOURCE CADENCE"),
    CAP_60("60 FPS CAP")
}

data class ConversionConfig(
    val profile: HdrProfile = HdrProfile.HDR10,
    val fpsMode: FpsMode = FpsMode.SOURCE_CADENCE,
    val expansionPercent: Float = 70f, // 0..100
    val manualBitrateMbps: Int? = null,
    val targetResolution: Pair<Int, Int>? = null // null means source resolution
)
