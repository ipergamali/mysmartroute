package com.ioannapergamali.mysmartroute.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * Δομή για δυναμικά θέματα που προέρχονται από το themes.json.
 */
data class CustomTheme(
    val label: String,
    val seed: Color,
    val fontFamily: FontFamily = FontFamily.SansSerif
)
