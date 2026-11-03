package com.hivid.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivid.app.R
import com.hivid.app.ui.theme.HdrColors
import com.hivid.app.ui.theme.SansSerifFont

enum class NavTab(val titleRes: Int, val icon: ImageVector) {
    CONVERT(R.string.tab_convert, Icons.Default.HdrOn),
    REMUX(R.string.tab_remux, Icons.Default.Layers),
    GUIDE(R.string.tab_guide, Icons.AutoMirrored.Filled.MenuBook),
    HISTORY(R.string.tab_history, Icons.Default.History)
}

@Composable
fun BottomNavPill(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(HdrColors.Bg)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .border(1.dp, HdrColors.Border, RoundedCornerShape(29.dp))
                .background(HdrColors.SurfaceHigh)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.values().forEach { tab ->
                val isSelected = currentTab == tab
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) HdrColors.Accent else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "tab_bg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) HdrColors.OnAccent else HdrColors.TextMuted,
                    animationSpec = tween(durationMillis = 200),
                    label = "tab_content"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(23.dp))
                        .background(bgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(id = tab.titleRes),
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = tab.titleRes),
                                fontFamily = SansSerifFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
