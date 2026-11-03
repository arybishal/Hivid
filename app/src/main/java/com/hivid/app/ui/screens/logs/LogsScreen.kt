package com.hivid.app.ui.screens.logs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivid.app.data.local.LogEntity
import com.hivid.app.domain.model.LogLevel
import com.hivid.app.ui.components.InfoCard
import com.hivid.app.ui.components.PrimaryButton
import com.hivid.app.ui.theme.HdrColors
import com.hivid.app.ui.theme.SansSerifFont
import com.hivid.app.ui.theme.TechnicalMonoFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: LogsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logs by viewModel.logs.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val errorCount by viewModel.errorCount.collectAsState()
    val warningCount by viewModel.warningCount.collectAsState()
    val healthStatus by viewModel.healthStatus.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. Top Header with Status Indicator
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Logs & Diagnostics",
                    fontFamily = SansSerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = HdrColors.Text
                )
                Text(
                    text = "100% on-device telemetry",
                    fontFamily = SansSerifFont,
                    fontSize = 11.sp,
                    color = HdrColors.TextMuted
                )
            }

            // Diagnostic Health Indicator Pill
            val (statusText, statusColor) = when (healthStatus) {
                SystemHealthStatus.HEALTHY -> Pair("HEALTHY", HdrColors.Success)
                SystemHealthStatus.WARNING -> Pair("WARNING", HdrColors.Warning)
                SystemHealthStatus.ERROR -> Pair("ERROR", HdrColors.Error)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        fontFamily = TechnicalMonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = statusColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Metrics Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(label = "TOTAL", count = totalCount, color = HdrColors.Accent, modifier = Modifier.weight(1f))
            MetricCard(label = "ERRORS", count = errorCount, color = if (errorCount > 0) HdrColors.Error else HdrColors.TextMuted, modifier = Modifier.weight(1f))
            MetricCard(label = "WARNINGS", count = warningCount, color = if (warningCount > 0) HdrColors.Warning else HdrColors.TextMuted, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, HdrColors.Border, RoundedCornerShape(14.dp)),
            placeholder = {
                Text(
                    text = "Search logs, tags, exceptions, ID...",
                    fontFamily = SansSerifFont,
                    fontSize = 13.sp,
                    color = HdrColors.TextFaint
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = HdrColors.TextMuted
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = HdrColors.TextMuted
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = HdrColors.Surface,
                unfocusedContainerColor = HdrColors.Surface,
                focusedTextColor = HdrColors.Text,
                unfocusedTextColor = HdrColors.Text,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChipItem(
                label = "ALL",
                isSelected = selectedFilter == null,
                onClick = { viewModel.setFilter(null) },
                modifier = Modifier.weight(1f)
            )
            FilterChipItem(
                label = "INFO",
                isSelected = selectedFilter == LogLevel.INFO,
                onClick = { viewModel.setFilter(LogLevel.INFO) },
                modifier = Modifier.weight(1f)
            )
            FilterChipItem(
                label = "DEBUG",
                isSelected = selectedFilter == LogLevel.DEBUG,
                onClick = { viewModel.setFilter(LogLevel.DEBUG) },
                modifier = Modifier.weight(1f)
            )
            FilterChipItem(
                label = "WARN",
                isSelected = selectedFilter == LogLevel.WARNING,
                onClick = { viewModel.setFilter(LogLevel.WARNING) },
                modifier = Modifier.weight(1f)
            )
            FilterChipItem(
                label = "ERROR",
                isSelected = selectedFilter == LogLevel.ERROR,
                onClick = { viewModel.setFilter(LogLevel.ERROR) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Action Buttons (Export, Copy All, Clear)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.copyAllLogsToClipboard(context) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(HdrColors.Border))
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = HdrColors.TextMuted, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Copy All", fontFamily = SansSerifFont, fontSize = 12.sp, color = HdrColors.Text)
                }

                OutlinedButton(
                    onClick = { viewModel.exportAndShareLogs(context) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(HdrColors.Border))
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = HdrColors.TextMuted, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Export / Share", fontFamily = SansSerifFont, fontSize = 12.sp, color = HdrColors.Text)
                }
            }

            IconButton(
                onClick = { showClearConfirmDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Clear logs",
                    tint = HdrColors.Error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 6. Logs LazyColumn
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                InfoCard {
                    Text(
                        text = "No logs match the current search or filter.",
                        fontFamily = SansSerifFont,
                        fontSize = 13.sp,
                        color = HdrColors.TextMuted,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(logs, key = { it.id }) { logItem ->
                    LogCard(
                        log = logItem,
                        onCopy = { viewModel.copyLogToClipboard(context, logItem) }
                    )
                }
            }
        }
    }

    // Clear Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear Application Logs?",
                    fontFamily = SansSerifFont,
                    fontWeight = FontWeight.Bold,
                    color = HdrColors.Text
                )
            },
            text = {
                Text(
                    text = "This will erase all recorded diagnostic logs and error traces from local storage. This action cannot be undone.",
                    fontFamily = SansSerifFont,
                    fontSize = 13.sp,
                    color = HdrColors.TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLogs()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text(text = "CLEAR ALL", color = HdrColors.Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(text = "CANCEL", color = HdrColors.TextMuted)
                }
            },
            containerColor = HdrColors.SurfaceHigh,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HdrColors.Surface)
            .border(1.dp, HdrColors.Border, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                fontFamily = TechnicalMonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
            Text(
                text = label,
                fontFamily = SansSerifFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                color = HdrColors.TextFaint
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) HdrColors.Accent else HdrColors.Surface)
            .border(1.dp, if (isSelected) HdrColors.Accent else HdrColors.Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = SansSerifFont,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (isSelected) HdrColors.OnAccent else HdrColors.TextMuted
        )
    }
}

@Composable
private fun LogCard(
    log: LogEntity,
    onCopy: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val (badgeColor, badgeTextColor) = when (log.logLevel) {
        LogLevel.INFO -> Pair(HdrColors.SurfaceHigh, HdrColors.TextMuted)
        LogLevel.DEBUG -> Pair(HdrColors.SurfaceHigh, HdrColors.TextFaint)
        LogLevel.WARNING -> Pair(HdrColors.Warning.copy(alpha = 0.2f), HdrColors.Warning)
        LogLevel.ERROR -> Pair(HdrColors.Error.copy(alpha = 0.2f), HdrColors.Error)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HdrColors.Surface)
            .border(
                1.dp,
                if (log.logLevel == LogLevel.ERROR) HdrColors.Error.copy(alpha = 0.4f) else HdrColors.Border,
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Level Badge, Tag, Diagnostic ID, Timestamp, Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = log.level,
                        fontFamily = TechnicalMonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = badgeTextColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tag
                Text(
                    text = log.tag,
                    fontFamily = SansSerifFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = HdrColors.Text
                )

                if (!log.diagnosticId.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(HdrColors.AccentDim.copy(alpha = 0.3f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ID:${log.diagnosticId.take(6)}",
                            fontFamily = TechnicalMonoFont,
                            fontSize = 9.sp,
                            color = HdrColors.Accent
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Timestamp
                Text(
                    text = log.formattedTime,
                    fontFamily = TechnicalMonoFont,
                    fontSize = 11.sp,
                    color = HdrColors.TextFaint
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(onClick = onCopy, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy log",
                        tint = HdrColors.TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Message text
            Text(
                text = log.message,
                fontFamily = TechnicalMonoFont,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = if (log.logLevel == LogLevel.ERROR) HdrColors.Error else HdrColors.Text
            )

            // Exception details if available
            if (!log.exceptionMessage.isNullOrEmpty() || !log.stackTrace.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "HIDE ERROR TRACE ▲" else "VIEW ERROR TRACE ▼",
                        fontFamily = TechnicalMonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = HdrColors.Accent
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(HdrColors.SurfaceHigh)
                            .border(1.dp, HdrColors.Border, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        if (!log.exceptionMessage.isNullOrEmpty()) {
                            Text(
                                text = "Exception: ${log.exceptionMessage}",
                                fontFamily = TechnicalMonoFont,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = HdrColors.Error
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (!log.stackTrace.isNullOrEmpty()) {
                            Text(
                                text = log.stackTrace,
                                fontFamily = TechnicalMonoFont,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                color = HdrColors.TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
