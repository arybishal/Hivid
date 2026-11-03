package com.hivid.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SansSerifFont = FontFamily.SansSerif
val TechnicalMonoFont = FontFamily.Monospace

val HividTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SansSerifFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = Color.Unspecified
    ),
    headlineMedium = TextStyle(
        fontFamily = SansSerifFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = Color.Unspecified
    ),
    titleLarge = TextStyle(
        fontFamily = SansSerifFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = Color.Unspecified
    ),
    titleMedium = TextStyle(
        fontFamily = SansSerifFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = Color.Unspecified
    ),
    labelSmall = TextStyle(
        fontFamily = SansSerifFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp,
        color = Color.Unspecified
    ),
    bodyMedium = TextStyle(
        fontFamily = SansSerifFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = Color.Unspecified
    ),
    labelLarge = TextStyle(
        fontFamily = SansSerifFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = Color.Unspecified
    ),
    bodySmall = TextStyle(
        fontFamily = TechnicalMonoFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = Color.Unspecified
    )
)
