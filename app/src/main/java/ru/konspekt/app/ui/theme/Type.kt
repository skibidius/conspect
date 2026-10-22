package ru.konspekt.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ru.konspekt.app.R

/** Official Google Sans (SIL OFL) with full Latin + Cyrillic coverage. */
val GoogleSansFamily = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_medium, FontWeight.Medium),
    Font(R.font.google_sans_medium, FontWeight.SemiBold),
    Font(R.font.google_sans_bold, FontWeight.Bold)
)

private val base = Typography()

val KonspektTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = GoogleSansFamily),
    displayMedium = base.displayMedium.copy(fontFamily = GoogleSansFamily),
    displaySmall = base.displaySmall.copy(fontFamily = GoogleSansFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = GoogleSansFamily, fontWeight = FontWeight.Medium),
    headlineMedium = base.headlineMedium.copy(fontFamily = GoogleSansFamily, fontWeight = FontWeight.Medium),
    headlineSmall = base.headlineSmall.copy(fontFamily = GoogleSansFamily, fontWeight = FontWeight.Medium),
    titleLarge = base.titleLarge.copy(fontFamily = GoogleSansFamily, fontWeight = FontWeight.Medium),
    titleMedium = base.titleMedium.copy(fontFamily = GoogleSansFamily, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontFamily = GoogleSansFamily),
    bodyLarge = base.bodyLarge.copy(fontFamily = GoogleSansFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = GoogleSansFamily),
    bodySmall = base.bodySmall.copy(fontFamily = GoogleSansFamily),
    labelLarge = base.labelLarge.copy(fontFamily = GoogleSansFamily, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = GoogleSansFamily),
    labelSmall = base.labelSmall.copy(fontFamily = GoogleSansFamily)
)
