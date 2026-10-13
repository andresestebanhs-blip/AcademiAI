package com.academiai.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Purple,
    onPrimary = TextWhite,
    secondary = Violet,
    onSecondary = TextWhite,
    background = Navy,
    onBackground = TextWhite,
    surface = NavyLight,
    onSurface = TextWhite,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextGray,
    error = RedStatus
)

@Composable
fun AcademiAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
