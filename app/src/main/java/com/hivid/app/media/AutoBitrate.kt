package com.hivid.app.media

import kotlin.math.max
import kotlin.math.min

object AutoBitrate {

    fun calculateBitrateBps(
        width: Int,
        height: Int,
        fps: Float,
        sourceBitrateBps: Long,
        manualOverrideMbps: Int? = null,
        encoderMaxBitrateMbps: Int = 100
    ): Int {
        if (manualOverrideMbps != null && manualOverrideMbps > 0) {
            val clampedMbps = min(manualOverrideMbps, encoderMaxBitrateMbps)
            return clampedMbps * 1_000_000
        }

        val pixels = width * height
        val is60Fps = fps >= 45f

        val tableMbps = when {
            // 4K UHD (~8.3M pixels)
            pixels >= 3840 * 2160 * 0.85 -> if (is60Fps) 50 else 35
            // 1440p QHD (~3.7M pixels)
            pixels >= 2560 * 1440 * 0.85 -> if (is60Fps) 30 else 22
            // 1080p FHD (~2.07M pixels)
            pixels >= 1920 * 1080 * 0.85 -> if (is60Fps) 18 else 12
            // 720p HD (~0.9M pixels)
            pixels >= 1280 * 720 * 0.85 -> if (is60Fps) 10 else 7
            // SD or lower
            else -> 6
        }

        val sourceBoostedMbps = if (sourceBitrateBps > 0) {
            (sourceBitrateBps * 1.6 / 1_000_000.0).toInt()
        } else {
            0
        }

        val targetMbps = max(tableMbps, sourceBoostedMbps)
        val clampedMbps = min(max(targetMbps, 4), min(encoderMaxBitrateMbps, 80))

        return clampedMbps * 1_000_000
    }
}
