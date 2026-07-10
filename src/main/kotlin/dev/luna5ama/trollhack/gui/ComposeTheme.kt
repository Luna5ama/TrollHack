package dev.luna5ama.trollhack.gui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

internal object GuiPalette {
    val Accent = Color(0xFFE777A3)
    val AccentStrong = Color(0xFFF08DB5)
    val AccentMuted = Color(0x663E2430)
    val Backdrop = Color(0x8A080A0D)
    val Panel = Color(0xE6221C20)
    val PanelStrong = Color(0xF52A2227)
    val PanelSoft = Color(0xD91B181B)
    val Border = Color(0xFFB95E82)
    val BorderQuiet = Color(0x805E454F)
    val Text = Color(0xFFF5F2F3)
    val TextMuted = Color(0xFFB8ADB2)
    val TextDim = Color(0xFF81767B)
    val Good = Color(0xFF69D0A4)
    val Info = Color(0xFF69B7D0)
}

@Composable
internal fun TrollHackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = darkColors(
            primary = GuiPalette.Accent,
            primaryVariant = GuiPalette.AccentStrong,
            secondary = GuiPalette.Info,
            background = GuiPalette.PanelSoft,
            surface = GuiPalette.Panel,
            onPrimary = Color(0xFF211017),
            onSecondary = Color(0xFF071519),
            onBackground = GuiPalette.Text,
            onSurface = GuiPalette.Text
        ),
        typography = Typography(
            defaultFontFamily = FontFamily.SansSerif,
            body1 = TextStyle(fontSize = 13.sp, color = GuiPalette.Text),
            body2 = TextStyle(fontSize = 12.sp, color = GuiPalette.TextMuted),
            button = TextStyle(fontSize = 12.sp),
            caption = TextStyle(fontSize = 10.sp, color = GuiPalette.TextDim)
        )
    ) {
        CompositionLocalProvider(
            androidx.compose.material.LocalContentColor provides GuiPalette.Text,
            content = content
        )
    }
}
