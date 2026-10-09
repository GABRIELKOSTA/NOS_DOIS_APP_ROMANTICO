package com.nosdois.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.nosdois.app.R

// ---------- Tipografia ----------

val Nunito = FontFamily(
    Font(R.font.nunito_400, FontWeight.Normal),
    Font(R.font.nunito_600, FontWeight.SemiBold),
    Font(R.font.nunito_700, FontWeight.Bold),
    Font(R.font.nunito_800, FontWeight.ExtraBold),
)

/** Fonte cursiva romântica, para nomes e frases especiais. */
val Script = FontFamily(Font(R.font.dancing_700, FontWeight.Bold))

private fun TextStyle.n(weight: FontWeight) = copy(fontFamily = Nunito, fontWeight = weight)

private val base = Typography()
private val AppTypography = Typography(
    displayLarge = base.displayLarge.n(FontWeight.ExtraBold),
    displayMedium = base.displayMedium.n(FontWeight.ExtraBold),
    displaySmall = base.displaySmall.n(FontWeight.ExtraBold),
    headlineLarge = base.headlineLarge.n(FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.n(FontWeight.ExtraBold),
    headlineSmall = base.headlineSmall.n(FontWeight.ExtraBold),
    titleLarge = base.titleLarge.n(FontWeight.Bold),
    titleMedium = base.titleMedium.n(FontWeight.Bold),
    titleSmall = base.titleSmall.n(FontWeight.Bold),
    bodyLarge = base.bodyLarge.n(FontWeight.Normal),
    bodyMedium = base.bodyMedium.n(FontWeight.Normal),
    bodySmall = base.bodySmall.n(FontWeight.Normal),
    labelLarge = base.labelLarge.n(FontWeight.Bold),
    labelMedium = base.labelMedium.n(FontWeight.Bold),
    labelSmall = base.labelSmall.n(FontWeight.Bold),
)

// ---------- Cores extras (degradês e pastéis) ----------

/** Par de cores pastel: fundo suave + destaque. */
data class Pastel(val bg: Color, val fg: Color)

data class LoveColors(
    val love: Brush,
    val soft: Brush,
    val banner: Brush,
    val page: Brush,
    val pastels: List<Pastel>,
    val shadow: Color,
    val isDark: Boolean,
)

private val LightLove = LoveColors(
    love = Brush.linearGradient(listOf(Color(0xFFFF9EB5), Color(0xFFE94E86), Color(0xFFA8235A))),
    soft = Brush.linearGradient(listOf(Color(0xFFFFF0F5), Color(0xFFFFE0EA))),
    banner = Brush.linearGradient(listOf(Color(0xFFFFD1DE), Color(0xFFFFEBC2))),
    page = Brush.verticalGradient(listOf(Color(0xFFFFF7FA), Color(0xFFFFF0F5))),
    pastels = listOf(
        Pastel(Color(0xFFFFE4EC), Color(0xFFD63E74)),
        Pastel(Color(0xFFEFE6FF), Color(0xFF7E57C2)),
        Pastel(Color(0xFFFFEADB), Color(0xFFD9703C)),
        Pastel(Color(0xFFDDF4EA), Color(0xFF2E8B67)),
        Pastel(Color(0xFFE1EEFF), Color(0xFF3F72C9)),
        Pastel(Color(0xFFFFF3CF), Color(0xFFB8860B)),
    ),
    shadow = Color(0xFFE94E86),
    isDark = false,
)

private val DarkLove = LoveColors(
    love = Brush.linearGradient(listOf(Color(0xFFE0608F), Color(0xFFA8235A), Color(0xFF52102B))),
    soft = Brush.linearGradient(listOf(Color(0xFF2E1B24), Color(0xFF3A2130))),
    banner = Brush.linearGradient(listOf(Color(0xFF5C2440), Color(0xFF5A4220))),
    page = Brush.verticalGradient(listOf(Color(0xFF170E12), Color(0xFF1C1016))),
    pastels = listOf(
        Pastel(Color(0xFF4A2132), Color(0xFFFF9DBB)),
        Pastel(Color(0xFF342650), Color(0xFFC9A8FF)),
        Pastel(Color(0xFF4A2E1F), Color(0xFFFFB38A)),
        Pastel(Color(0xFF1F3D33), Color(0xFF7FD8B4)),
        Pastel(Color(0xFF1F2E4A), Color(0xFF9DBEFF)),
        Pastel(Color(0xFF4A3F1A), Color(0xFFFFD86B)),
    ),
    shadow = Color.Black,
    isDark = true,
)

private val LocalLove = staticCompositionLocalOf { LightLove }

/** Acesso às cores extras do tema atual: `love.love`, `love.pastels`… */
val love: LoveColors
    @Composable @ReadOnlyComposable get() = LocalLove.current

private val LightScheme = lightColorScheme(
    primary = Color(0xFFE0457B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4EC),
    onPrimaryContainer = Color(0xFF9E1F4E),
    secondary = Color(0xFFFF8FAB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD6E0),
    onSecondaryContainer = Color(0xFF5A1030),
    tertiary = Color(0xFFB57BD6),
    background = Color(0xFFFFF7FA),
    onBackground = Color(0xFF3A1F2B),
    surface = Color.White,
    onSurface = Color(0xFF3A1F2B),
    surfaceVariant = Color(0xFFF8E1E9),
    onSurfaceVariant = Color(0xFF85606F),
    outline = Color(0xFFD9A7B8),
    outlineVariant = Color(0xFFF5DCE5),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF5F8),
    surfaceContainer = Color(0xFFFFEEF3),
    surfaceContainerHigh = Color(0xFFFFE8EF),
    surfaceContainerHighest = Color(0xFFFCE1EA),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFF7AA2),
    onPrimary = Color(0xFF4A0E24),
    primaryContainer = Color(0xFF5C1A36),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = Color(0xFFFFB1C5),
    onSecondary = Color(0xFF5A1030),
    secondaryContainer = Color(0xFF6E2A45),
    onSecondaryContainer = Color(0xFFFFD9E3),
    tertiary = Color(0xFFD7B4F0),
    background = Color(0xFF170E12),
    onBackground = Color(0xFFF5E1E8),
    surface = Color(0xFF231620),
    onSurface = Color(0xFFF5E1E8),
    surfaceVariant = Color(0xFF3A2630),
    onSurfaceVariant = Color(0xFFD3B5C1),
    outline = Color(0xFF8E6E7B),
    outlineVariant = Color(0xFF3E2A35),
    surfaceContainerLowest = Color(0xFF120A0E),
    surfaceContainerLow = Color(0xFF1E1217),
    surfaceContainer = Color(0xFF26171E),
    surfaceContainerHigh = Color(0xFF301E26),
    surfaceContainerHighest = Color(0xFF3B2630),
)

/** themeMode: 0 = automático (segue o celular), 1 = claro, 2 = escuro */
@Composable
fun isDarkTheme(themeMode: Int): Boolean = when (themeMode) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun NosDoisTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLove provides if (dark) DarkLove else LightLove) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
