package com.hivid.app

import com.hivid.app.media.AutoBitrate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBitrateTest {

    @Test
    fun test1080p30BitrateCalculation() {
        val bitrate = AutoBitrate.calculateBitrateBps(
            width = 1920,
            height = 1080,
            fps = 30f,
            sourceBitrateBps = 6_000_000L
        )
        // 1080p30 table is 12 Mbps, source * 1.6 is 9.6 Mbps -> max is 12 Mbps
        assertEquals(12_000_000, bitrate)
    }

    @Test
    fun test1080p60BitrateCalculation() {
        val bitrate = AutoBitrate.calculateBitrateBps(
            width = 1920,
            height = 1080,
            fps = 60f,
            sourceBitrateBps = 8_000_000L
        )
        // 1080p60 table is 18 Mbps
        assertEquals(18_000_000, bitrate)
    }

    @Test
    fun test4K60BitrateCalculation() {
        val bitrate = AutoBitrate.calculateBitrateBps(
            width = 3840,
            height = 2160,
            fps = 60f,
            sourceBitrateBps = 20_000_000L
        )
        // 4K60 table is 50 Mbps, source * 1.6 is 32 Mbps -> max is 50 Mbps
        assertEquals(50_000_000, bitrate)
    }

    @Test
    fun testManualOverride() {
        val bitrate = AutoBitrate.calculateBitrateBps(
            width = 1920,
            height = 1080,
            fps = 30f,
            sourceBitrateBps = 5_000_000L,
            manualOverrideMbps = 25
        )
        assertEquals(25_000_000, bitrate)
    }

    @Test
    fun testClampedToEncoderCeiling() {
        val bitrate = AutoBitrate.calculateBitrateBps(
            width = 3840,
            height = 2160,
            fps = 60f,
            sourceBitrateBps = 60_000_000L,
            encoderMaxBitrateMbps = 40
        )
        // Clamped to 40 Mbps encoder ceiling
        assertTrue(bitrate <= 40_000_000)
    }
}
