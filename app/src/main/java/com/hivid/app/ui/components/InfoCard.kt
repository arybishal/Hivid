package com.hivid.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivid.app.ui.theme.HdrColors
import com.hivid.app.ui.theme.SansSerifFont

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        fontFamily = SansSerifFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.2.sp,
        color = if (HdrColors.IsDark) HdrColors.Accent else androidx.compose.ui.graphics.Color(0xFF997A00),
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    headerTrailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, HdrColors.Border, RoundedCornerShape(20.dp))
            .background(HdrColors.Surface)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (title != null || headerTrailing != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            fontFamily = SansSerifFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = HdrColors.Text,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (headerTrailing != null) {
                        Row {
                            headerTrailing()
                        }
                    }
                }
            }

            if (description != null) {
                if (title != null) Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    fontFamily = SansSerifFont,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = HdrColors.TextMuted
                )
            }

            if (title != null || description != null) {
                Spacer(modifier = Modifier.height(14.dp))
            }

            content()
        }
    }
}
