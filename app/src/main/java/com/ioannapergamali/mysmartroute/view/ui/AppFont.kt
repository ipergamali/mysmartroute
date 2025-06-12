package com.ioannapergamali.mysmartroute.view.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ioannapergamali.mysmartroute.R

enum class AppFont(val label: String, val fontFamily: FontFamily) {
    SansSerif("Sans Serif", FontFamily.SansSerif),
    Serif("Serif", FontFamily.Serif),
    Monospace("Monospace", FontFamily.Monospace),
    Cursive("Cursive", FontFamily.Cursive),
    OpenSans(
        "Open Sans",
        FontFamily(
            Font(R.font.opensans_regular, FontWeight.Normal),
            Font(R.font.opensans_bold, FontWeight.Bold)
        )
    )
}
