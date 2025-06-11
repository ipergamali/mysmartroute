package com.ioannapergamali.mysmartroute.view.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppTheme(val label: String, val seed: Color) {
    Ocean("Ocean", Color(0xFF2196F3)),
    Sunset("Sunset", Color(0xFFEF5350)),
    Forest("Forest", Color(0xFF2E7D32)),
    Lemon("Lemon", Color(0xFFF9A825)),
    Rose("Rose", Color(0xFFD81B60)),
    Orange("Orange", Color(0xFFFB8C00)),
    Purple("Purple", Color(0xFF8E24AA)),
    Coffee("Coffee", Color(0xFF795548)),
    Cyan("Cyan", Color(0xFF00838F)),
    Teal("Teal", Color(0xFF00796B)),
    Indigo("Indigo", Color(0xFF3F51B5)),
    LightBlue("Light Blue", Color(0xFF03A9F4)),
    DeepPurple("Deep Purple", Color(0xFF673AB7)),
    BlueGrey("Blue Grey", Color(0xFF607D8B)),
    Lime("Lime", Color(0xFFCDDC39)),
    Amber("Amber", Color(0xFFFFC107)),
    DeepOrange("Deep Orange", Color(0xFFFF5722)),
    Gray("Gray", Color(0xFF9E9E9E));

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
}

@Composable
fun MysmartrouteTheme(theme: AppTheme, darkTheme: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) theme.darkColors else theme.lightColors
    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.MaterialTheme.typography,
        content = content
    )
}
