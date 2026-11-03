package com.hivid.app.media

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import com.hivid.app.domain.model.DeviceCapabilities

class CapabilityProbe {

    fun probe(): DeviceCapabilities {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecInfos = codecList.codecInfos

        var bestEncoderInfo: MediaCodecInfo? = null
        var bestCaps: MediaCodecInfo.CodecCapabilities? = null
        var supportsMain10 = false
        var supportsHdr10Plus = false
        var supportsSurface = false

        // Search for hardware HEVC encoders
        for (info in codecInfos) {
            if (!info.isEncoder) continue

            val types = info.supportedTypes
            val hasHevc = types.any { it.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true) }
            if (!hasHevc) continue

            val isHardware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                info.isHardwareAccelerated
            } else {
                !info.name.startsWith("OMX.google.", ignoreCase = true) &&
                        !info.name.startsWith("c2.android.", ignoreCase = true)
            }

            try {
                val caps = info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_HEVC)
                var has10Bit = false
                var hasPlus = false

                caps.profileLevels?.forEach { profileLevel ->
                    if (profileLevel.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10) {
                        has10Bit = true
                    }
                    if (profileLevel.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus) {
                        hasPlus = true
                    }
                }

                val hasSurfaceInput = caps.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)

                if (isHardware && (bestEncoderInfo == null || (has10Bit && !supportsMain10))) {
                    bestEncoderInfo = info
                    bestCaps = caps
                    supportsMain10 = has10Bit
                    supportsHdr10Plus = hasPlus
                    supportsSurface = hasSurfaceInput
                    if (has10Bit) {
                        // Found ideal hardware encoder
                        break
                    }
                } else if (bestEncoderInfo == null) {
                    bestEncoderInfo = info
                    bestCaps = caps
                    supportsMain10 = has10Bit
                    supportsHdr10Plus = hasPlus
                    supportsSurface = hasSurfaceInput
                }
            } catch (_: Exception) {
                // Ignore unsupported types
            }
        }

        if (bestEncoderInfo == null || bestCaps == null) {
            return DeviceCapabilities(
                encoderName = "None",
                isHardwareAccelerated = false,
                isHdr10Ready = false,
                isHdr10PlusReady = false,
                isSurfaceInputSupported = false,
                maxWidth = 1920,
                maxHeight = 1080,
                maxFps = 30,
                maxBitrateMbps = 10,
                unsupportedReason = "No HEVC encoder found on device"
            )
        }

        val videoCaps = bestCaps.videoCapabilities
        val maxWidth = videoCaps?.supportedWidths?.upper ?: 1920
        val maxHeight = videoCaps?.supportedHeights?.upper ?: 1080
        val maxBitrate = (videoCaps?.bitrateRange?.upper ?: 20_000_000) / 1_000_000

        val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bestEncoderInfo.isHardwareAccelerated
        } else {
            !bestEncoderInfo.name.startsWith("OMX.google.", ignoreCase = true)
        }

        val isHdr10Ready = supportsMain10 && supportsSurface

        return DeviceCapabilities(
            encoderName = bestEncoderInfo.name,
            isHardwareAccelerated = isHw,
            isHdr10Ready = isHdr10Ready,
            isHdr10PlusReady = isHdr10Ready && supportsHdr10Plus,
            isSurfaceInputSupported = supportsSurface,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            maxFps = 60,
            maxBitrateMbps = maxBitrate,
            unsupportedReason = if (!isHdr10Ready) "HEVC Main10 or Surface input not supported by encoder ${bestEncoderInfo.name}" else null
        )
    }
}
