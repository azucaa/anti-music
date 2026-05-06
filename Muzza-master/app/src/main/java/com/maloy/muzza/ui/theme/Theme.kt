package com.maloy.muzza.ui.theme

import android.graphics.Bitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette

@Composable
fun MuzzaTheme(
    darkTheme: Boolean = true, // Force dark theme permanen sebagai default
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    // Membangun skema warna bernuansa Spotify + YouTube Music secara konsisten
    val colorScheme = remember(pureBlack) {
        ColorScheme(
            primary = SpotifyGreen,
            onPrimary = Color.Black,
            primaryContainer = SpotifyGreen.copy(alpha = 0.2f),
            onPrimaryContainer = SpotifyGreen,
            inversePrimary = Color.Black,
            secondary = YTRed,
            onSecondary = Color.White,
            secondaryContainer = YTRed.copy(alpha = 0.2f),
            onSecondaryContainer = YTRed,
            tertiary = SpotifyGreen,
            onTertiary = Color.Black,
            tertiaryContainer = SpotifyGreen.copy(alpha = 0.2f),
            onTertiaryContainer = SpotifyGreen,
            background = if (pureBlack) Color.Black else MainBackground,
            onBackground = TextMain,
            surface = CardBackground,
            onSurface = TextMain,
            surfaceVariant = ElevatedBackground,
            onSurfaceVariant = TextSecondary,
            surfaceTint = SpotifyGreen,
            inverseSurface = TextMain,
            inverseOnSurface = CardBackground,
            error = Color(0xFFCF6679),
            onError = Color.Black,
            errorContainer = Color(0xFFB00020),
            onErrorContainer = Color.White,
            outline = Color(0xFF333333),
            outlineVariant = Color(0xFF555555),
            scrim = Color.Black,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = Shapes, // Terapkan shapes dari Shape.kt
        content = content
    )
}

// Fungsi pembantu ekstraksi warna menggunakan Jetpack Palette bawaan (sangat cepat dan stabil)
fun Bitmap.extractThemeColor(): Color {
    val palette = Palette.from(this).maximumColorCount(8).generate()
    val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.swatches.firstOrNull()
    return swatch?.rgb?.let { Color(it) } ?: SpotifyGreen
}

fun Bitmap.extractGradientColors(): List<Color> {
    val palette = Palette.from(this).maximumColorCount(16).generate()
    val swatches = palette.swatches.sortedByDescending { it.population }
    val res = mutableListOf<Color>()
    if (swatches.isNotEmpty()) {
        res.add(Color(swatches[0].rgb))
        if (swatches.size >= 2) {
            res.add(Color(swatches[1].rgb))
        } else {
            res.add(Color(swatches[0].rgb))
        }
    }
    return res.sortedByDescending { it.luminance() }
}

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
