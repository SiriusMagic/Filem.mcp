package com.zirakusa.filem.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Filem's colours (dark, with the soft green of the phone's Files). Screens never use fixed colours of their own. */
object FilemColors {
    val Background = Color(0xFF121212)
    val Bar = Color(0xFF1A221C)
    val Card = Color(0xFF1E1E1E)
    val Tile = Color(0xFF2A2A2A)
    val Text = Color(0xFFECECEC)
    val Muted = Color(0xFFADADAD)
    val Accent = Color(0xFF8FD8A6)
    val Folder = Color(0xFFB7CDB8)
    val Fab = Color(0xFF0D5A2E)
    val OnFab = Color(0xFFA8F0C0)
    val Chip = Color(0xFF2C3B30)
    val OnAccent = Color(0xFF0F1A14)
    val Danger = Color(0xFFF28B82)
    val Scrim = Color(0xFF242424)
    /** «Navegador»: the round-square category icons, Xiaomi Files style, with a white glyph. */
    val CatBlue = Color(0xFF3D8BFF)
    val CatPurple = Color(0xFF9B6BFF)
    val CatYellow = Color(0xFFF5B82E)
    val CatRed = Color(0xFFFF5F57)
    val CatGreen = Color(0xFF34C77B)
    val CatSky = Color(0xFF2FA8F5)
    val CatOrange = Color(0xFFFF8A34)
    val OnCat = Color(0xFFFFFFFF)
    val Track = Color(0xFF333333)
}

@Composable fun FilemTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(primary = FilemColors.Accent, onPrimary = FilemColors.OnAccent, background = FilemColors.Background,
        surface = FilemColors.Background, onSurface = FilemColors.Text, onBackground = FilemColors.Text,
        surfaceContainerHigh = FilemColors.Scrim, surfaceContainer = FilemColors.Card, surfaceVariant = FilemColors.Card,
        onSurfaceVariant = FilemColors.Muted, secondaryContainer = FilemColors.Chip, onSecondaryContainer = FilemColors.Text,
        primaryContainer = FilemColors.Fab, onPrimaryContainer = FilemColors.OnFab, error = FilemColors.Danger),
    content = content)
