package com.ioannapergamali.mysmartroute.view.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Typography

enum class AppTheme(val label: String, val seed: Color, val fontFamily: FontFamily) {
    Ocean("Ocean", Color(0xFF2196F3), FontFamily.SansSerif),
    Sunset("Sunset", Color(0xFFEF5350), FontFamily.Serif),
    Forest("Forest", Color(0xFF2E7D32), FontFamily.Monospace),
    Lemon("Lemon", Color(0xFFF9A825), FontFamily.Cursive),
    Rose("Rose", Color(0xFFD81B60), FontFamily.SansSerif),
    Orange("Orange", Color(0xFFFB8C00), FontFamily.Serif),
    Purple("Purple", Color(0xFF8E24AA), FontFamily.Monospace),
    Coffee("Coffee", Color(0xFF795548), FontFamily.Cursive),
    Cyan("Cyan", Color(0xFF00838F), FontFamily.SansSerif),
    Teal("Teal", Color(0xFF00796B), FontFamily.Serif),
    Indigo("Indigo", Color(0xFF3F51B5), FontFamily.Monospace),
    LightBlue("Light Blue", Color(0xFF03A9F4), FontFamily.Cursive),
    DeepPurple("Deep Purple", Color(0xFF673AB7), FontFamily.SansSerif),
    BlueGrey("Blue Grey", Color(0xFF607D8B), FontFamily.Serif),
    Lime("Lime", Color(0xFFCDDC39), FontFamily.Monospace),
    Amber("Amber", Color(0xFFFFC107), FontFamily.Cursive),
    DeepOrange("Deep Orange", Color(0xFFFF5722), FontFamily.SansSerif),
    Gray("Gray", Color(0xFF9E9E9E), FontFamily.Serif);

    private fun isColorDark(color: Color): Boolean {
        val darkness = 1 - (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
        return darkness >= 0.5
    }

    private fun onColor(seed: Color): Color = if (isColorDark(seed)) Color.White else Color.Black

    val lightColors: ColorScheme
        get() = lightColorScheme(
            primary = seed,
            secondary = seed,
            tertiary = seed,
            onPrimary = onColor(seed),
            onSecondary = onColor(seed),
            onTertiary = onColor(seed)
        )

    val darkColors: ColorScheme
        get() = darkColorScheme(
            primary = seed,
            secondary = seed,
            tertiary = seed,
            onPrimary = onColor(seed),
            onSecondary = onColor(seed),
            onTertiary = onColor(seed)
        )

    val typography: Typography
        get() = Typography()
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun typographyWithFont(font: FontFamily): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = font),
        displayMedium = base.displayMedium.copy(fontFamily = font),
        displaySmall = base.displaySmall.copy(fontFamily = font),
        headlineLarge = base.headlineLarge.copy(fontFamily = font),
        headlineMedium = base.headlineMedium.copy(fontFamily = font),
        headlineSmall = base.headlineSmall.copy(fontFamily = font),
        titleLarge = base.titleLarge.copy(fontFamily = font),
        titleMedium = base.titleMedium.copy(fontFamily = font),
        titleSmall = base.titleSmall.copy(fontFamily = font),
        bodyLarge = base.bodyLarge.copy(fontFamily = font),
        bodyMedium = base.bodyMedium.copy(fontFamily = font),
        bodySmall = base.bodySmall.copy(fontFamily = font),
        labelLarge = base.labelLarge.copy(fontFamily = font),
        labelMedium = base.labelMedium.copy(fontFamily = font),
        labelSmall = base.labelSmall.copy(fontFamily = font),
        displayLargeEmphasized = base.displayLargeEmphasized.copy(fontFamily = font),
        displayMediumEmphasized = base.displayMediumEmphasized.copy(fontFamily = font),
        displaySmallEmphasized = base.displaySmallEmphasized.copy(fontFamily = font),
        headlineLargeEmphasized = base.headlineLargeEmphasized.copy(fontFamily = font),
        headlineMediumEmphasized = base.headlineMediumEmphasized.copy(fontFamily = font),
        headlineSmallEmphasized = base.headlineSmallEmphasized.copy(fontFamily = font),
        titleLargeEmphasized = base.titleLargeEmphasized.copy(fontFamily = font),
        titleMediumEmphasized = base.titleMediumEmphasized.copy(fontFamily = font),
        titleSmallEmphasized = base.titleSmallEmphasized.copy(fontFamily = font),
        bodyLargeEmphasized = base.bodyLargeEmphasized.copy(fontFamily = font),
        bodyMediumEmphasized = base.bodyMediumEmphasized.copy(fontFamily = font),
        bodySmallEmphasized = base.bodySmallEmphasized.copy(fontFamily = font),
        labelLargeEmphasized = base.labelLargeEmphasized.copy(fontFamily = font),
        labelMediumEmphasized = base.labelMediumEmphasized.copy(fontFamily = font),
        labelSmallEmphasized = base.labelSmallEmphasized.copy(fontFamily = font),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun MysmartrouteTheme(
    theme: AppTheme,
    darkTheme: Boolean,
    font: FontFamily,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) theme.darkColors else theme.lightColors
    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyWithFont(font),
        content = content
    )
}
