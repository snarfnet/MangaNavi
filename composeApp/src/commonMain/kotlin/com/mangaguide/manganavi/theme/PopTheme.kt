package com.mangaguide.manganavi.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF111015)
val InkSoft = Color(0xFF25212B)
val Paper = Color(0xFFFFF4DC)
val PaperWarm = Color(0xFFFFE7B9)
val Panel = Color(0xFFFFFFFF)
val PanelAlt = Color(0xFFFFFAF0)
val Line = Color(0x1F111015)

val HeroRed = Color(0xFFE6362E)
val HeroRedDark = Color(0xFF951E1B)
val Gold = Color(0xFFFFC857)
val Cyan = Color(0xFF38D8FF)
val Violet = Color(0xFF7357FF)
val Emerald = Color(0xFF1FBF75)
val Flame = Color(0xFFFF7A1A)
val Rose = Color(0xFFFF4F87)

val PopPink = HeroRed
val PopOrange = Flame
val PopYellow = Gold
val PopGreen = Emerald
val PopBlue = Cyan
val PopPurple = Violet
val PopRed = HeroRed

val PopBackground = Paper
val PopSurface = Panel
val PopOnBackground = Ink
val PopOnSurface = Ink
val PopGrayLight = Color(0xFFF1E6D2)
val PopGrayMedium = Color(0xFF9A8D7B)
val PopGrayDark = Color(0xFF6F6356)
val StarYellow = Gold

private val MangaColorScheme = lightColorScheme(
    primary = HeroRed,
    onPrimary = Color.White,
    secondary = Ink,
    onSecondary = Paper,
    tertiary = Gold,
    background = PopBackground,
    onBackground = PopOnBackground,
    surface = PopSurface,
    onSurface = PopOnSurface,
    surfaceVariant = PanelAlt,
    onSurfaceVariant = PopGrayDark,
    error = Color(0xFFD42525),
    onError = Color.White
)

@Composable
fun PopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MangaColorScheme,
        content = content
    )
}

val genreColors = listOf(
    HeroRed, Flame, Emerald, Cyan, Violet, Rose,
    Color(0xFF00A2A8), Color(0xFFB041FF), Color(0xFF5D7C00),
    Color(0xFFFFB000), Color(0xFF2366FF), Color(0xFFD9235F),
    Color(0xFF6247AA), Color(0xFF148C6C)
)
