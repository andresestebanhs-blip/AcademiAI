package com.academiai.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, color = TextWhite),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextWhite),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = TextWhite),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, color = TextWhite),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, color = TextWhite),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = TextGray),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextWhite),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, color = TextGray)
)
