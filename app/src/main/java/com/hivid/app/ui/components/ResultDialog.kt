package com.hivid.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hivid.app.R
import com.hivid.app.domain.model.ConversionReport
import com.hivid.app.ui.theme.HdrColors
import com.hivid.app.ui.theme.SansSerifFont
import com.hivid.app.ui.theme.TechnicalMonoFont

@Composable
fun ResultDialog(
    report: ConversionReport,
    onDismiss: () -> Unit,
    onOpen: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, HdrColors.Border, RoundedCornerShape(16.dp))
                .background(HdrColors.SurfaceHigh)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (report.isRemux) stringResource(R.string.dialog_remux_complete) else stringResource(R.string.dialog_hdr_complete),
                        fontFamily = SansSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = HdrColors.Accent
                    )
                    SpecPill(
                        text = report.profile,
                        backgroundColor = HdrColors.Accent.copy(alpha = 0.2f),
                        contentColor = HdrColors.Accent
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Technical Report
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    MonoLine(keyText = "Output File", valueText = report.outputName)
                    MonoLine(keyText = "Profile", valueText = report.profile, valueColor = HdrColors.Accent)
                    MonoLine(keyText = "Color Gamut", valueText = report.colorStandard)
                    MonoLine(keyText = "Transfer Function", valueText = report.colorTransfer)
                    MonoLine(keyText = "Encoder", valueText = report.encoder)
                    MonoLine(keyText = "Source Dimensions", valueText = "${report.sourceWidth}x${report.sourceHeight}")
                    MonoLine(keyText = "Output Pixels", valueText = "${report.outputWidth}x${report.outputHeight}")
                    MonoLine(keyText = "Rotation", valueText = "${report.rotation}°")
                    MonoLine(keyText = "Source FPS", valueText = String.format("%.2f", report.sourceFps))
                    MonoLine(keyText = "Output FPS Target", valueText = String.format("%.2f", report.targetFps))
                    MonoLine(keyText = "Source Bitrate", valueText = "${report.sourceBitrateMbps} Mbps")
                    MonoLine(keyText = "Requested Bitrate", valueText = "${report.requestedBitrateMbps} Mbps")
                    MonoLine(keyText = "Actual Bitrate", valueText = "${report.actualBitrateMbps} Mbps", valueColor = HdrColors.Success)
                    MonoLine(keyText = "Encoded Samples", valueText = "${report.encodedSamples}")
                    MonoLine(keyText = "Measured Cadence", valueText = report.measuredCadence)
                    MonoLine(keyText = "Output Size", valueText = report.formattedSize)
                    MonoLine(keyText = "Audio Passthrough", valueText = report.audioPolicy)
                    MonoLine(keyText = "Location", valueText = report.storageLocation, valueColor = HdrColors.TextMuted)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Compatibility Note
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(HdrColors.Surface)
                            .border(1.dp, HdrColors.Border, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = report.compatibilityNote,
                            fontFamily = TechnicalMonoFont,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = HdrColors.TextFaint
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onShare != null) {
                        TextButton(onClick = onShare) {
                            Text(
                                text = stringResource(R.string.dialog_share),
                                fontFamily = SansSerifFont,
                                fontWeight = FontWeight.SemiBold,
                                color = HdrColors.Text
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (onOpen != null) {
                        TextButton(onClick = onOpen) {
                            Text(
                                text = stringResource(R.string.dialog_open),
                                fontFamily = SansSerifFont,
                                fontWeight = FontWeight.SemiBold,
                                color = HdrColors.Text
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    PrimaryButton(
                        text = stringResource(R.string.dialog_ok),
                        onClick = onDismiss,
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
        }
    }
}
