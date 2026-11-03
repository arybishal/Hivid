package com.hivid.app.ui.screens.history

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.hivid.app.R
import com.hivid.app.data.local.HistoryEntity
import com.hivid.app.ui.components.InfoCard
import com.hivid.app.ui.components.SectionLabel
import com.hivid.app.ui.components.SpecPill
import com.hivid.app.ui.theme.HdrColors
import com.hivid.app.ui.theme.SansSerifFont
import com.hivid.app.ui.theme.TechnicalMonoFont
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val historyList by viewModel.historyList.collectAsState()
    val context = LocalContext.current
    var itemToDelete by remember { mutableStateOf<HistoryEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SectionLabel(text = stringResource(R.string.history_title))

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                InfoCard {
                    Text(
                        text = stringResource(R.string.history_empty),
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(historyList, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        onOpen = {
                            if (item.outputUriString.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(item.outputUriString), "video/mp4")
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.startActivity(Intent.createChooser(intent, "Open Video"))
                            }
                        },
                        onShare = {
                            if (item.outputUriString.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(item.outputUriString))
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Video"))
                            }
                        },
                        onDelete = {
                            itemToDelete = item
                        }
                    )
                }
            }
        }
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = stringResource(R.string.confirm_delete),
                    fontFamily = SansSerifFont,
                    fontWeight = FontWeight.Bold,
                    color = HdrColors.Text
                )
            },
            text = {
                Text(
                    text = item.title,
                    fontFamily = TechnicalMonoFont,
                    fontSize = 12.sp,
                    color = HdrColors.TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(item)
                        itemToDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.btn_delete),
                        color = HdrColors.Error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(text = "CANCEL", color = HdrColors.TextMuted)
                }
            },
            containerColor = HdrColors.SurfaceHigh,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun HistoryItemCard(
    item: HistoryEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(item.createdAt) { dateFormat.format(Date(item.createdAt)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, HdrColors.Border, RoundedCornerShape(16.dp))
            .background(HdrColors.Surface)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail preview via Coil
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HdrColors.SurfaceHigh)
                        .border(0.5.dp, HdrColors.Border, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.outputUriString)
                            .videoFrameMillis(1000)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Video Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            fontFamily = SansSerifFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = HdrColors.Text,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        SpecPill(text = item.profile)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${item.resolution} · ${item.formattedSize}",
                        fontFamily = TechnicalMonoFont,
                        fontSize = 11.sp,
                        color = HdrColors.TextMuted
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = formattedDate,
                        fontFamily = TechnicalMonoFont,
                        fontSize = 10.sp,
                        color = HdrColors.TextFaint
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpen, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open",
                        tint = HdrColors.Accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = HdrColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = HdrColors.Error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
