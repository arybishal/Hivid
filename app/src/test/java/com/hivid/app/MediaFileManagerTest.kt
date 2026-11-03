package com.hivid.app

import com.hivid.app.util.MediaFileManager
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFileManagerTest {

    @Test
    fun testGenerateOutputFileNameFormat() {
        val fileName = MediaFileManager.generateOutputFileName("HDR10")
        assertTrue(fileName.startsWith("Hivid_HDR10_"))
        assertTrue(fileName.endsWith(".mp4"))
    }

    @Test
    fun testGenerateRemuxFileNameFormat() {
        val fileName = MediaFileManager.generateOutputFileName("Remux")
        assertTrue(fileName.startsWith("Hivid_Remux_"))
        assertTrue(fileName.endsWith(".mp4"))
    }
}
