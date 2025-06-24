package com.ioannapergamali.mysmartroute.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.ioannapergamali.mysmartroute.model.interfaces.ThemeOption

/**
 * Δομή για δυναμικά θέματα που προέρχονται από το themes.json.
 */
data class CustomTheme(
    override val label: String,
    override val seed: Color,
    override val fontFamily: FontFamily = FontFamily.SansSerif
) : ThemeOption
