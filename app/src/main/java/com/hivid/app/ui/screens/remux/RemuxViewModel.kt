package com.hivid.app.ui.screens.remux

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hivid.app.data.local.HistoryEntity
import com.hivid.app.data.local.HividDatabase
import com.hivid.app.domain.model.ConversionReport
import com.hivid.app.domain.model.ConversionState
import com.hivid.app.domain.model.SourceInfo
import com.hivid.app.media.RemuxConfig
import com.hivid.app.media.RemuxEngine
import com.hivid.app.media.SourceAnalyzer
import com.hivid.app.media.TechnicalAccessDetails
import com.hivid.app.util.MediaFileManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RemuxUiState(
    val sourceInfo: SourceInfo? = null,
    val isAnalyzingSource: Boolean = false,
    val config: RemuxConfig = RemuxConfig(),
    val remuxState: ConversionState = ConversionState.Idle,
    val technicalDetails: TechnicalAccessDetails? = null,
    val showTechnicalDialog: Boolean = false,
    val errorMessage: String? = null
)

class RemuxViewModel(application: Application) : AndroidViewModel(application) {

    private val sourceAnalyzer = SourceAnalyzer(application)
    private val remuxEngine = RemuxEngine(application)
    private val historyDao = HividDatabase.getDatabase(application).historyDao()

    private val _uiState = MutableStateFlow(RemuxUiState())
    val uiState: StateFlow<RemuxUiState> = _uiState.asStateFlow()

    private var remuxJob: Job? = null

    fun onSourceVideoSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingSource = true, errorMessage = null) }
            val result = sourceAnalyzer.analyze(uri)
            result.onSuccess { info ->
                _uiState.update { it.copy(sourceInfo = info, isAnalyzingSource = false) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isAnalyzingSource = false, errorMessage = "Failed to read video: ${err.localizedMessage}")
                }
            }
        }
    }

    fun toggleSafeH264(enabled: Boolean) {
        _uiState.update { it.copy(config = it.config.copy(safeH264 = enabled)) }
    }

    fun toggleOriginalBitrate(enabled: Boolean) {
        _uiState.update { it.copy(config = it.config.copy(originalBitrate = enabled)) }
    }

    fun toggleSafeHdrSdr(enabled: Boolean) {
        _uiState.update { it.copy(config = it.config.copy(safeHdrSdr = enabled)) }
    }

    fun toggleStreaming(enabled: Boolean) {
        _uiState.update { it.copy(config = it.config.copy(streamingFastStart = enabled)) }
    }

    fun startRemux() {
        val state = _uiState.value
        val source = state.sourceInfo ?: return
        val app = getApplication<Application>()

        remuxJob?.cancel()
        remuxJob = viewModelScope.launch {
            _uiState.update { it.copy(remuxState = ConversionState.Preparing) }
            val tempFile = MediaFileManager.createTempFile(app, "hivid_remux")

            val result = remuxEngine.process(
                sourceInfo = source,
                config = state.config,
                outputFile = tempFile,
                onProgress = { progress ->
                    _uiState.update { it.copy(remuxState = ConversionState.Processing(progress)) }
                }
            )

            result.onSuccess { (report, tech) ->
                val finalFileName = MediaFileManager.generateOutputFileName("Remux")
                val mediaUri = MediaFileManager.saveToMediaStore(app, tempFile, finalFileName)
                if (tempFile.exists()) {
                    tempFile.delete()
                }

                val finalReport = report.copy(
                    outputName = finalFileName,
                    storageLocation = mediaUri?.toString() ?: "Movies/Hivid"
                )

                // Save to History
                historyDao.insert(
                    HistoryEntity(
                        title = finalFileName,
                        outputUriString = mediaUri?.toString() ?: "",
                        profile = "Lossless Remux",
                        resolution = "${source.visibleWidth}x${source.visibleHeight}",
                        fileSizeBytes = finalReport.fileSizeBytes,
                        durationMs = source.durationMs,
                        reportJson = ""
                    )
                )

                _uiState.update {
                    it.copy(
                        remuxState = ConversionState.Done(finalReport),
                        technicalDetails = tech
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(remuxState = ConversionState.Error(err.localizedMessage ?: "Remux failed"))
                }
            }
        }
    }

    fun openTechnicalAccess() {
        _uiState.update { it.copy(showTechnicalDialog = true) }
    }

    fun dismissTechnicalAccess() {
        _uiState.update { it.copy(showTechnicalDialog = false) }
    }

    fun dismissResultDialog() {
        _uiState.update { it.copy(remuxState = ConversionState.Idle) }
    }
}
