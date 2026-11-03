package com.hivid.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivid.app.R
import com.hivid.app.ui.components.*
import com.hivid.app.ui.theme.HdrColors
import com.hivid.app.ui.theme.SansSerifFont
import com.hivid.app.ui.theme.TechnicalMonoFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.userSettings.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(HdrColors.Surface)
                    .border(1.dp, HdrColors.Border, RoundedCornerShape(20.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = HdrColors.Text
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.settings_title),
                fontFamily = SansSerifFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = HdrColors.Text
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. APPEARANCE (Light / Dark)
        SectionLabel(text = "APPEARANCE")
        InfoCard(
            title = null,
            description = "Choose between Dark Mode (Night) and Light Mode (Day). Dark Mode is the high-contrast studio default."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeModeOption(
                    label = "Dark Mode",
                    subtitle = "Night (Default)",
                    icon = Icons.Default.DarkMode,
                    isSelected = settings.themeMode != "LIGHT",
                    onClick = { viewModel.setThemeMode("DARK") },
                    modifier = Modifier.weight(1f)
                )
                ThemeModeOption(
                    label = "Light Mode",
                    subtitle = "Day",
                    icon = Icons.Default.LightMode,
                    isSelected = settings.themeMode == "LIGHT",
                    onClick = { viewModel.setThemeMode("LIGHT") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. DEFAULT OUTPUT PROFILE
        SectionLabel(text = "DEFAULT OUTPUT PROFILE")
        InfoCard(
            title = null,
            description = "Default HDR profile pre-selected when loading new SDR source media."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioChip(
                    label = "HDR10",
                    selected = settings.defaultProfile == "HDR10",
                    onClick = { viewModel.setDefaultProfile("HDR10") },
                    modifier = Modifier.weight(1f)
                )
                RadioChip(
                    label = "HDR10+ MAX",
                    selected = settings.defaultProfile == "HDR10+ MAX",
                    onClick = { viewModel.setDefaultProfile("HDR10+ MAX") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. DEFAULT HDR EXPANSION
        SectionLabel(text = "DEFAULT HDR EXPANSION")
        InfoCard(
            title = null,
            description = "Default highlight expansion intensity applied to SDR luminance curves."
        ) {
            ExpansionSlider(
                value = settings.defaultExpansion,
                onValueChange = { viewModel.setDefaultExpansion(it) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. FRAME RATE PREFERENCE
        SectionLabel(text = "FRAME RATE PREFERENCE")
        InfoCard(
            title = null,
            description = "Target playback rate. Capping at 60 FPS optimizes thermal stability on mobile chipsets."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioChip(
                    label = "60 FPS (Default)",
                    selected = settings.fpsCap == 60,
                    onClick = { viewModel.setFpsCap(60) },
                    modifier = Modifier.weight(1f)
                )
                RadioChip(
                    label = "Match Source",
                    selected = settings.fpsCap == 0,
                    onClick = { viewModel.setFpsCap(0) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. MANUAL BITRATE OVERRIDE
        SectionLabel(text = "MANUAL BITRATE OVERRIDE")
        InfoCard(
            title = null,
            description = "Encoding bandwidth. 'Auto' determines optimal Mbps from hardware codec capabilities."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val bitrates = listOf(
                    0 to "Auto",
                    80 to "80 Mbps",
                    120 to "120 Mbps",
                    160 to "160 Mbps"
                )
                bitrates.forEach { (mbps, label) ->
                    val isSelected = settings.manualBitrateOverride == mbps
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                if (isSelected) HdrColors.Accent else HdrColors.Border,
                                RoundedCornerShape(8.dp)
                            )
                            .background(if (isSelected) HdrColors.Accent else HdrColors.Surface)
                            .clickable { viewModel.setManualBitrate(mbps) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = SansSerifFont,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (isSelected) HdrColors.OnAccent else HdrColors.TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. OUTPUT FOLDER
        SectionLabel(text = "OUTPUT FOLDER")
        InfoCard(
            title = null,
            description = "Processed videos are stored in the device standard shared Movies/Hivid directory, indexed automatically in Gallery."
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HdrColors.SurfaceHigh)
                    .border(1.dp, HdrColors.Border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Folder",
                    tint = HdrColors.Accent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Movies/Hivid",
                        fontFamily = TechnicalMonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = HdrColors.Text
                    )
                    Text(
                        text = "Internal Shared Storage (MediaStore)",
                        fontFamily = SansSerifFont,
                        fontSize = 11.sp,
                        color = HdrColors.TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7. KEEP SCREEN ON
        SectionLabel(text = "SYSTEM & DISPLAY")
        InfoCard(
            title = null,
            description = "Display options during hardware encoding and GPU rendering."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Keep Screen On",
                        fontFamily = SansSerifFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = HdrColors.Text
                    )
                    Text(
                        text = "Prevents device sleep while encoding large 4K files",
                        fontFamily = SansSerifFont,
                        fontSize = 12.sp,
                        color = HdrColors.TextMuted
                    )
                }
                Switch(
                    checked = settings.keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HdrColors.OnAccent,
                        checkedTrackColor = HdrColors.Accent,
                        uncheckedThumbColor = HdrColors.TextFaint,
                        uncheckedTrackColor = HdrColors.SurfaceHigh
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 8. NOTIFICATIONS
        SectionLabel(text = "NOTIFICATIONS")
        InfoCard(
            title = null,
            description = "Foreground service notification updates during pipeline execution."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Foreground Notifications",
                        fontFamily = SansSerifFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = HdrColors.Text
                    )
                    Text(
                        text = "Show live progress notification and cancel action",
                        fontFamily = SansSerifFont,
                        fontSize = 12.sp,
                        color = HdrColors.TextMuted
                    )
                }
                Switch(
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotifications(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HdrColors.OnAccent,
                        checkedTrackColor = HdrColors.Accent,
                        uncheckedThumbColor = HdrColors.TextFaint,
                        uncheckedTrackColor = HdrColors.SurfaceHigh
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 9. LOGS & DIAGNOSTICS
        SectionLabel(text = "DIAGNOSTICS & TELEMETRY")
        InfoCard(
            title = "Logs & Diagnostics",
            description = "Inspect real-time application logs, hardware codec probes, OpenGL ES pipeline events, and export diagnostic error incident reports."
        ) {
            PrimaryButton(
                text = "OPEN LOGS & DIAGNOSTICS",
                onClick = onOpenLogs,
                icon = Icons.Default.BugReport
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 10. ABOUT THE DEVELOPER (Credits)
        SectionLabel(text = "CREDITS")
        InfoCard(
            title = "ABOUT THE DEVELOPER",
            description = null
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Author
                Column {
                    Text(
                        text = "Author",
                        fontFamily = SansSerifFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = HdrColors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Bishal Aryal",
                        fontFamily = SansSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = HdrColors.Text
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = HdrColors.Border,
                    thickness = 1.dp
                )

                // Website
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bishalaryal.com.np"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        }
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "Website",
                        fontFamily = SansSerifFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = HdrColors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "https://bishalaryal.com.np",
                            fontFamily = TechnicalMonoFont,
                            fontSize = 13.sp,
                            color = HdrColors.Accent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open Website",
                            tint = HdrColors.Accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = HdrColors.Border,
                    thickness = 1.dp
                )

                // Contact
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:ib@bishalaryal.com")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val fallback = Intent(Intent.ACTION_SEND).apply {
                                        type = "message/rfc822"
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf("ib@bishalaryal.com"))
                                    }
                                    context.startActivity(Intent.createChooser(fallback, "Send Email"))
                                } catch (e2: Exception) {
                                    // Fallback
                                }
                            }
                        }
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "Contact",
                        fontFamily = SansSerifFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = HdrColors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ib@bishalaryal.com",
                            fontFamily = TechnicalMonoFont,
                            fontSize = 13.sp,
                            color = HdrColors.Accent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Send Email",
                            tint = HdrColors.Accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ThemeModeOption(
    label: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) HdrColors.Accent else HdrColors.Border,
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (isSelected) HdrColors.SurfaceHigh else HdrColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) HdrColors.Accent else HdrColors.SurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) HdrColors.OnAccent else HdrColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    fontFamily = SansSerifFont,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (isSelected) HdrColors.Text else HdrColors.TextMuted
                )
                Text(
                    text = subtitle,
                    fontFamily = SansSerifFont,
                    fontSize = 11.sp,
                    color = HdrColors.TextFaint
                )
            }
        }
    }
}
