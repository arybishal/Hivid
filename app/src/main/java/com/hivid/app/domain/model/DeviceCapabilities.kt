package com.hivid.app.domain.model

data class DeviceCapabilities(
    val encoderName: String,
    val isHardwareAccelerated: Boolean,
    val isHdr10Ready: Boolean,
    val isHdr10PlusReady: Boolean,
    val isSurfaceInputSupported: Boolean,
    val maxWidth: Int,
    val maxHeight: Int,
    val maxFps: Int,
    val maxBitrateMbps: Int,
    val unsupportedReason: String? = null
) {
    val hdr10StatusText: String
        get() = if (isHdr10Ready) "HDR10: READY" else "HDR10: NOT SUPPORTED"

    val hdr10PlusStatusText: String
        get() = if (isHdr10PlusReady) "HDR10+: READY" else "HDR10+: NOT SUPPORTED"
}
