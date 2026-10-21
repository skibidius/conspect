package ru.konspekt.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import ru.konspekt.app.data.Palette

val LocalKonspektDark = staticCompositionLocalOf { false }

private val lightNeutrals = mapOf(
    "background" to Color(0xFFF8FAF3),
    "onBackground" to Color(0xFF191D18),
    "surface" to Color(0xFFF8FAF3),
    "onSurface" to Color(0xFF191D18),
    "surfaceVariant" to Color(0xFFE1E5DB),
    "onSurfaceVariant" to Color(0xFF424940),
    "outline" to Color(0xFF72796F),
    "outlineVariant" to Color(0xFFC2C9BD),
    "surfaceDim" to Color(0xFFD9DDD4),
    "surfaceBright" to Color(0xFFF8FAF3),
    "surfaceContainerLowest" to Color(0xFFFFFFFF),
    "surfaceContainerLow" to Color(0xFFF2F5ED),
    "surfaceContainer" to Color(0xFFECEFE7),
    "surfaceContainerHigh" to Color(0xFFE6E9E1),
    "surfaceContainerHighest" to Color(0xFFE0E4DB),
    "inverseSurface" to Color(0xFF2E322C),
    "inverseOnSurface" to Color(0xFFEFF2EA),
    "error" to Color(0xFFBA1A1A),
    "onError" to Color(0xFFFFFFFF),
    "errorContainer" to Color(0xFFFFDAD6),
    "onErrorContainer" to Color(0xFF410002)
)

private val darkNeutrals = mapOf(
    "background" to Color(0xFF111410),
    "onBackground" to Color(0xFFE1E4DC),
    "surface" to Color(0xFF111410),
    "onSurface" to Color(0xFFE1E4DC),
    "surfaceVariant" to Color(0xFF424940),
    "onSurfaceVariant" to Color(0xFFC1C9BC),
    "outline" to Color(0xFF8B9387),
    "outlineVariant" to Color(0xFF424940),
    "surfaceDim" to Color(0xFF111410),
    "surfaceBright" to Color(0xFF373A34),
    "surfaceContainerLowest" to Color(0xFF0C0F0B),
    "surfaceContainerLow" to Color(0xFF191D18),
    "surfaceContainer" to Color(0xFF1D211C),
    "surfaceContainerHigh" to Color(0xFF282C26),
    "surfaceContainerHighest" to Color(0xFF333731),
    "inverseSurface" to Color(0xFFE1E4DC),
    "inverseOnSurface" to Color(0xFF2E322C),
    "error" to Color(0xFFFFB4AB),
    "onError" to Color(0xFF690005),
    "errorContainer" to Color(0xFF93000A),
    "onErrorContainer" to Color(0xFFFFDAD6)
)

private data class Accent(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val inversePrimary: Color
)

private val accentsLight = mapOf(
    Palette.SAGE to Accent(
        primary = Color(0xFF3F6348),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD3EBCF),
        onPrimaryContainer = Color(0xFF0B2413),
        secondary = Color(0xFF52644F),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD6E6CF),
        onSecondaryContainer = Color(0xFF101F0E),
        tertiary = Color(0xFF7A5D35),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF4E1B9),
        onTertiaryContainer = Color(0xFF291C00),
        inversePrimary = Color(0xFFB8D8B6)
    ),
    Palette.SKY to Accent(
        primary = Color(0xFF35617F),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCFE5F6),
        onPrimaryContainer = Color(0xFF0A2638),
        secondary = Color(0xFF52626E),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD4E4F1),
        onSecondaryContainer = Color(0xFF101E28),
        tertiary = Color(0xFF725C3A),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF3E2BC),
        onTertiaryContainer = Color(0xFF271A00),
        inversePrimary = Color(0xFFA4CBE4)
    ),
    Palette.LAVENDER to Accent(
        primary = Color(0xFF61558A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE7DEFF),
        onPrimaryContainer = Color(0xFF241447),
        secondary = Color(0xFF635B70),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE8DEF2),
        onSecondaryContainer = Color(0xFF1F182A),
        tertiary = Color(0xFF7C5735),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF5E0BF),
        onTertiaryContainer = Color(0xFF291800),
        inversePrimary = Color(0xFFC9BBF3)
    ),
    Palette.CLAY to Accent(
        primary = Color(0xFF805443),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFF7DCCF),
        onPrimaryContainer = Color(0xFF2F140A),
        secondary = Color(0xFF6E5A50),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF1DED4),
        onSecondaryContainer = Color(0xFF271812),
        tertiary = Color(0xFF60613F),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE6E4BE),
        onTertiaryContainer = Color(0xFF1C1D05),
        inversePrimary = Color(0xFFE9BDA9)
    )
)

private val accentsDark = mapOf(
    Palette.SAGE to Accent(
        primary = Color(0xFFA2D2A4),
        onPrimary = Color(0xFF0D381C),
        primaryContainer = Color(0xFF2A5032),
        onPrimaryContainer = Color(0xFFD3EBCF),
        secondary = Color(0xFFB9CDB4),
        onSecondary = Color(0xFF243423),
        secondaryContainer = Color(0xFF3B4B38),
        onSecondaryContainer = Color(0xFFD6E6CF),
        tertiary = Color(0xFFE3C58F),
        onTertiary = Color(0xFF442F08),
        tertiaryContainer = Color(0xFF5E4520),
        onTertiaryContainer = Color(0xFFF4E1B9),
        inversePrimary = Color(0xFF3F6348)
    ),
    Palette.SKY to Accent(
        primary = Color(0xFFA4CBE4),
        onPrimary = Color(0xFF06334B),
        primaryContainer = Color(0xFF234A63),
        onPrimaryContainer = Color(0xFFCFE5F6),
        secondary = Color(0xFFBAC9D5),
        onSecondary = Color(0xFF24323D),
        secondaryContainer = Color(0xFF3A4853),
        onSecondaryContainer = Color(0xFFD4E4F1),
        tertiary = Color(0xFFDEC18B),
        onTertiary = Color(0xFF402D0B),
        tertiaryContainer = Color(0xFF57431F),
        onTertiaryContainer = Color(0xFFF3E2BC),
        inversePrimary = Color(0xFF35617F)
    ),
    Palette.LAVENDER to Accent(
        primary = Color(0xFFC9BBF3),
        onPrimary = Color(0xFF32245A),
        primaryContainer = Color(0xFF493C72),
        onPrimaryContainer = Color(0xFFE7DEFF),
        secondary = Color(0xFFCCC2D9),
        onSecondary = Color(0xFF342D40),
        secondaryContainer = Color(0xFF4B4357),
        onSecondaryContainer = Color(0xFFE8DEF2),
        tertiary = Color(0xFFE3C28C),
        onTertiary = Color(0xFF432C09),
        tertiaryContainer = Color(0xFF5D421D),
        onTertiaryContainer = Color(0xFFF5E0BF),
        inversePrimary = Color(0xFF61558A)
    ),
    Palette.CLAY to Accent(
        primary = Color(0xFFEFBEA9),
        onPrimary = Color(0xFF4A2619),
        primaryContainer = Color(0xFF633D2D),
        onPrimaryContainer = Color(0xFFF7DCCF),
        secondary = Color(0xFFD9C2B7),
        onSecondary = Color(0xFF3D2D25),
        secondaryContainer = Color(0xFF55433A),
        onSecondaryContainer = Color(0xFFF1DED4),
        tertiary = Color(0xFFC9C89D),
        onTertiary = Color(0xFF303116),
        tertiaryContainer = Color(0xFF474829),
        onTertiaryContainer = Color(0xFFE6E4BE),
        inversePrimary = Color(0xFF805443)
    )
)

fun konspektColorScheme(palette: Palette, dark: Boolean): ColorScheme {
    val n = if (dark) darkNeutrals else lightNeutrals
    val a = (if (dark) accentsDark else accentsLight).getValue(palette)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = a.primary,
        onPrimary = a.onPrimary,
        primaryContainer = a.primaryContainer,
        onPrimaryContainer = a.onPrimaryContainer,
        secondary = a.secondary,
        onSecondary = a.onSecondary,
        secondaryContainer = a.secondaryContainer,
        onSecondaryContainer = a.onSecondaryContainer,
        tertiary = a.tertiary,
        onTertiary = a.onTertiary,
        tertiaryContainer = a.tertiaryContainer,
        onTertiaryContainer = a.onTertiaryContainer,
        background = n.getValue("background"),
        onBackground = n.getValue("onBackground"),
        surface = n.getValue("surface"),
        onSurface = n.getValue("onSurface"),
        surfaceVariant = n.getValue("surfaceVariant"),
        onSurfaceVariant = n.getValue("onSurfaceVariant"),
        outline = n.getValue("outline"),
        outlineVariant = n.getValue("outlineVariant"),
        surfaceDim = n.getValue("surfaceDim"),
        surfaceBright = n.getValue("surfaceBright"),
        surfaceContainerLowest = n.getValue("surfaceContainerLowest"),
        surfaceContainerLow = n.getValue("surfaceContainerLow"),
        surfaceContainer = n.getValue("surfaceContainer"),
        surfaceContainerHigh = n.getValue("surfaceContainerHigh"),
        surfaceContainerHighest = n.getValue("surfaceContainerHighest"),
        inverseSurface = n.getValue("inverseSurface"),
        inverseOnSurface = n.getValue("inverseOnSurface"),
        inversePrimary = a.inversePrimary,
        error = n.getValue("error"),
        onError = n.getValue("onError"),
        errorContainer = n.getValue("errorContainer"),
        onErrorContainer = n.getValue("onErrorContainer"),
        scrim = Color.Black
    )
}
