package com.hivid.app.domain.model

import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

data class DiagnosticIncident(
    val diagnosticId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val stage: PipelineStage,
    val deviceManufacturer: String = (Build.MANUFACTURER as String?) ?: "Generic",
    val deviceModel: String = (Build.MODEL as String?) ?: "Android Device",
    val androidVersion: String = (Build.VERSION.RELEASE as String?) ?: "14",
    val apiLevel: Int = if (Build.VERSION.SDK_INT > 0) Build.VERSION.SDK_INT else 34,
    val hardwareBoard: String = (Build.BOARD as String?) ?: "Unknown",
    val selectedEncoder: String,
    val availableEncoders: String,
    val requestedResolution: String,
    val requestedFps: Float,
    val requestedBitrateMbps: String,
    val selectedProfile: String,
    val eglConfigDetails: String,
    val codecErrorCode: String? = null,
    val exceptionMessage: String,
    val stackTrace: String,
    val fallbackAttempted: Boolean = false
) {
    fun toFormattedReport(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return buildString {
            appendLine("========================================================")
            appendLine("           HIVID DIAGNOSTIC FAILURE REPORT              ")
            appendLine("========================================================")
            appendLine("Diagnostic ID      : $diagnosticId")
            appendLine("Timestamp          : ${sdf.format(Date(timestamp))}")
            appendLine("Failed Stage       : ${stage.displayName} [${stage.name}]")
            appendLine("Device             : $deviceManufacturer $deviceModel ($hardwareBoard)")
            appendLine("Android OS         : Android $androidVersion (API $apiLevel)")
            appendLine("Selected Encoder   : $selectedEncoder")
            appendLine("Available Codecs   : $availableEncoders")
            appendLine("Requested Output   : $requestedResolution @ ${String.format("%.1f", requestedFps)} FPS")
            appendLine("Target Bitrate     : $requestedBitrateMbps Mbps")
            appendLine("Target Profile     : $selectedProfile")
            appendLine("EGL Configuration  : $eglConfigDetails")
            if (!codecErrorCode.isNullOrEmpty()) {
                appendLine("Codec Error Code   : $codecErrorCode")
            }
            appendLine("Fallback Attempted : $fallbackAttempted")
            appendLine("--------------------------------------------------------")
            appendLine("EXCEPTION MESSAGE:")
            appendLine(exceptionMessage)
            appendLine("--------------------------------------------------------")
            appendLine("STACK TRACE:")
            appendLine(stackTrace)
            appendLine("========================================================")
        }
    }
}
