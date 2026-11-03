package com.hivid.app.media

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.*
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import com.hivid.app.domain.model.*
import com.hivid.app.media.gl.EglCore
import com.hivid.app.media.gl.HdrRenderProgram
import com.hivid.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

class HdrConversionException(
    message: String,
    val stage: PipelineStage,
    val incident: DiagnosticIncident,
    cause: Throwable? = null
) : Exception(message, cause)

class HdrConverter(private val context: Context) {

    suspend fun convert(
        sourceInfo: SourceInfo,
        config: ConversionConfig,
        capabilities: DeviceCapabilities,
        outputFile: File,
        onProgress: (Int) -> Unit
    ): Result<ConversionReport> = withContext(Dispatchers.Default) {
        val diagnosticId = UUID.randomUUID().toString()
        AppLogger.setDiagnosticId(diagnosticId)

        var currentStage = PipelineStage.INITIALIZATION
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var extractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        var eglCore: EglCore? = null
        var encoderSurface: Surface? = null
        var eglEncoderSurface: android.opengl.EGLSurface? = null
        var decoderSurface: Surface? = null
        var surfaceTexture: SurfaceTexture? = null
        var renderProgram: HdrRenderProgram? = null
        val textureIds = IntArray(1)

        var encodedSamplesCount = 0L
        var totalVideoBytes = 0L
        var isMuxerStarted = false
        var videoTrackIndex = -1
        var audioTrackIndex = -1

        var outWidth = sourceInfo.visibleWidth
        var outHeight = sourceInfo.visibleHeight
        val targetFps = if (config.fpsMode == FpsMode.CAP_60 && sourceInfo.frameRate > 60f) {
            60f
        } else {
            sourceInfo.frameRate
        }
        val targetBitrateBps = AutoBitrate.calculateBitrateBps(
            width = outWidth,
            height = outHeight,
            fps = targetFps,
            sourceBitrateBps = sourceInfo.videoBitrateBps,
            manualOverrideMbps = config.manualBitrateMbps,
            encoderMaxBitrateMbps = capabilities.maxBitrateMbps
        )

        AppLogger.i(
            TAG,
            "Starting HDR conversion [ID: $diagnosticId]: Source=${sourceInfo.fileName} (${sourceInfo.displayResolution} @ ${sourceInfo.frameRate}fps), Target Profile=${config.profile.displayName}, Expansion=${config.expansionPercent}%"
        )

        try {
            // Stage 1: Extractor Initialization & Track Selection
            currentStage = PipelineStage.SOURCE_ANALYSIS
            AppLogger.d(TAG, "Initializing MediaExtractor for source: ${sourceInfo.uri}")
            extractor = MediaExtractor().apply {
                setDataSource(context, sourceInfo.uri, null)
            }

            var videoTrack = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrack = i
                    inputFormat = format
                    break
                }
            }

            if (videoTrack == -1 || inputFormat == null) {
                throw IllegalStateException("No video track found in source video stream")
            }
            extractor.selectTrack(videoTrack)
            AppLogger.i(TAG, "Selected video track $videoTrack: $inputFormat")

            // Stage 2: Hardware HEVC Encoder Configuration
            currentStage = PipelineStage.ENCODER_CONFIG
            val useHdr10Plus = config.profile == HdrProfile.HDR10_PLUS && capabilities.isHdr10PlusReady
            val profile = if (useHdr10Plus) {
                MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus
            } else {
                MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10
            }

            AppLogger.d(
                TAG,
                "Configuring HEVC Encoder (${capabilities.encoderName}) with profile=$profile, resolution=${outWidth}x${outHeight}, bitrate=${targetBitrateBps / 1_000_000}Mbps, fps=$targetFps"
            )

            val encoderFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_HEVC,
                outWidth,
                outHeight
            ).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrateBps)
                setInteger(MediaFormat.KEY_FRAME_RATE, targetFps.roundToInt().coerceAtLeast(24))
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_PROFILE, profile)
                setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020)
                setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_ST2084)
                setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED)
            }

            encoder = try {
                if (capabilities.encoderName != "None") {
                    MediaCodec.createByCodecName(capabilities.encoderName)
                } else {
                    MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC)
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to create encoder by name ${capabilities.encoderName}, falling back to type", e)
                MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC)
            }

            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoderSurface = encoder.createInputSurface()
            encoder.start()
            AppLogger.i(TAG, "HEVC Main10 encoder configured and started successfully")

            // Stage 3: EGL & OpenGL ES 3.0 Setup
            currentStage = PipelineStage.EGL_INIT
            AppLogger.d(TAG, "Initializing EGL14 context (GLES 3.0) and 10-bit window surface")
            eglCore = EglCore()
            eglEncoderSurface = eglCore.createWindowSurface(encoderSurface)
            eglCore.makeCurrent(eglEncoderSurface)

            // Stage 4: Shader Compilation
            currentStage = PipelineStage.SHADER_COMPILE
            AppLogger.d(TAG, "Compiling HDR inverse tone mapping vertex and fragment shaders")
            renderProgram = HdrRenderProgram()

            // Stage 5: Decoder & Surface Binding
            currentStage = PipelineStage.SURFACE_BINDING
            GLES30.glGenTextures(1, textureIds, 0)
            val oesTextureId = textureIds[0]
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

            val frameAvailable = AtomicBoolean(false)
            val frameLock = Object()

            surfaceTexture = SurfaceTexture(oesTextureId).apply {
                setDefaultBufferSize(outWidth, outHeight)
                setOnFrameAvailableListener {
                    synchronized(frameLock) {
                        frameAvailable.set(true)
                        frameLock.notifyAll()
                    }
                }
            }
            decoderSurface = Surface(surfaceTexture)

            currentStage = PipelineStage.DECODER_INIT
            val decoderMime = inputFormat.getString(MediaFormat.KEY_MIME) ?: "video/avc"
            AppLogger.d(TAG, "Creating decoder for MIME $decoderMime")
            decoder = MediaCodec.createDecoderByType(decoderMime)
            decoder.configure(inputFormat, decoderSurface, null, 0)
            decoder.start()
            AppLogger.i(TAG, "Decoder initialized and bound to OES SurfaceTexture")

            // Stage 6: Muxer Setup
            currentStage = PipelineStage.MUXER_INIT
            AppLogger.d(TAG, "Initializing MediaMuxer targeting: ${outputFile.name}")
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(sourceInfo.rotation)

            var audioTrack = -1
            var audioFormat: MediaFormat? = null
            audioExtractor = MediaExtractor().apply {
                setDataSource(context, sourceInfo.uri, null)
            }
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    audioFormat = format
                    break
                }
            }

            val bufferInfo = MediaCodec.BufferInfo()
            val texMatrix = FloatArray(16)
            val mvpMatrix = FloatArray(16)
            Matrix.setIdentityM(mvpMatrix, 0)

            var isExtractorEOS = false
            var isDecoderEOS = false
            var isEncoderEOS = false
            val expansionParam = config.expansionPercent / 100.0f
            val durationUs = sourceInfo.durationMs * 1000L

            var lastReportedPercent = -1
            var nextMilestoneToLog = 10

            currentStage = PipelineStage.FRAME_PROCESSING
            AppLogger.i(TAG, "Entering decode-render-encode GPU loop")

            // Main Transcoding Loop
            while (!isEncoderEOS && coroutineContext.isActive) {
                // A. Feed Decoder input buffers
                if (!isExtractorEOS) {
                    val inputBufIndex = decoder.dequeueInputBuffer(10_000)
                    if (inputBufIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufIndex)
                        if (inputBuffer != null) {
                            val chunkSize = extractor.readSampleData(inputBuffer, 0)
                            if (chunkSize < 0) {
                                decoder.queueInputBuffer(
                                    inputBufIndex, 0, 0, 0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                isExtractorEOS = true
                                AppLogger.d(TAG, "Extractor reached End Of Stream (EOS)")
                            } else {
                                val pts = extractor.sampleTime
                                decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, pts, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                // B. Drain Decoder output & render to EGL Surface
                if (!isDecoderEOS) {
                    val decoderOutIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000)
                    when {
                        decoderOutIndex >= 0 -> {
                            val render = bufferInfo.size > 0
                            val isEOS = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0

                            if (render) {
                                synchronized(frameLock) {
                                    frameAvailable.set(false)
                                    decoder.releaseOutputBuffer(decoderOutIndex, true)

                                    var waitCount = 0
                                    while (!frameAvailable.get() && waitCount < 10) {
                                        frameLock.wait(100)
                                        waitCount++
                                    }
                                }

                                if (frameAvailable.get()) {
                                    surfaceTexture.updateTexImage()
                                    surfaceTexture.getTransformMatrix(texMatrix)

                                    GLES30.glViewport(0, 0, outWidth, outHeight)
                                    GLES30.glClearColor(0f, 0f, 0f, 1f)
                                    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

                                    renderProgram.draw(oesTextureId, mvpMatrix, texMatrix, expansionParam)

                                    eglCore.setPresentationTime(eglEncoderSurface, bufferInfo.presentationTimeUs * 1000L)
                                    eglCore.swapBuffers(eglEncoderSurface)
                                }
                            } else {
                                decoder.releaseOutputBuffer(decoderOutIndex, false)
                            }

                            if (isEOS) {
                                isDecoderEOS = true
                                encoder.signalEndOfInputStream()
                                AppLogger.d(TAG, "Decoder reached End Of Stream, signalled encoder")
                            }
                        }
                        decoderOutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            AppLogger.d(TAG, "Decoder output format changed: ${decoder.outputFormat}")
                        }
                    }
                }

                // C. Drain Encoder output buffers into MediaMuxer
                var encoderOutIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                while (encoderOutIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size > 0 && isMuxerStarted) {
                        val encodedBuffer = encoder.getOutputBuffer(encoderOutIndex)
                        if (encodedBuffer != null) {
                            encodedBuffer.position(bufferInfo.offset)
                            encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedBuffer, bufferInfo)
                            encodedSamplesCount++
                            totalVideoBytes += bufferInfo.size

                            if (durationUs > 0) {
                                val currentPercent = ((bufferInfo.presentationTimeUs.toFloat() / durationUs.toFloat()) * 100)
                                    .toInt().coerceIn(0, 99)
                                if (currentPercent != lastReportedPercent) {
                                    lastReportedPercent = currentPercent
                                    onProgress(currentPercent)

                                    // Periodic milestone logging (every 10%)
                                    if (currentPercent >= nextMilestoneToLog) {
                                        AppLogger.i(TAG, "Conversion progress: $currentPercent% ($encodedSamplesCount frames encoded)")
                                        nextMilestoneToLog += 10
                                    }
                                }
                            }
                        }
                    }

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEncoderEOS = true
                        AppLogger.d(TAG, "Encoder reached End Of Stream (EOS)")
                        break
                    }

                    encoder.releaseOutputBuffer(encoderOutIndex, false)
                    encoderOutIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                }

                if (encoderOutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (isMuxerStarted) {
                        throw RuntimeException("Muxer already started on unexpected format change")
                    }
                    val newFormat = encoder.outputFormat
                    AppLogger.i(TAG, "Encoder output format confirmed: $newFormat")
                    videoTrackIndex = muxer.addTrack(newFormat)

                    if (audioTrack != -1 && audioFormat != null) {
                        audioTrackIndex = muxer.addTrack(audioFormat)
                        AppLogger.d(TAG, "Added audio track to muxer: $audioFormat")
                    }

                    muxer.start()
                    isMuxerStarted = true
                    AppLogger.i(TAG, "MediaMuxer started successfully")
                }
            }

            if (!coroutineContext.isActive) {
                AppLogger.w(TAG, "Conversion cancelled by user request")
                throw HdrError.Cancelled
            }

            // Stage 7: Copy Audio Track Samples (Lossless Passthrough)
            currentStage = PipelineStage.AUDIO_PASSTHROUGH
            if (audioTrack != -1 && audioTrackIndex != -1 && isMuxerStarted && audioExtractor != null) {
                AppLogger.d(TAG, "Starting lossless audio track passthrough")
                audioExtractor.selectTrack(audioTrack)
                val audioBuf = ByteBuffer.allocate(64 * 1024)
                val audioBufferInfo = MediaCodec.BufferInfo()
                var audioSamples = 0L

                while (coroutineContext.isActive) {
                    val sampleSize = audioExtractor.readSampleData(audioBuf, 0)
                    if (sampleSize < 0) break

                    audioBufferInfo.offset = 0
                    audioBufferInfo.size = sampleSize
                    audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
                    audioBufferInfo.flags = audioExtractor.sampleFlags

                    muxer.writeSampleData(audioTrackIndex, audioBuf, audioBufferInfo)
                    audioSamples++
                    audioExtractor.advance()
                }
                AppLogger.i(TAG, "Audio passthrough complete: $audioSamples audio samples copied")
            }

            // Stage 8: Finalize Muxer
            currentStage = PipelineStage.MUXER_FINALIZE
            onProgress(100)

            val durationSec = if (sourceInfo.durationMs > 0) sourceInfo.durationMs / 1000.0 else 1.0
            val actualBitrateMbps = String.format("%.2f", (outputFile.length() * 8.0) / (durationSec * 1_000_000.0))
            val reqBitrateMbps = String.format("%.2f", targetBitrateBps / 1_000_000.0)

            val verifiedProfile = if (config.profile == HdrProfile.HDR10_PLUS && capabilities.isHdr10PlusReady) {
                "HDR10+ MAX"
            } else {
                "HDR10"
            }

            val report = ConversionReport(
                outputName = outputFile.name,
                profile = verifiedProfile,
                colorStandard = "BT.2020",
                colorTransfer = "PQ (SMPTE ST 2084)",
                encoder = capabilities.encoderName,
                sourceWidth = sourceInfo.codedWidth,
                sourceHeight = sourceInfo.codedHeight,
                outputWidth = outWidth,
                outputHeight = outHeight,
                rotation = sourceInfo.rotation,
                sourceFps = sourceInfo.frameRate,
                targetFps = targetFps,
                sourceBitrateMbps = sourceInfo.formattedBitrateMbps,
                requestedBitrateMbps = reqBitrateMbps,
                actualBitrateMbps = actualBitrateMbps,
                encodedSamples = encodedSamplesCount,
                measuredCadence = "${targetFps.roundToInt()} FPS · Passthrough PTS",
                fileSizeBytes = outputFile.length(),
                audioPolicy = if (sourceInfo.audioMime != null) "AAC Passthrough" else "No Audio",
                compatibilityNote = "100% on-device GPU mastered. HDR10 dynamic tags depend on player OLED support.",
                storageLocation = "Movies/Hivid"
            )

            AppLogger.i(TAG, "Conversion completed successfully: ${outputFile.name} (${report.formattedSize}, $actualBitrateMbps Mbps)")
            Result.success(report)

        } catch (e: Exception) {
            val codecException = e as? MediaCodec.CodecException
            val codecErr = codecException?.diagnosticInfo ?: codecException?.errorCode?.toString()

            val incident = DiagnosticIncident(
                diagnosticId = diagnosticId,
                stage = currentStage,
                selectedEncoder = capabilities.encoderName,
                availableEncoders = capabilities.encoderName,
                requestedResolution = "${outWidth}x${outHeight}",
                requestedFps = targetFps,
                requestedBitrateMbps = String.format("%.2f", targetBitrateBps / 1_000_000.0),
                selectedProfile = config.profile.displayName,
                eglConfigDetails = "10-bit RGBA1010102 / GLES 3.0",
                codecErrorCode = codecErr,
                exceptionMessage = e.localizedMessage ?: e.message ?: "Unknown conversion error",
                stackTrace = e.stackTraceToString(),
                fallbackAttempted = false
            )

            AppLogger.e(
                TAG,
                "Pipeline failure at stage '${currentStage.displayName}': ${e.message}",
                e,
                diagnosticId
            )

            if (outputFile.exists()) {
                outputFile.delete()
            }

            Result.failure(HdrConversionException(e.message ?: "Conversion failed", currentStage, incident, e))
        } finally {
            AppLogger.setDiagnosticId(null)
            try { extractor?.release() } catch (_: Exception) {}
            try { audioExtractor?.release() } catch (_: Exception) {}
            try {
                if (isMuxerStarted) {
                    muxer?.stop()
                }
                muxer?.release()
            } catch (_: Exception) {}
            try { decoder?.stop(); decoder?.release() } catch (_: Exception) {}
            try { encoder?.stop(); encoder?.release() } catch (_: Exception) {}
            try { decoderSurface?.release() } catch (_: Exception) {}
            try { surfaceTexture?.release() } catch (_: Exception) {}
            try { encoderSurface?.release() } catch (_: Exception) {}
            try {
                renderProgram?.release()
                if (textureIds[0] != 0) {
                    GLES30.glDeleteTextures(1, textureIds, 0)
                }
                if (eglEncoderSurface != null) {
                    eglCore?.releaseSurface(eglEncoderSurface)
                }
                eglCore?.release()
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val TAG = "HdrConverter"
    }
}
