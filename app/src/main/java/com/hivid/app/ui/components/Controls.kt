package com.hivid.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.hivid.app.ui.theme.TechnicalMonoFont

@Composable
fun MonoLine(
    keyText: String,
    valueText: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified
) {
    val resolvedValueColor = if (valueColor != Color.Unspecified) valueColor else HdrColors.Text
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = keyText,
            fontFamily = TechnicalMonoFont,
            fontSize = 12.sp,
            color = HdrColors.TextMuted
        )
        Text(
            text = valueText,
            fontFamily = TechnicalMonoFont,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = resolvedValueColor
        )
    }
}

@Composable
fun HividProgressBar(
    progress: Float, // 0.0f .. 1.0f
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(HdrColors.TextFaint.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(2.dp))
                .background(HdrColors.Accent)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpansionSlider(
    value: Float, // 0..100
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val tierLabel = when {
        value <= 30f -> "Natural"
        value <= 65f -> "Balanced"
        else -> "Aggressive"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PEAK EXPANSION",
                fontFamily = TechnicalMonoFont,
                fontSize = 12.sp,
                color = HdrColors.TextMuted
            )
            Text(
                text = "${value.toInt()}% · $tierLabel",
                fontFamily = TechnicalMonoFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = HdrColors.Accent
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = HdrColors.Accent,
                activeTrackColor = HdrColors.Accent,
                inactiveTrackColor = HdrColors.Border
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(HdrColors.Accent)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = SliderDefaults.colors(
                        activeTrackColor = HdrColors.Accent,
                        inactiveTrackColor = HdrColors.Border
                    ),
                    modifier = Modifier.height(3.dp)
                )
            }
        )
    }
}

@Composable
fun GlossaryRow(
    term: String,
    explanation: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = term,
            fontFamily = SansSerifFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = HdrColors.Text
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = explanation,
            fontFamily = SansSerifFont,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = HdrColors.TextMuted
        )
    }
}
