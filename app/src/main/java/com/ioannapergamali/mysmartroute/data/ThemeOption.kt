package com.ioannapergamali.mysmartroute.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * Κοινή διεπαφή για όλα τα θέματα της εφαρμογής.
 */
interface ThemeOption {
    val label: String
    val seed: Color
    val fontFamily: FontFamily
}
