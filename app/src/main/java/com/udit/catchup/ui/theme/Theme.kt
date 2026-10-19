package com.udit.catchup.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.udit.catchup.R

// A calm, earthy sage-green palette (we skip dynamic color so the app keeps its
// own personality on every phone).
private val LightColors = lightColorScheme(
    primary = Color(0xFF406836),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC0EFAF),
    onPrimaryContainer = Color(0xFF002201),
    secondary = Color(0xFF54634D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E8CD),
    onSecondaryContainer = Color(0xFF121F0E),
    tertiary = Color(0xFF386569),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCEBEF),
    onTertiaryContainer = Color(0xFF002022),
    background = Color(0xFFF8FBF1),
    onBackground = Color(0xFF191D17),
    surface = Color(0xFFF8FBF1),
    onSurface = Color(0xFF191D17),
    surfaceVariant = Color(0xFFDFE4D7),
    onSurfaceVariant = Color(0xFF43483E),
    outline = Color(0xFF73796E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5D395),
    onPrimary = Color(0xFF113809),
    primaryContainer = Color(0xFF295020),
    onPrimaryContainer = Color(0xFFC0EFAF),
    secondary = Color(0xFFBCCBB2),
    onSecondary = Color(0xFF273421),
    secondaryContainer = Color(0xFF3D4B36),
    onSecondaryContainer = Color(0xFFD7E8CD),
    tertiary = Color(0xFFA0CFD2),
    onTertiary = Color(0xFF00363A),
    tertiaryContainer = Color(0xFF1E4D51),
    onTertiaryContainer = Color(0xFFBCEBEF),
    background = Color(0xFF11140E),
    onBackground = Color(0xFFE1E4DA),
    surface = Color(0xFF11140E),
    onSurface = Color(0xFFE1E4DA),
    surfaceVariant = Color(0xFF43483E),
    onSurfaceVariant = Color(0xFFC3C8BB),
    outline = Color(0xFF8D9287),
)

private val Nunito = FontFamily(
    Font(R.font.nunito, FontWeight.Normal),
    Font(R.font.nunito, FontWeight.Medium),
    Font(R.font.nunito, FontWeight.SemiBold),
    Font(R.font.nunito, FontWeight.Bold),
    Font(R.font.nunito, FontWeight.ExtraBold),
)

private val Base = Typography()

private val OrbitTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
    displayMedium = Base.displayMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
    displaySmall = Base.displaySmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    titleSmall = Base.titleSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontFamily = Nunito),
    bodyMedium = Base.bodyMedium.copy(fontFamily = Nunito),
    bodySmall = Base.bodySmall.copy(fontFamily = Nunito),
    labelLarge = Base.labelLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    labelMedium = Base.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    labelSmall = Base.labelSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
)

private val OrbitShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun CatchUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = OrbitTypography,
        shapes = OrbitShapes,
        content = content,
    )
}
