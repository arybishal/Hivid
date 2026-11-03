package com.hivid.app.ui.screens.convert

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hivid.app.data.local.HistoryEntity
import com.hivid.app.data.local.HividDatabase
import com.hivid.app.data.preferences.SettingsRepository
import com.hivid.app.domain.model.*
import com.hivid.app.media.CapabilityProbe
import com.hivid.app.media.HdrConversionException
import com.hivid.app.media.HdrConversionService
import com.hivid.app.media.HdrConverter
import com.hivid.app.media.SourceAnalyzer
import com.hivid.app.util.AppLogger
import com.hivid.app.util.MediaFileManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConvertUiState(
    val sourceInfo: SourceInfo? = null,
    val isAnalyzingSource: Boolean = false,
    val capabilities: DeviceCapabilities = DeviceCapabilities(
        encoderName = "Probing...",
        isHardwareAccelerated = false,
        isHdr10Ready = false,
        isHdr10PlusReady = false,
        isSurfaceInputSupported = false,
        maxWidth = 1920,
        maxHeight = 1080,
        maxFps = 60,
        maxBitrateMbps = 50
    ),
    val config: ConversionConfig = ConversionConfig(),
    val conversionState: ConversionState = ConversionState.Idle,
    val thermalWarning: Boolean = false,
    val errorMessage: String? = null
)

class ConvertViewModel(application: Application) : AndroidViewModel(application) {

    private val sourceAnalyzer = SourceAnalyzer(application)
    private val capabilityProbe = CapabilityProbe()
    private val hdrConverter = HdrConverter(application)
    private val settingsRepository = SettingsRepository(application)
    private val historyDao = HividDatabase.getDatabase(application).historyDao()

    private val _uiState = MutableStateFlow(ConvertUiState())
    val uiState: StateFlow<ConvertUiState> = _uiState.asStateFlow()

    private var conversionJob: Job? = null

    init {
        probeCapabilities()
        loadPersistedSettings()
        observeService()
    }

    private fun probeCapabilities() {
        viewModelScope.launch {
            val caps = capabilityProbe.probe()
            _uiState.update { it.copy(capabilities = caps) }
            AppLogger.i(
                "CapabilityProbe",
                "Hardware probe completed: Encoder=${caps.encoderName}, Main10=${caps.isHdr10Ready}, HDR10+=${caps.isHdr10PlusReady}, MaxBitrate=${caps.maxBitrateMbps}Mbps"
            )
        }
    }

    private fun loadPersistedSettings() {
        viewModelScope.launch {
            settingsRepository.userSettingsFlow.firstOrNull()?.let { settings ->
                val profile = if (settings.defaultProfile == "HDR10+ MAX") HdrProfile.HDR10_PLUS else HdrProfile.HDR10
                _uiState.update {
                    it.copy(
                        config = it.config.copy(
                            profile = profile,
                            expansionPercent = settings.defaultExpansion
                        )
                    )
                }
            }
        }
    }

    private fun observeService() {
        viewModelScope.launch {
            HdrConversionService.thermalThrottlingWarning.collect { warning ->
                _uiState.update { it.copy(thermalWarning = warning) }
                if (warning) {
                    AppLogger.w("ThermalManager", "Thermal status SEVERE reported by PowerManager")
                }
            }
        }
        viewModelScope.launch {
            HdrConversionService.cancelRequested.collect { cancel ->
                if (cancel && _uiState.value.conversionState is ConversionState.Processing) {
                    cancelConversion()
                }
            }
        }
    }

    fun onSourceVideoSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingSource = true, errorMessage = null) }
            AppLogger.i("SourceAnalyzer", "Source video selected for analysis: $uri")
            val result = sourceAnalyzer.analyze(uri)
            result.onSuccess { info ->
                _uiState.update { it.copy(sourceInfo = info, isAnalyzingSource = false) }
                AppLogger.i(
                    "SourceAnalyzer",
                    "Source analyzed: ${info.fileName} (${info.displayResolution}, ${info.formattedDuration}, ${info.formattedBitrateMbps}Mbps, Codec=${info.videoMime}, AlreadyHDR=${info.isAlreadyHdr})"
                )
                if (info.isAlreadyHdr) {
                    AppLogger.w("SourceAnalyzer", "Source video is already HDR: ${info.hdrTypeDescription}")
                }
            }.onFailure { error ->
                AppLogger.e("SourceAnalyzer", "Failed to analyze video: ${error.message}", error)
                _uiState.update {
                    it.copy(
                        isAnalyzingSource = false,
                        errorMessage = "Failed to analyze video: ${error.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onProfileSelected(profile: HdrProfile) {
        _uiState.update { it.copy(config = it.config.copy(profile = profile)) }
        AppLogger.d("Settings", "HDR profile changed to ${profile.displayName}")
        viewModelScope.launch {
            settingsRepository.updateDefaultProfile(profile.displayName)
        }
    }

    fun onFpsModeSelected(mode: FpsMode) {
        _uiState.update { it.copy(config = it.config.copy(fpsMode = mode)) }
        AppLogger.d("Settings", "FPS mode changed to ${mode.displayName}")
    }

    fun onExpansionChanged(percent: Float) {
        _uiState.update { it.copy(config = it.config.copy(expansionPercent = percent)) }
        viewModelScope.launch {
            settingsRepository.updateDefaultExpansion(percent)
        }
    }

    fun startConversion() {
        val state = _uiState.value
        val source = state.sourceInfo ?: return
        if (!state.capabilities.isHdr10Ready) {
            AppLogger.e("ConvertViewModel", "Cannot start conversion: HDR10 hardware encoder not ready")
            return
        }

        val app = getApplication<Application>()

        // Check storage space (estimate ~1.2x source size)
        val estimatedBytes = (source.fileSize * 1.3).toLong()
        if (!MediaFileManager.hasAvailableDiskSpace(app, estimatedBytes)) {
            val err = "Insufficient storage space. Need at least 1.5x free space (${estimatedBytes / 1_000_000} MB)."
            AppLogger.e("Storage", err)
            _uiState.update {
                it.copy(
                    errorMessage = err,
                    conversionState = ConversionState.Error(
                        message = err,
                        stage = PipelineStage.INITIALIZATION
                    )
                )
            }
            return
        }

        conversionJob?.cancel()
        conversionJob = viewModelScope.launch {
            _uiState.update { it.copy(conversionState = ConversionState.Preparing, errorMessage = null) }
            HdrConversionService.start(app)

            val tempFile = MediaFileManager.createTempFile(app, "hivid_hdr")
            _uiState.update { it.copy(conversionState = ConversionState.Processing(0)) }

            val result = hdrConverter.convert(
                sourceInfo = source,
                config = state.config,
                capabilities = state.capabilities,
                outputFile = tempFile,
                onProgress = { progress ->
                    _uiState.update { it.copy(conversionState = ConversionState.Processing(progress)) }
                    HdrConversionService.updateProgress(progress)
                }
            )

            HdrConversionService.stop(app)

            result.onSuccess { report ->
                // Publish to MediaStore Movies/Hivid
                val prefix = if (state.config.profile == HdrProfile.HDR10_PLUS && state.capabilities.isHdr10PlusReady) {
                    "HDR10Plus"
                } else {
                    "HDR10"
                }
                val finalFileName = MediaFileManager.generateOutputFileName(prefix)
                val mediaUri = MediaFileManager.saveToMediaStore(app, tempFile, finalFileName)
                if (tempFile.exists()) {
                    tempFile.delete()
                }

                if (mediaUri == null) {
                    AppLogger.e("MediaStore", "Failed to save final video to MediaStore: $finalFileName")
                } else {
                    AppLogger.i("MediaStore", "Video published to MediaStore: $mediaUri")
                }

                val finalReport = report.copy(
                    outputName = finalFileName,
                    storageLocation = mediaUri?.toString() ?: "Movies/Hivid"
                )

                // Persist to Room History
                historyDao.insert(
                    HistoryEntity(
                        title = finalFileName,
                        outputUriString = mediaUri?.toString() ?: "",
                        profile = finalReport.profile,
                        resolution = "${finalReport.outputWidth}x${finalReport.outputHeight}",
                        fileSizeBytes = finalReport.fileSizeBytes,
                        durationMs = source.durationMs,
                        reportJson = ""
                    )
                )

                _uiState.update { it.copy(conversionState = ConversionState.Done(finalReport)) }
            }.onFailure { error ->
                val conversionEx = error as? HdrConversionException
                val incident = conversionEx?.incident
                val stage = conversionEx?.stage

                _uiState.update {
                    it.copy(
                        conversionState = ConversionState.Error(
                            message = error.localizedMessage ?: "Conversion failed",
                            stage = stage,
                            diagnosticIncident = incident
                        ),
                        errorMessage = error.localizedMessage
                    )
                }
            }
        }
    }

    fun cancelConversion() {
        conversionJob?.cancel()
        conversionJob = null
        HdrConversionService.stop(getApplication())
        AppLogger.w("ConvertViewModel", "Conversion cancelled by user")
        _uiState.update { it.copy(conversionState = ConversionState.Cancelled) }
    }

    fun dismissResultDialog() {
        _uiState.update { it.copy(conversionState = ConversionState.Idle) }
    }
}
