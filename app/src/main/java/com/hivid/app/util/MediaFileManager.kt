package com.hivid.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

object MediaFileManager {

    fun generateOutputFileName(prefix: String = "HDR10"): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
        val timestamp = dateFormat.format(Date())
        return "Hivid_${prefix}_${timestamp}.mp4"
    }

    fun createTempFile(context: Context, prefix: String = "hivid_temp"): File {
        return File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.mp4")
    }

    fun hasAvailableDiskSpace(context: Context, estimatedBytes: Long): Boolean {
        return try {
            val stat = StatFs(context.cacheDir.absolutePath)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            available >= (estimatedBytes * 1.5)
        } catch (e: Exception) {
            true
        }
    }

    fun saveToMediaStore(
        context: Context,
        tempFile: File,
        displayName: String,
        mimeType: String = "video/mp4"
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, mimeType)
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Hivid")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collectionUri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val targetUri = resolver.insert(collectionUri, contentValues) ?: return null

        try {
            resolver.openOutputStream(targetUri)?.use { out ->
                FileInputStream(tempFile).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(targetUri, contentValues, null, null)
            }

            return targetUri
        } catch (e: Exception) {
            resolver.delete(targetUri, null, null)
            return null
        }
    }
}
