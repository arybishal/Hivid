package com.hivid.app.ui.screens.remux

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hivid.app.R
import com.hivid.app.domain.model.ConversionState
import com.hivid.app.ui.components.*
import com.hivid.app.ui.theme.HdrColors
import com.hivid.app.ui.theme.SansSerifFont
import com.hivid.app.ui.theme.TechnicalMonoFont

@Composable
fun RemuxScreen(
    viewModel: RemuxViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.onSourceVideoSelected(it) }
    }

    val isProcessing = uiState.remuxState is ConversionState.Processing ||
            uiState.remuxState is ConversionState.Preparing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Section 1: MAIN ENGINE
        SectionLabel(text = "MAIN ENGINE")
        InfoCard(
            title = stringResource(R.string.remux_title),
            description = stringResource(R.string.remux_desc)
        ) {
            // 4 Toggle Chips (2x2 grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleChip(
                    label = stringResource(R.string.chip_safe_h264),
                    checked = uiState.config.safeH264,
                    onCheckedChange = { viewModel.toggleSafeH264(it) },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                )
                ToggleChip(
                    label = stringResource(R.string.chip_orig_bitrate),
                    checked = uiState.config.originalBitrate,
                    onCheckedChange = { viewModel.toggleOriginalBitrate(it) },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleChip(
                    label = stringResource(R.string.chip_safe_hdr),
                    checked = uiState.config.safeHdrSdr,
                    onCheckedChange = { viewModel.toggleSafeHdrSdr(it) },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                )
                ToggleChip(
                    label = stringResource(R.string.chip_streaming),
                    checked = uiState.config.streamingFastStart,
                    onCheckedChange = { viewModel.toggleStreaming(it) },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pick MP4 Outline Button
            OutlineButton(
                text = stringResource(R.string.btn_pick_mp4),
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 2: PROCESS
        SectionLabel(text = "PROCESS")
        InfoCard(
            title = null,
            description = null
        ) {
            val statusText = when (val state = uiState.remuxState) {
                is ConversionState.Idle -> if (uiState.sourceInfo != null) stringResource(R.string.remux_ready) else stringResource(R.string.remux_waiting)
                is ConversionState.Preparing -> "Preparing container remux..."
                is ConversionState.Processing -> "Lossless packaging · ${state.percent}%"
                is ConversionState.Done -> "Lossless repackaging complete"
                is ConversionState.Cancelled -> "Cancelled"
                is ConversionState.Error -> "Error: ${state.message}"
            }

            Text(
                text = statusText,
                fontFamily = TechnicalMonoFont,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (uiState.remuxState is ConversionState.Error) HdrColors.Error else HdrColors.Accent
            )

            if (isProcessing) {
                val p = when (val s = uiState.remuxState) {
                    is ConversionState.Processing -> s.percent / 100f
                    else -> 0f
                }
                Spacer(modifier = Modifier.height(10.dp))
                HividProgressBar(progress = p)
            }

            Spacer(modifier = Modifier.height(14.dp))

            PrimaryButton(
                text = stringResource(R.string.btn_process),
                onClick = { viewModel.startRemux() },
                icon = Icons.Default.Layers,
                enabled = uiState.sourceInfo != null && !isProcessing
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 3: VERIFICATION
        SectionLabel(text = "VERIFICATION")
        InfoCard(
            title = null,
            description = null
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (uiState.technicalDetails?.isLosslessExactMatch == true) HdrColors.Success else HdrColors.Accent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (uiState.technicalDetails?.isLosslessExactMatch == true) {
                        stringResource(R.string.verification_passed)
                    } else {
                        stringResource(R.string.verification_ready)
                    },
                    fontFamily = SansSerifFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = HdrColors.Text
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlineButton(
                text = stringResource(R.string.btn_open_tech),
                onClick = { viewModel.openTechnicalAccess() },
                icon = Icons.Default.Code,
                enabled = uiState.technicalDetails != null
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Technical Access Dialog
    if (uiState.showTechnicalDialog && uiState.technicalDetails != null) {
        val tech = uiState.technicalDetails!!
        Dialog(onDismissRequest = { viewModel.dismissTechnicalAccess() }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, HdrColors.Border, RoundedCornerShape(16.dp))
                    .background(HdrColors.SurfaceHigh)
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "TECHNICAL STREAM VERIFICATION",
                        fontFamily = SansSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = HdrColors.Accent
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    MonoLine(keyText = "Input Video SHA-256", valueText = tech.inputVideoSha256.take(16) + "...")
                    MonoLine(keyText = "Output Video SHA-256", valueText = tech.outputVideoSha256.take(16) + "...", valueColor = HdrColors.Success)
                    if (tech.inputAudioSha256 != null && tech.outputAudioSha256 != null) {
                        MonoLine(keyText = "Input Audio SHA-256", valueText = tech.inputAudioSha256.take(16) + "...")
                        MonoLine(keyText = "Output Audio SHA-256", valueText = tech.outputAudioSha256.take(16) + "...", valueColor = HdrColors.Success)
                    }
                    MonoLine(keyText = "Video Samples Processed", valueText = "${tech.videoSampleCount}")
                    MonoLine(keyText = "Audio Samples Processed", valueText = "${tech.audioSampleCount}")
                    MonoLine(
                        keyText = "Bit-Identical Payload",
                        valueText = if (tech.isLosslessExactMatch) "100% MATCH" else "DIFFERENT",
                        valueColor = if (tech.isLosslessExactMatch) HdrColors.Success else HdrColors.Error
                    )
                    MonoLine(keyText = "Fast-Start Relocation", valueText = if (tech.moovRelocated) "PASSED (moov before mdat)" else "STANDARD")

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = tech.mp4BoxSummary,
                        fontFamily = TechnicalMonoFont,
                        fontSize = 11.sp,
                        color = HdrColors.TextFaint
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = stringResource(R.string.dialog_ok),
                        onClick = { viewModel.dismissTechnicalAccess() }
                    )
                }
            }
        }
    }

    // Completion Dialog
    val remuxState = uiState.remuxState
    if (remuxState is ConversionState.Done) {
        ResultDialog(
            report = remuxState.report,
            onDismiss = { viewModel.dismissResultDialog() },
            onOpen = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(remuxState.report.storageLocation), "video/mp4")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(intent, "Open Remuxed Video"))
            },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(remuxState.report.storageLocation))
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(intent, "Share Remuxed Video"))
            }
        )
    }
}
