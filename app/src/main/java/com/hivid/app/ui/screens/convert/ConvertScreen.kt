package com.hivid.app.ui.screens.convert

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivid.app.R
import com.hivid.app.domain.model.*
import com.hivid.app.ui.components.*
import com.hivid.app.ui.theme.HdrColors
import com.hivid.app.ui.theme.SansSerifFont
import com.hivid.app.ui.theme.TechnicalMonoFont

@Composable
fun ConvertScreen(
    viewModel: ConvertViewModel,
    onOpenLogs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.onSourceVideoSelected(it) }
    }

    val isProcessing = uiState.conversionState is ConversionState.Processing ||
            uiState.conversionState is ConversionState.Preparing

    val currentPercent = when (val state = uiState.conversionState) {
        is ConversionState.Processing -> state.percent
        is ConversionState.Preparing -> 0
        else -> 0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Thermal Throttling Warning Banner if active
        if (uiState.thermalWarning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, HdrColors.Warning, RoundedCornerShape(12.dp))
                    .background(HdrColors.SurfaceHigh)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = HdrColors.Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device is warm. Conversion speed may be throttled to protect battery.",
                        fontFamily = SansSerifFont,
                        fontSize = 12.sp,
                        color = HdrColors.Warning
                    )
                }
            }
        }

        // 1. HDR MASTERING ENGINE (Card 1)
        SectionLabel(text = stringResource(R.string.hdr_mastering_engine))
        InfoCard(
            title = null,
            description = stringResource(R.string.hdr_engine_desc)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecPill(text = stringResource(R.string.spec_10bit))
                SpecPill(text = stringResource(R.string.spec_bt2020))
                SpecPill(text = stringResource(R.string.spec_pq))
                SpecPill(text = stringResource(R.string.spec_gpu))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. SOURCE VIDEO (Card 2)
        SectionLabel(text = stringResource(R.string.source_video))
        InfoCard(
            title = null,
            description = stringResource(R.string.source_video_desc)
        ) {
            PrimaryButton(
                text = stringResource(R.string.btn_pick_video),
                onClick = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                icon = Icons.Default.VideoFile,
                enabled = !isProcessing
            )

            Spacer(modifier = Modifier.height(10.dp))

            val sourceText = uiState.sourceInfo?.let {
                stringResource(R.string.video_prefix, it.fileName)
            } ?: stringResource(R.string.no_video_selected)

            Text(
                text = sourceText,
                fontFamily = TechnicalMonoFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (uiState.sourceInfo != null) HdrColors.Accent else HdrColors.TextFaint
            )

            if (uiState.sourceInfo?.isAlreadyHdr == true) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, HdrColors.Warning, RoundedCornerShape(8.dp))
                        .background(HdrColors.SurfaceHigh)
                        .padding(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.warn_already_hdr, uiState.sourceInfo?.hdrTypeDescription ?: "HDR"),
                        fontFamily = SansSerifFont,
                        fontSize = 12.sp,
                        color = HdrColors.Warning
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. OUTPUT PROFILE (Card 3)
        SectionLabel(text = stringResource(R.string.output_profile))
        InfoCard(
            title = null,
            description = null
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioChip(
                    label = stringResource(R.string.profile_hdr10),
                    selected = uiState.config.profile == HdrProfile.HDR10,
                    onClick = { viewModel.onProfileSelected(HdrProfile.HDR10) },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                )
                RadioChip(
                    label = stringResource(R.string.profile_hdr10_plus),
                    selected = uiState.config.profile == HdrProfile.HDR10_PLUS,
                    onClick = { viewModel.onProfileSelected(HdrProfile.HDR10_PLUS) },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing && uiState.capabilities.isHdr10PlusReady
                )
            }

            if (!uiState.capabilities.isHdr10PlusReady) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.hdr10_plus_disabled_reason),
                    fontFamily = SansSerifFont,
                    fontSize = 11.sp,
                    color = HdrColors.TextFaint
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mono technical lines
            val source = uiState.sourceInfo
            val caps = uiState.capabilities

            val resFps = if (source != null) {
                "${source.displayResolution} · ${String.format("%.1f", source.frameRate)}fps · ${source.rotation}°"
            } else {
                "--- · --- · 0°"
            }
            MonoLine(keyText = "Source Dimensions", valueText = resFps)

            val codedVsVis = if (source != null) {
                "${source.codedWidth}x${source.codedHeight} → ${source.visibleWidth}x${source.visibleHeight}"
            } else {
                "---"
            }
            MonoLine(keyText = "Coded vs Visible", valueText = codedVsVis)

            val bitrate = if (source != null) "${source.formattedBitrateMbps} Mbps" else "---"
            MonoLine(keyText = "Source Bitrate", valueText = bitrate)

            val statusPair = "${caps.hdr10StatusText} · ${caps.hdr10PlusStatusText}"
            MonoLine(
                keyText = "Hardware Check",
                valueText = statusPair,
                valueColor = if (caps.isHdr10Ready) HdrColors.Success else HdrColors.Error
            )

            MonoLine(keyText = "Encoder", valueText = caps.encoderName)
            MonoLine(keyText = "Max Request", valueText = "${caps.maxBitrateMbps} Mbps")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. FRAME RATE ENGINE (Card 4)
        SectionLabel(text = stringResource(R.string.frame_rate_engine))
        InfoCard(
            title = null,
            description = stringResource(R.string.frame_rate_desc)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioChip(
                    label = stringResource(R.string.fps_source),
                    selected = uiState.config.fpsMode == FpsMode.SOURCE_CADENCE,
                    onClick = { viewModel.onFpsModeSelected(FpsMode.SOURCE_CADENCE) },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                )
                RadioChip(
                    label = stringResource(R.string.fps_cap_60),
                    selected = uiState.config.fpsMode == FpsMode.CAP_60,
                    onClick = { viewModel.onFpsModeSelected(FpsMode.CAP_60) },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. HDR EXPANSION (Card 5)
        SectionLabel(text = stringResource(R.string.hdr_expansion))
        InfoCard(
            title = null,
            description = stringResource(R.string.hdr_expansion_desc)
        ) {
            ExpansionSlider(
                value = uiState.config.expansionPercent,
                onValueChange = { viewModel.onExpansionChanged(it) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 6. PROCESSING STATUS (Card 6)
        SectionLabel(text = stringResource(R.string.processing_status))
        InfoCard(
            title = null,
            description = null
        ) {
            val statusTitle = when (val state = uiState.conversionState) {
                is ConversionState.Idle -> stringResource(R.string.status_idle)
                is ConversionState.Preparing -> "PREPARING PIPELINE · 0%"
                is ConversionState.Processing -> stringResource(R.string.status_processing, state.percent)
                is ConversionState.Done -> stringResource(R.string.status_done)
                is ConversionState.Cancelled -> stringResource(R.string.status_cancelled)
                is ConversionState.Error -> "${stringResource(R.string.status_error)}: ${state.message}"
            }

            Text(
                text = statusTitle,
                fontFamily = TechnicalMonoFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (uiState.conversionState is ConversionState.Error) HdrColors.Error else HdrColors.Accent
            )

            Spacer(modifier = Modifier.height(10.dp))

            HividProgressBar(progress = currentPercent / 100f)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.output_hint),
                fontFamily = SansSerifFont,
                fontSize = 11.sp,
                color = HdrColors.TextFaint
            )

            if (uiState.conversionState is ConversionState.Error) {
                val errState = uiState.conversionState as ConversionState.Error
                Spacer(modifier = Modifier.height(10.dp))
                if (errState.stage != null) {
                    MonoLine(
                        keyText = "Failed Stage",
                        valueText = errState.stage.displayName,
                        valueColor = HdrColors.Error
                    )
                }
                if (errState.diagnosticIncident != null) {
                    MonoLine(
                        keyText = "Diagnostic ID",
                        valueText = errState.diagnosticIncident.diagnosticId.take(8),
                        valueColor = HdrColors.Accent
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlineButton(
                        text = "VIEW ERROR LOG",
                        onClick = onOpenLogs,
                        icon = Icons.Default.Warning,
                        modifier = Modifier.weight(1f)
                    )
                    if (errState.diagnosticIncident != null) {
                        OutlineButton(
                            text = "EXPORT REPORT",
                            onClick = {
                                val reportText = errState.diagnosticIncident.toFormattedReport()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Hivid Diagnostic Report")
                                    putExtra(Intent.EXTRA_TEXT, reportText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Diagnostic Report"))
                            },
                            icon = Icons.Default.Share,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Full-Width Action Button (CONVERT TO HDR / CANCEL)
        if (isProcessing) {
            PrimaryButton(
                text = stringResource(R.string.btn_cancel),
                onClick = { viewModel.cancelConversion() },
                icon = Icons.Default.Close,
                backgroundColor = HdrColors.Error,
                contentColor = HdrColors.Text
            )
        } else {
            val canConvert = uiState.sourceInfo != null && uiState.capabilities.isHdr10Ready
            PrimaryButton(
                text = stringResource(R.string.btn_convert),
                onClick = { viewModel.startConversion() },
                icon = Icons.Default.PlayArrow,
                enabled = canConvert
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 8. PIPELINE DEFINITIONS (Card 8)
        SectionLabel(text = stringResource(R.string.pipeline_definitions))
        InfoCard(
            title = null,
            description = null
        ) {
            GlossaryRow(
                term = stringResource(R.string.def_hevc_title),
                explanation = stringResource(R.string.def_hevc_desc)
            )
            GlossaryRow(
                term = stringResource(R.string.def_bt2020_title),
                explanation = stringResource(R.string.def_bt2020_desc)
            )
            GlossaryRow(
                term = stringResource(R.string.def_pq_title),
                explanation = stringResource(R.string.def_pq_desc)
            )
            GlossaryRow(
                term = stringResource(R.string.def_hdr10_title),
                explanation = stringResource(R.string.def_hdr10_desc)
            )
            GlossaryRow(
                term = stringResource(R.string.def_hdr10plus_title),
                explanation = stringResource(R.string.def_hdr10plus_desc)
            )
            GlossaryRow(
                term = stringResource(R.string.def_gallery_title),
                explanation = stringResource(R.string.def_gallery_desc)
            )
        }

        Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom floating nav
    }

    // 9. Completion Result Dialog
    val conversionState = uiState.conversionState
    if (conversionState is ConversionState.Done) {
        ResultDialog(
            report = conversionState.report,
            onDismiss = { viewModel.dismissResultDialog() },
            onOpen = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(conversionState.report.storageLocation), "video/mp4")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(intent, "Open HDR Video"))
            },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(conversionState.report.storageLocation))
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(intent, "Share HDR Video"))
            }
        )
    }
}
