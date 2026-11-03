package com.hivid.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivid.app.R
import com.hivid.app.ui.theme.HdrColors
import com.hivid.app.ui.theme.SansSerifFont

@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HdrColors.Bg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 48dp squircle app icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, HdrColors.Border, RoundedCornerShape(12.dp))
                    .background(HdrColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_hivid_logo),
                    contentDescription = "Hivid Logo",
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App Title and Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontFamily = SansSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = HdrColors.Text
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Badge chip "MAX"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(HdrColors.Accent)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.badge_max),
                            fontFamily = SansSerifFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = HdrColors.OnAccent
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(id = R.string.app_subtitle),
                    fontFamily = SansSerifFont,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = HdrColors.TextMuted
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(HdrColors.Surface)
                    .border(1.dp, HdrColors.Border, RoundedCornerShape(20.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(id = R.string.settings_title),
                    tint = HdrColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend Line (Mandated UX principle in DESIGN.md)
        Text(
            text = stringResource(id = R.string.legend_line),
            fontFamily = SansSerifFont,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            color = HdrColors.TextFaint,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
