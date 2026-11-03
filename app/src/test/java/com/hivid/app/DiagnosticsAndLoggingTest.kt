package com.hivid.app

import com.hivid.app.data.local.LogEntity
import com.hivid.app.domain.model.DiagnosticIncident
import com.hivid.app.domain.model.LogLevel
import com.hivid.app.domain.model.PipelineStage
import com.hivid.app.media.HdrConversionException
import com.hivid.app.util.AppLogger
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class DiagnosticsAndLoggingTest {

    @Test
    fun testLogEntityFormatting() {
        val entity = LogEntity(
            id = 1,
            timestamp = 1700000000000L,
            level = LogLevel.ERROR.name,
            tag = "TestTag",
            message = "Test error message",
            exceptionMessage = "IllegalStateException: failed",
            stackTrace = "at com.hivid.app.Test.run(Test.kt:10)",
            diagnosticId = "diag-12345"
        )

        val display = entity.toDisplayString()
        assertTrue(display.contains("[ERROR  ]"))
        assertTrue(display.contains("[TestTag]"))
        assertTrue(display.contains("[ID:diag-123]"))
        assertTrue(display.contains("Test error message"))
        assertTrue(display.contains("IllegalStateException: failed"))
        assertTrue(display.contains("at com.hivid.app.Test.run"))
    }

    @Test
    fun testDiagnosticIncidentReportGeneration() {
        val incident = DiagnosticIncident(
            diagnosticId = "test-uuid-9999",
            stage = PipelineStage.ENCODER_CONFIG,
            selectedEncoder = "c2.qti.hevc.encoder",
            availableEncoders = "c2.qti.hevc.encoder, c2.android.hevc.encoder",
            requestedResolution = "3840x2160",
            requestedFps = 60f,
            requestedBitrateMbps = "50.00",
            selectedProfile = "HDR10",
            eglConfigDetails = "10-bit RGBA1010102 / GLES 3.0",
            codecErrorCode = "0x80001001",
            exceptionMessage = "MediaCodec configuration rejected resolution",
            stackTrace = "java.lang.IllegalArgumentException\n\tat com.hivid.app.media.HdrConverter.convert"
        )

        val report = incident.toFormattedReport()
        assertTrue(report.contains("HIVID DIAGNOSTIC FAILURE REPORT"))
        assertTrue(report.contains("test-uuid-9999"))
        assertTrue(report.contains("HEVC Main10 Encoder Configuration"))
        assertTrue(report.contains("c2.qti.hevc.encoder"))
        assertTrue(report.contains("3840x2160 @ 60.0 FPS"))
        assertTrue(report.contains("0x80001001"))
        assertTrue(report.contains("MediaCodec configuration rejected resolution"))
    }

    @Test
    fun testTriggerUnsupportedEncoderDiagnostic() {
        val incident = DiagnosticIncident(
            stage = PipelineStage.CAPABILITY_PROBE,
            selectedEncoder = "None",
            availableEncoders = "None",
            requestedResolution = "1920x1080",
            requestedFps = 30f,
            requestedBitrateMbps = "12.00",
            selectedProfile = "HDR10",
            eglConfigDetails = "N/A",
            exceptionMessage = "Device does not provide HEVC Main10 hardware encoder",
            stackTrace = "com.hivid.app.domain.model.HdrError\$UnsupportedDevice"
        )
        assertEquals(PipelineStage.CAPABILITY_PROBE, incident.stage)
        assertTrue(incident.toFormattedReport().contains("Device does not provide HEVC Main10 hardware encoder"))
    }

    @Test
    fun testTriggerDecoderFailureDiagnostic() {
        val cause = IOException("Extractor could not demux input track")
        val incident = DiagnosticIncident(
            stage = PipelineStage.DECODER_INIT,
            selectedEncoder = "c2.qti.hevc.encoder",
            availableEncoders = "c2.qti.hevc.encoder",
            requestedResolution = "1920x1080",
            requestedFps = 30f,
            requestedBitrateMbps = "12.00",
            selectedProfile = "HDR10",
            eglConfigDetails = "10-bit RGBA1010102",
            exceptionMessage = cause.message ?: "",
            stackTrace = cause.stackTraceToString()
        )
        val conversionEx = HdrConversionException("Decoder initialization failed", PipelineStage.DECODER_INIT, incident, cause)
        assertEquals(PipelineStage.DECODER_INIT, conversionEx.stage)
        assertEquals(cause, conversionEx.cause)
    }

    @Test
    fun testTriggerEglFailureDiagnostic() {
        val incident = DiagnosticIncident(
            stage = PipelineStage.EGL_INIT,
            selectedEncoder = "c2.qti.hevc.encoder",
            availableEncoders = "c2.qti.hevc.encoder",
            requestedResolution = "1920x1080",
            requestedFps = 30f,
            requestedBitrateMbps = "12.00",
            selectedProfile = "HDR10",
            eglConfigDetails = "EGL_SUCCESS=false, unable to allocate 10-bit surface",
            exceptionMessage = "eglCreateWindowSurface failed with EGL_BAD_ALLOC",
            stackTrace = "java.lang.RuntimeException: surface was null"
        )
        assertEquals(PipelineStage.EGL_INIT, incident.stage)
        assertTrue(incident.toFormattedReport().contains("EGL_BAD_ALLOC"))
    }

    @Test
    fun testTriggerStorageFailureDiagnostic() {
        val incident = DiagnosticIncident(
            stage = PipelineStage.INITIALIZATION,
            selectedEncoder = "c2.qti.hevc.encoder",
            availableEncoders = "c2.qti.hevc.encoder",
            requestedResolution = "3840x2160",
            requestedFps = 60f,
            requestedBitrateMbps = "50.00",
            selectedProfile = "HDR10",
            eglConfigDetails = "N/A",
            exceptionMessage = "Insufficient storage space: Need 1500 MB free",
            stackTrace = "com.hivid.app.domain.model.HdrError\$LowStorage"
        )
        assertEquals(PipelineStage.INITIALIZATION, incident.stage)
        assertTrue(incident.toFormattedReport().contains("Insufficient storage space"))
    }

    @Test
    fun testExportLogsToString() {
        val logs = listOf(
            LogEntity(id = 1, timestamp = 1000L, level = "INFO", tag = "App", message = "Startup"),
            LogEntity(id = 2, timestamp = 2000L, level = "ERROR", tag = "Encoder", message = "Failed to configure")
        )

        val exported = AppLogger.exportLogsToString(logs)
        assertTrue(exported.contains("HIVID APPLICATION LOGS DUMP"))
        assertTrue(exported.contains("Total Entries: 2"))
        assertTrue(exported.contains("Startup"))
        assertTrue(exported.contains("Failed to configure"))
    }

    @Test
    fun testMaxLogRetentionConstant() {
        assertEquals(2000, AppLogger.MAX_LOG_ENTRIES)
    }
}
