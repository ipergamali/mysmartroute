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
    Teal("Teal", Color(0xFF00796B));

    val lightColors: ColorScheme
        get() = lightColorScheme(
            primary = seed,
            secondary = seed,
            tertiary = seed
        )

    val darkColors: ColorScheme
        get() = darkColorScheme(
            primary = seed,
            secondary = seed,
            tertiary = seed
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
