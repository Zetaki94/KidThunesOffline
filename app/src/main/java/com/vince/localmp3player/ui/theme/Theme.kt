package com.vince.localmp3player.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KidColorScheme = lightColorScheme(
    primary = MeadowGreen,
    onPrimary = CardCream,
    secondary = StoryBlue,
    onSecondary = CardCream,
    tertiary = SoundRed,
    onTertiary = CardCream,
    background = HoneyBackground,
    onBackground = Ink,
    surface = CardCream,
    onSurface = Ink,
    surfaceVariant = Butter,
    onSurfaceVariant = SoftInk,
    outline = CardStroke,
)

private val KidShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val KidTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 34.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.ExtraBold,
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.ExtraBold,
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
    ),
)

@Composable
fun KidTunesTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = KidColorScheme,
        typography = KidTypography,
        shapes = KidShapes,
        content = content,
    )
}
