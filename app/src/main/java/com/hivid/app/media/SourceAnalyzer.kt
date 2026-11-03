package com.hivid.app.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.hivid.app.domain.model.SourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SourceAnalyzer(private val context: Context) {

    suspend fun analyze(uri: Uri): Result<SourceInfo> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val extractor = MediaExtractor()

        try {
            // Retrieve file name and size from ContentResolver
            var fileName = "video_${System.currentTimeMillis()}.mp4"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            retriever.setDataSource(context, uri)
            extractor.setDataSource(context, uri, null)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val rotation = rotationStr?.toIntOrNull() ?: 0

            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            var videoBitrateBps = bitrateStr?.toLongOrNull() ?: 0L

            var videoMime = "video/avc"
            var codedWidth = 0
            var codedHeight = 0
            var visibleWidth = 0
            var visibleHeight = 0
            var frameRate = 30f
            var colorStandard = 0
            var colorTransfer = 0
            var colorRange = 0
            var isAlreadyHdr = false
            var hdrTypeDescription: String? = null

            var audioMime: String? = null
            var audioChannels = 0
            var audioSampleRate = 0

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                if (mime.startsWith("video/")) {
                    videoMime = mime
                    codedWidth = format.getIntegerSafely(MediaFormat.KEY_WIDTH, 0)
                    codedHeight = format.getIntegerSafely(MediaFormat.KEY_HEIGHT, 0)

                    // Check crop rectangle for visible size
                    val cropLeft = format.getIntegerSafely("crop-left", 0)
                    val cropRight = format.getIntegerSafely("crop-right", codedWidth - 1)
                    val cropTop = format.getIntegerSafely("crop-top", 0)
                    val cropBottom = format.getIntegerSafely("crop-bottom", codedHeight - 1)

                    visibleWidth = if (cropRight >= cropLeft) cropRight - cropLeft + 1 else codedWidth
                    visibleHeight = if (cropBottom >= cropTop) cropBottom - cropTop + 1 else codedHeight

                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = try {
                            format.getFloat(MediaFormat.KEY_FRAME_RATE)
                        } catch (e: Exception) {
                            format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                        }
                    }

                    colorStandard = format.getIntegerSafely(MediaFormat.KEY_COLOR_STANDARD, 0)
                    colorTransfer = format.getIntegerSafely(MediaFormat.KEY_COLOR_TRANSFER, 0)
                    colorRange = format.getIntegerSafely(MediaFormat.KEY_COLOR_RANGE, 0)

                    // HDR Detection
                    if (colorTransfer == MediaFormat.COLOR_TRANSFER_ST2084) {
                        isAlreadyHdr = true
                        hdrTypeDescription = "HDR10 / SMPTE ST 2084"
                    } else if (colorTransfer == MediaFormat.COLOR_TRANSFER_HLG) {
                        isAlreadyHdr = true
                        hdrTypeDescription = "HLG (Hybrid Log-Gamma)"
                    } else if (colorStandard == MediaFormat.COLOR_STANDARD_BT2020) {
                        isAlreadyHdr = true
                        hdrTypeDescription = "BT.2020 Gamut"
                    }

                    // Sample frame rate verification by reading up to 30 sample timestamps
                    if (frameRate <= 0f) {
                        extractor.selectTrack(i)
                        var sampleCount = 0
                        var firstPts = -1L
                        var lastPts = -1L
                        while (extractor.readSampleData(java.nio.ByteBuffer.allocate(1024), 0) >= 0 && sampleCount < 30) {
                            val pts = extractor.sampleTime
                            if (firstPts == -1L) firstPts = pts
                            lastPts = pts
                            sampleCount++
                            extractor.advance()
                        }
                        extractor.unselectTrack(i)
                        if (sampleCount > 1 && lastPts > firstPts) {
                            val durationSec = (lastPts - firstPts) / 1_000_000.0
                            frameRate = (sampleCount / durationSec).toFloat()
                        } else {
                            frameRate = 30f
                        }
                    }
                } else if (mime.startsWith("audio/")) {
                    audioMime = mime
                    audioChannels = format.getIntegerSafely(MediaFormat.KEY_CHANNEL_COUNT, 2)
                    audioSampleRate = format.getIntegerSafely(MediaFormat.KEY_SAMPLE_RATE, 44100)
                }
            }

            // Fallback for dimensions from retriever if codedWidth is 0
            if (codedWidth == 0 || codedHeight == 0) {
                val rw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
                val rh = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
                codedWidth = rw
                codedHeight = rh
                visibleWidth = rw
                visibleHeight = rh
            }

            if (videoBitrateBps <= 0L && durationMs > 0 && fileSize > 0) {
                videoBitrateBps = (fileSize * 8000L) / durationMs
            }

            Result.success(
                SourceInfo(
                    uri = uri,
                    fileName = fileName,
                    fileSize = fileSize,
                    durationMs = durationMs,
                    videoMime = videoMime,
                    codedWidth = codedWidth,
                    codedHeight = codedHeight,
                    visibleWidth = visibleWidth,
                    visibleHeight = visibleHeight,
                    rotation = rotation,
                    frameRate = frameRate,
                    videoBitrateBps = videoBitrateBps,
                    audioMime = audioMime,
                    audioChannels = audioChannels,
                    audioSampleRate = audioSampleRate,
                    colorStandard = colorStandard,
                    colorTransfer = colorTransfer,
                    colorRange = colorRange,
                    isAlreadyHdr = isAlreadyHdr,
                    hdrTypeDescription = hdrTypeDescription
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                retriever.release()
                extractor.release()
            } catch (_: Exception) {}
        }
    }

    private fun MediaFormat.getIntegerSafely(key: String, defaultVal: Int): Int {
        return if (containsKey(key)) {
            try {
                getInteger(key)
            } catch (e: Exception) {
                defaultVal
            }
        } else {
            defaultVal
        }
    }
}
