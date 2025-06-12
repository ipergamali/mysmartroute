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
    Ainsley(
        "Ainsley",
        FontFamily(
            Font(R.font.ainsley, FontWeight.Normal),
            Font(R.font.ainsley_bold, FontWeight.Bold)
        )
    ),
    ChocolatineDuDimanche(
        "Chocolatine du Dimanche",
        FontFamily(Font(R.font.chocolatine_du_dimanche, FontWeight.Normal))
    ),
    GreekElegantLDR(
        "Greek Elegant LDR",
        FontFamily(Font(R.font.greek_elegant_ldr, FontWeight.Normal))
    ),
    PersonaAura(
        "Persona Aura",
        FontFamily(Font(R.font.persona_aura, FontWeight.Normal))
    )
}
