package com.ioannapergamali.mysmartroute.model.interfaces

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * Κοινή διεπαφή για ενσωματωμένα και προσαρμοσμένα θέματα.
 */
interface ThemeOption {
    val label: String
    val seed: Color
    val fontFamily: FontFamily
}
