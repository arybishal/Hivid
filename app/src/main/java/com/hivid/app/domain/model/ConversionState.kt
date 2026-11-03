package com.hivid.app.domain.model

sealed class ConversionState {
    object Idle : ConversionState()
    object Preparing : ConversionState()
    data class Processing(val percent: Int) : ConversionState()
    data class Done(val report: ConversionReport) : ConversionState()
    object Cancelled : ConversionState()
    data class Error(
        val message: String,
        val stage: PipelineStage? = null,
        val diagnosticIncident: DiagnosticIncident? = null
    ) : ConversionState()
}
