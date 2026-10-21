package ru.konspekt.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

/**
 * Stock Material 3 type scale with Roboto and no custom letter spacing.
 * Keeping the system metrics makes the app feel native on Pixel devices.
 */
val KonspektTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.Medium),
    headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.Medium),
    headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.Medium),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.Medium),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.Medium),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.Medium)
)
