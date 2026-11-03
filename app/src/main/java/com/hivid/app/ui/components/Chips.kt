package com.hivid.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivid.app.ui.theme.HdrColors
import com.hivid.app.ui.theme.SansSerifFont

@Composable
fun ToggleChip(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = if (checked) HdrColors.Accent else HdrColors.Border,
                shape = RoundedCornerShape(24.dp)
            )
            .background(if (checked) HdrColors.Accent else Color.Transparent)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontFamily = SansSerifFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (checked) HdrColors.OnAccent else HdrColors.Text
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (checked) "ON" else "OFF",
                fontFamily = SansSerifFont,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (checked) HdrColors.OnAccent else HdrColors.TextFaint
            )
        }
    }
}

@Composable
fun RadioChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glyph: String = "H"
) {
    val borderColor = when {
        !enabled -> HdrColors.Border.copy(alpha = 0.5f)
        selected -> HdrColors.Accent
        else -> HdrColors.Border
    }
    val bgColor = when {
        !enabled -> HdrColors.Surface.copy(alpha = 0.5f)
        selected -> HdrColors.SurfaceHigh
        else -> HdrColors.Surface
    }
    val textColor = when {
        !enabled -> HdrColors.TextFaint
        selected -> HdrColors.Text
        else -> HdrColors.TextMuted
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio dot indicator
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .border(
                        1.5.dp,
                        if (selected) HdrColors.Accent else HdrColors.TextFaint,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(HdrColors.Accent)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Glyph badge
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (selected) HdrColors.Accent else HdrColors.Border),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = glyph,
                    fontFamily = SansSerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (selected) HdrColors.OnAccent else HdrColors.TextMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = label,
                fontFamily = SansSerifFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = textColor
            )
        }
    }
}

@Composable
fun SpecPill(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified
) {
    val resolvedBg = if (backgroundColor != Color.Unspecified) backgroundColor else HdrColors.AccentDim.copy(alpha = 0.4f)
    val resolvedContent = if (contentColor != Color.Unspecified) contentColor else HdrColors.Accent
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(resolvedBg)
            .border(0.5.dp, resolvedContent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = SansSerifFont,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            color = resolvedContent
        )
    }
}
